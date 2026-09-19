package com.kidstok.app

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ServiceWorkerController
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
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
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var splashContainer: LinearLayout
    private var firstPageRevealed = false
    private var showingOffline = false
    private var lastOnline = false
    private val prefs by lazy { getSharedPreferences("kidstok_app", MODE_PRIVATE) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ServiceWorkerController.getInstance().serviceWorkerWebSettings.apply {
                allowContentAccess = true
                allowFileAccess = true
            }
        }

        web = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
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

            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            settings.loadsImagesAutomatically = true
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (!firstPageRevealed && newProgress >= 72) {
                        view?.postDelayed({ revealWebsite() }, 40)
                    }
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    if (!firstPageRevealed) view?.postDelayed({ revealWebsite() }, 25)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (!showingOffline && !url.isNullOrBlank() && url.startsWith("https://kids-tok.com/")) {
                        if (url.contains("child_feed.php") || url.contains("channels.php") || url.contains("channel.php")) {
                            prefs.edit().putString("last_url", url).apply()
                        }
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true && !Network.isOnline(this@MainActivity)) {
                        showOffline()
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
            addView(logo)
            addView(progress)
            addView(title)
            addView(subtitle)
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(web, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
            addView(splashContainer, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        setContentView(root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (showingOffline) finish()
                else if (web.canGoBack()) web.goBack()
                else finish()
            }
        })

        scheduleSync()
        lastOnline = Network.isOnline(this)

        web.postDelayed({
            if (!firstPageRevealed) {
                if (!Network.isOnline(this)) showOffline() else revealWebsite()
            }
        }, 2500)

        if (lastOnline) {
            loadPreferredOnlineUrl()
            web.postDelayed({
                if (Network.isOnline(this@MainActivity)) enqueueSync()
            }, 15000)
        } else {
            showOffline()
        }
    }

    private fun loadPreferredOnlineUrl() {
        showingOffline = false
        val deep = intent?.data?.toString()?.takeIf { isKidsTokVideoLink(it) }
        val last = prefs.getString("last_url", null)
        web.loadUrl(deep ?: last ?: AppConfig.SITE_URL)
    }

    private fun showOffline() {
        if (isFinishing || isDestroyed) return
        showingOffline = true
        val html = OfflinePage.build(this)
        web.loadDataWithBaseURL(
            "file://" + filesDir.absolutePath + "/",
            html,
            "text/html",
            "UTF-8",
            null
        )
        if (!firstPageRevealed) revealWebsite()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val url = intent.data?.toString()
        if (Network.isOnline(this) && url != null && isKidsTokVideoLink(url)) {
            showingOffline = false
            web.loadUrl(url)
        } else if (!Network.isOnline(this)) {
            showOffline()
        }
    }

    private fun isKidsTokVideoLink(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            uri.scheme.equals("https", true) &&
                uri.host.equals("kids-tok.com", true) &&
                (uri.path ?: "").startsWith("/child_feed.php")
        } catch (_: Throwable) {
            false
        }
    }

    private val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val online = Network.isOnline(this@MainActivity)
            if (online == lastOnline) return
            lastOnline = online

            if (online) {
                if (showingOffline) loadPreferredOnlineUrl()
                web.postDelayed({
                    if (Network.isOnline(this@MainActivity)) enqueueSync()
                }, 10000)
            } else {
                showOffline()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(connectivityReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(connectivityReceiver, filter)
        }
    }

    override fun onStop() {
        runCatching { unregisterReceiver(connectivityReceiver) }
        super.onStop()
    }

    private fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<FeedSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "kidstok-immediate-sync",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<FeedSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "kidstok-periodic-sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic
        )
    }

    private fun revealWebsite() {
        if (firstPageRevealed || isFinishing || isDestroyed) return
        firstPageRevealed = true
        web.visibility = View.VISIBLE
        web.animate().alpha(1f).setDuration(70).start()
        splashContainer.animate()
            .alpha(0f)
            .setDuration(70)
            .withEndAction { splashContainer.visibility = View.GONE }
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
