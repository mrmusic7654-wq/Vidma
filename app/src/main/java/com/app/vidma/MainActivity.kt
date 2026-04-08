package com.app.vidma

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: TextInputEditText
    private lateinit var downloadButton: Button
    private lateinit var qualitySpinner: Spinner
    private lateinit var formatSpinner: Spinner
    private lateinit var statusText: TextView

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()
        checkPermissions()
        setupYtDlp()
    }

    private fun setupViews() {
        urlInput = findViewById(R.id.urlInput)
        downloadButton = findViewById(R.id.downloadButton)
        qualitySpinner = findViewById(R.id.qualitySpinner)
        formatSpinner = findViewById(R.id.formatSpinner)
        statusText = findViewById(R.id.statusText)

        downloadButton.setOnClickListener {
            startDownload()
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun setupYtDlp() {
        try {
            val ytDlpFile = filesDir.resolve("yt-dlp")
            if (!ytDlpFile.exists()) {
                copyYtDlpFromAssets()
            }
            ytDlpFile.setExecutable(true)
        } catch (e: Exception) {
            statusText.text = "Error: ${e.message}"
        }
    }

    private fun copyYtDlpFromAssets() {
        val inputStream = assets.open("yt-dlp")
        val outputFile = filesDir.resolve("yt-dlp")
        outputFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
    }

    private fun startDownload() {
        val url = urlInput.text.toString().trim()
        if (url.isEmpty()) {
            urlInput.error = "Enter a valid URL"
            return
        }

        val quality = qualitySpinner.selectedItem.toString()
        val format = formatSpinner.selectedItem.toString()

        downloadButton.isEnabled = false
        statusText.text = "Starting download..."

        val intent = android.content.Intent(this, DownloadService::class.java).apply {
            putExtra("url", url)
            putExtra("quality", quality)
            putExtra("format", format)
        }
        startService(intent)
    }

    fun onDownloadComplete(success: Boolean, message: String) {
        runOnUiThread {
            downloadButton.isEnabled = true
            statusText.text = if (success) "✓ Download complete: $message" else "✗ Error: $message"
        }
    }
}
