package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AttendanceViewModel

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    private var viewModelInstance: AttendanceViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        setContent {
            MyApplicationTheme {
                val viewModel: AttendanceViewModel = viewModel()
                viewModelInstance = viewModel
                LaunchedEffect(Unit) {
                    handleIncomingIntent(intent, viewModel)
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModelInstance?.let { handleIncomingIntent(intent, it) }
    }

    private fun handleIncomingIntent(incomingIntent: Intent?, viewModel: AttendanceViewModel) {
        if (incomingIntent == null) return
        val action = incomingIntent.action ?: return
        val uri: Uri? = incomingIntent.data ?: incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM)
        val extraText = incomingIntent.getStringExtra(Intent.EXTRA_TEXT)

        if (Intent.ACTION_VIEW == action || Intent.ACTION_SEND == action) {
            if (uri != null) {
                try {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val content = stream.bufferedReader(Charsets.UTF_8).readText().trim()
                        if (content.isNotBlank()) {
                            viewModel.importConfigFromJson(content)
                        }
                    }
                } catch (_: Exception) {}
            } else if (!extraText.isNullOrBlank() && (extraText.contains("{") || extraText.contains("["))) {
                viewModel.importConfigFromJson(extraText.trim())
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Otomatis simpan data saat meninggalkan aplikasi
        viewModelInstance?.autoSaveData()
    }

    override fun onStop() {
        super.onStop()
        // Otomatis simpan data saat aplikasi berhenti / ke background
        viewModelInstance?.autoSaveData()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notifikasi KIKAV 2/JRTR"
            val descriptionText = "Peringatan Siaga, Alarm Militer, dan Perintah Komando"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("kikav_military_alerts", name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
