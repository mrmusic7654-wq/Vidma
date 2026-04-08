package com.app.vidma

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File

class DownloadService : Service() {

    private lateinit var mainActivity: MainActivity
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "download_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mainActivity = MainActivity()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY
        val quality = intent.getStringExtra("quality") ?: "best"
        val format = intent.getStringExtra("format") ?: "mp4"

        startForeground(NOTIFICATION_ID, createNotification("Starting download..."))

        Thread {
            val result = downloadVideo(url, quality, format)
            stopForeground(false)
            stopSelf()

            val resultIntent = Intent("DOWNLOAD_COMPLETE").apply {
                putExtra("success", result.first)
                putExtra("message", result.second)
            }
            sendBroadcast(resultIntent)
        }.start()

        return START_NOT_STICKY
    }

    private fun downloadVideo(url: String, quality: String, format: String): Pair<Boolean, String> {
        return try {
            val ytDlp = filesDir.resolve("yt-dlp").absolutePath
            val outputDir = getExternalFilesDir(null)?.absolutePath ?: filesDir.absolutePath
            val outputTemplate = "$outputDir/%(title)s.%(ext)s"

            val formatArg = when {
                format == "mp3" -> "-x --audio-format mp3"
                quality == "1080p" -> "-f bestvideo[height<=1080]+bestaudio/best"
                quality == "720p" -> "-f bestvideo[height<=720]+bestaudio/best"
                quality == "480p" -> "-f bestvideo[height<=480]+bestaudio/best"
                else -> "-f best"
            }

            val process = ProcessBuilder()
                .command(ytDlp, formatArg, "-o", outputTemplate, url)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            if (exitCode == 0) Pair(true, "Download saved to $outputDir")
            else Pair(false, "Download failed with code $exitCode")

        } catch (e: Exception) {
            Pair(false, e.message ?: "Unknown error")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vidma Downloader")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
