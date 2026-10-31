package com.frametune.extension.vidy.player

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.frametune.extension.vidy.R

/**
 * Fullscreen, hardware-accelerated Android TV Activity hosting the Vidy Embed Player.
 * Preserves Vidy's inbuilt multi-server switcher, audio tracks, and subtitle controls
 * with D-pad remote navigation.
 */
class VidyPlayerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VidyPlayerActivity"
        private const val USER_AGENT_TV = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        fun buildVidyUrl(type: String, tmdbId: Int, season: Int? = null, episode: Int? = null): String {
            return if (type.equals("tv", ignoreCase = true) || type.equals("series", ignoreCase = true)) {
                val s = season ?: 1
                val e = episode ?: 1
                "https://vidy.st/tv/$tmdbId/$s/$e?color=E50914&autoplay=true&nextEpisode=true&episodeSelector=true&autoplayNextEpisode=true"
            } else {
                "https://vidy.st/movie/$tmdbId?color=E50914&autoplay=true"
            }
        }
    }

    private lateinit var webView: WebView
    private lateinit var customViewContainer: FrameLayout
    private lateinit var pbLoading: ProgressBar
    private lateinit var layoutDpadHint: LinearLayout

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on and hide status/navigation bars for full-screen cinema mode
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        setContentView(R.layout.activity_vidy_player)

        webView = findViewById(R.id.webViewPlayer)
        customViewContainer = findViewById(R.id.flCustomViewContainer)
        pbLoading = findViewById(R.id.pbLoading)
        layoutDpadHint = findViewById(R.id.layoutDpadHint)

        // Fade out D-pad instruction banner after 4.5 seconds
        mainHandler.postDelayed({
            layoutDpadHint.animate().alpha(0f).setDuration(400).withEndAction {
                layoutDpadHint.visibility = View.GONE
            }.start()
        }, 4500)

        setupWebView()
        loadVidyUrlFromIntent()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.setBackgroundColor(Color.BLACK)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.userAgentString = USER_AGENT_TV

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                customView = view
                customViewCallback = callback
                customViewContainer.addView(view)
                customViewContainer.visibility = View.VISIBLE
                webView.visibility = View.GONE
            }

            override fun onHideCustomView() {
                customView?.let { customViewContainer.removeView(it) }
                customView = null
                customViewContainer.visibility = View.GONE
                webView.visibility = View.VISIBLE
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d(TAG, "[Vidy JS] ${consoleMessage?.message()}")
                return true
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress >= 85) {
                    pbLoading.visibility = View.GONE
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                pbLoading.visibility = View.GONE
                injectTvBridgeScript()
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                // Allows cross-origin video assets and requests from Vidy to load cleanly
                return super.shouldInterceptRequest(view, request)
            }
        }
    }

    private fun injectTvBridgeScript() {
        try {
            val script = assets.open("tv_dpad_bridge.js").bufferedReader().use { it.readText() }
            webView.evaluateJavascript(script, null)
            Log.d(TAG, "Injected tv_dpad_bridge.js into Vidy player")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load tv_dpad_bridge.js: ${e.message}")
        }
    }

    private fun loadVidyUrlFromIntent() {
        val uri = intent.data
        if (uri == null) {
            Toast.makeText(this, "No media playback URI specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val type = uri.getQueryParameter("type") ?: "movie"
        val tmdbParam = uri.getQueryParameter("tmdb")
        val tmdbId = tmdbParam?.toIntOrNull()

        if (tmdbId == null || tmdbId <= 0) {
            Toast.makeText(this, "Invalid TMDB ID: $tmdbParam", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val season = uri.getQueryParameter("season")?.toIntOrNull()
        val episode = uri.getQueryParameter("episode")?.toIntOrNull()

        val vidyUrl = buildVidyUrl(type, tmdbId, season, episode)
        Log.i(TAG, "Launching Vidy player: $vidyUrl")
        webView.loadUrl(vidyUrl)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }

        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                // If a modal or menu item is focused, click it; else toggle play/pause
                webView.evaluateJavascript("window.TvBridge ? window.TvBridge.clickFocused() : false") { result ->
                    if (result == "false" || result == "null") {
                        webView.evaluateJavascript("window.TvBridge && window.TvBridge.togglePlayPause()", null)
                    }
                }
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                webView.evaluateJavascript("window.TvBridge && window.TvBridge.togglePlayPause()", null)
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                webView.evaluateJavascript("window.TvBridge && window.TvBridge.seek(-10)", null)
                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                webView.evaluateJavascript("window.TvBridge && window.TvBridge.seek(10)", null)
                return true
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                webView.evaluateJavascript("window.TvBridge && window.TvBridge.moveFocus('prev')", null)
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                webView.evaluateJavascript("window.TvBridge && window.TvBridge.moveFocus('next')", null)
                return true
            }

            KeyEvent.KEYCODE_BACK -> {
                if (customView != null) {
                    (webView.webChromeClient as? WebChromeClient)?.onHideCustomView()
                    return true
                }
                finish()
                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        webView.loadUrl("about:blank")
        webView.stopLoading()
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }
}
