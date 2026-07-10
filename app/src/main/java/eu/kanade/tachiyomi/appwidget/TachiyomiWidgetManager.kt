package eu.kanade.tachiyomi.appwidget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import co.touchlab.kermit.Logger

class TachiyomiWidgetManager {

    suspend fun Context.init() {
        try {
            val manager = GlanceAppWidgetManager(this)
            if (manager.getGlanceIds(UpdatesGridGlanceWidget::class.java).isNotEmpty()) {
                UpdatesGridGlanceWidget().loadData()
            }
        } catch (e: Exception) {
            // Some OEMs (notably Samsung) can throw SecurityException from Glance
            // widget APIs during startup; never let that crash the whole app.
            Logger.e(e) { "Failed to initialize app widgets" }
        }
    }
}
