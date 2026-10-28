package com.habitquest.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.habitquest.app.R

/** Simple bar chart of completion percentages (0..100). The last bar is highlighted. */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var values: List<Int> = List(7) { 0 }
    private var labels: List<String> = List(7) { "" }

    init {
        trackPaint.color = context.getColor(R.color.heat_empty)
        valuePaint.color = context.getColor(R.color.text_main)
        valuePaint.textSize = 11f * density
        valuePaint.textAlign = Paint.Align.CENTER
        valuePaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        labelPaint.color = context.getColor(R.color.text_secondary)
        labelPaint.textSize = 12f * density
        labelPaint.textAlign = Paint.Align.CENTER
    }

    fun setData(values: List<Int>, labels: List<String>) {
        this.values = values
        this.labels = labels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val n = values.size
        if (n == 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        val labelArea = 12f * density + 10f * density
        val topArea = 18f * density
        val chartH = h - labelArea - topArea
        val slot = w / n
        val barW = slot * 0.55f
        val radius = barW / 2f
        val baseY = h - labelArea
        for (i in 0 until n) {
            val v = values[i].coerceIn(0, 100)
            val cx = i * slot + slot / 2f
            val left = cx - barW / 2f
            val right = cx + barW / 2f
            canvas.drawRoundRect(left, baseY - chartH, right, baseY, radius, radius, trackPaint)
            val barH = maxOf(6f * density, chartH * v / 100f)
            barPaint.color = if (i == n - 1) context.getColor(R.color.terracotta) else context.getColor(R.color.sage_green)
            canvas.drawRoundRect(left, baseY - barH, right, baseY, radius, radius, barPaint)
            canvas.drawText("$v%", cx, baseY - chartH - 4f * density, valuePaint)
            val label = if (i < labels.size) labels[i] else ""
            canvas.drawText(label, cx, h - 4f * density, labelPaint)
        }
    }
}
