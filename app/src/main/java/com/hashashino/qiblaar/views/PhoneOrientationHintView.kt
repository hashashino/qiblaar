package com.hashashino.qiblaar.views

import android.animation.Keyframe
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.hashashino.qiblaar.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated pose-guidance illustration.
 *
 * FLAT  — mirrors PhoneFlatIllustration from pose-illustrations.jsx:
 *   A phone side-profile pivots at its bottom-left corner from -90° (upright) to 0° (flat),
 *   lingering flat while the mini-compass fades in and the motion arrow fades out.
 *
 * UPRIGHT — mirrors PhoneUprightIllustration:
 *   A portrait phone pivots at hand position from +90° (horizontal) to 0° (upright),
 *   lingering upright while the camera ray and Kaaba marker appear.
 *
 * Cycle: 3600ms, cubic-bezier(.55,.05,.25,1) (FastOutSlowIn-ish).
 * Keyframes: 0%,14% wrong pose → 34%,82% correct → 100% reset.
 */
class PhoneOrientationHintView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Mode { FLAT, UPRIGHT }

    var mode: Mode = Mode.FLAT
        set(value) { field = value; invalidate() }

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Float) = v * density
    private fun sp(v: Float) = v * context.resources.displayMetrics.scaledDensity

    // t: 0 = wrong pose, 1 = correct pose
    private val animator: ValueAnimator = run {
        // 0%,14% → wrong; 34%,82% → correct; 100% → wrong (reset)
        val kf0  = Keyframe.ofFloat(0f,    0f)
        val kf1  = Keyframe.ofFloat(0.14f, 0f)
        val kf2  = Keyframe.ofFloat(0.34f, 1f)
        val kf3  = Keyframe.ofFloat(0.82f, 1f)
        val kf4  = Keyframe.ofFloat(1f,    0f)
        val pvh = PropertyValuesHolder.ofKeyframe("t", kf0, kf1, kf2, kf3, kf4)
        ValueAnimator.ofPropertyValuesHolder(pvh).apply {
            duration = 3600
            repeatCount = ValueAnimator.INFINITE
            repeatMode  = ValueAnimator.RESTART
            addUpdateListener { invalidate() }
        }
    }

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.PhoneOrientationHintView)
            mode = if (a.getInt(R.styleable.PhoneOrientationHintView_orientationMode, 0) == 1)
                Mode.UPRIGHT else Mode.FLAT
            a.recycle()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) { if (!animator.isRunning) animator.start() }
        else animator.pause()
    }

    override fun onDraw(canvas: Canvas) {
        val t = (animator.animatedValue as? Float) ?: 0f
        when (mode) {
            Mode.FLAT    -> drawFlatScene(canvas, t)
            Mode.UPRIGHT -> drawUprightScene(canvas, t)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // FLAT scene  (280×200 logical coordinate space, scaled to view)
    // ─────────────────────────────────────────────────────────────
    private fun drawFlatScene(canvas: Canvas, t: Float) {
        val vw = width.toFloat()
        val vh = height.toFloat()
        // Scale the 280×200 design viewport to the actual view
        val sx = vw / 280f
        val sy = vh / 200f
        canvas.save()
        canvas.scale(sx, sy)

        // Table parallelogram
        val tableStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f / sx
            color = Color.parseColor("#2EFFFFFF")
        }
        val tableFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(0f, 168f, 0f, 188f,
                Color.parseColor("#0FFFFFFF"), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        val tablePath = Path().also { p ->
            p.moveTo(40f, 168f); p.lineTo(246f, 168f); p.lineTo(230f, 188f); p.lineTo(56f, 188f); p.close()
        }
        canvas.drawPath(tablePath, tableFill)
        canvas.drawPath(tablePath, tableStroke)
        val tableTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#5CFFFFFF")
            textSize = 9f
            isFakeBoldText = true
            letterSpacing = 0.15f
        }
        canvas.drawText("TABLE · LEVEL", 143f, 183f, tableTextPaint)

        // Green wash when flat (fades in when t > 0.4)
        val washAlpha = ((t - 0.4f).coerceIn(0f, 0.42f) / 0.42f * 255).toInt()
        if (washAlpha > 0) {
            val washPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(160f, 145f, 70f,
                    Color.argb(washAlpha, 34, 197, 94), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(160f, 145f, 70f, washPaint)
        }

        // Shadow under phone when flat
        val shadowAlpha = ((t - 0.3f).coerceIn(0f, 0.55f) / 0.55f * 140).toInt()
        if (shadowAlpha > 0) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.argb(shadowAlpha, 0, 0, 0)
            }
            canvas.drawOval(RectF(98f, 167f, 222f, 173f), shadowPaint)
        }

        // Motion arrow (fades OUT as phone flattens — visible when t < ~0.3)
        val arrowAlpha = ((0.3f - t).coerceIn(0f, 0.3f) / 0.3f * 220).toInt()
        if (arrowAlpha > 0) {
            val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = Color.argb(arrowAlpha, 34, 197, 94)
                strokeCap = Paint.Cap.ROUND
            }
            // Curved dashed motion path (simplified as a quadratic arc)
            val curvedPath = Path().also { p ->
                p.moveTo(120f, 50f); p.quadTo(140f, 80f, 170f, 100f)
            }
            canvas.drawPath(curvedPath, arrowPaint)
            // Arrow head
            val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 2f
                strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
                color = Color.argb(arrowAlpha, 34, 197, 94)
            }
            val headPath = Path().also { p ->
                p.moveTo(164f, 92f); p.lineTo(172f, 102f); p.moveTo(172f, 102f); p.lineTo(162f, 102f)
            }
            canvas.drawPath(headPath, headPaint)
        }

        // Mini compass — fades in when flat (t > 0.4)
        val compassAlpha = ((t - 0.4f).coerceIn(0f, 0.42f) / 0.42f * 230).toInt()
        if (compassAlpha > 0) {
            canvas.save()
            canvas.translate(160f, 56f)
            val cBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.argb(compassAlpha, 20, 21, 42)
            }
            val cRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 1f
                color = Color.argb(compassAlpha / 2, 34, 197, 94)
            }
            canvas.drawCircle(0f, 0f, 22f, cBg)
            canvas.drawCircle(0f, 0f, 22f, cRing)
            val cText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER; isFakeBoldText = true
            }
            cText.color = Color.argb(compassAlpha, 239, 68, 68); cText.textSize = 9f
            canvas.drawText("N", 0f, -6f, cText)
            cText.color = Color.argb((compassAlpha * 0.6f).toInt(), 255, 255, 255); cText.textSize = 7f
            canvas.drawText("S", 0f, 17f, cText)
            canvas.drawText("W", -15f, 5f, cText)
            canvas.drawText("E", 15f, 5f, cText)
            // Qibla pointer (green line)
            val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 2.2f; strokeCap = Paint.Cap.ROUND
                color = Color.argb(compassAlpha, 34, 197, 94)
            }
            canvas.drawLine(0f, 0f, -11f, -11f, needlePaint)
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL; color = Color.argb(compassAlpha, 255, 255, 255)
            }
            canvas.drawCircle(0f, 0f, 2f, dotPaint)
            canvas.restore()
        }

        // Phone — pivots at bottom-left corner (94, 152), rotating from -90° to 0°
        // rotation = -90° + t * 90° (wrong=-90°, correct=0°)
        val phoneAngle = -90f + t * 90f
        canvas.save()
        canvas.rotate(phoneAngle, 94f, 152f)

        // Phone body (laid flat: 94,138 → width 132, height 14)
        val phoneFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(0f, 138f, 0f, 152f,
                Color.parseColor("#2A2C46"), Color.parseColor("#1A1C34"), Shader.TileMode.CLAMP)
        }
        val phoneStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.4f
            color = Color.parseColor("#80FFFFFF")
        }
        val phoneRect = RectF(94f, 138f, 226f, 152f)
        canvas.drawRoundRect(phoneRect, 3f, 3f, phoneFill)
        canvas.drawRoundRect(phoneRect, 3f, 3f, phoneStroke)

        // Camera bump on top edge
        val camFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.parseColor("#2A2C46")
        }
        val camStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.parseColor("#66FFFFFF")
        }
        canvas.drawRoundRect(RectF(106f, 134f, 124f, 140f), 1.5f, 1.5f, camFill)
        canvas.drawRoundRect(RectF(106f, 134f, 124f, 140f), 1.5f, 1.5f, camStroke)
        val camLensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.argb((255 * 0.85f).toInt(), 34, 197, 94)
        }
        canvas.drawCircle(115f, 137f, 1.6f, camLensPaint)

        // Side button
        val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.parseColor("#59FFFFFF")
        }
        canvas.drawRoundRect(RectF(224f, 142f, 227.5f, 146.5f), 0.5f, 0.5f, btnPaint)

        // Bubble level
        val levelBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.parseColor("#0E0F22")
        }
        val levelStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 0.8f; color = Color.parseColor("#40FFFFFF")
        }
        canvas.drawRoundRect(RectF(140f, 144f, 196f, 150f), 3f, 3f, levelBg)
        canvas.drawRoundRect(RectF(140f, 144f, 196f, 150f), 3f, 3f, levelStroke)
        // Center tick
        val tickP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 0.8f; color = Color.parseColor("#80FFFFFF")
        }
        canvas.drawLine(168f, 142.5f, 168f, 151.5f, tickP)
        // Bubble (green when centered — centered when flat)
        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.parseColor("#22c55e")
        }
        canvas.drawCircle(168f, 147f, 2.4f, bubblePaint)

        canvas.restore() // phone rotation
        canvas.restore() // design scale
    }

    // ─────────────────────────────────────────────────────────────
    // UPRIGHT scene  (280×200 logical viewport)
    // ─────────────────────────────────────────────────────────────
    private fun drawUprightScene(canvas: Canvas, t: Float) {
        val vw = width.toFloat()
        val vh = height.toFloat()
        val sx = vw / 280f
        val sy = vh / 200f
        canvas.save()
        canvas.scale(sx, sy)

        // Floor line
        val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.parseColor("#1FFFFFFF")
        }
        canvas.drawLine(20f, 178f, 260f, 178f, floorPaint)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f; isFakeBoldText = true; color = Color.parseColor("#5CFFFFFF"); letterSpacing = 0.15f
        }
        canvas.drawText("YOU", 40f, 194f, labelPaint)
        labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("WALL", 240f, 194f, labelPaint)

        // Wall on the right
        val wallFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(0f, 20f, 0f, 178f,
                Color.parseColor("#1AFFFFFF"), Color.parseColor("#08FFFFFF"), Shader.TileMode.CLAMP)
        }
        val wallStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.2f; color = Color.parseColor("#2EFFFFFF")
        }
        canvas.drawRect(218f, 20f, 262f, 178f, wallFill)
        canvas.drawRect(218f, 20f, 262f, 178f, wallStroke)
        // Window/picture on wall
        canvas.drawRect(226f, 44f, 254f, 92f, wallStroke)

        // Kaaba marker on wall — fades in and scales up when t > 0.4
        val kaabaAlpha = ((t - 0.4f).coerceIn(0f, 0.42f) / 0.42f).let { it * it } // ease
        if (kaabaAlpha > 0) {
            canvas.save()
            canvas.translate(240f, 110f)
            val scale = 0.85f + 0.15f * kaabaAlpha
            canvas.scale(scale, scale)
            // Glow
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(0f, 0f, 22f,
                    Color.argb((kaabaAlpha * 107).toInt(), 34, 197, 94),
                    Color.TRANSPARENT, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(0f, 0f, 22f, glowPaint)
            // Kaaba glyph
            val kaabaP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 1.8f; strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = Color.argb((kaabaAlpha * 220).toInt(), 34, 197, 94)
            }
            val kaabaFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.argb((kaabaAlpha * 46).toInt(), 34, 197, 94)
            }
            canvas.drawRect(RectF(-8f, -6f, 9f, 7f), kaabaFill)
            canvas.drawRect(RectF(-8f, -6f, 9f, 7f), kaabaP)
            canvas.drawLine(-8f, -1f, 9f, -1f, kaabaP)
            canvas.drawLine(-2f, -6f, -2f, 7f, kaabaP)
            canvas.drawLine(4f, -6f, 4f, 7f, kaabaP)
            canvas.restore()
        }

        // Camera ray from upright phone (~146, 110) to Kaaba on wall (224, 110)
        val rayAlpha = ((t - 0.4f).coerceIn(0f, 0.42f) / 0.42f).let { it * it }
        if (rayAlpha > 0) {
            val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 2f; strokeCap = Paint.Cap.ROUND
                color = Color.argb((rayAlpha * 220).toInt(), 34, 197, 94)
            }
            // Dashed line (draw as segments)
            val segLen = 6f; val gapLen = 8f; val total = 224f - 146f
            var x = 146f
            var dash = true
            while (x < 224f) {
                val end = (x + if (dash) segLen else gapLen).coerceAtMost(224f)
                if (dash) canvas.drawLine(x, 110f, end, 110f, rayPaint)
                x = end; dash = !dash
            }
        }

        // Upward motion arrow — visible when t < 0.3 (phone still horizontal)
        val arrowAlpha2 = ((0.3f - t).coerceIn(0f, 0.3f) / 0.3f * 220).toInt()
        if (arrowAlpha2 > 0) {
            canvas.save()
            canvas.translate(180f, 90f)
            val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 2f; strokeCap = Paint.Cap.ROUND
                color = Color.argb(arrowAlpha2, 34, 197, 94)
            }
            val curvePath = Path().also { p -> p.moveTo(0f, 60f); p.quadTo(-16f, 30f, 0f, 0f) }
            canvas.drawPath(curvePath, arrowPaint)
            val headPath = Path().also { p ->
                p.moveTo(-6f, 7f); p.lineTo(0f, 0f); p.lineTo(6f, 7f)
            }
            canvas.drawPath(headPath, arrowPaint)
            canvas.restore()
        }

        // Hand grip (static)
        val handFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.parseColor("#0DFFFFFF")
        }
        val handStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.parseColor("#33FFFFFF")
        }
        val handPath = Path().also { p ->
            p.moveTo(88f, 168f); p.quadTo(92f, 184f, 120f, 184f); p.lineTo(124f, 184f)
            p.quadTo(152f, 184f, 156f, 168f); p.lineTo(156f, 178f)
            p.quadTo(152f, 192f, 124f, 192f); p.lineTo(120f, 192f)
            p.quadTo(92f, 192f, 88f, 178f); p.close()
        }
        canvas.drawPath(handPath, handFill)
        canvas.drawPath(handPath, handStroke)

        // Phone — pivots at hand position (120, 165), starts horizontal (+90°), ends vertical (0°)
        val phoneAngle = 90f - t * 90f
        canvas.save()
        canvas.rotate(phoneAngle, 120f, 165f)

        // Phone body (portrait: 102,78 → 36×88)
        val phoneFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(0f, 78f, 0f, 166f,
                Color.parseColor("#2A2C46"), Color.parseColor("#1A1C34"), Shader.TileMode.CLAMP)
        }
        val phoneStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.4f; color = Color.parseColor("#80FFFFFF")
        }
        canvas.drawRoundRect(RectF(102f, 78f, 138f, 166f), 6f, 6f, phoneFill)
        canvas.drawRoundRect(RectF(102f, 78f, 138f, 166f), 6f, 6f, phoneStroke)

        // Screen on left (user-facing)
        val screenFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.parseColor("#0E0F22")
        }
        val screenStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 0.5f; color = Color.parseColor("#2EFFFFFF")
        }
        canvas.drawRoundRect(RectF(106f, 84f, 112f, 160f), 1.5f, 1.5f, screenFill)
        canvas.drawRoundRect(RectF(106f, 84f, 112f, 160f), 1.5f, 1.5f, screenStroke)
        // Notch
        val notchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.parseColor("#66FFFFFF")
        }
        canvas.drawCircle(109f, 88f, 1.2f, notchPaint)

        // Camera lens (back/wall-facing side)
        val camFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.parseColor("#0A0A14")
        }
        val camStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 0.8f; color = Color.parseColor("#66FFFFFF")
        }
        canvas.drawRoundRect(RectF(130f, 90f, 140f, 104f), 2f, 2f, camFill)
        canvas.drawRoundRect(RectF(130f, 90f, 140f, 104f), 2f, 2f, camStroke)
        val lens1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.2f
            color = Color.parseColor("#E522c55e")
        }
        canvas.drawCircle(135f, 95f, 2.2f, lens1)
        val lens2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 0.8f; color = Color.parseColor("#73FFFFFF")
        }
        canvas.drawCircle(135f, 100f, 1.4f, lens2)

        canvas.restore() // phone rotation
        canvas.restore() // design scale
    }
}
