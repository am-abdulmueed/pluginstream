package com.lagradost.cloudstream3.ui.protube

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.*
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentProtubeBinding
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setSystemBarsPadding
import com.lagradost.cloudstream3.ui.protube.webview.BinaryStreamManager
import com.lagradost.cloudstream3.ui.protube.webview.YTProWebView
import com.lagradost.cloudstream3.ui.protube.webview.YTProWebViewClient
import com.lagradost.cloudstream3.ui.protube.webview.YTProWebChromeClient
import com.lagradost.cloudstream3.ui.protube.webview.WebAppInterface
import com.lagradost.cloudstream3.ui.protube.receivers.MediaCommandReceiver

class ProTubeFragment : BaseFragment<FragmentProtubeBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentProtubeBinding::inflate)
) {

    @JvmField
    var portrait = false
    private var mediaReceiver: MediaCommandReceiver? = null
    private var audioManager: AudioManager? = null

    @JvmField
    var isPlaying = false
    @JvmField
    var mediaSession = false
    @JvmField
    var isPip = false

    private lateinit var web: YTProWebView
    @JvmField
    var streamManager: BinaryStreamManager? = null

    companion object {
        private var webViewState: Bundle? = null
        private var cachedWeb: YTProWebView? = null
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
        val root = binding.root as ViewGroup

        binding.protubeRetryButton.setOnClickListener {
            if (isNetworkAvailable(requireContext())) {
                hideOfflineScreen()
                web.reload()
            } else {
                Toast.makeText(context, "Still no internet connection", Toast.LENGTH_SHORT).show()
            }
        }

        if (cachedWeb == null) {
            web = binding.web as YTProWebView
            cachedWeb = web
            setupWebView()
        } else {
            root.removeView(binding.web)
            val parent = cachedWeb?.parent as? ViewGroup
            parent?.removeView(cachedWeb)
            root.addView(cachedWeb)

            web = cachedWeb!!
            web.addJavascriptInterface(WebAppInterface(this, web), "Android")
            setReceiver()
        }

        audioManager = activity?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val prefs = activity?.getSharedPreferences("YTPRO", Context.MODE_PRIVATE)
        val launches = (prefs?.getInt("launch_count", 0) ?: 0) + 1
        prefs?.edit()?.putInt("launch_count", launches)?.apply()

        if (prefs?.contains("bgplay") != true) {
            prefs?.edit()?.putBoolean("bgplay", true)?.apply()
        }

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

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        streamManager = BinaryStreamManager(web, requireContext())
        
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
            val url = arguments?.getString("url")
            if (url != null) {
                web.loadUrl(url)
            } else {
                web.loadUrl("https://m.youtube.com/")
            }
        }
        
        web.addJavascriptInterface(WebAppInterface(this, web), "Android")
        web.webViewClient = YTProWebViewClient(this, web)
        web.webChromeClient = YTProWebChromeClient(this, web)
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(web, true)
        }

        setReceiver()
    }

    private fun setReceiver() {
        mediaReceiver = MediaCommandReceiver(web)
        val filter = IntentFilter("TRACKS_TRACKS")
        if (Build.VERSION.SDK_INT >= 34) {
            activity?.registerReceiver(mediaReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            activity?.registerReceiver(mediaReceiver, filter)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        web.loadUrl(if (isInPictureInPictureMode) "javascript:PIPlayer();" else "javascript:removePIP();")
        isPip = isInPictureInPictureMode
    }

    private fun showOfflineScreen() {
        binding?.offlineScreen?.visibility = View.VISIBLE
        binding?.offlineShimmer?.startShimmer()
        binding?.web?.visibility = View.GONE
    }

    private fun hideOfflineScreen() {
        binding?.offlineScreen?.visibility = View.GONE
        binding?.offlineShimmer?.stopShimmer()
        binding?.web?.visibility = View.VISIBLE
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    override fun onResume() {
        super.onResume()
        web.evaluateJavascript("playVideo();", null)
    }

    override fun onPause() {
        super.onPause()
        web.evaluateJavascript("pauseVideo();", null)
        CookieManager.getInstance().flush()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            web.evaluateJavascript("pauseVideo();", null)
            context?.let { ctx ->
                activity?.stopService(Intent(ctx.applicationContext, ForegroundService::class.java))
            }
        } else {
            web.evaluateJavascript("playVideo();", null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        context?.let { ctx ->
            activity?.stopService(Intent(ctx.applicationContext, ForegroundService::class.java))
        }
        mediaReceiver?.let { activity?.unregisterReceiver(it) }
        streamManager?.cleanup()
    }
}
