package com.habitquest.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.habitquest.app.ads.AdManager
import com.habitquest.app.data.BackupManager
import com.habitquest.app.databinding.FragmentSettingsBinding
import com.habitquest.app.util.Prefs
import com.habitquest.app.util.ReminderScheduler
import com.habitquest.app.util.fadeIn
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
                val ctx = requireContext()
        val appCtx = ctx.applicationContext

        // Theme
        val checkedId = when (Prefs.themeMode(ctx)) {
            AppCompatDelegate.MODE_NIGHT_NO -> R.id.btn_theme_light
            AppCompatDelegate.MODE_NIGHT_YES -> R.id.btn_theme_dark
            else -> R.id.btn_theme_system
        }
        binding.themeToggle.check(checkedId)
        binding.themeToggle.addOnButtonCheckedListener { _, id, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (id) {
                R.id.btn_theme_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.btn_theme_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            if (mode != Prefs.themeMode(appCtx)) {
                Prefs.setThemeMode(appCtx, mode)
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }

        // Haptics
        binding.switchHaptics.isChecked = Prefs.hapticsEnabled(ctx)
        binding.switchHaptics.setOnCheckedChangeListener { _, enabled ->
            Prefs.setHapticsEnabled(appCtx, enabled)
        }

        // Ad Free Focus Mode
        

        // Export Backup
        binding.rowExportBackup.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val json = BackupManager.exportToJson(ctx)
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_SUBJECT, "HabitQuest_Backup_${System.currentTimeMillis()}.json")
                        putExtra(Intent.EXTRA_TEXT, json)
                    }
                    startActivity(Intent.createChooser(share, "Export HabitQuest Backup"))
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Failed to export backup: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        

        binding.rowRate.setOnClickListener {
            openUri(
                "market://details?id=" + ctx.packageName,
                "https://play.google.com/store/apps/details?id=" + ctx.packageName
            )
        }

        binding.rowPrivacy.setOnClickListener {
            showPrivacyPolicy()
        }

        binding.tvVersion.text = "1.2.0 (Premium Quest)"

        if (!Prefs.isAdFreeMode(ctx)) {
            binding.bannerContainer.addView(AdManager.createBanner(ctx))
        }
    }

    private fun openUri(primary: String, fallback: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(primary)))
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallback)))
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "No browser found to open link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
            }

    private fun showPrivacyPolicy() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Privacy Policy")
            .setMessage(
                "Last Updated: October 2026\n\n" +
                "HabitQuest respects your privacy. This policy explains how your information is handled.\n\n" +
                "1. INFORMATION WE COLLECT\n" +
                "We do NOT collect, store, or share any personal information on our servers. All your habits, streaks, and completion data are stored ONLY on your device using a local Room database.\n\n" +
                "2. NO ACCOUNT NEEDED\n" +
                "HabitQuest works 100% offline. You do not need to create an account.\n\n" +
                "3. THIRD-PARTY ADVERTISING\n" +
                "This app uses Google AdMob to show ads. AdMob may collect anonymous data (like your device ID) to serve personalized ads. Learn more: https://policies.google.com/technologies/partner-sites\n\n" +
                "4. NOTIFICATIONS\n" +
                "HabitQuest sends local notifications to remind you about your habits. You can disable reminders anytime in Settings.\n\n" +
                "5. DATA SECURITY\n" +
                "All data stays on your device. If you uninstall the app, your habits will be permanently deleted. Use the Backup feature to export your data.\n\n" +
                "6. CHILDREN'S PRIVACY\n" +
                "Our app does not knowingly collect personal information from children under 13.\n\n" +
                "7. CONTACT US\n" +
                "Questions? Email us at: cartoniq.business@gmail.com\n\n" +
                "By using HabitQuest, you agree to this Privacy Policy."
            )
            .setPositiveButton("Close", null)
            .show()
    }

}
