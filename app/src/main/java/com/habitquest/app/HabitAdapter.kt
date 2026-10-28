package com.habitquest.app

import android.content.res.ColorStateList
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.habitquest.app.databinding.ItemHabitBinding
import com.habitquest.app.util.Prefs

private object HabitDiff : DiffUtil.ItemCallback<HabitUi>() {
    override fun areItemsTheSame(oldItem: HabitUi, newItem: HabitUi): Boolean =
        oldItem.habit.id == newItem.habit.id

    override fun areContentsTheSame(oldItem: HabitUi, newItem: HabitUi): Boolean =
        oldItem == newItem
}

class HabitAdapter(
    private val onToggle: (HabitUi) -> Unit,
    private val onLongClick: (HabitUi) -> Unit
) : ListAdapter<HabitUi, HabitAdapter.VH>(HabitDiff) {

    class VH(val b: ItemHabitBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.b
        val ctx = b.root.context

        b.tvEmoji.text = item.habit.emoji
        b.tvEmoji.backgroundTintList =
            ColorStateList.valueOf(ColorUtils.setAlphaComponent(item.habit.color, 70))

        b.tvTitle.text = item.habit.title
        b.tvCategoryBadge.text = item.habit.category
        b.tvXpBadge.text = "+${item.habit.xpReward} XP"

        if (item.streak > 0) {
            b.tvStreak.text = "\uD83D\uDD25 " + item.streak + " day streak"
            b.tvStreak.setTextColor(ctx.getColor(R.color.fire_orange))
        } else {
            b.tvStreak.text = "Start streak today"
            b.tvStreak.setTextColor(ctx.getColor(R.color.text_secondary))
        }

        applyState(b, item.completedToday, false)

        b.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val current = getItem(pos)

            if (Prefs.hapticsEnabled(ctx)) {
                b.root.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }

            b.root.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).withEndAction {
                b.root.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }.start()

            applyState(b, !current.completedToday, true)
            onToggle(current)
        }

        b.root.setOnLongClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onLongClick(getItem(pos))
            }
            true
        }
    }

    private fun applyState(b: ItemHabitBinding, done: Boolean, animate: Boolean) {
        b.checkContainer.setBackgroundResource(
            if (done) R.drawable.bg_check_on else R.drawable.bg_check_off
        )
        b.ivCheck.animate().cancel()
        if (done) {
            b.ivCheck.visibility = View.VISIBLE
            if (animate) {
                b.ivCheck.scaleX = 0f
                b.ivCheck.scaleY = 0f
                b.ivCheck.alpha = 0f
                b.ivCheck.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(350)
                    .setInterpolator(OvershootInterpolator(2.5f))
                    .start()
            } else {
                b.ivCheck.scaleX = 1f
                b.ivCheck.scaleY = 1f
                b.ivCheck.alpha = 1f
            }
        } else {
            b.ivCheck.visibility = View.INVISIBLE
        }
    }
}
