package com.mysocialbattery.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.widget.RemoteViews
import com.mysocialbattery.R
import com.mysocialbattery.activity.MainActivity

class SocialBatteryWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val PREFS_NAME = "social_battery_prefs"
        private const val KEY_CURRENT_MOOD = "current_mood"

        fun getCurrentMood(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_CURRENT_MOOD, 5)
        }

        fun setCurrentMood(context: Context, mood: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_CURRENT_MOOD, mood).apply()
        }

        fun getCurrentPosition(context: Context): Float {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat("current_position", moodToPosition(5))
        }

        fun setCurrentPosition(context: Context, position: Float) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putFloat("current_position", position).apply()
        }

        fun moodToPosition(mood: Int): Float = (mood - 1).toFloat() / 6f
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val options = manager.getAppWidgetOptions(widgetId)
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)

        val density = context.resources.displayMetrics.density
        val widthPx = (minW * density).toInt().coerceAtLeast(400)
        val heightPx = (minH * density).toInt().coerceAtLeast(140)

        val bitmap = renderWidgetBitmap(context, widthPx, heightPx)

        val views = RemoteViews(context.packageName, R.layout.widget_social_battery)
        views.setImageViewBitmap(R.id.widget_image, bitmap)

        // Open app on tap
        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent)

        manager.updateAppWidget(widgetId, views)
    }

    private fun renderWidgetBitmap(context: Context, w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#424242")
            strokeCap = Paint.Cap.ROUND
        }
        val tempPath = Path()
        val tempRect = RectF()

        val position = getCurrentPosition(context)
        val padding = w * 0.03f

        // --- Widget background (neumorphic raised card) ---
        // Bottom-right shadow
        shadowPaint.maskFilter = BlurMaskFilter(8f * (w / 400f), BlurMaskFilter.Blur.NORMAL)
        shadowPaint.color = Color.parseColor("#60A8A39B")
        tempRect.set(padding + 3, 3f, w - padding + 3, h.toFloat() + 1)
        canvas.drawRoundRect(tempRect, 28f, 28f, shadowPaint)

        // Top-left highlight
        shadowPaint.color = Color.parseColor("#80FAFAF6")
        tempRect.set(padding - 2, -2f, w - padding - 2, h.toFloat() - 4)
        canvas.drawRoundRect(tempRect, 28f, 28f, shadowPaint)
        shadowPaint.maskFilter = null

        // Main card
        paint.color = Color.parseColor("#EDE8E0")
        tempRect.set(padding, 1f, w - padding, h.toFloat() - 2)
        canvas.drawRoundRect(tempRect, 26f, 26f, paint)

        // Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.parseColor("#D6D1C9")
        }
        canvas.drawRoundRect(tempRect, 26f, 26f, borderPaint)

        // --- Title ---
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5D5549")
            textSize = h * 0.13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.12f
        }
        canvas.drawText(context.getString(R.string.widget_title), w / 2f, h * 0.18f, titlePaint)

        // --- Colored zones with faces ---
        val innerLeft = padding + w * 0.03f
        val innerRight = w - padding - w * 0.03f
        val totalW = innerRight - innerLeft
        val gap = totalW * 0.012f
        val zoneW = (totalW - gap * 6) / 7f
        val zoneTop = h * 0.22f
        val zoneBottom = h * 0.52f
        val zoneH = zoneBottom - zoneTop
        val zoneRadius = zoneW * 0.22f

        val moodColors = intArrayOf(
            Color.parseColor("#E53935"), Color.parseColor("#FB8C00"),
            Color.parseColor("#FDD835"), Color.parseColor("#C0CA33"),
            Color.parseColor("#7CB342"), Color.parseColor("#43A047"),
            Color.parseColor("#00897B")
        )
        val moodDark = intArrayOf(
            Color.parseColor("#B71C1C"), Color.parseColor("#BF360C"),
            Color.parseColor("#F57F17"), Color.parseColor("#827717"),
            Color.parseColor("#33691E"), Color.parseColor("#1B5E20"),
            Color.parseColor("#004D40")
        )
        val moodLight = intArrayOf(
            Color.parseColor("#EF5350"), Color.parseColor("#FFA726"),
            Color.parseColor("#FFEE58"), Color.parseColor("#D4E157"),
            Color.parseColor("#9CCC65"), Color.parseColor("#66BB6A"),
            Color.parseColor("#26A69A")
        )

        // Inset panel behind zones + groove
        val grooveH = h * 0.05f
        val grooveTop = zoneBottom + h * 0.08f
        val indW = zoneW * 0.7f
        val indH = indW * 1.4f

        val panelRect = RectF(innerLeft - 5, zoneTop - 5, innerRight + 5, grooveTop + grooveH + indH * 0.5f)
        val panelR = zoneRadius + 4
        shadowPaint.maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
        shadowPaint.color = Color.parseColor("#40A8A39B")
        tempRect.set(panelRect.left + 2, panelRect.top + 2, panelRect.right + 2, panelRect.bottom + 2)
        canvas.drawRoundRect(tempRect, panelR, panelR, shadowPaint)
        shadowPaint.color = Color.parseColor("#60FAFAF6")
        tempRect.set(panelRect.left - 2, panelRect.top - 2, panelRect.right - 2, panelRect.bottom - 2)
        canvas.drawRoundRect(tempRect, panelR, panelR, shadowPaint)
        shadowPaint.maskFilter = null

        paint.color = Color.parseColor("#E5E0D8")
        canvas.drawRoundRect(panelRect, panelR, panelR, paint)

        for (i in 0 until 7) {
            val left = innerLeft + i * (zoneW + gap)
            val right = left + zoneW

            // Shadow
            shadowPaint.maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
            shadowPaint.color = Color.parseColor("#30000000")
            tempRect.set(left + 1.5f, zoneTop + 1.5f, right + 1.5f, zoneBottom + 2)
            canvas.drawRoundRect(tempRect, zoneRadius, zoneRadius, shadowPaint)
            shadowPaint.maskFilter = null

            // Dark edge
            tempRect.set(left, zoneTop + 1, right + 0.5f, zoneBottom + 1)
            paint.color = moodDark[i]
            canvas.drawRoundRect(tempRect, zoneRadius, zoneRadius, paint)

            // Main
            tempRect.set(left, zoneTop, right, zoneBottom)
            paint.color = moodColors[i]
            canvas.drawRoundRect(tempRect, zoneRadius, zoneRadius, paint)

            // Highlight
            paint.shader = LinearGradient(
                left, zoneTop, left + zoneW * 0.5f, zoneTop + zoneH * 0.5f,
                moodLight[i], moodColors[i], Shader.TileMode.CLAMP
            )
            tempRect.set(left, zoneTop, right, zoneTop + zoneH * 0.5f)
            canvas.drawRoundRect(tempRect, zoneRadius, zoneRadius, paint)
            paint.shader = null

            // Gloss
            paint.shader = LinearGradient(
                left, zoneTop, left, zoneTop + zoneH * 0.3f,
                Color.parseColor("#35FFFFFF"), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            tempRect.set(left, zoneTop, right, zoneBottom)
            canvas.drawRoundRect(tempRect, zoneRadius, zoneRadius, paint)
            paint.shader = null

            // Face
            val cx = left + zoneW / 2
            val cy = zoneTop + zoneH / 2
            val faceR = zoneW * 0.30f
            drawWidgetFace(canvas, paint, strokePaint, tempPath, i, cx, cy, faceR)
        }

        // --- Gauge groove ---
        val grooveRect = RectF(innerLeft, grooveTop, innerRight, grooveTop + grooveH)
        val grooveRadius = grooveH / 2

        shadowPaint.maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
        shadowPaint.color = Color.parseColor("#50908880")
        tempRect.set(grooveRect.left, grooveRect.top - 1, grooveRect.right, grooveRect.bottom)
        canvas.drawRoundRect(tempRect, grooveRadius, grooveRadius, shadowPaint)
        shadowPaint.maskFilter = null

        paint.color = Color.parseColor("#D8D3CB")
        tempRect.set(grooveRect.left, grooveRect.top + 1, grooveRect.right, grooveRect.bottom + 1)
        canvas.drawRoundRect(tempRect, grooveRadius, grooveRadius, paint)

        paint.shader = LinearGradient(0f, grooveRect.top, 0f, grooveRect.bottom,
            Color.parseColor("#A09888"), Color.parseColor("#BDB5AA"), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(grooveRect, grooveRadius, grooveRadius, paint)
        paint.shader = null

        // --- Indicator ---
        val gaugeLeft = innerLeft + zoneW * 0.5f
        val gaugeRight = innerLeft + 6 * (zoneW + gap) + zoneW * 0.5f
        // Bolt path spans 0.15..0.85 → center at 0.50
        val boltCenterX = indW * 0.50f
        val indX = gaugeLeft + position * (gaugeRight - gaugeLeft) - boltCenterX
        val indY = zoneBottom - indH * 0.05f

        val currentMoodIndex = (position * 6).toInt().coerceIn(0, 6)
        drawBolt(canvas, paint, indX, indY, indW, indH, moodColors[currentMoodIndex])

        return bitmap
    }

    private fun drawWidgetFace(canvas: Canvas, paint: Paint, strokePaint: Paint, path: Path,
                               index: Int, cx: Float, cy: Float, r: Float) {
        paint.shader = null
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, r, paint)

        paint.shader = RadialGradient(cx - r * 0.2f, cy - r * 0.2f, r,
            Color.WHITE, Color.parseColor("#F0F0F0"), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null

        strokePaint.strokeWidth = r * 0.14f

        when (index) {
            0 -> {
                val eo = r * 0.3f; val es = r * 0.18f
                canvas.drawLine(cx-eo-es,cy-es-r*0.1f, cx-eo+es,cy+es-r*0.1f, strokePaint)
                canvas.drawLine(cx-eo+es,cy-es-r*0.1f, cx-eo-es,cy+es-r*0.1f, strokePaint)
                canvas.drawLine(cx+eo-es,cy-es-r*0.1f, cx+eo+es,cy+es-r*0.1f, strokePaint)
                canvas.drawLine(cx+eo+es,cy-es-r*0.1f, cx+eo-es,cy+es-r*0.1f, strokePaint)
                path.reset(); path.moveTo(cx-r*0.5f,cy+r*0.55f); path.quadTo(cx,cy+r*0.15f,cx+r*0.5f,cy+r*0.55f)
                canvas.drawPath(path, strokePaint)
            }
            1 -> {
                paint.color = Color.parseColor("#424242")
                canvas.drawCircle(cx-r*0.3f,cy-r*0.02f,r*0.1f,paint)
                canvas.drawCircle(cx+r*0.3f,cy-r*0.02f,r*0.1f,paint)
                canvas.drawLine(cx-r*0.55f,cy-r*0.35f,cx-r*0.1f,cy-r*0.18f,strokePaint)
                canvas.drawLine(cx+r*0.55f,cy-r*0.35f,cx+r*0.1f,cy-r*0.18f,strokePaint)
                path.reset(); path.moveTo(cx-r*0.4f,cy+r*0.5f); path.quadTo(cx,cy+r*0.2f,cx+r*0.4f,cy+r*0.5f)
                canvas.drawPath(path, strokePaint)
            }
            2 -> {
                paint.color = Color.parseColor("#424242")
                canvas.drawCircle(cx-r*0.3f,cy-r*0.08f,r*0.1f,paint)
                canvas.drawCircle(cx+r*0.3f,cy-r*0.08f,r*0.1f,paint)
                path.reset(); path.moveTo(cx-r*0.35f,cy+r*0.45f); path.quadTo(cx,cy+r*0.25f,cx+r*0.35f,cy+r*0.45f)
                canvas.drawPath(path, strokePaint)
            }
            3 -> {
                paint.color = Color.parseColor("#424242")
                canvas.drawCircle(cx-r*0.3f,cy-r*0.08f,r*0.1f,paint)
                canvas.drawCircle(cx+r*0.3f,cy-r*0.08f,r*0.1f,paint)
                canvas.drawLine(cx-r*0.3f,cy+r*0.38f,cx+r*0.3f,cy+r*0.38f,strokePaint)
            }
            4 -> {
                paint.color = Color.parseColor("#424242")
                canvas.drawCircle(cx-r*0.3f,cy-r*0.08f,r*0.1f,paint)
                canvas.drawCircle(cx+r*0.3f,cy-r*0.08f,r*0.1f,paint)
                path.reset(); path.moveTo(cx-r*0.35f,cy+r*0.3f); path.quadTo(cx,cy+r*0.55f,cx+r*0.35f,cy+r*0.3f)
                canvas.drawPath(path, strokePaint)
            }
            5 -> {
                path.reset(); path.moveTo(cx-r*0.5f,cy); path.quadTo(cx-r*0.3f,cy-r*0.3f,cx-r*0.1f,cy)
                canvas.drawPath(path, strokePaint)
                path.reset(); path.moveTo(cx+r*0.1f,cy); path.quadTo(cx+r*0.3f,cy-r*0.3f,cx+r*0.5f,cy)
                canvas.drawPath(path, strokePaint)
                path.reset(); path.moveTo(cx-r*0.5f,cy+r*0.25f); path.quadTo(cx,cy+r*0.7f,cx+r*0.5f,cy+r*0.25f)
                canvas.drawPath(path, strokePaint)
            }
            6 -> {
                drawWidgetStar(canvas, paint, cx-r*0.3f,cy-r*0.05f,r*0.2f)
                drawWidgetStar(canvas, paint, cx+r*0.3f,cy-r*0.05f,r*0.2f)
                path.reset(); path.moveTo(cx-r*0.55f,cy+r*0.2f); path.quadTo(cx,cy+r*0.8f,cx+r*0.55f,cy+r*0.2f)
                canvas.drawPath(path, strokePaint)
            }
        }
    }

    private fun drawWidgetStar(canvas: Canvas, paint: Paint, cx: Float, cy: Float, r: Float) {
        val path = Path()
        paint.color = Color.parseColor("#424242")
        for (i in 0 until 5) {
            val oa = Math.toRadians(-90.0 + i * 72)
            val ia = Math.toRadians(-90.0 + i * 72 + 36)
            val ox = cx + r * Math.cos(oa).toFloat(); val oy = cy + r * Math.sin(oa).toFloat()
            val ix = cx + r * 0.4f * Math.cos(ia).toFloat(); val iy = cy + r * 0.4f * Math.sin(ia).toFloat()
            if (i == 0) path.moveTo(ox, oy) else path.lineTo(ox, oy)
            path.lineTo(ix, iy)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawBolt(canvas: Canvas, paint: Paint,
                         x: Float, y: Float, w: Float, h: Float, color: Int) {
        val cx = x + w * 0.5f
        val arrowH = h * 0.7f
        val halfBase = w * 0.35f
        val top = y
        val bot = y + arrowH

        val arrow = Path().apply {
            moveTo(cx, top)
            lineTo(cx + halfBase, bot)
            lineTo(cx - halfBase, bot)
            close()
        }

        paint.shader = null
        paint.color = color
        canvas.drawPath(arrow, paint)
    }
}
