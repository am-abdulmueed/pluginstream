package io.github.aedev.flow.player.dlna

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.wifi.WifiManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local HTTP proxy server that relays YouTube streams to DLNA renderers.
 *
 * YouTube's googlevideo.com URLs are IP-locked and session-bound.
 * DLNA renderers (Kodi, smart TVs, etc.) can't fetch them directly
 * because the request comes from a different device/user-agent.
 *
 * This proxy runs on the phone, accepts HTTP requests from DLNA devices
 * on the local network, and relays the content from YouTube's CDN using
 * the phone's own network session.
 *
 * Architecture:
 * - Runs a minimal HTTP/1.1 server on a random port
 * - Supports Range requests (required for seeking on DLNA renderers)
 * - Supports HEAD requests (required for some renderers to probe content)
 * - Streams data in chunks without buffering the entire video in memory
 * - Handles multiple concurrent connections (audio + video streams)
 *
 * Usage:
 *   val proxy = StreamProxyServer.getInstance()
 *   proxy.start(context)
 *   val localUrl = proxy.registerStream(youtubeStreamUrl, contentType)
 *   proxy.stop()
 */
class StreamProxyServer private constructor() {

    companion object {
        private const val TAG = "StreamProxy"
        private const val BUFFER_SIZE = 64 * 1024 // 64KB chunks
        private const val MAX_CONNECTIONS = 8
        // MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_2_TS (API 26+)
        private const val MUXER_OUTPUT_MPEG_2_TS = 8

        @Volatile
        private var instance: StreamProxyServer? = null

        fun getInstance(): StreamProxyServer {
            return instance ?: synchronized(this) {
                instance ?: StreamProxyServer().also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private var localAddress: String = "127.0.0.1"
    private var localPort: Int = 0

    /**
     * Map of path → StreamEntry.
     * Path is a short random ID like "/s/abc123"
     * StreamEntry holds the real YouTube URL and content type.
     */
    private val streams = ConcurrentHashMap<String, StreamEntry>()

    private data class StreamEntry(
        val realUrl: String,
        val contentType: String,
        val contentLength: Long = -1
    )

    private data class MuxEntry(val videoUrl: String, val audioUrl: String)

    private val muxedStreams = ConcurrentHashMap<String, MuxEntry>()

    /**
     * Starts the proxy server on a random available port.
     * Must be called before registerStream().
     */
    fun start(context: Context) {
        if (isRunning.get()) return

        localAddress = getDeviceIpAddress(context)

        scope.launch {
            try {
                serverSocket = ServerSocket(0, MAX_CONNECTIONS, InetAddress.getByName("0.0.0.0"))
                localPort = serverSocket!!.localPort
                isRunning.set(true)

                Log.i(TAG, "Proxy started at http://$localAddress:$localPort")

                while (isRunning.get()) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        scope.launch {
                            handleClient(clientSocket)
                        }
                    } catch (e: SocketException) {
                        if (isRunning.get()) {
                            Log.e(TAG, "Accept error", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Proxy server error", e)
            } finally {
                isRunning.set(false)
            }
        }
    }

    /**
     * Stops the proxy server and clears all registered streams.
     */
    fun stop() {
        isRunning.set(false)
        streams.clear()
        muxedStreams.clear()
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing server socket", e)
        }
        serverSocket = null
        Log.i(TAG, "Proxy stopped")
    }

    /**
     * Registers a YouTube stream URL and returns a local proxy URL
     * that can be sent to DLNA devices.
     *
     * @param realUrl The actual googlevideo.com stream URL
     * @param contentType MIME type (e.g., "video/mp4", "audio/mp4")
     * @return Local URL like "http://192.168.1.5:8080/s/abc123"
     */
    fun registerStream(realUrl: String, contentType: String = "video/mp4"): String {
        if (!isRunning.get()) {
            Log.w(TAG, "Proxy not running, cannot register stream")
            return realUrl 
        }

        val pathId = java.util.UUID.randomUUID().toString().take(8)
        val path = "/s/$pathId"

        val contentLength = probeContentLength(realUrl)

        streams.clear()
        streams[path] = StreamEntry(realUrl, contentType, contentLength)

        val proxyUrl = "http://$localAddress:$localPort$path"
        Log.i(TAG, "Registered stream: $proxyUrl → ${realUrl.take(80)}...")
        return proxyUrl
    }

    /**
     * Registers two adaptive streams (video-only + audio-only) for on-the-fly MPEG-TS muxing.
     * The returned URL, when fetched by a DLNA renderer, produces a single merged stream so
     * the renderer gets both video and audio.  Requires API 26+ (Android 8.0).
     *
     * @param videoUrl  Direct video-only stream URL (googlevideo.com)
     * @param audioUrl  Direct audio-only stream URL (googlevideo.com)
     * @return Local proxy URL served as video/mp2t
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun registerMuxedStream(videoUrl: String, audioUrl: String): String {
        if (!isRunning.get()) {
            Log.w(TAG, "Proxy not running, cannot register muxed stream")
            return videoUrl
        }
        val pathId = java.util.UUID.randomUUID().toString().take(8)
        val path = "/m/$pathId"
        muxedStreams.clear()
        muxedStreams[path] = MuxEntry(videoUrl, audioUrl)
        val proxyUrl = "http://$localAddress:$localPort$path"
        Log.i(TAG, "Registered muxed stream: $proxyUrl")
        return proxyUrl
    }

    /**
     * Handles an incoming HTTP request from a DLNA renderer.
     * Supports GET (with optional Range header) and HEAD.
     */
    private fun handleClient(clientSocket: Socket) {
        try {
            clientSocket.soTimeout = 30_000

            val input = clientSocket.getInputStream()
            val output = clientSocket.getOutputStream()

            val requestLine = readLine(input) ?: return
            val headers = mutableMapOf<String, String>()

            while (true) {
                val line = readLine(input) ?: break
                if (line.isEmpty()) break
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    headers[line.substring(0, colonIdx).trim().lowercase()] =
                        line.substring(colonIdx + 1).trim()
                }
            }

            Log.d(TAG, "Request: $requestLine")

            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                sendError(output, 400, "Bad Request")
                return
            }

            val method = parts[0].uppercase()
            val path = parts[1]

            // Muxed stream endpoint (/m/…) — MPEG-TS merged video+audio (API 26+)
            if (path.startsWith("/m/")) {
                val muxEntry = muxedStreams[path]
                if (muxEntry == null) {
                    Log.w(TAG, "Unknown mux path: $path")
                    sendError(output, 404, "Not Found")
                    return
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    when (method) {
                        "HEAD" -> handleMuxedHead(output)
                        "GET"  -> handleMuxedGet(output, muxEntry)
                        else   -> sendError(output, 405, "Method Not Allowed")
                    }
                } else {
                    sendError(output, 501, "Not Implemented")
                }
                return
            }

            val streamEntry = streams[path]
            if (streamEntry == null) {
                Log.w(TAG, "Unknown path: $path")
                sendError(output, 404, "Not Found")
                return
            }

            when (method) {
                "HEAD" -> handleHead(output, streamEntry)
                "GET" -> handleGet(output, streamEntry, headers["range"])
                else -> sendError(output, 405, "Method Not Allowed")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Client handler error: ${e.message}")
        } finally {
            try {
                clientSocket.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Handles HEAD requests. DLNA renderers use this to probe
     * content type and length before starting playback.
     */
    private fun handleHead(output: OutputStream, entry: StreamEntry) {
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: ${entry.contentType}\r\n")
            if (entry.contentLength > 0) {
                append("Content-Length: ${entry.contentLength}\r\n")
            }
            append("Accept-Ranges: bytes\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }
        output.write(headers.toByteArray())
        output.flush()
    }

    /**
     * Handles GET requests with optional Range support.
     * Fetches the content from YouTube and streams it to the DLNA renderer.
     */
    private fun handleGet(
        output: OutputStream,
        entry: StreamEntry,
        rangeHeader: String?
    ) {
        try {
            val requestBuilder = Request.Builder().url(entry.realUrl)

            if (rangeHeader != null) {
                requestBuilder.addHeader("Range", rangeHeader)
            }

            val response = http.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful && response.code != 206) {
                Log.e(TAG, "YouTube returned ${response.code} for ${entry.realUrl.take(80)}")
                sendError(output, response.code, "Upstream Error")
                response.close()
                return
            }

            val body = response.body ?: run {
                sendError(output, 502, "No Body")
                response.close()
                return
            }

            val responseHeaders = buildString {
                if (response.code == 206) {
                    append("HTTP/1.1 206 Partial Content\r\n")
                    response.header("Content-Range")?.let {
                        append("Content-Range: $it\r\n")
                    }
                } else {
                    append("HTTP/1.1 200 OK\r\n")
                }

                val contentType = response.header("Content-Type") ?: entry.contentType
                append("Content-Type: $contentType\r\n")

                val contentLength = response.header("Content-Length")
                if (contentLength != null) {
                    append("Content-Length: $contentLength\r\n")
                }

                append("Accept-Ranges: bytes\r\n")
                append("Access-Control-Allow-Origin: *\r\n")
                append("Connection: close\r\n")
                append("transferMode.dlna.org: Streaming\r\n")
                append("contentFeatures.dlna.org: DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000\r\n")
                append("\r\n")
            }

            output.write(responseHeaders.toByteArray())
            output.flush()

            val buffer = ByteArray(BUFFER_SIZE)
            val inputStream = body.byteStream()
            var bytesWritten = 0L

            while (true) {
                val read = inputStream.read(buffer)
                if (read == -1) break
                try {
                    output.write(buffer, 0, read)
                    bytesWritten += read
                } catch (e: SocketException) {
                    Log.d(TAG, "Client disconnected after ${bytesWritten / 1024}KB")
                    break
                }
            }

            output.flush()
            response.close()
            Log.d(TAG, "Streamed ${bytesWritten / 1024}KB to renderer")

        } catch (e: Exception) {
            Log.e(TAG, "GET handler error: ${e.message}")
            try {
                sendError(output, 502, "Proxy Error")
            } catch (_: Exception) {}
        }
    }

    /**
     * Returns headers for a muxed MPEG-TS stream (no Content-Length because it is live-generated).
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleMuxedHead(output: OutputStream) {
        val headers = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: video/mp2t\r\n" +
            "Accept-Ranges: none\r\n" +
            "transferMode.dlna.org: Streaming\r\n" +
            "contentFeatures.dlna.org: DLNA.ORG_OP=00;DLNA.ORG_CI=0;" +
                "DLNA.ORG_FLAGS=01700000000000000000000000000000\r\n" +
            "Connection: close\r\n\r\n"
        output.write(headers.toByteArray())
        output.flush()
    }

    /**
     * Streams a real-time muxed MPEG-TS response from a video-only and an audio-only source.
     *
     * Internally uses [MediaExtractor] to read samples from each remote URL and [MediaMuxer]
     * (MPEG_2_TS format) to interleave them.  A [ParcelFileDescriptor] pipe connects the muxer
     * output to this HTTP response, so the renderer receives a continuous MPEG-TS stream.
     *
     * The MPEG-TS container is streamable without a seek table, making it ideal for DLNA.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleMuxedGet(output: OutputStream, entry: MuxEntry) {
        val pipe = ParcelFileDescriptor.createPipe()
        val readFd = pipe[0]
        val writeFd = pipe[1]
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()

        try {
            val extractorHeaders = mapOf(
                "User-Agent" to
                    "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}) AppleWebKit/537.36"
            )
            videoExtractor.setDataSource(entry.videoUrl, extractorHeaders)
            audioExtractor.setDataSource(entry.audioUrl, extractorHeaders)

            val vTrack = (0 until videoExtractor.trackCount).firstOrNull {
                videoExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("video/") == true
            } ?: throw IOException("No video track in ${entry.videoUrl.take(60)}")

            val aTrack = (0 until audioExtractor.trackCount).firstOrNull {
                audioExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: throw IOException("No audio track in ${entry.audioUrl.take(60)}")

            videoExtractor.selectTrack(vTrack)
            audioExtractor.selectTrack(aTrack)

            val muxer = MediaMuxer(writeFd.fileDescriptor, MUXER_OUTPUT_MPEG_2_TS)
            val muxVTrack = muxer.addTrack(videoExtractor.getTrackFormat(vTrack))
            val muxATrack = muxer.addTrack(audioExtractor.getTrackFormat(aTrack))
            muxer.start()

            // Send HTTP headers before blocking on the pipe read
            output.write(("HTTP/1.1 200 OK\r\n" +
                "Content-Type: video/mp2t\r\n" +
                "Accept-Ranges: none\r\n" +
                "transferMode.dlna.org: Streaming\r\n" +
                "contentFeatures.dlna.org: DLNA.ORG_OP=00;DLNA.ORG_CI=0;" +
                    "DLNA.ORG_FLAGS=01700000000000000000000000000000\r\n" +
                "Connection: close\r\n\r\n").toByteArray())
            output.flush()

            // Mux thread: reads samples from both extractors → writes MPEG-TS to writeFd.
            // Owns videoExtractor, audioExtractor, muxer, and writeFd; releases all on exit.
            Thread {
                try {
                    val buf = java.nio.ByteBuffer.allocate(1024 * 1024) // 1 MB sample buffer
                    val info = MediaCodec.BufferInfo()
                    var vDone = false
                    var aDone = false
                    while (!vDone || !aDone) {
                        val vTime = if (!vDone) videoExtractor.sampleTime else Long.MAX_VALUE
                        val aTime = if (!aDone) audioExtractor.sampleTime else Long.MAX_VALUE
                        if (aTime <= vTime && !aDone) {
                            buf.clear()
                            val sz = audioExtractor.readSampleData(buf, 0)
                            if (sz < 0) {
                                aDone = true
                            } else {
                                val keyFrame = if (audioExtractor.sampleFlags and
                                        MediaExtractor.SAMPLE_FLAG_SYNC != 0)
                                    MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                                info.set(0, sz, audioExtractor.sampleTime, keyFrame)
                                muxer.writeSampleData(muxATrack, buf, info)
                                audioExtractor.advance()
                            }
                        } else if (!vDone) {
                            buf.clear()
                            val sz = videoExtractor.readSampleData(buf, 0)
                            if (sz < 0) {
                                vDone = true
                            } else {
                                val keyFrame = if (videoExtractor.sampleFlags and
                                        MediaExtractor.SAMPLE_FLAG_SYNC != 0)
                                    MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                                info.set(0, sz, videoExtractor.sampleTime, keyFrame)
                                muxer.writeSampleData(muxVTrack, buf, info)
                                videoExtractor.advance()
                            }
                        }
                    }
                    try { muxer.stop() } catch (_: Exception) {}
                    Log.i(TAG, "Mux thread finished successfully")
                } catch (e: Exception) {
                    // Broken pipe when renderer disconnects — normal shutdown path
                    Log.d(TAG, "Mux thread ended: ${e.message}")
                } finally {
                    try { muxer.release() } catch (_: Exception) {}
                    try { writeFd.close() } catch (_: Exception) {}
                    videoExtractor.release()
                    audioExtractor.release()
                }
            }.also { it.isDaemon = true; it.name = "dlna-mux"; it.start() }

            // Stream pipe output to the DLNA renderer
            try {
                ParcelFileDescriptor.AutoCloseInputStream(readFd).use { pipeIn ->
                    val buf = ByteArray(BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val n = pipeIn.read(buf)
                        if (n == -1) break
                        output.write(buf, 0, n)
                        total += n
                    }
                    output.flush()
                    Log.d(TAG, "Muxed stream: ${total / 1024}KB sent")
                }
            } catch (_: SocketException) {
                // Renderer closed connection — mux thread will see broken pipe and exit
            }

        } catch (e: Exception) {
            Log.e(TAG, "handleMuxedGet failed (${e.javaClass.simpleName}): ${e.message}" +
                " — falling back to video-only relay")
            // Release resources that the mux thread never took ownership of
            videoExtractor.release()
            audioExtractor.release()
            try { writeFd.close() } catch (_: Exception) {}
            try { readFd.close() } catch (_: Exception) {}
            handleGet(output, StreamEntry(entry.videoUrl, "video/mp4"), null)
        }
    }

    /**
     * Sends an HTTP error response to the client.
     */
    private fun sendError(output: OutputStream, code: Int, message: String) {
        val response = "HTTP/1.1 $code $message\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
        try {
            output.write(response.toByteArray())
            output.flush()
        } catch (_: Exception) {}
    }

    /**
     * Probes the content length of a URL with a HEAD request.
     * Returns -1 if unknown.
     */
    private fun probeContentLength(url: String): Long {
        return try {
            val request = Request.Builder().url(url).head().build()
            val response = http.newCall(request).execute()
            val length = response.header("Content-Length")?.toLongOrNull() ?: -1
            response.close()
            length
        } catch (e: Exception) {
            Log.d(TAG, "Content length probe failed: ${e.message}")
            -1
        }
    }

    /**
     * Gets the device's WiFi IP address for the local proxy URL.
     */
    private fun getDeviceIpAddress(context: Context): String {
        try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiInfo = wifiManager?.connectionInfo
            val ip = wifiInfo?.ipAddress ?: 0
            if (ip != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    ip and 0xff,
                    (ip shr 8) and 0xff,
                    (ip shr 16) and 0xff,
                    (ip shr 24) and 0xff
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get WiFi IP", e)
        }

        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress ?: continue
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enumerate interfaces", e)
        }

        return "127.0.0.1"
    }

    /**
     * Reads a line from an InputStream (HTTP protocol uses \r\n line endings).
     */
    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\r'.code) {
                val next = input.read()
                if (next == '\n'.code) return sb.toString()
                sb.append('\r')
                if (next != -1) sb.append(next.toChar())
            } else {
                sb.append(b.toChar())
            }
        }
    }
}
