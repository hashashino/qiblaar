package com.hashashino.qiblaar.views

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
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Compass disk — redesigned per spec in compass-disk.jsx.
 * Rotates the full dial (ticks + cardinals + Qibla pointer) against azimuth
 * so north always points up. Heading indicator is fixed at top.
 */
class CompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var azimuth: Float = 0f
        set(value) { field = value; invalidate() }

    var qiblaBearing: Float = 0f
        set(value) { field = value; invalidate() }

    /** `true` = compass UNRELIABLE (desaturate Qibla pointer to gray) */
    var accuracyLow: Boolean = false
        set(value) { field = value; invalidate() }

    /** `true` = severe interference or wrong pose: dim the entire disk */
    var dimmed: Boolean = false
        set(value) { field = value; invalidate() }

    /** `true` = direction pre-set — show "PRE-SET DIRECTION" badge in center */
    var locked: Boolean = false
        set(value) { field = value; invalidate() }

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    // Paints (stateless style; colors updated in onDraw)
    private val diskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = Color.parseColor("#0FFFFFFF")
    }
    private val innerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = Color.parseColor("#0DFFFFFF")
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val cardinalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val qiblaLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(4f)
    }
    private val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val badgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    // Hub
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0E0F22")
    }
    private val hubStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = Color.parseColor("#66FFFFFF")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        // Scale r down ~8% so the Kaaba circle that extends past ringR stays within View bounds
        val r = min(cx, cy) * 0.92f
        val ringR = r * 0.93f
        val tickOuter = r * 0.86f

        val opacity = if (dimmed) 0.35f else 1f
        canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), (opacity * 255).toInt())

        // Disk with radial gradient
        diskPaint.shader = RadialGradient(
            cx, cy * 0.84f, r * 0.62f,
            intArrayOf(Color.parseColor("#22264E"), Color.parseColor("#171935"), Color.parseColor("#0E0F22")),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, ringR, diskPaint)
        canvas.drawCircle(cx, cy, ringR, ringPaint)
        canvas.drawCircle(cx, cy, r * 0.74f, innerRingPaint)

        // Rotating dial group
        canvas.save()
        canvas.rotate(-azimuth, cx, cy)

        // Qibla line — Kaaba circle sits just outside the outer ring so it overlays the heading pointer on alignment
        val qibRad = Math.toRadians((qiblaBearing - 90.0))
        val qibTipX = cx + cos(qibRad).toFloat() * r * 0.97f
        val qibTipY = cy + sin(qibRad).toFloat() * r * 0.97f

        // Alignment check: green when facing Qibla (|diff| ≤ 5°), red otherwise, gray if inaccurate
        var qibDiff = qiblaBearing - azimuth
        qibDiff = ((qibDiff + 540) % 360) - 180
        val isAligned = Math.abs(qibDiff) <= 5f
        val qiblaColor = when {
            accuracyLow -> Color.parseColor("#9CA3AF")
            isAligned   -> Color.parseColor("#22c55e")
            else        -> Color.parseColor("#EF4444")
        }
        val glowInner = when {
            accuracyLow -> Color.parseColor("#209CA3AF")
            isAligned   -> Color.parseColor("#8C22c55e")
            else        -> Color.parseColor("#8CEF4444")
        }
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                qibTipX, qibTipY, r * 0.22f,
                intArrayOf(glowInner, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(qibTipX, qibTipY, r * 0.22f, glowPaint)

        // Tick marks
        for (i in 0 until 360 step 5) {
            val major = i % 90 == 0
            val med = i % 30 == 0
            val small = i % 10 == 0
            val len = when {
                major -> r * 0.16f
                med -> r * 0.12f
                small -> r * 0.07f
                else -> r * 0.04f
            }
            tickPaint.strokeWidth = when {
                major -> dp(2.5f)
                med -> dp(1.8f)
                else -> dp(1f)
            }
            tickPaint.color = when {
                major -> Color.WHITE
                small -> Color.parseColor("#99FFFFFF")
                else -> Color.parseColor("#47FFFFFF")
            }
            val rad = Math.toRadians((i - 90.0))
            val x1 = cx + cos(rad).toFloat() * tickOuter
            val y1 = cy + sin(rad).toFloat() * tickOuter
            val x2 = cx + cos(rad).toFloat() * (tickOuter - len)
            val y2 = cy + sin(rad).toFloat() * (tickOuter - len)
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        // Cardinal labels
        val cardinals = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
        cardinalPaint.textSize = r * 0.14f
        for ((label, angle) in cardinals) {
            val rad = Math.toRadians((angle - 90.0))
            val tr = r * 0.62f
            val x = cx + cos(rad).toFloat() * tr
            val y = cy + sin(rad).toFloat() * tr + cardinalPaint.textSize * 0.36f
            cardinalPaint.color = if (label == "N") Color.parseColor("#EF4444") else Color.WHITE
            cardinalPaint.textSize = if (label == "N") r * 0.155f else r * 0.125f
            canvas.drawText(label, x, y, cardinalPaint)
        }

        // Qibla arrow line from center to tip
        qiblaLinePaint.color = qiblaColor
        canvas.drawLine(cx, cy, qibTipX, qibTipY, qiblaLinePaint)

        // Kaaba glyph circle at tip — stylised cube matching the AR marker
        val kaabaCircleR = r * 0.10f
        val gold = Color.parseColor("#D4A35E")
        val nearBlack = Color.argb(242, 8, 10, 20)

        // Outer ring
        val kaabaRingFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = nearBlack }
        val kaabaRingStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = dp(1.5f); color = qiblaColor
        }
        canvas.drawCircle(qibTipX, qibTipY, kaabaCircleR, kaabaRingFill)
        canvas.drawCircle(qibTipX, qibTipY, kaabaCircleR, kaabaRingStroke)

        // Cube body — same proportions as AR marker
        val kw = kaabaCircleR * 0.50f
        val kh = kaabaCircleR * 0.62f
        val bodyFillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = nearBlack }
        canvas.drawRect(RectF(qibTipX - kw, qibTipY - kh, qibTipX + kw, qibTipY + kh), bodyFillP)

        // Gold kiswa band — top 30% of body
        val bandBottom = qibTipY - kh + kh * 2f * 0.30f
        val bandFillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = gold }
        canvas.drawRect(RectF(qibTipX - kw, qibTipY - kh, qibTipX + kw, bandBottom), bandFillP)

        // Gold body outline
        val bodyStrokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = dp(0.6f); color = gold
        }
        canvas.drawRect(RectF(qibTipX - kw, qibTipY - kh, qibTipX + kw, qibTipY + kh), bodyStrokeP)

        // Arched door — centered, flush with base
        val dw = kw * 0.52f; val dh = kh * 0.50f
        val doorFillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = nearBlack }
        val doorStrokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = dp(0.5f); color = gold
        }
        val doorRect = RectF(qibTipX - dw, qibTipY + kh - dh, qibTipX + dw, qibTipY + kh)
        canvas.drawRoundRect(doorRect, dp(1.5f), dp(1.5f), doorFillP)
        canvas.drawRoundRect(doorRect, dp(1.5f), dp(1.5f), doorStrokeP)

        canvas.restore()

        // Fixed heading indicator at top (doesn't rotate)
        // When aligned: large bold green chevron so even elderly users can't miss it
        if (isAligned) {
            val tipY  = cy - ringR + dp(3f)           // apex, just inside ring edge
            val baseY = tipY - r * 0.12f              // tall enough to be obvious
            val halfW = r * 0.14f
            val greenFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.parseColor("#22c55e")
            }
            val alignedHead = Path()
            alignedHead.moveTo(cx, tipY)
            alignedHead.lineTo(cx - halfW, baseY)
            alignedHead.lineTo(cx + halfW, baseY)
            alignedHead.close()
            canvas.drawPath(alignedHead, greenFill)
        } else {
            val headingPath = Path()
            headingPath.moveTo(cx, cy - ringR + dp(2f))
            headingPath.lineTo(cx - r * 0.05f, cy - ringR + dp(2f) - r * 0.045f)
            headingPath.lineTo(cx + r * 0.05f, cy - ringR + dp(2f) - r * 0.045f)
            headingPath.close()
            canvas.drawPath(headingPath, headingPaint)
        }

        // Center hub
        canvas.drawCircle(cx, cy, r * 0.055f, hubPaint)
        canvas.drawCircle(cx, cy, r * 0.055f, hubStrokePaint)

        // Pre-set locked badge (drawn after hub, still inside opacity layer)
        if (locked) {
            val badgeText = "PRE-SET DIRECTION"
            badgeTextPaint.textSize = dp(9f)
            val textW = badgeTextPaint.measureText(badgeText)
            val badgePadH = dp(5f)
            val badgePadV = dp(3f)
            val badgeW = textW + badgePadH * 2
            val badgeH = badgeTextPaint.textSize + badgePadV * 2
            val badgeTop = cy + r * 0.24f
            val badgeRect = RectF(cx - badgeW / 2, badgeTop, cx + badgeW / 2, badgeTop + badgeH)
            badgePaint.color = Color.parseColor("#2922c55e")
            badgeStrokePaint.color = Color.parseColor("#5922c55e")
            canvas.drawRoundRect(badgeRect, dp(999f), dp(999f), badgePaint)
            canvas.drawRoundRect(badgeRect, dp(999f), dp(999f), badgeStrokePaint)
            badgeTextPaint.color = Color.parseColor("#6EE7A0")
            canvas.drawText(badgeText, cx, badgeTop + badgePadV + badgeTextPaint.textSize * 0.85f, badgeTextPaint)
        }

        canvas.restore()
    }
}
