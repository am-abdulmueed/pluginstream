package com.lagradost.cloudstream3.ui.protube

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.DownloadManager
import android.app.PictureInPictureParams
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.view.*
import android.webkit.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.abdul.protube.ForegroundService
import com.google.android.abdul.protube.GeminiWrapper
import com.google.android.abdul.protube.YTProWebview
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentProtubeBinding
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setSystemBarsPadding
import com.lagradost.cloudstream3.utils.AdsManager
import com.lagradost.cloudstream3.utils.Coroutines.main
import com.unity3d.ads.*
import com.google.android.abdul.protube.R as ProTubeR
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import javax.net.ssl.HttpsURLConnection

class ProTubeFragment : BaseFragment<FragmentProtubeBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentProtubeBinding::inflate)
) {

    private var portrait = false
    private var broadcastReceiver: BroadcastReceiver? = null
    private var audioManager: AudioManager? = null

    private var icon = ""
    private var title = ""
    private var subtitle = ""
    private var duration: Long = 0
    private var isPlaying = false
    private var mediaSession = false
    private var isPip = false

    private var webViewState: Bundle? = null

    private lateinit var web: YTProWebview

    private val unityGameID = "6072815"
    private val rewardedID = "Rewarded_Android"
    private val testMode = false

    companion object {
        private var hasShownAdInSession = false
    }

    override fun fixLayout(view: View) {
        setSystemBarsPadding()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        return root
    }

    override fun onDestroyView() {
        webViewState = Bundle()
        web.saveState(webViewState!!)
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroyView()
    }

    override fun onBindingCreated(binding: FragmentProtubeBinding) {
        web = binding.web
        audioManager = activity?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val prefs = activity?.getSharedPreferences("YTPRO", Context.MODE_PRIVATE)
        val launches = (prefs?.getInt("launch_count", 0) ?: 0) + 1
        prefs?.edit()?.putInt("launch_count", launches)?.apply()

        if (prefs?.contains("bgplay") != true) {
            prefs?.edit()?.putBoolean("bgplay", true)?.apply()
        }

        setupWebView()
        initUnityAds()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) {
                    web.goBack()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })
    }

    private fun initUnityAds() {
        context?.let { ctx ->
            UnityAds.initialize(ctx.applicationContext, unityGameID, testMode, object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    showRewardDialog()
                }

                override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                }
            })
        }
    }

    private fun showRewardDialog() {
        main {
            val dialog = Dialog(requireContext(), ProTubeR.style.CustomDialog)
            val dialogView = layoutInflater.inflate(ProTubeR.layout.dialog_reward_ad, null)
            dialog.setContentView(dialogView)
            dialog.setCancelable(false)

            val dialogIcon = dialogView.findViewById<ImageView>(ProTubeR.id.dialog_icon)
            if (Build.VERSION.SDK_INT >= 28) {
                try {
                    val source = ImageDecoder.createSource(resources, ProTubeR.drawable.ytpro)
                    val drawable = ImageDecoder.decodeDrawable(source)
                    dialogIcon.setImageDrawable(drawable)
                    if (drawable is AnimatedImageDrawable) {
                        drawable.start()
                    }
                } catch (e: IOException) {
                    dialogIcon.setImageResource(ProTubeR.drawable.ytpro)
                }
            } else {
                dialogIcon.setImageResource(ProTubeR.drawable.ytpro)
            }

            val btnWatch = dialogView.findViewById<Button>(ProTubeR.id.btn_watch_ad)
            btnWatch.setOnClickListener {
                dialog.dismiss()
                loadRewardedAd()
            }

            val window = dialog.window
            if (window != null) {
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(window.attributes)
                lp.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT
                window.attributes = lp
            }

            dialog.show()
        }
    }

    private fun loadRewardedAd() {
        val act = activity ?: return
        AdsManager.loadAndShowAdWithRetry(
            act,
            loadingText = "Please wait, watch a short ad & continue",
            onAdFinished = {
                val prefs = act.getSharedPreferences("YTPRO", Context.MODE_PRIVATE)
                if (prefs.getInt("launch_count", 0) >= 3 && !prefs.getBoolean("ig_followed", false)) {
                    prefs.edit().putBoolean("ig_followed", true).apply()
                    showInstagramFollowDialog()
                }
            }
        )
    }

    private fun showInstagramFollowDialog() {
        main {
            val dialog = Dialog(requireContext(), ProTubeR.style.CustomDialog)
            val dialogView = layoutInflater.inflate(ProTubeR.layout.dialog_follow_instagram, null)
            dialog.setContentView(dialogView)
            dialog.setCancelable(true)

            val btnFollow = dialogView.findViewById<Button>(ProTubeR.id.btn_follow_ig)
            btnFollow.setOnClickListener {
                dialog.dismiss()
                openInstagram()
            }

            dialogView.findViewById<View>(ProTubeR.id.btn_close).setOnClickListener { dialog.dismiss() }

            val window = dialog.window
            if (window != null) {
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(window.attributes)
                lp.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT
                window.attributes = lp
            }
            dialog.show()
        }
    }

    private fun openInstagram() {
        val handle = "a.b.d.u.l.m.u.e.e.d"
        val uri = Uri.parse("http://instagram.com/_u/$handle")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.instagram.android")
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/$handle")))
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        web.settings.apply {
            javaScriptEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }

        if (webViewState != null) {
            web.restoreState(webViewState!!)
        } else {
            web.loadUrl("https://m.youtube.com/")
        }
        
        web.addJavascriptInterface(WebAppInterface(requireContext()), "Android")
        web.webChromeClient = CustomWebClient()
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(web, true)

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url.toString()

                if (url.contains("youtube.com/ytpro_cdn/")) {
                    try {
                        var assetPath: String? = null
                        if (url.contains("npm/ytpro/bgplay.js")) {
                            assetPath = "bgplay.js"
                        } else if (url.contains("npm/ytpro/innertube.js")) {
                            assetPath = "innertube.js"
                        } else if (url.contains("npm/ytpro")) {
                            assetPath = "ytpro"
                        }

                        if (assetPath != null) {
                            val inputStream: InputStream = requireContext().assets.open(assetPath)
                            var mimeType = "application/javascript"
                            if (assetPath.endsWith(".js") || assetPath == "ytpro") {
                                mimeType = "application/javascript"
                            }
                            return WebResourceResponse(mimeType, "UTF-8", inputStream)
                        }

                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    var modifiedUrl: String? = null
                    if (url.contains("youtube.com/ytpro_cdn/esm")) {
                        modifiedUrl = url.replace("youtube.com/ytpro_cdn/esm", "esm.sh")
                    } else if (url.contains("youtube.com/ytpro_cdn/npm")) {
                        modifiedUrl = url.replace("youtube.com/ytpro_cdn", "cdn.jsdelivr.net")
                    }
                    try {
                        val newUrl = URL(modifiedUrl)
                        val connection = newUrl.openConnection() as HttpsURLConnection
                        connection.setRequestProperty("User-Agent", "YTPRO")
                        connection.requestMethod = "GET"
                        connection.connect()
                        return WebResourceResponse(connection.contentType, connection.contentEncoding, connection.inputStream)
                    } catch (e: Exception) {
                        return super.shouldInterceptRequest(view, request)
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                web.evaluateJavascript(
                    "if (window.trustedTypes && window.trustedTypes.createPolicy && !window.trustedTypes.defaultPolicy) {window.trustedTypes.createPolicy('default', {createHTML: (string) => string,createScriptURL: string => string, createScript: string => string, });}",
                    null
                )
                web.evaluateJavascript(
                    "(function () { var script = document.createElement('script'); script.src='https://youtube.com/ytpro_cdn/npm/ytpro'; document.body.appendChild(script);  })();",
                    null
                )
                web.evaluateJavascript(
                    "(function () { var script = document.createElement('script'); script.src='https://youtube.com/ytpro_cdn/npm/ytpro/bgplay.js'; document.body.appendChild(script);  })();",
                    null
                )
                web.evaluateJavascript(
                    "(function () { var script = document.createElement('script');script.type='module';script.src='https://youtube.com/ytpro_cdn/npm/ytpro/innertube.js'; document.body.appendChild(script);  })();",
                    null
                )

                if (url != null && !url.contains("youtube.com/watch") && !url.contains("youtube.com/shorts") && isPlaying) {
                    isPlaying = false
                    mediaSession = false
                    activity?.stopService(Intent(requireContext().applicationContext, ForegroundService::class.java))
                }

                super.onPageFinished(view, url)
            }
        }

        setReceiver()
    }

    private fun setReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.extras?.getString("actionname")
                Log.e("Action Fragment", action ?: "")

                when (action) {
                    "PLAY_ACTION" -> web.evaluateJavascript("playVideo();", null)
                    "PAUSE_ACTION" -> web.evaluateJavascript("pauseVideo();", null)
                    "NEXT_ACTION" -> web.evaluateJavascript("playNext();", null)
                    "PREV_ACTION" -> web.evaluateJavascript("playPrev();", null)
                    "SEEKTO" -> web.evaluateJavascript("seekTo('${intent.extras?.getString("pos")}');", null)
                }
            }
        }

        val filter = IntentFilter("TRACKS_TRACKS")
        if (Build.VERSION.SDK_INT >= 34) {
            activity?.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            activity?.registerReceiver(broadcastReceiver, filter)
        }
    }

    inner class CustomWebClient : WebChromeClient() {
        private var mCustomView: View? = null
        private var mCustomViewCallback: CustomViewCallback? = null
        private var mOriginalOrientation = 0
        private var mOriginalSystemUiVisibility = 0

        override fun getDefaultVideoPoster(): Bitmap? {
            return BitmapFactory.decodeResource(resources, 2130837573)
        }

        override fun onShowCustomView(paramView: View?, viewCallback: CustomViewCallback?) {
            mOriginalOrientation = if (portrait) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

            if (isPip) mOriginalOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                val params = activity?.window?.attributes
                params?.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                activity?.window?.attributes = params
            }

            if (mCustomView != null) {
                onHideCustomView()
                return
            }
            mCustomView = paramView
            mOriginalSystemUiVisibility = activity?.window?.decorView?.systemUiVisibility ?: 0
            activity?.requestedOrientation = mOriginalOrientation
            mOriginalOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            mCustomViewCallback = viewCallback
            (activity?.window?.decorView as? FrameLayout)?.addView(mCustomView, FrameLayout.LayoutParams(-1, -1))
            activity?.window?.decorView?.systemUiVisibility = 3846
        }

        override fun onHideCustomView() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                val params = activity?.window?.attributes
                params?.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                activity?.window?.attributes = params
            }

            (activity?.window?.decorView as? FrameLayout)?.removeView(mCustomView)
            mCustomView = null
            activity?.window?.decorView?.systemUiVisibility = mOriginalSystemUiVisibility
            activity?.requestedOrientation = mOriginalOrientation
            mOriginalOrientation = if (portrait) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

            mCustomViewCallback = null
            web.clearFocus()
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            if (Build.VERSION.SDK_INT > 22 && request?.origin?.toString()?.contains("youtube.com") == true) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 101)
                } else {
                    request.grant(request.resources)
                }
            }
        }
    }

    private fun downloadFile(filename: String, url: String, mtype: String) {
        if (Build.VERSION.SDK_INT > 22 && Build.VERSION.SDK_INT < Build.VERSION_CODES.R && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            showToast(ProTubeR.string.grant_storage)
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 102)
        }
        try {
            val encodedFileName = URLEncoder.encode(filename, "UTF-8").replace("+", "%20")
            val downloadManager = activity?.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            val request = DownloadManager.Request(Uri.parse(url))
            request.setTitle(filename)
                .setDescription(filename)
                .setMimeType(mtype)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, encodedFileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE or DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            downloadManager?.enqueue(request)
            showToast(ProTubeR.string.dl_started)
        } catch (e: Exception) {
            showToast(e.toString())
        }
    }

    inner class WebAppInterface(val mContext: Context) {
        @JavascriptInterface
        fun showToast(txt: String) {
            Toast.makeText(requireContext().applicationContext, txt, Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun gohome(x: String) {
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startMain)
        }

        @JavascriptInterface
        fun downvid(name: String, url: String, m: String) {
            downloadFile(name, url, m)
        }

        @JavascriptInterface
        fun fullScreen(value: Boolean) {
            portrait = value
        }

        @JavascriptInterface
        fun oplink(url: String) {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(i)
        }

        @JavascriptInterface
        fun getInfo(): String {
            val manager = requireContext().applicationContext.packageManager
            return try {
                val info = manager.getPackageInfo(requireContext().applicationContext.packageName, 0)
                info.versionName ?: "1.0"
            } catch (e: PackageManager.NameNotFoundException) {
                "1.0"
            }
        }

        @JavascriptInterface
        fun setBgPlay(bgplay: Boolean) {
            val prefs = requireContext().getSharedPreferences("YTPRO", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("bgplay", bgplay).apply()
        }

        @JavascriptInterface
        fun bgStart(iconn: String, titlen: String, subtitlen: String, dura: Long) {
            icon = iconn
            title = titlen
            subtitle = subtitlen
            duration = dura
            isPlaying = true
            mediaSession = true

            val intent = Intent(requireContext().applicationContext, ForegroundService::class.java).apply {
                putExtra("icon", icon)
                putExtra("title", title)
                putExtra("subtitle", subtitle)
                putExtra("duration", duration)
                putExtra("currentPosition", 0L)
                putExtra("action", "play")
            }
            activity?.startService(intent)
        }

        @JavascriptInterface
        fun bgUpdate(iconn: String, titlen: String, subtitlen: String, dura: Long) {
            icon = iconn
            title = titlen
            subtitle = subtitlen
            duration = dura
            isPlaying = true

            requireContext().applicationContext.sendBroadcast(Intent("UPDATE_NOTIFICATION").apply {
                putExtra("icon", icon)
                putExtra("title", title)
                putExtra("subtitle", subtitle)
                putExtra("duration", duration)
                putExtra("currentPosition", 0L)
                putExtra("action", "pause")
            })
        }

        @JavascriptInterface
        fun bgStop() {
            isPlaying = false
            mediaSession = false
            activity?.stopService(Intent(requireContext().applicationContext, ForegroundService::class.java))
        }

        @JavascriptInterface
        fun bgPause(ct: Long) {
            isPlaying = false
            requireContext().applicationContext.sendBroadcast(Intent("UPDATE_NOTIFICATION").apply {
                putExtra("icon", icon)
                putExtra("title", title)
                putExtra("subtitle", subtitle)
                putExtra("duration", duration)
                putExtra("currentPosition", ct)
                putExtra("action", "pause")
            })
        }

        @JavascriptInterface
        fun bgPlay(ct: Long) {
            isPlaying = true
            requireContext().applicationContext.sendBroadcast(Intent("UPDATE_NOTIFICATION").apply {
                putExtra("icon", icon)
                putExtra("title", title)
                putExtra("subtitle", subtitle)
                putExtra("duration", duration)
                putExtra("currentPosition", ct)
                putExtra("action", "play")
            })
        }

        @JavascriptInterface
        fun bgBuffer(ct: Long) {
            isPlaying = true
            requireContext().applicationContext.sendBroadcast(Intent("UPDATE_NOTIFICATION").apply {
                putExtra("icon", icon)
                putExtra("title", title)
                putExtra("subtitle", subtitle)
                putExtra("duration", duration)
                putExtra("currentPosition", ct)
                putExtra("action", "buffer")
            })
        }

        @JavascriptInterface
        fun getSNlM0e(cookies: String) {
            Thread {
                val response = GeminiWrapper.getSNlM0e(cookies)
                activity?.runOnUiThread { web.evaluateJavascript("callbackSNlM0e.resolve(`$response`)", null) }
            }.start()
        }

        @JavascriptInterface
        fun GeminiClient(url: String, headers: String, body: String) {
            Thread {
                val response = GeminiWrapper.getStream(url, headers, body)
                activity?.runOnUiThread { web.evaluateJavascript("callbackGeminiClient.resolve($response)", null) }
            }.start()
        }

        @JavascriptInterface
        fun getAllCookies(url: String): String? {
            return CookieManager.getInstance().getCookie(url)
        }

        @JavascriptInterface
        fun getVolume(): Float {
            val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
            return currentVolume.toFloat() / maxVolume
        }

        @JavascriptInterface
        fun setVolume(volume: Float) {
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
            val targetVolume = (max * volume).toInt()
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        }

        @JavascriptInterface
        fun getBrightness(): Float {
            return try {
                val sysBrightness = Settings.System.getInt(requireContext().contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                (sysBrightness / 255f) * 100f
            } catch (e: Exception) {
                50f
            }
        }

        @JavascriptInterface
        fun setBrightness(brightnessValue: Float) {
            activity?.runOnUiThread {
                val brightness = brightnessValue.coerceIn(0f, 1f)
                val layout = activity?.window?.attributes
                layout?.screenBrightness = brightness
                activity?.window?.attributes = layout
            }
        }

        @JavascriptInterface
        fun pipvid(x: String) {
            if (Build.VERSION.SDK_INT >= 26) {
                try {
                    val aspectRatio = if (x == "portrait") Rational(9, 16) else Rational(16, 9)
                    val params = PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build()
                    activity?.enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    showToast(ProTubeR.string.no_pip)
                }
            } else {
                showToast(ProTubeR.string.no_pip)
            }
        }

        @JavascriptInterface
        fun share(text: String) {
            val i = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(i, "Share via"))
        }
    }

    override fun onResume() {
        super.onResume()
        web.evaluateJavascript("playVideo();", null)
    }

    override fun onPause() {
        super.onPause()
        web.evaluateJavascript("pauseVideo();", null)
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.stopService(Intent(requireContext().applicationContext, ForegroundService::class.java))
        broadcastReceiver?.let { activity?.unregisterReceiver(it) }
    }
}
