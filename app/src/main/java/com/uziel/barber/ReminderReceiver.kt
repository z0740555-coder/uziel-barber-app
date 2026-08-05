package com.uziel.barber

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val customerName = intent.getStringExtra("customerName") ?: ""
        val timeText = intent.getStringExtra("timeText") ?: ""

        createChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("תזכורת לתור")
            .setContentText("עוד 3 דקות תור ל-$customerName בשעה $timeText")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        manager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "appointment_reminders"

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "תזכורות תורים",
                    NotificationManager.IMPORTANCE_HIGH
                )
                channel.description = "התראות לפני תורים קרובים"
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }
    }
}
