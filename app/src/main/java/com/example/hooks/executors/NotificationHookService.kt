package com.example.hooks.executors

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import java.util.concurrent.atomic.AtomicInteger

class NotificationHookService(private val context: Context) {

  private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
  private val notificationIdCounter = AtomicInteger(1000)

  companion object {
    const val CHANNEL_ID = "claude_code_automations_channel"
    const val CHANNEL_NAME = "Claude Code Automation Alerts"
  }

  init {
    createChannelIfNeeded()
  }

  private fun createChannelIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Notifications for Claude Code automated hooks, builds, and agent completions"
      }
      notificationManager.createNotificationChannel(channel)
    }
  }

  fun sendAutomationNotification(title: String, message: String): Boolean {
    if (notificationManager == null) return false
    return try {
      val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.cat_logo)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

      notificationManager.notify(notificationIdCounter.getAndIncrement(), notification)
      true
    } catch (e: Exception) {
      false
    }
  }
}
