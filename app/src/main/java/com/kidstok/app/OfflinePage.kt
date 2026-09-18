package com.kidstok.app

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File

object OfflinePage {
    fun build(context: Context): String {
        val feed = File(context.filesDir, AppConfig.FEED_FILE)
        val mediaDir = File(context.filesDir, AppConfig.CACHE_DIR)
        if (!feed.exists()) return emptyState()

        return try {
            val root = JSONObject(feed.readText(Charsets.UTF_8))
            val videos = root.optJSONArray("videos")
            val cards = StringBuilder()
            if (videos != null) {
                for (i in 0 until videos.length()) {
                    val v = videos.optJSONObject(i) ?: continue
                    val id = v.optLong("id")
                    val title = esc(v.optString("title"))
                    val creator = esc(v.optString("creator_name"))
                    val desc = esc(v.optString("description"))
                    val videoFile = findLocal(mediaDir, "video_${id}.")
                    if (videoFile != null) {
                        val uri = Uri.fromFile(videoFile).toString()
                        cards.append("""
                          <article class="card">
                            <video controls playsinline preload="metadata" src="$uri"></video>
                            <div class="meta"><h2>$title</h2><b>@$creator</b><p>$desc</p></div>
                          </article>
                        """.trimIndent())
                    }
                }
            }
            if (cards.isEmpty()) return emptyState("تم حفظ بيانات الموقع، لكن لم يكتمل تنزيل فيديو Offline بعد.")
            template(cards.toString())
        } catch (_: Throwable) {
            emptyState()
        }
    }

    private fun findLocal(dir: File, prefix: String): File? =
        dir.listFiles()?.firstOrNull { it.name.startsWith(prefix) && it.isFile }

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;")

    private fun template(cards: String) = """
      <!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8">
      <meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
      <style>
      *{box-sizing:border-box}body{margin:0;background:#0b0d13;color:white;font-family:Arial,Tahoma,sans-serif}
      header{position:sticky;top:0;z-index:5;padding:14px 18px;background:rgba(11,13,19,.96);border-bottom:1px solid #252938;display:flex;justify-content:space-between;align-items:center}
      .brand{font-weight:900;font-size:21px}.badge{background:#ffc31a;color:#3b2b00;padding:6px 10px;border-radius:999px;font-size:12px;font-weight:900}
      main{max-width:1200px;margin:auto;padding:18px;display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:18px}
      .card{background:#151823;border:1px solid #262b3a;border-radius:22px;overflow:hidden;box-shadow:0 12px 35px #0003}.card video{width:100%;aspect-ratio:16/9;background:#000;display:block}.meta{padding:14px}.meta h2{font-size:18px;margin:0 0 8px}.meta b{color:#70c7ff}.meta p{color:#b7bece;line-height:1.6}
      @media(max-width:650px){main{display:block;padding:0}.card{border-radius:0;margin:0 0 12px}.card video{aspect-ratio:9/16;object-fit:contain}.meta{padding:14px 16px}}
      </style></head><body><header><div class="brand">▶ Kids-tok</div><div class="badge">وضع بدون إنترنت</div></header><main>$cards</main></body></html>
    """.trimIndent()

    private fun emptyState(message: String = "لا توجد نسخة Offline محفوظة بعد. اتصل بالإنترنت مرة واحدة ليتم تنزيل المحتوى.") = """
      <!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
      <style>body{margin:0;background:#10131c;color:#fff;font-family:Arial,Tahoma,sans-serif;min-height:100vh;display:grid;place-items:center;text-align:center}.box{max-width:560px;padding:30px}.logo{font-size:46px}.box h1{font-size:28px}.box p{color:#bbc2d3;line-height:1.8}</style></head><body><div class="box"><div class="logo">▶</div><h1>Kids-tok</h1><p>$message</p></div></body></html>
    """.trimIndent()
}
