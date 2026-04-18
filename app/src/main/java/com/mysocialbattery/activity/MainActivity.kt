package com.mysocialbattery.activity

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mysocialbattery.R
import com.mysocialbattery.widget.SocialBatteryGaugeView
import com.mysocialbattery.widget.SocialBatteryWidgetProvider

class MainActivity : AppCompatActivity() {

    private lateinit var moodLabels: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        moodLabels = resources.getStringArray(R.array.mood_labels)

        val gaugeView = findViewById<SocialBatteryGaugeView>(R.id.gauge_view)
        val moodLabel = findViewById<TextView>(R.id.mood_label)

        // Restore saved position
        val savedPosition = SocialBatteryWidgetProvider.getCurrentPosition(this)
        val savedMood = SocialBatteryWidgetProvider.getCurrentMood(this)
        gaugeView.indicatorPosition = savedPosition
        moodLabel.text = moodLabels[savedMood - 1]

        gaugeView.onMoodChangeListener = object : SocialBatteryGaugeView.OnMoodChangeListener {
            override fun onMoodChanged(moodIndex: Int, position: Float) {
                moodLabel.text = moodLabels[moodIndex - 1]
                SocialBatteryWidgetProvider.setCurrentMood(this@MainActivity, moodIndex)
                SocialBatteryWidgetProvider.setCurrentPosition(this@MainActivity, position)
                updateWidgets()
            }
        }
    }

    private fun updateWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(this, SocialBatteryWidgetProvider::class.java)
        )
        if (widgetIds.isNotEmpty()) {
            val intent = Intent(this, SocialBatteryWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            }
            sendBroadcast(intent)
        }
    }
}
