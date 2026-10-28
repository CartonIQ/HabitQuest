package com.habitquest.app.util

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View

fun View.fadeIn(duration: Long = 350L) {
    alpha = 0f
    translationY = 16f * resources.displayMetrics.density
    animate().alpha(1f).translationY(0f).setDuration(duration).start()
}

/** Gentle looping alpha pulse used for the glowing empty-state icons. */
fun View.pulse() {
    val animator = ObjectAnimator.ofFloat(this, "alpha", 0.55f, 1f).apply {
        duration = 1400L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        start()
    }
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            animator.cancel()
        }
    })
}
