package com.kidstok.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ServiceWorkerController
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var splash: ImageView
    private var firstPageRevealed = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            ServiceWorkerController.getInstance().serviceWorkerWebSettings.apply {
                allowContentAccess = true
                allowFileAccess = true
            }
        }

        web = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            visibility = View.INVISIBLE
            alpha = 0f

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean = false

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (!firstPageRevealed) {
                        view?.postDelayed({ revealWebsite() }, 180)
                    }
                }
            }
        }

        splash = ImageView(this).apply {
            setImageResource(R.drawable.kidstok_logo)
            setBackgroundColor(Color.WHITE)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "Kids-tok"
            setPadding(dp(34), dp(34), dp(34), dp(34))
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(
                web,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            addView(
                splash,
                FrameLayout.LayoutParams(
                    dp(300),
                    dp(220),
                    Gravity.CENTER
                )
            )
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        setContentView(root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else finish()
            }
        })

        // Safety fallback: never leave the user stuck on the splash screen.
        web.postDelayed({
            if (!firstPageRevealed) revealWebsite()
        }, 8000)

        web.loadUrl(AppConfig.SITE_URL)
    }

    private fun revealWebsite() {
        if (firstPageRevealed || isFinishing || isDestroyed) return
        firstPageRevealed = true

        web.visibility = View.VISIBLE
        web.animate()
            .alpha(1f)
            .setDuration(180)
            .start()

        splash.animate()
            .alpha(0f)
            .setDuration(160)
            .withEndAction {
                splash.visibility = View.GONE
            }
            .start()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        web.stopLoading()
        web.destroy()
        super.onDestroy()
    }
}
