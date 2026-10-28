package com.habitquest.app.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.habitquest.app.R
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Particle(
        var x: Float,
        var y: Float,
        val vx: Float,
        val vy: Float,
        val size: Float,
        val color: Int,
        var rotation: Float,
        val spin: Float
    )

    private val particles = ArrayList<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null
    private var lastNs = 0L

    fun start() {
        if (width == 0 || height == 0) return
        val density = resources.displayMetrics.density
        val colors = intArrayOf(
            context.getColor(R.color.terracotta),
            context.getColor(R.color.sage_green),
            context.getColor(R.color.warm_yellow),
            context.getColor(R.color.fire_orange)
        )
        particles.clear()
        repeat(140) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * width,
                    y = -Random.nextFloat() * height * 0.4f,
                    vx = (Random.nextFloat() - 0.5f) * 160f * density,
                    vy = (200f + Random.nextFloat() * 300f) * density,
                    size = (4f + Random.nextFloat() * 6f) * density,
                    color = colors[Random.nextInt(colors.size)],
                    rotation = Random.nextFloat() * 360f,
                    spin = (Random.nextFloat() - 0.5f) * 720f
                )
            )
        }
        animator?.cancel()
        lastNs = System.nanoTime()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3200L
            interpolator = LinearInterpolator()
            addUpdateListener {
                val now = System.nanoTime()
                val dt = (now - lastNs) / 1_000_000_000f
                lastNs = now
                for (p in particles) {
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.rotation += p.spin * dt
                }
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    particles.clear()
                    invalidate()
                }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (p in particles) {
            paint.color = p.color
            canvas.save()
            canvas.rotate(p.rotation, p.x, p.y)
            canvas.drawRect(p.x - p.size / 2f, p.y - p.size / 4f, p.x + p.size / 2f, p.y + p.size / 4f, paint)
            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
