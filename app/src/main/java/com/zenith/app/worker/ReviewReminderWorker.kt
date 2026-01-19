package com.zenith.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.zenith.app.R
import com.zenith.app.domain.repository.ReviewTaskRepository
import com.zenith.app.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@HiltWorker
class ReviewReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val reviewTaskRepository: ReviewTaskRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "review_reminder_channel"
        const val NOTIFICATION_ID = 100
        const val WORK_NAME = "review_reminder_work"

        fun scheduleDaily(context: Context) {
            // Schedule to run at 9:00 AM every day
            val now = LocalTime.now()
            val targetTime = LocalTime.of(9, 0)

            val initialDelay = if (now.isBefore(targetTime)) {
                Duration.between(now, targetTime).toMinutes()
            } else {
                // If it's already past 9 AM, schedule for tomorrow
                Duration.between(now, targetTime).plusHours(24).toMinutes()
            }

            val workRequest = PeriodicWorkRequestBuilder<ReviewReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelay, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }

        fun cancelSchedule(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        createNotificationChannel()

        val today = LocalDate.now()
        val pendingCount = reviewTaskRepository.getPendingTaskCountForDate(today)

        if (pendingCount > 0) {
            showNotification(pendingCount)
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "復習リマインダー",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "今日の復習タスクを通知します"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun showNotification(taskCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("SHOW_REVIEWS", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("復習の時間です！")
            .setContentText("今日は${taskCount}件の復習タスクがあります")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
}
