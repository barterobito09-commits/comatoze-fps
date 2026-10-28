package com.ultrapower.fps

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import kotlin.math.ceil

/**
 * Pill gelap transparan ala gaming: bar aksen berwarna, angka FPS tebal,
 * dan grafik mini 15 detik terakhir. Warna aksen mengikuti angka FPS.
 */
class FpsView(ctx: Context) : View(ctx) {

    companion object {
        const val CAP = 30 // jumlah titik grafik
        private val GREEN = Color.parseColor("#00E58B")
        private val AMBER = Color.parseColor("#FFC233")
        private val RED = Color.parseColor("#FF4D5E")
        private val GREY = Color.parseColor("#8A94A6")
    }

    private val d = ctx.resources.displayMetrics.density
    private var scale = 1f
    private var graph = true
    private var fps = -1
    private val hist = FloatArray(CAP)
    private var n = 0

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#CC0B0F14") }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val num = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        color = Color.WHITE
    }
    private val lbl = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        color = GREY
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val base = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#26FFFFFF") }
    private val path = Path()
    private val rect = RectF()

    init {
        applyMetrics()
    }

    private fun u(v: Float) = v * d * scale

    private fun applyMetrics() {
        num.textSize = u(20f)
        lbl.textSize = u(9f)
        edge.strokeWidth = u(1f)
        line.strokeWidth = u(1.6f)
        base.strokeWidth = u(1f)
        requestLayout()
        invalidate()
    }

    fun configure(scalePercent: Int, showGraph: Boolean) {
        scale = scalePercent / 100f
        graph = showGraph
        applyMetrics()
    }

    fun setFps(value: Int, record: Boolean) {
        fps = value
        if (record && value >= 0) {
            if (n == CAP) {
                System.arraycopy(hist, 1, hist, 0, CAP - 1)
                hist[CAP - 1] = value.toFloat()
            } else {
                hist[n++] = value.toFloat()
            }
        }
        invalidate()
    }

    private fun accent() = when {
        fps < 0 -> GREY
        fps >= 55 -> GREEN
        fps >= 30 -> AMBER
        else -> RED
    }

    private fun numW() = num.measureText("000")

    private fun contentW(): Float {
        var w = u(10f) + u(3f) + u(8f) + numW() + u(4f) + lbl.measureText("FPS") + u(10f)
        if (graph) w += u(8f) + u(44f)
        return w
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(ceil(contentW().toDouble()).toInt(), u(34f).toInt())
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val a = accent()
        val r = u(12f)

        rect.set(0f, 0f, w, h)
        c.drawRoundRect(rect, r, r, bg)

        edge.color = (a and 0x00FFFFFF) or 0x59000000
        val half = edge.strokeWidth / 2f
        rect.set(half, half, w - half, h - half)
        c.drawRoundRect(rect, r, r, edge)

        var x = u(10f)
        bar.color = a
        rect.set(x, u(7f), x + u(3f), h - u(7f))
        c.drawRoundRect(rect, u(1.5f), u(1.5f), bar)
        x += u(3f) + u(8f)

        val txt = if (fps < 0) "--" else fps.toString()
        val fm = num.fontMetrics
        val by = h / 2f - (fm.ascent + fm.descent) / 2f
        c.drawText(txt, x + numW() - num.measureText(txt), by, num)
        x += numW() + u(4f)
        c.drawText("FPS", x, by, lbl)

        if (graph) {
            x += lbl.measureText("FPS") + u(8f)
            val gw = u(44f)
            val top = u(9f)
            val gh = h - u(18f)
            c.drawLine(x, top + gh, x + gw, top + gh, base)
            if (n >= 2) {
                var mx = 60f
                for (i in 0 until n) if (hist[i] > mx) mx = hist[i]
                path.reset()
                for (i in 0 until n) {
                    val px = x + gw * (CAP - n + i) / (CAP - 1)
                    val py = top + gh - gh * (hist[i] / mx)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                line.color = a
                c.drawPath(path, line)
            }
        }
    }
}
