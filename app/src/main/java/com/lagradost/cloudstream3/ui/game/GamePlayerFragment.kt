package com.lagradost.cloudstream3.ui.game

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.facebook.shimmer.ShimmerFrameLayout
import com.lagradost.cloudstream3.R

class GamePlayerFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var offlineScreen: LinearLayout
    private lateinit var offlineShimmer: ShimmerFrameLayout
    private var isOnline = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_game_player, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gameUrl = arguments?.getString("game_url") ?: ""
        val gameTitle = arguments?.getString("game_title") ?: "Game"

        webView = view.findViewById(R.id.webView)
        progressBar = view.findViewById(R.id.progressBar)
        offlineScreen = view.findViewById(R.id.offlineScreen)
        offlineShimmer = view.findViewById(R.id.offlineShimmer)
        val retryButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.retryButton)

        retryButton.setOnClickListener {
            isOnline = isNetworkAvailable(requireContext())
            if (isOnline) {
                hideOfflineScreen()
                loadGame(gameUrl)
            }
        }

        // Check internet connection
        isOnline = isNetworkAvailable(requireContext())
        
        if (!isOnline) {
            // Show offline screen
            showOfflineScreen()
        } else {
            setupWebView(gameUrl)
        }

        // Handle back press with callback
        val callback = object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun setupWebView(gameUrl: String) {
        // Setup WebView settings for smooth gaming
        webView.settings.apply {
            // JavaScript & DOM
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
            
            // Performance optimizations
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            
            // Hardware acceleration for smooth gaming
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                // Enable hardware acceleration
                setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL)
            }
            
            // Enable mixed content (HTTP + HTTPS)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            
            // Cache for better performance
            cacheMode = WebSettings.LOAD_DEFAULT
            
            // Text and font settings
            defaultTextEncodingName = "UTF-8"
            
            // Enable WebGL (for 3D games)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                javaScriptEnabled = true
            }
        }
        
        // Enable hardware acceleration on WebView itself
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Enable all permissions from iframe allow attributes
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                
                // Handle mailto links
                if (url.startsWith("mailto:")) {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse(url))
                        startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        return false
                    }
                }

                // If the URL is not the same as the game URL domain, open it in external browser
                // We check if it's a "http" or "https" link first
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    val gameUri = android.net.Uri.parse(gameUrl)
                    val currentUri = request.url
                    
                    // If domains don't match, it's likely an external link/ad
                    if (gameUri.host != currentUri.host) {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, currentUri)
                            startActivity(intent)
                            return true // Handled by external browser
                        } catch (e: Exception) {
                            // Fallback if no browser can handle it
                            return false
                        }
                    }
                }
                return false // Load in WebView
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                progressBar.visibility = View.GONE
            }
        }

        // WebViewChromeClient for all permissions (iframe allow attributes)
        webView.webChromeClient = object : WebChromeClient() {
            // Geolocation permission
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: android.webkit.GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }

            // Microphone, Camera, and other permissions
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
            
            // Fullscreen support (for games that go fullscreen)
            private var customView: View? = null
            private var customViewCallback: CustomViewCallback? = null
            
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                customView = view
                customViewCallback = callback
                // You can add fullscreen handling here if needed
            }
            
            override fun onHideCustomView() {
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
            }
            
            // Console messages for debugging
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    android.util.Log.d("GameWebView", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                }
                return true
            }
        }

        // Hide status bar and enable immersive mode
        enableImmersiveMode()
        
        // Enable screen rotation for games
        enableScreenRotation()
        
        // Load the game
        loadGame(gameUrl)
    }

    private fun loadGame(gameUrl: String) {
        if (gameUrl.isNotEmpty()) {
            webView.loadUrl(gameUrl)
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        // Re-enable immersive mode when returning to game
        enableImmersiveMode()
        // Re-enable rotation
        enableScreenRotation()
        // Re-check connection
        if (!isOnline && isNetworkAvailable(requireContext())) {
            // Connection restored, reload game
            isOnline = true
            hideOfflineScreen()
            val gameUrl = arguments?.getString("game_url") ?: ""
            if (gameUrl.isNotEmpty()) {
                setupWebView(gameUrl)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        webView.onPause()
        // Restore system UI when leaving game
        disableImmersiveMode()
        // Lock to portrait when not playing
        disableScreenRotation()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
        // Clean up
        disableScreenRotation()
    }
    
    /**
     * Enable immersive mode - hide status bar and navigation bar
     */
    private fun enableImmersiveMode() {
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            // Keep screen on while playing game
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    /**
     * Disable immersive mode - show status bar and navigation bar
     */
    private fun disableImmersiveMode() {
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            // Remove keep screen on flag
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    /**
     * Enable screen rotation for gaming
     */
    private fun enableScreenRotation() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    
    /**
     * Disable screen rotation (lock to portrait)
     */
    private fun disableScreenRotation() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    
    /**
     * Check if network is available
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Show offline screen
     */
    private fun showOfflineScreen() {
        webView.visibility = View.GONE
        progressBar.visibility = View.GONE
        offlineScreen.visibility = View.VISIBLE
    }
    
    /**
     * Hide offline screen
     */
    private fun hideOfflineScreen() {
        offlineScreen.visibility = View.GONE
        webView.visibility = View.VISIBLE
    }
}
