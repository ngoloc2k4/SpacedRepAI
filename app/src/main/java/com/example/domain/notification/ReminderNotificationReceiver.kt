package com.example.domain.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.FlashcardApplication
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? FlashcardApplication
        val repository = app?.repository

        CoroutineScope(Dispatchers.IO).launch {
            val dueCount = try {
                val now = System.currentTimeMillis()
                val allCards = repository?.getAllCardsSnapshot() ?: emptyList()
                val queue = repository?.getScheduler()?.buildDailyQueue(allCards, currentTime = now)
                queue?.totalCount ?: 0
            } catch (e: Exception) {
                0
            }

            showNotification(context, dueCount)
        }
    }

    private fun showNotification(context: Context, dueCount: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notification_title)
        val content = if (dueCount > 0) {
            context.getString(R.string.notification_message_cards, dueCount)
        } else {
            context.getString(R.string.notification_message_empty)
        }

        val notification = NotificationCompat.Builder(context, ReminderManager.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ReminderManager.NOTIFICATION_ID, notification)
    }
}
