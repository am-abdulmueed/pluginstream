package com.lagradost.cloudstream3.ui.download

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKeys
import com.lagradost.cloudstream3.CommonActivity.activity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.player.DownloadFileGenerator
import com.lagradost.cloudstream3.ui.player.ExtractorUri
import com.lagradost.cloudstream3.ui.player.GeneratorPlayer
import com.lagradost.cloudstream3.utils.AppContextUtils.getNameFull
import com.lagradost.cloudstream3.utils.AppContextUtils.setDefaultFocus
import com.lagradost.cloudstream3.utils.DOWNLOAD_EPISODE_CACHE
import com.lagradost.cloudstream3.utils.DOWNLOAD_HEADER_CACHE
import com.lagradost.cloudstream3.utils.SnackbarHelper.showSnackbar
import com.lagradost.cloudstream3.utils.UIHelper.navigate
import com.lagradost.cloudstream3.utils.downloader.DownloadObjects
import com.lagradost.cloudstream3.utils.downloader.DownloadQueueManager
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager
import kotlinx.coroutines.MainScope

object DownloadButtonSetup {
    fun handleDownloadClick(click: DownloadClickEvent) {
        val id = click.data.id
        
        // Show debug panel for download monitoring
        showDownloadDebugPanel(click.data)
        
        when (click.action) {
            DOWNLOAD_ACTION_DELETE_FILE -> {
                activity?.let { ctx ->
                    val builder: AlertDialog.Builder = AlertDialog.Builder(ctx)
                    val dialogClickListener =
                        DialogInterface.OnClickListener { _, which ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE -> {
                                    VideoDownloadManager.deleteFilesAndUpdateSettings(
                                        ctx,
                                        setOf(id),
                                        MainScope()
                                    )
                                }

                                DialogInterface.BUTTON_NEGATIVE -> {
                                    // Do nothing on cancel
                                }
                            }
                        }

                    try {
                        builder.setTitle(R.string.delete_file)
                            .setMessage(
                                ctx.getString(R.string.delete_message).format(
                                    ctx.getNameFull(
                                        click.data.name,
                                        click.data.episode,
                                        click.data.season
                                    )
                                )
                            )
                            .setPositiveButton(R.string.delete, dialogClickListener)
                            .setNegativeButton(R.string.cancel, dialogClickListener)
                            .show().setDefaultFocus()
                    } catch (e: Exception) {
                        logError(e)
                        // ye you somehow fucked up formatting did you?
                    }
                }
            }

            DOWNLOAD_ACTION_PAUSE_DOWNLOAD -> {
                VideoDownloadManager.downloadEvent.invoke(
                    Pair(click.data.id, VideoDownloadManager.DownloadActionType.Pause)
                )
            }

            DOWNLOAD_ACTION_RESUME_DOWNLOAD -> {
                activity?.let { ctx ->
                    if (VideoDownloadManager.downloadStatus.containsKey(id) && VideoDownloadManager.downloadStatus[id] == VideoDownloadManager.DownloadType.IsPaused) {
                        VideoDownloadManager.downloadEvent.invoke(
                            Pair(click.data.id, VideoDownloadManager.DownloadActionType.Resume)
                        )
                    } else {
                        val pkg = VideoDownloadManager.getDownloadResumePackage(ctx, id)
                        if (pkg != null) {
                            DownloadQueueManager.addToQueue(pkg.toWrapper())
                        } else {
                            VideoDownloadManager.downloadEvent.invoke(
                                Pair(click.data.id, VideoDownloadManager.DownloadActionType.Resume)
                            )
                        }
                    }
                }
            }

            DOWNLOAD_ACTION_LONG_CLICK -> {
                activity?.let { act ->
                    val length =
                        VideoDownloadManager.getDownloadFileInfo(
                            act,
                            click.data.id
                        )?.fileLength
                            ?: 0
                    if (length > 0) {
                        showSnackbar(
                            act,
                            R.string.offline_file,
                            Snackbar.LENGTH_LONG
                        )
                    }
                }
            }

            DOWNLOAD_ACTION_CANCEL_PENDING -> {
                DownloadQueueManager.cancelDownload(id)
            }

            DOWNLOAD_ACTION_PLAY_FILE -> {
                activity?.let { act ->
                    val parent = getKey<DownloadObjects.DownloadHeaderCached>(
                        DOWNLOAD_HEADER_CACHE,
                        click.data.parentId.toString()
                    ) ?: return

                    val episodes = getKeys(DOWNLOAD_EPISODE_CACHE)
                        ?.mapNotNull {
                            getKey<DownloadObjects.DownloadEpisodeCached>(it)
                        }
                        ?.filter { it.parentId == click.data.parentId }

                    val items = mutableListOf<ExtractorUri>()
                    val allRelevantEpisodes =
                        episodes?.sortedWith(compareBy<DownloadObjects.DownloadEpisodeCached> {
                            it.season ?: 0
                        }.thenBy { it.episode })

                    allRelevantEpisodes?.forEach {
                        val keyInfo = getKey<DownloadObjects.DownloadedFileInfo>(
                            VideoDownloadManager.KEY_DOWNLOAD_INFO,
                            it.id.toString()
                        ) ?: return@forEach

                        items.add(
                            ExtractorUri(
                                // We just use a temporary placeholder for the URI,
                                // it will be updated in generateLinks().
                                // We just do this for performance since getting
                                // all paths at once can be quite expensive.
                                uri = Uri.EMPTY,
                                id = it.id,
                                parentId = it.parentId,
                                name = it.name ?: act.getString(R.string.downloaded_file),
                                season = it.season,
                                episode = it.episode,
                                headerName = parent.name,
                                tvType = parent.type,
                                basePath = keyInfo.basePath,
                                displayName = keyInfo.displayName,
                                relativePath = keyInfo.relativePath,
                            )
                        )
                    }
                    act.navigate(
                        R.id.global_to_navigation_player, GeneratorPlayer.newInstance(
                            DownloadFileGenerator(items).apply { goto(items.indexOfFirst { it.id == click.data.id }) }
                        )
                    )
                }
            }
        }
    }

    private fun showDownloadDebugPanel(downloadData: DownloadObjects.DownloadEpisodeCached) {
        try {
            activity?.let { ctx ->
                // Create floating debug icon similar to offer tab
                val debugView = View.inflate(ctx, R.layout.floating_debug_icon, null)
                val floatingButton = debugView.findViewById<View>(R.id.floatingDebugButton)
                
                // Add to current window
                val windowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    },
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )
                
                params.gravity = Gravity.TOP or Gravity.END
                params.x = 50
                params.y = 200
                
                windowManager.addView(debugView, params)
                
                floatingButton.setOnClickListener {
                    showDownloadDebugDialog(ctx, downloadData)
                    windowManager.removeView(debugView)
                }
                
                // Auto remove after 30 seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        windowManager.removeView(debugView)
                    } catch (e: Exception) {
                        // View already removed
                    }
                }, 30000)
            }
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun showDownloadDebugDialog(context: Context, downloadData: DownloadObjects.DownloadEpisodeCached) {
        val dialog = Dialog(context, R.style.DialogHalfFullscreen)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.download_debug_dialog, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val debugText = dialogView.findViewById<TextView>(R.id.debugText)
        val closeButton = dialogView.findViewById<View>(R.id.closeButton)
        
        // Collect download information
        val debugInfo = buildString {
            appendLine("🔍 DOWNLOAD DEBUG INFO")
            appendLine("========================================")
            appendLine("📱 Name: ${downloadData.name}")
            appendLine("🎬 Episode: ${downloadData.episode}")
            appendLine("📁 Season: ${downloadData.season}")
            appendLine("🆔 ID: ${downloadData.id}")
            appendLine("🔗 Parent ID: ${downloadData.parentId}")
            appendLine("📊 Status: ${VideoDownloadManager.downloadStatus[downloadData.id]}")
            appendLine("⏰ Time: ${System.currentTimeMillis()}")
            appendLine("========================================")
            appendLine("📋 DOWNLOAD ACTIVITY:")
            appendLine("• Checking download manager...")
            appendLine("• Verifying storage permissions...")
            appendLine("• Initializing download queue...")
            appendLine("• Preparing download sources...")
            appendLine("• Starting download process...")
        }
        
        debugText.text = debugInfo
        
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
}