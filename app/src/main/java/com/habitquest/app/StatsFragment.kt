package com.habitquest.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.habitquest.app.databinding.FragmentStatsBinding
import com.habitquest.app.util.fadeIn
import com.habitquest.app.util.pulse

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val vm: StatsViewModel by viewModels()
    private var latest: StatsUi? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        vm.stats.observe(viewLifecycleOwner) { s ->
            latest = s
            val density = resources.displayMetrics.density

            binding.tvCurrentStreak.text = s.current.toString()
            binding.tvBestStreak.text = s.best.toString()
            binding.tvTotalCheckins.text = s.totalCheckIns.toString()
            binding.tvRate30.text = "${s.rate30}%"
            binding.heatmapView.setData(s.counts, s.habitCount, s.today)
            binding.barChart.setData(s.weekly, s.labels)
            binding.tvWeekAvg.text = "7-day average: ${s.average}%"

            // RPG Progress Hero Card
            val p = s.levelProgress
            binding.tvStatsRankEmoji.text = p.rankEmoji
            binding.tvStatsLevelName.text = "Level ${p.level} \u00b7 ${p.rankTitle}"
            binding.tvStatsXpInfo.text = "${p.currentXp} Total XP \u00b7 Next rank at ${p.nextLevelXp} XP"
            binding.statsXpProgress.progress = p.progressPercent

            // Achievements Container
            val achContainer = binding.achievementsContainer
            achContainer.removeAllViews()

            for (a in s.achievements) {
                val card = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams((140 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        marginEnd = (10 * density).toInt()
                    }
                    setBackgroundResource(R.drawable.bg_habit_card)
                    val pad = (12 * density).toInt()
                    setPadding(pad, pad, pad, pad)
                    alpha = if (a.isUnlocked) 1f else 0.6f
                }

                val topRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val iconTv = TextView(requireContext()).apply {
                    text = a.iconEmoji
                    textSize = 22f
                }
                topRow.addView(iconTv)

                val xpBadge = TextView(requireContext()).apply {
                    text = "+${a.xpReward} XP"
                    textSize = 10f
                    setTextColor(requireContext().getColor(if (a.isUnlocked) R.color.sage_green else R.color.text_secondary))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        gravity = android.view.Gravity.END
                    }
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_END
                }
                topRow.addView(xpBadge)
                card.addView(topRow)

                val titleTv = TextView(requireContext()).apply {
                    text = a.title
                    textSize = 13f
                    setTextColor(requireContext().getColor(R.color.text_main))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, (6 * density).toInt(), 0, (2 * density).toInt())
                }
                card.addView(titleTv)

                val descTv = TextView(requireContext()).apply {
                    text = a.description
                    textSize = 10f
                    setTextColor(requireContext().getColor(R.color.text_secondary))
                    maxLines = 2
                }
                card.addView(descTv)

                val statusTv = TextView(requireContext()).apply {
                    text = if (a.isUnlocked) "UNLOCKED ✨" else "${a.progress}/${a.maxProgress}"
                    textSize = 10f
                    setTextColor(requireContext().getColor(if (a.isUnlocked) R.color.terracotta else R.color.text_secondary))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, (4 * density).toInt(), 0, 0)
                }
                card.addView(statusTv)

                achContainer.addView(card)
            }

            // Dynamic Habit Breakdown Leaderboard
            val container = binding.leaderboardContainer
            container.removeAllViews()

            if (s.breakdowns.isEmpty()) {
                val tv = TextView(requireContext()).apply {
                    text = "No active quests to analyze."
                    setTextColor(requireContext().getColor(R.color.text_secondary))
                    textSize = 13f
                }
                container.addView(tv)
            } else {
                for (b in s.breakdowns) {
                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding(0, (6 * density).toInt(), 0, (6 * density).toInt())
                    }

                    val emojiTv = TextView(requireContext()).apply {
                        text = b.habit.emoji
                        textSize = 20f
                        layoutParams = LinearLayout.LayoutParams((32 * density).toInt(), (32 * density).toInt())
                    }
                    row.addView(emojiTv)

                    val titleCol = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            marginStart = (8 * density).toInt()
                        }
                    }

                    val titleTv = TextView(requireContext()).apply {
                        text = b.habit.title
                        textSize = 14f
                        setTextColor(requireContext().getColor(R.color.text_main))
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    }
                    titleCol.addView(titleTv)

                    val subTv = TextView(requireContext()).apply {
                        text = "${b.totalDone} total check-ins \u00b7 ${b.rate30}% 30-day consistency"
                        textSize = 11f
                        setTextColor(requireContext().getColor(R.color.text_secondary))
                    }
                    titleCol.addView(subTv)
                    row.addView(titleCol)

                    val streakTv = TextView(requireContext()).apply {
                        text = "🔥 ${b.streak}d"
                        textSize = 13f
                        setTextColor(requireContext().getColor(R.color.fire_orange))
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    }
                    row.addView(streakTv)

                    container.addView(row)
                }
            }
        }

        binding.btnShare.setOnClickListener {
            val s = latest
            val streak = s?.current ?: 0
            val lvl = s?.levelProgress?.level ?: 1
            val rank = s?.levelProgress?.rankTitle ?: "Novice Seeker"
            val text = "⚔️ I am Level $lvl ($rank) on HabitQuest with a 🔥 $streak-day streak!\nBuilding better habits, one day at a time."

            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(send, "Share your quest streak"))
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
