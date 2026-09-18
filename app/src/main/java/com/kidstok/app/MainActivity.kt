package com.kidstok.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var splashContainer: LinearLayout
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
                        view?.postDelayed({ revealWebsite() }, 220)
                    }
                }
            }
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.kidstok_logo)
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(dp(240), ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val progress = ProgressBar(this).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { lp ->
                lp.topMargin = dp(24)
                lp.gravity = Gravity.CENTER_HORIZONTAL
            }
        }

        val title = TextView(this).apply {
            text = "برجاء الانتظار"
            setTextColor(Color.parseColor("#333333"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { lp ->
                lp.topMargin = dp(16)
                lp.gravity = Gravity.CENTER_HORIZONTAL
            }
        }

        val subtitle = TextView(this).apply {
            text = "جاري تحميل التطبيق..."
            setTextColor(Color.parseColor("#777777"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { lp ->
                lp.topMargin = dp(8)
                lp.gravity = Gravity.CENTER_HORIZONTAL
            }
        }

        splashContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            alpha = 1f
            addView(logo)
            addView(progress)
            addView(title)
            addView(subtitle)
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
                splashContainer,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
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

        splashContainer.animate()
            .alpha(0f)
            .setDuration(180)
            .withEndAction {
                splashContainer.visibility = View.GONE
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
