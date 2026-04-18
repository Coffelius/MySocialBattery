package com.mysocialbattery.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SocialBatteryGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnMoodChangeListener {
        fun onMoodChanged(moodIndex: Int, position: Float)
    }

    var onMoodChangeListener: OnMoodChangeListener? = null

    var indicatorPosition: Float = 4f / 6f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val moodColors = intArrayOf(
        Color.parseColor("#E53935"), Color.parseColor("#FB8C00"),
        Color.parseColor("#FDD835"), Color.parseColor("#C0CA33"),
        Color.parseColor("#7CB342"), Color.parseColor("#43A047"),
        Color.parseColor("#00897B")
    )
    private val moodDarkColors = intArrayOf(
        Color.parseColor("#B71C1C"), Color.parseColor("#BF360C"),
        Color.parseColor("#F57F17"), Color.parseColor("#827717"),
        Color.parseColor("#33691E"), Color.parseColor("#1B5E20"),
        Color.parseColor("#004D40")
    )
    private val moodLightColors = intArrayOf(
        Color.parseColor("#EF5350"), Color.parseColor("#FFA726"),
        Color.parseColor("#FFEE58"), Color.parseColor("#D4E157"),
        Color.parseColor("#9CCC65"), Color.parseColor("#66BB6A"),
        Color.parseColor("#26A69A")
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#424242")
        strokeCap = Paint.Cap.ROUND
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tempPath = Path()
    private val tempRect = RectF()

    private var isDragging = false
    private var gaugeLeft = 0f
    private var gaugeRight = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        // Faces at top, gauge + bolt below
        val h = (w * 0.48f).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val padding = w * 0.03f

        // --- Layout calculations ---
        val zoneAreaTop = h * 0.02f
        val zoneAreaBottom = h * 0.48f
        val zoneHeight = zoneAreaBottom - zoneAreaTop

        val totalZoneWidth = w - padding * 2
        val gap = totalZoneWidth * 0.012f
        val zoneWidth = (totalZoneWidth - gap * 6) / 7f
        val zoneRadius = zoneWidth * 0.25f

        val grooveHeight = h * 0.06f
        val grooveTop = zoneAreaBottom + h * 0.08f
        gaugeLeft = padding + zoneWidth * 0.5f
        gaugeRight = padding + 6 * (zoneWidth + gap) + zoneWidth * 0.5f

        // Indicator sits between faces and groove, overlapping both slightly
        val indicatorHeight = h * 0.48f
        val indicatorWidth = indicatorHeight * 0.6f

        // --- Draw neumorphic inset panel behind zones + groove + indicator area ---
        val panelRect = RectF(
            padding - 6, zoneAreaTop - 6,
            w - padding + 6, grooveTop + grooveHeight + indicatorHeight * 0.55f
        )
        val panelRadius = zoneRadius + 4

        // Outer soft shadow (dark, bottom-right)
        shadowPaint.maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
        shadowPaint.color = Color.parseColor("#50A8A39B")
        tempRect.set(panelRect.left + 3, panelRect.top + 3, panelRect.right + 3, panelRect.bottom + 3)
        canvas.drawRoundRect(tempRect, panelRadius, panelRadius, shadowPaint)

        // Outer soft highlight (light, top-left)
        shadowPaint.color = Color.parseColor("#80FAFAF6")
        tempRect.set(panelRect.left - 3, panelRect.top - 3, panelRect.right - 3, panelRect.bottom - 3)
        canvas.drawRoundRect(tempRect, panelRadius, panelRadius, shadowPaint)

        shadowPaint.maskFilter = null

        // Inset panel surface
        paint.color = Color.parseColor("#E2DDD5")
        canvas.drawRoundRect(panelRect, panelRadius, panelRadius, paint)

        // Inner shadow (top-left, to create pressed-in look)
        shadowPaint.maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
        shadowPaint.color = Color.parseColor("#38A09888")
        val innerShadowRect = RectF(panelRect.left, panelRect.top, panelRect.right - 2, panelRect.bottom - 2)
        canvas.drawRoundRect(innerShadowRect, panelRadius, panelRadius, shadowPaint)
        shadowPaint.maskFilter = null

        // Re-draw center to clean inner shadow to inset effect
        val innerFillRect = RectF(panelRect.left + 2, panelRect.top + 2, panelRect.right - 1, panelRect.bottom - 1)
        paint.color = Color.parseColor("#E5E0D8")
        canvas.drawRoundRect(innerFillRect, panelRadius - 1, panelRadius - 1, paint)

        // --- Draw 7 colored zones ---
        for (i in 0 until 7) {
            val left = padding + i * (zoneWidth + gap)
            val top = zoneAreaTop
            val right = left + zoneWidth
            val bottom = zoneAreaBottom

            // Neumorphic shadow under each zone
            shadowPaint.maskFilter = BlurMaskFilter(5f, BlurMaskFilter.Blur.NORMAL)
            shadowPaint.color = Color.parseColor("#40000000")
            tempRect.set(left + 2, top + 2, right + 2, bottom + 3)
            canvas.drawRoundRect(tempRect, zoneRadius, zoneRadius, shadowPaint)
            shadowPaint.maskFilter = null

            // Dark bottom edge
            tempRect.set(left, top + 1, right + 1, bottom + 1)
            paint.color = moodDarkColors[i]
            canvas.drawRoundRect(tempRect, zoneRadius, zoneRadius, paint)

            // Main color fill
            tempRect.set(left, top, right, bottom)
            paint.color = moodColors[i]
            canvas.drawRoundRect(tempRect, zoneRadius, zoneRadius, paint)

            // Top-left highlight for convex/raised look
            paint.shader = LinearGradient(
                left, top, left + zoneWidth * 0.5f, top + zoneHeight * 0.6f,
                moodLightColors[i], moodColors[i], Shader.TileMode.CLAMP
            )
            val highlightRect = RectF(left, top, right, top + zoneHeight * 0.55f)
            canvas.drawRoundRect(highlightRect, zoneRadius, zoneRadius, paint)
            paint.shader = null

            // Glossy specular highlight at top
            paint.shader = LinearGradient(
                left, top, left, top + zoneHeight * 0.3f,
                Color.parseColor("#40FFFFFF"), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(tempRect, zoneRadius, zoneRadius, paint)
            paint.shader = null

            // Draw face
            val cx = left + zoneWidth / 2
            val cy = top + zoneHeight / 2
            val faceR = zoneWidth * 0.32f
            drawFace(canvas, i, cx, cy, faceR)
        }

        // --- Draw gauge groove (neumorphic inset) ---
        val grooveRect = RectF(padding + 2, grooveTop, w - padding - 2, grooveTop + grooveHeight)
        val grooveRadius = grooveHeight / 2

        // Dark inset shadow (top)
        shadowPaint.maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
        shadowPaint.color = Color.parseColor("#60908880")
        tempRect.set(grooveRect.left, grooveRect.top - 1, grooveRect.right, grooveRect.bottom)
        canvas.drawRoundRect(tempRect, grooveRadius, grooveRadius, shadowPaint)
        shadowPaint.maskFilter = null

        // Light bottom highlight
        paint.color = Color.parseColor("#D8D3CB")
        tempRect.set(grooveRect.left, grooveRect.top + 1, grooveRect.right, grooveRect.bottom + 1.5f)
        canvas.drawRoundRect(tempRect, grooveRadius, grooveRadius, paint)

        // Groove body
        paint.shader = LinearGradient(
            0f, grooveRect.top, 0f, grooveRect.bottom,
            Color.parseColor("#A09888"), Color.parseColor("#BDB5AA"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(grooveRect, grooveRadius, grooveRadius, paint)
        paint.shader = null

        // --- Draw big metallic indicator below faces, over the gauge ---
        val boltVisualCenterX = indicatorWidth * 0.50f
        val indicatorX = gaugeLeft + indicatorPosition * (gaugeRight - gaugeLeft) - boltVisualCenterX
        // Position bolt so its top starts just below the face zones and its body covers the groove
        val indicatorY = zoneAreaBottom - indicatorHeight * 0.05f
        val currentMoodIndex = (indicatorPosition * 6).toInt().coerceIn(0, 6)
        drawBigMetalIndicator(canvas, indicatorX, indicatorY, indicatorWidth, indicatorHeight, moodColors[currentMoodIndex])
    }

    private fun drawFace(canvas: Canvas, index: Int, cx: Float, cy: Float, r: Float) {
        // White circle
        paint.shader = null
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, r, paint)

        // Subtle shadow on face circle
        paint.shader = RadialGradient(
            cx - r * 0.2f, cy - r * 0.2f, r,
            Color.WHITE, Color.parseColor("#F0F0F0"), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null

        strokePaint.strokeWidth = r * 0.13f

        when (index) {
            0 -> {
                val eo = r * 0.3f; val es = r * 0.18f
                canvas.drawLine(cx - eo - es, cy - es - r * 0.1f, cx - eo + es, cy + es - r * 0.1f, strokePaint)
                canvas.drawLine(cx - eo + es, cy - es - r * 0.1f, cx - eo - es, cy + es - r * 0.1f, strokePaint)
                canvas.drawLine(cx + eo - es, cy - es - r * 0.1f, cx + eo + es, cy + es - r * 0.1f, strokePaint)
                canvas.drawLine(cx + eo + es, cy - es - r * 0.1f, cx + eo - es, cy + es - r * 0.1f, strokePaint)
                tempPath.reset()
                tempPath.moveTo(cx - r * 0.5f, cy + r * 0.55f)
                tempPath.quadTo(cx, cy + r * 0.15f, cx + r * 0.5f, cy + r * 0.55f)
                canvas.drawPath(tempPath, strokePaint)
            }
            1 -> {
                paint.color = Color.parseColor("#424242")
                canvas.drawCircle(cx - r * 0.3f, cy - r * 0.02f, r * 0.1f, paint)
                canvas.drawCircle(cx + r * 0.3f, cy - r * 0.02f, r * 0.1f, paint)
                canvas.drawLine(cx - r * 0.55f, cy - r * 0.35f, cx - r * 0.1f, cy - r * 0.18f, strokePaint)
                canvas.drawLine(cx + r * 0.55f, cy - r * 0.35f, cx + r * 0.1f, cy - r * 0.18f, strokePaint)
                tempPath.reset()
                tempPath.moveTo(cx - r * 0.4f, cy + r * 0.5f)
                tempPath.quadTo(cx, cy + r * 0.2f, cx + r * 0.4f, cy + r * 0.5f)
                canvas.drawPath(tempPath, strokePaint)
            }
            2 -> {
                paint.color = Color.parseColor("#424242")
                canvas.drawCircle(cx - r * 0.3f, cy - r * 0.08f, r * 0.1f, paint)
                canvas.drawCircle(cx + r * 0.3f, cy - r * 0.08f, r * 0.1f, paint)
                tempPath.reset()
                tempPath.moveTo(cx - r * 0.35f, cy + r * 0.45f)
                tempPath.quadTo(cx, cy + r * 0.25f, cx + r * 0.35f, cy + r * 0.45f)
                canvas.drawPath(tempPath, strokePaint)
            }
            3 -> {
                paint.color = Color.parseColor("#424242")
                canvas.drawCircle(cx - r * 0.3f, cy - r * 0.08f, r * 0.1f, paint)
                canvas.drawCircle(cx + r * 0.3f, cy - r * 0.08f, r * 0.1f, paint)
                canvas.drawLine(cx - r * 0.3f, cy + r * 0.38f, cx + r * 0.3f, cy + r * 0.38f, strokePaint)
            }
            4 -> {
                paint.color = Color.parseColor("#424242")
                canvas.drawCircle(cx - r * 0.3f, cy - r * 0.08f, r * 0.1f, paint)
                canvas.drawCircle(cx + r * 0.3f, cy - r * 0.08f, r * 0.1f, paint)
                tempPath.reset()
                tempPath.moveTo(cx - r * 0.35f, cy + r * 0.3f)
                tempPath.quadTo(cx, cy + r * 0.55f, cx + r * 0.35f, cy + r * 0.3f)
                canvas.drawPath(tempPath, strokePaint)
            }
            5 -> {
                tempPath.reset()
                tempPath.moveTo(cx - r * 0.5f, cy); tempPath.quadTo(cx - r * 0.3f, cy - r * 0.3f, cx - r * 0.1f, cy)
                canvas.drawPath(tempPath, strokePaint)
                tempPath.reset()
                tempPath.moveTo(cx + r * 0.1f, cy); tempPath.quadTo(cx + r * 0.3f, cy - r * 0.3f, cx + r * 0.5f, cy)
                canvas.drawPath(tempPath, strokePaint)
                tempPath.reset()
                tempPath.moveTo(cx - r * 0.5f, cy + r * 0.25f)
                tempPath.quadTo(cx, cy + r * 0.7f, cx + r * 0.5f, cy + r * 0.25f)
                canvas.drawPath(tempPath, strokePaint)
            }
            6 -> {
                drawStar(canvas, cx - r * 0.3f, cy - r * 0.05f, r * 0.2f)
                drawStar(canvas, cx + r * 0.3f, cy - r * 0.05f, r * 0.2f)
                tempPath.reset()
                tempPath.moveTo(cx - r * 0.55f, cy + r * 0.2f)
                tempPath.quadTo(cx, cy + r * 0.8f, cx + r * 0.55f, cy + r * 0.2f)
                canvas.drawPath(tempPath, strokePaint)
            }
        }
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val path = Path()
        paint.color = Color.parseColor("#424242")
        for (i in 0 until 5) {
            val oa = Math.toRadians(-90.0 + i * 72)
            val ia = Math.toRadians(-90.0 + i * 72 + 36)
            val ox = cx + r * Math.cos(oa).toFloat()
            val oy = cy + r * Math.sin(oa).toFloat()
            val ix = cx + r * 0.4f * Math.cos(ia).toFloat()
            val iy = cy + r * 0.4f * Math.sin(ia).toFloat()
            if (i == 0) path.moveTo(ox, oy) else path.lineTo(ox, oy)
            path.lineTo(ix, iy)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawBigMetalIndicator(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int) {
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                updatePositionFromTouch(event.x)
                parent.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    updatePositionFromTouch(event.x)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    updatePositionFromTouch(event.x)
                    parent.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updatePositionFromTouch(touchX: Float) {
        if (gaugeRight <= gaugeLeft) return
        val newPos = ((touchX - gaugeLeft) / (gaugeRight - gaugeLeft)).coerceIn(0f, 1f)
        indicatorPosition = newPos
        val moodIndex = (newPos * 6).toInt().coerceIn(0, 6) + 1
        onMoodChangeListener?.onMoodChanged(moodIndex, newPos)
    }
}
