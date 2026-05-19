package com.hashashino.qiblaar.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

class ArOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var qiblaBearing: Float = 0f
        set(value) { field = value; invalidate(); notifyAlignmentChange() }

    var deviceAzimuth: Float = 0f
        set(value) { field = value; invalidate(); notifyAlignmentChange() }

    var onAlignmentChanged: ((aligned: Boolean) -> Unit)? = null
    private var lastAlignedNotified = false

    private fun notifyAlignmentChange() {
        val nowAligned = arState == ArState.ALIGNED
        if (nowAligned != lastAlignedNotified) {
            lastAlignedNotified = nowAligned
            onAlignmentChanged?.invoke(nowAligned)
        }
    }

    private enum class ArState { ALIGNED, CLOSE, FAR, BEHIND }

    private val angleDiff: Float
        get() {
            var d = qiblaBearing - deviceAzimuth
            d = ((d + 540) % 360) - 180
            return d
        }

    private val arState: ArState
        get() = when {
            abs(angleDiff) <= 5f  -> ArState.ALIGNED
            abs(angleDiff) >= 150f -> ArState.BEHIND
            abs(angleDiff) <= 30f -> ArState.CLOSE
            else                  -> ArState.FAR
        }

    private val stateColor: Int
        get() = when (arState) {
            ArState.ALIGNED           -> Color.parseColor("#22c55e")
            ArState.CLOSE             -> Color.parseColor("#F59E0B")
            ArState.FAR, ArState.BEHIND -> Color.parseColor("#EF4444")
        }

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    // Kaaba marker
    private val markerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val kaabaCircleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#D90A0C1C")
    }
    private val kaabaCircleStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
    }
    private val kaabaGlyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.8f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Label paint reused for compass needle text and chips
    private val sweepTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    // Distance pill
    private val pillFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val pillStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    var distanceKm: Float = 6420f
        set(value) { field = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val color = stateColor

        when (arState) {
            ArState.ALIGNED -> {
                drawKaabaMarker(canvas, cx, cy * 0.78f, color, aligned = true)
                drawDistancePill(canvas, cx, cy * 0.78f + dp(100f), aligned = true)
            }
            else -> {
                drawCompassNeedle(canvas, cx, cy * 0.78f)
            }
        }
    }

    private fun drawKaabaMarker(
        canvas: Canvas, cx: Float, cy: Float, accentColor: Int,
        aligned: Boolean, dim: Boolean = false
    ) {
        val bracketR = dp(90f)
        val alpha = if (dim) 76 else 255

        // Radial glow
        markerGlowPaint.shader = RadialGradient(
            cx, cy, bracketR,
            if (aligned) Color.argb((alpha * 0.45f).toInt(), 34, 197, 94)
            else         Color.argb((alpha * 0.15f).toInt(), 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, bracketR, markerGlowPaint)

        // Corner brackets
        val bc = if (dim) Color.argb(alpha, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)) else accentColor
        bracketPaint.color = bc
        val bInset = bracketR * 0.44f
        val bLen   = bracketR * 0.22f
        val brackets = listOf(
            // top-left
            Triple(cx - bInset, cy - bInset, floatArrayOf(0f, bLen, bLen, 0f)),
            // top-right
            Triple(cx + bInset, cy - bInset, floatArrayOf(0f, -bLen, bLen, 0f)),
            // bottom-left
            Triple(cx - bInset, cy + bInset, floatArrayOf(0f, bLen, -bLen, 0f)),
            // bottom-right
            Triple(cx + bInset, cy + bInset, floatArrayOf(0f, -bLen, -bLen, 0f)),
        )
        for ((bx, by, d) in brackets) {
            val path = Path()
            // d = [dxCorner, dxOuter, dyCorner, dyOuter]
            // Actually encode as: [hDir, hLen, vDir, vLen] corner outward
            val hLen = d[1]; val vLen = d[3]
            path.moveTo(bx + hLen, by)
            path.lineTo(bx, by)
            path.lineTo(bx, by + vLen)
            canvas.drawPath(path, bracketPaint)
        }

        // Center circle (background disk)
        val circR = dp(24f)
        kaabaCircleFill.color = Color.argb((alpha * 0.92f).toInt(), 8, 10, 20)
        kaabaCircleStroke.color = bc
        canvas.drawCircle(cx, cy, circR, kaabaCircleFill)
        canvas.drawCircle(cx, cy, circR, kaabaCircleStroke)

        // ── Kaaba illustration ────────────────────────────────────────────────
        // Black cube body with gold kiswa band + arched door.
        // Dimensions chosen so corners sit at ~80% of circR (safely inside the ring).
        val kw = circR * 0.50f      // half-width  (~12dp)
        val kh = circR * 0.62f      // half-height (~15dp), portrait like the real building
        val gold = Color.argb(alpha, 212, 163, 94)          // warm gold #d4a35e
        val dark = Color.argb((alpha * 0.95f).toInt(), 8, 10, 20)

        // Body fill (near-black)
        val bodyFillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = dark
        }
        canvas.drawRect(RectF(cx - kw, cy - kh, cx + kw, cy + kh), bodyFillP)

        // Gold kiswa band — top 30% of body height
        val bandBottom = cy - kh + kh * 2f * 0.30f
        val bandFillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = gold
        }
        canvas.drawRect(RectF(cx - kw, cy - kh, cx + kw, bandBottom), bandFillP)

        // Body outline in gold
        val bodyStrokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(0.9f)
            color = gold
        }
        canvas.drawRect(RectF(cx - kw, cy - kh, cx + kw, cy + kh), bodyStrokeP)

        // Arched door — centered, flush with base, slight top-radius for arch shape
        val dw = kw * 0.52f
        val dh = kh * 0.50f
        val doorFillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = dark
        }
        val doorStrokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(0.8f)
            color = gold
        }
        val doorRect = RectF(cx - dw, cy + kh - dh, cx + dw, cy + kh)
        canvas.drawRoundRect(doorRect, dp(2f), dp(2f), doorFillP)
        canvas.drawRoundRect(doorRect, dp(2f), dp(2f), doorStrokeP)

        // Aligned chevron — large downward-pointing V above the brackets
        if (aligned) {
            val chevApexY = cy - bracketR - dp(10f)
            val chevBaseY  = chevApexY + dp(26f)
            val chevHalfW  = dp(30f)
            val chevPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = dp(7f)
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = Color.parseColor("#22c55e")
            }
            val chevPath = Path()
            chevPath.moveTo(cx - chevHalfW, chevBaseY)
            chevPath.lineTo(cx, chevApexY)
            chevPath.lineTo(cx + chevHalfW, chevBaseY)
            canvas.drawPath(chevPath, chevPaint)
        }
    }

    private fun drawCompassNeedle(canvas: Canvas, cx: Float, cy: Float) {
        val r = dp(72f)
        val red = Color.parseColor("#EF4444")

        // Background disk
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(180, 8, 10, 20)
        }
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1.5f)
            color = Color.argb(60, 255, 255, 255)
        }
        canvas.drawCircle(cx, cy, r, bgPaint)
        canvas.drawCircle(cx, cy, r, ringPaint)

        // Fixed forward marker at 12 o'clock (does not rotate) — small white triangle
        val fwdPath = Path()
        fwdPath.moveTo(cx, cy - r + dp(4f))
        fwdPath.lineTo(cx - dp(5f), cy - r + dp(13f))
        fwdPath.lineTo(cx + dp(5f), cy - r + dp(13f))
        fwdPath.close()
        val fwdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(160, 255, 255, 255)
        }
        canvas.drawPath(fwdPath, fwdPaint)

        // Rotating needle — points toward Qibla (up = aligned)
        canvas.save()
        canvas.rotate(angleDiff, cx, cy)

        val needleLen = r * 0.72f
        val tailLen   = r * 0.32f

        // Needle body
        val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(4f)
            strokeCap = Paint.Cap.ROUND
            color = red
        }
        canvas.drawLine(cx, cy + tailLen, cx, cy - needleLen, needlePaint)

        // Arrowhead at needle tip
        val tipY = cy - needleLen
        val headPath = Path()
        headPath.moveTo(cx - dp(9f), tipY + dp(16f))
        headPath.lineTo(cx, tipY)
        headPath.lineTo(cx + dp(9f), tipY + dp(16f))
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(4f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = red
        }
        canvas.drawPath(headPath, headPaint)

        // Tail dot
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(120, 239, 68, 68)
        }
        canvas.drawCircle(cx, cy + tailLen, dp(4f), dotPaint)

        canvas.restore()

        // Degrees label below the disk
        sweepTextPaint.textSize = dp(12f)
        sweepTextPaint.color = Color.parseColor("#FCA5A5")
        val deg = abs(angleDiff.toInt())
        val dir = if (angleDiff > 0) "Turn right  $deg°" else "Turn left  $deg°"
        canvas.drawText(dir, cx, cy + r + dp(22f), sweepTextPaint)
    }

    private fun drawDistancePill(canvas: Canvas, cx: Float, y: Float, aligned: Boolean) {
        val dist = "%.0f km".format(distanceKm)
        // Arabic + distance
        val text = "ﺍﻟْﻜَﻌْﺒَﺔُ · $dist"
        pillTextPaint.textSize = dp(11.5f)
        val tw = pillTextPaint.measureText(text)
        val ph = dp(28f)
        val pw = tw + dp(24f)
        val rect = RectF(cx - pw / 2, y - ph / 2, cx + pw / 2, y + ph / 2)
        if (aligned) {
            pillFill.color = Color.parseColor("#2E22c55e")
            pillStroke.color = Color.parseColor("#7322c55e")
            pillTextPaint.color = Color.parseColor("#86EFAC")
        } else {
            pillFill.color = Color.parseColor("#B20A0C1C")
            pillStroke.color = Color.parseColor("#1AFFFFFF")
            pillTextPaint.color = Color.WHITE
        }
        canvas.drawRoundRect(rect, dp(999f), dp(999f), pillFill)
        canvas.drawRoundRect(rect, dp(999f), dp(999f), pillStroke)
        canvas.drawText(text, cx, y + pillTextPaint.textSize * 0.35f, pillTextPaint)
    }
}
