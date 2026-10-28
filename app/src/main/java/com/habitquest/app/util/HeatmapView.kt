package com.habitquest.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import com.habitquest.app.R

/** GitHub-style contribution grid: 13 week columns x 7 weekday rows, ending today. */
class HeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val cols = 13
    private val rows = 7
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gap = resources.displayMetrics.density * 4f
    private val emptyColor = context.getColor(R.color.heat_empty)
    private val fullColor = context.getColor(R.color.heat_filled)

    private var counts: Map<Long, Int> = emptyMap()
    private var maxCount = 1
    private var today = DateUtil.today()

    fun setData(counts: Map<Long, Int>, maxCount: Int, today: Long) {
        this.counts = counts
        this.maxCount = if (maxCount < 1) 1 else maxCount
        this.today = today
        invalidate()
    }

    private fun cellSize(width: Int): Float = (width - gap * (cols - 1)) / cols

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val cell = cellSize(width)
        val height = (rows * cell + gap * (rows - 1)).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cell = cellSize(width)
        val radius = cell * 0.25f
        val start = today - DateUtil.weekdayIndex(today) - (cols - 1) * 7L
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val day = start + c * 7L + r
                if (day > today) continue
                val n = counts[day] ?: 0
                paint.color = if (n == 0) {
                    emptyColor
                } else {
                    val alpha = 90 + (165 * n) / maxCount
                    ColorUtils.setAlphaComponent(fullColor, alpha.coerceIn(90, 255))
                }
                val x = c * (cell + gap)
                val y = r * (cell + gap)
                canvas.drawRoundRect(x, y, x + cell, y + cell, radius, radius, paint)
            }
        }
    }
}
