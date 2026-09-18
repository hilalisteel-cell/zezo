package com.kidstok.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class FeedSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val jsonText = getText(AppConfig.FEED_API)
            val json = JSONObject(jsonText)
            if (!json.optBoolean("ok", false)) return@withContext Result.retry()

            File(applicationContext.filesDir, AppConfig.FEED_FILE).writeText(jsonText, Charsets.UTF_8)
            val mediaDir = File(applicationContext.filesDir, AppConfig.CACHE_DIR).apply { mkdirs() }
            val videos = json.optJSONArray("videos") ?: return@withContext Result.success()

            for (i in 0 until videos.length()) {
                if (isStopped) break
                val item = videos.optJSONObject(i) ?: continue
                val id = item.optLong("id")
                val videoUrl = item.optString("video_url")
                val thumbUrl = item.optString("thumbnail_url")
                val videoExt = extensionFromUrl(videoUrl, "mp4")
                val videoFile = File(mediaDir, "video_${id}.${videoExt}")
                if (videoUrl.startsWith("https://") && !videoFile.exists()) {
                    if (applicationContext.filesDir.usableSpace < AppConfig.MIN_FREE_BYTES) break
                    download(videoUrl, videoFile)
                }
                if (thumbUrl.startsWith("https://")) {
                    val thumbExt = extensionFromUrl(thumbUrl, "jpg")
                    val thumbFile = File(mediaDir, "thumb_${id}.${thumbExt}")
                    if (!thumbFile.exists()) runCatching { download(thumbUrl, thumbFile) }
                }
            }
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    private fun getText(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.setRequestProperty("User-Agent", "KidsTok-Android/1.0")
        c.connect()
        if (c.responseCode !in 200..299) throw IllegalStateException("HTTP ${c.responseCode}")
        return c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun download(url: String, dest: File) {
        val part = File(dest.absolutePath + ".part")
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 120000
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "KidsTok-Android/1.0")
        c.connect()
        if (c.responseCode !in 200..299) throw IllegalStateException("HTTP ${c.responseCode}")
        c.inputStream.use { input ->
            FileOutputStream(part).use { output -> input.copyTo(output, 1024 * 128) }
        }
        if (dest.exists()) dest.delete()
        if (!part.renameTo(dest)) {
            part.copyTo(dest, overwrite = true)
            part.delete()
        }
    }

    private fun extensionFromUrl(url: String, fallback: String): String {
        val clean = url.substringBefore('?').substringBefore('#')
        val ext = clean.substringAfterLast('.', fallback).lowercase()
        return if (ext.matches(Regex("[a-z0-9]{2,5}"))) ext else fallback
    }
}
