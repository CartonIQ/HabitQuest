package com.habitquest.app

import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.habitquest.app.ads.AdManager
import com.habitquest.app.data.HabitEntity
import com.habitquest.app.databinding.DialogAddHabitBinding
import com.habitquest.app.databinding.FragmentTodayBinding
import com.habitquest.app.rpg.LevelProgress
import com.habitquest.app.util.DateUtil
import com.habitquest.app.util.Prefs
import com.habitquest.app.util.fadeIn
import com.habitquest.app.util.pulse

class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private val vm: TodayViewModel by viewModels()
    private lateinit var adapter: HabitAdapter
    private var freezeChecked = false
    private var listAnimated = false
    private var lastAllDone: Boolean? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
                binding.tvDate.text = DateUtil.formatToday()
        binding.emptyGlow.pulse()

        adapter = HabitAdapter(
            onToggle = { item -> vm.toggle(item) },
            onLongClick = { item -> showHabitMenu(item) }
        )

        binding.habitsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.habitsRecycler.adapter = adapter
        (binding.habitsRecycler.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        attachSwipeToDelete()

        binding.fabAddHabit.setOnClickListener { showHabitDialog(null) }

        // Category filter chips
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull()
            val cat = when (id) {
                R.id.filter_health -> "Health"
                R.id.filter_focus -> "Focus & Study"
                R.id.filter_fitness -> "Fitness"
                R.id.filter_mind -> "Mindfulness"
                R.id.filter_routine -> "Routine"
                else -> "All"
            }
            vm.setCategory(cat)
        }

        // Observe RPG Progress
        vm.levelProgress.observe(viewLifecycleOwner) { p ->
            binding.tvRankEmoji.text = p.rankEmoji
            binding.tvLevelTitle.text = "Level ${p.level} \u00b7 ${p.rankTitle}"
            binding.tvLevelXp.text = "${p.currentXp} / ${p.nextLevelXp} XP (${p.progressPercent}%)"
            binding.tvLevelBadge.text = "LVL ${p.level}"
            binding.xpProgressBar.progress = p.progressPercent
        }

        // Level-Up Celebration
        vm.levelUpEvent.observe(viewLifecycleOwner) { p ->
            if (p != null) {
                binding.confettiView.start()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("🎉 Level Up! Level ${p.level}")
                    .setMessage("Congratulations! You have attained the rank of ${p.rankTitle} ${p.rankEmoji}.\nKeep crushing your daily quests!")
                    .setPositiveButton("Huzzah!", null)
                    .show()
                vm.clearLevelUp()
            }
        }

        // Habit list observation
        vm.items.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list) {
                if (!listAnimated && list.isNotEmpty()) {
                    listAnimated = true
                    val anim = AnimationUtils.loadLayoutAnimation(
                        requireContext(),
                        R.anim.layout_slide_up
                    )
                    binding.habitsRecycler.layoutAnimation = anim
                    binding.habitsRecycler.startLayoutAnimation()
                }
            }

            val total = list.size
            val done = list.count { it.completedToday }
            val hasHabits = total > 0

            binding.emptyState.visibility = if (hasHabits) View.GONE else View.VISIBLE
            binding.progressCard.visibility = if (hasHabits) View.VISIBLE else View.GONE

            if (hasHabits) {
                val pct = if (total == 0) 0 else (done * 100) / total
                binding.progressBar.setProgressCompat(pct, true)
                binding.tvProgress.text = "$done / $total done ($pct%)"
                binding.tvGreeting.text = when {
                    done == total -> "All daily quests fulfilled! ⚔️✨"
                    done > 0 -> "Making solid progress! Keep pushing 🛡️"
                    else -> "New quests await your discipline today! 🏹"
                }

                val allDone = done == total
                if (lastAllDone == false && allDone) {
                    binding.confettiView.start()
                }
                lastAllDone = allDone
            }

            if (!freezeChecked && list.isNotEmpty()) {
                freezeChecked = true
                checkStreakFreeze(list)
            }
        }

        // Milestone celebration
        vm.milestone.observe(viewLifecycleOwner) { streak ->
            streak ?: return@observe
            binding.confettiView.start()
            showMilestoneDialog(streak)
            vm.clearMilestone()
        }

        // Undo delete snackbar
        vm.deletedEvent.observe(viewLifecycleOwner) { deleted ->
            deleted ?: return@observe
            Snackbar.make(
                binding.root,
                "Deleted ${deleted.habit.emoji} ${deleted.habit.title}",
                Snackbar.LENGTH_LONG
            ).setAction("Undo") {
                vm.restoreHabit(deleted)
            }.show()
            vm.clearDeleted()
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    private fun showMilestoneDialog(streak: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🔥 $streak-Day Streak!")
            .setMessage("Incredible quest dedication! You've maintained your habit streak for $streak consecutive days! +50 Bonus XP.")
            .setPositiveButton("Onward!", null)
            .setOnDismissListener {
                if (_binding != null && isAdded && !Prefs.isAdFreeMode(requireContext())) {
                    AdManager.showInterstitial(requireActivity())
                }
            }
            .show()
    }

    private fun checkStreakFreeze(list: List<HabitUi>) {
        val risky = list.firstOrNull { it.atRisk } ?: return
        val today = DateUtil.today()
        if (Prefs.lastFreezePromptDay(requireContext()) == today) return
        Prefs.setLastFreezePromptDay(requireContext(), today)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🧊 Streak at Risk")
            .setMessage(
                "Your streak for ${risky.habit.emoji} ${risky.habit.title} is at risk!\nFreeze it now to protect your discipline record."
            )
            .setPositiveButton("Freeze Streak ❄️") { _, _ ->
                vm.freeze(risky.habit.id)
                Toast.makeText(requireContext(), "Streak successfully frozen! ❄️", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("I'll do it today", null)
            .show()
    }

    private fun showHabitMenu(item: HabitUi) {
        val options = arrayOf("Edit Quest", "Freeze Streak ❄️", "Archive Habit", "Delete Quest")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${item.habit.emoji} ${item.habit.title}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showHabitDialog(item.habit)
                    1 -> {
                        vm.freeze(item.habit.id)
                        Toast.makeText(requireContext(), "Streak frozen for yesterday! ❄️", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        vm.archiveHabit(item.habit.id)
                        Toast.makeText(requireContext(), "Habit archived.", Toast.LENGTH_SHORT).show()
                    }
                    3 -> vm.deleteHabit(item.habit)
                }
            }
            .show()
    }

    private fun attachSwipeToDelete() {
        val density = resources.displayMetrics.density
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = requireContext().getColor(R.color.terracotta)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = requireContext().getColor(R.color.white)
            textSize = 14f * density
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    vm.deleteHabit(adapter.currentList[pos].habit)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val v = viewHolder.itemView
                if (dX < 0f) {
                    val rect = RectF(v.right + dX, v.top.toFloat(), v.right.toFloat(), v.bottom.toFloat())
                    c.drawRoundRect(rect, 20f * density, 20f * density, bgPaint)
                    c.drawText(
                        "Delete",
                        v.right - 24f * density,
                        v.top + v.height / 2f + textPaint.textSize / 3f,
                        textPaint
                    )
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.habitsRecycler)
    }

    private fun showHabitDialog(existing: HabitEntity?) {
        val ctx = requireContext()
        val b = DialogAddHabitBinding.inflate(layoutInflater)
        val density = resources.displayMetrics.density

        val palette = intArrayOf(
            0xFFE07A5F.toInt(), // Terracotta
            0xFF81B29A.toInt(), // Sage Green
            0xFFF2CC8F.toInt(), // Warm Gold
            0xFF6C8EBF.toInt(), // Steel Blue
            0xFFB58DB6.toInt(), // Lavender
            0xFFE76F51.toInt(), // Coral
            0xFF2A9D8F.toInt(), // Emerald
            0xFF457B9D.toInt()  // Deep Ocean
        )

        var selectedColor = existing?.color ?: palette[0]
        if (!palette.contains(selectedColor)) selectedColor = palette[0]
        var hour = existing?.reminderHour ?: 8
        var minute = existing?.reminderMinute ?: 0
        var selectedCategory = existing?.category ?: "General"

        if (existing != null) {
            b.tvDialogTitle.text = "Edit Quest Habit"
            b.btnSave.text = "Save Changes"
            b.inputTitle.setText(existing.title)
            b.inputEmoji.setText(existing.emoji)
            b.switchReminder.isChecked = existing.reminderEnabled
            when (existing.category) {
                "Health" -> b.chipHealth.isChecked = true
                "Focus & Study" -> b.chipFocus.isChecked = true
                "Fitness" -> b.chipFitness.isChecked = true
                "Mindfulness" -> b.chipMind.isChecked = true
                "Routine" -> b.chipRoutine.isChecked = true
                else -> b.chipGeneral.isChecked = true
            }
        }

        b.chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategory = when (checkedIds.firstOrNull()) {
                R.id.chip_health -> "Health"
                R.id.chip_focus -> "Focus & Study"
                R.id.chip_fitness -> "Fitness"
                R.id.chip_mind -> "Mindfulness"
                R.id.chip_routine -> "Routine"
                else -> "General"
            }
        }

        b.btnPickTime.isEnabled = b.switchReminder.isChecked

        // Color swatches
        val swatches = ArrayList<View>()
        fun refreshSwatches() {
            swatches.forEachIndexed { i, v ->
                val selected = palette[i] == selectedColor
                v.scaleX = if (selected) 1.25f else 0.85f
                v.scaleY = if (selected) 1.25f else 0.85f
                v.alpha = if (selected) 1f else 0.5f
            }
        }

        for (c in palette) {
            val v = View(ctx)
            val size = (38 * density).toInt()
            val lp = LinearLayout.LayoutParams(size, size)
            lp.marginEnd = (10 * density).toInt()
            v.layoutParams = lp
            v.setBackgroundResource(R.drawable.bg_circle_icon)
            v.backgroundTintList = ColorStateList.valueOf(c)
            v.setOnClickListener {
                selectedColor = c
                refreshSwatches()
            }
            b.colorContainer.addView(v)
            swatches.add(v)
        }
        refreshSwatches()

        // Quick emoji picks
        val emojis = listOf("🌱", "💧", "📚", "🏃", "🧘", "💤", "🥗", "✍️", "🎸", "🎯", "🛡️", "🔥")
        for (e in emojis) {
            val tv = TextView(ctx)
            tv.text = e
            tv.textSize = 26f
            val pad = (6 * density).toInt()
            tv.setPadding(pad, pad, pad, pad)
            tv.setOnClickListener { b.inputEmoji.setText(e) }
            b.emojiContainer.addView(tv)
        }

        b.btnPickTime.text = DateUtil.formatTime(hour, minute)
        b.switchReminder.setOnCheckedChangeListener { _, on -> b.btnPickTime.isEnabled = on }
        b.btnPickTime.setOnClickListener {
            TimePickerDialog(ctx, { _, h, m ->
                hour = h
                minute = m
                b.btnPickTime.text = DateUtil.formatTime(h, m)
            }, hour, minute, true).show()
        }

        val dialog = MaterialAlertDialogBuilder(ctx).setView(b.root).create()
        b.btnCancel.setOnClickListener { dialog.dismiss() }
        b.btnSave.setOnClickListener {
            val title = b.inputTitle.text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) {
                b.tilTitle.error = "Give your habit a quest name"
                return@setOnClickListener
            }
            val emoji = b.inputEmoji.text?.toString()?.trim().orEmpty().ifEmpty { "🌱" }
            val remind = b.switchReminder.isChecked

            if (existing == null) {
                vm.addHabit(
                    title = title,
                    emoji = emoji,
                    color = selectedColor,
                    category = selectedCategory,
                    targetDaysMask = 127,
                    xpReward = 25,
                    reminderEnabled = remind,
                    hour = hour,
                    minute = minute
                )
            } else {
                vm.updateHabit(
                    existing.copy(
                        title = title,
                        emoji = emoji,
                        color = selectedColor,
                        category = selectedCategory,
                        reminderEnabled = remind,
                        reminderHour = hour,
                        reminderMinute = minute
                    )
                )
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
