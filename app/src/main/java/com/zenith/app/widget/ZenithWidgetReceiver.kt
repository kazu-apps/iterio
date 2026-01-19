package com.zenith.app.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class ZenithWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = ZenithWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_UPDATE_WIDGET -> {
                ZenithWidgetStateHelper.updateWidget(context)
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.zenith.app.action.UPDATE_WIDGET"

        fun sendUpdateBroadcast(context: Context) {
            val intent = Intent(context, ZenithWidgetReceiver::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }
}
