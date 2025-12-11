package com.android.sample.ui.gamification

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.android.sample.EduMonApp
import com.android.sample.R
import com.android.sample.ui.profile.LevelUpRewardUiEvent

object ToastNotifier {
    private val mainHandler = Handler(Looper.getMainLooper())

    // Deduplication tracking
    private var lastLevelUpTime = 0L
    private var lastLevelUpLevel = -1
    private var lastStatGainTime = 0L
    private var lastStatGainMessage = ""
    private const val DEBOUNCE_MS = 2000L

    private fun buildRewardMessage(event: LevelUpRewardUiEvent.RewardsGranted): String {
        val s = event.summary
        return buildString {
            append("🎉 Level ${event.newLevel} reached!")
            if (s.coinsGranted > 0) append(" 💰 +${s.coinsGranted} coins")
            if (s.accessoryIdsGranted.isNotEmpty()) {
                append(" 🎁 ${s.accessoryIdsGranted.size} new item")
                if (s.accessoryIdsGranted.size > 1) append("s")
            }
        }
    }

    fun showLevelUpEvent(event: LevelUpRewardUiEvent.RewardsGranted) {
        Log.d("ToastNotifier", "showLevelUpEvent called: level=${event.newLevel}")

        val now = System.currentTimeMillis()

        // Prevent duplicate level-up toasts
        if (event.newLevel == lastLevelUpLevel && (now - lastLevelUpTime) < DEBOUNCE_MS) {
            Log.d("ToastNotifier", "BLOCKED duplicate level-up toast")
            return
        }

        lastLevelUpLevel = event.newLevel
        lastLevelUpTime = now

        val message = buildRewardMessage(event)
        mainHandler.post {
            showCustomToast(message)
        }
    }

    fun showStatGain(points: Int, coins: Int) {
        Log.d("ToastNotifier", "showStatGain called: points=$points, coins=$coins")

        val msg = buildString {
            if (points > 0) append("✨ +$points pts")
            if (coins > 0) {
                if (points > 0) append("  ")
                append("💰 +$coins coins")
            }
        }.trim()

        if (msg.isBlank()) return

        val now = System.currentTimeMillis()

        // Prevent duplicate stat gain toasts
        if (msg == lastStatGainMessage && (now - lastStatGainTime) < DEBOUNCE_MS) {
            Log.d("ToastNotifier", "BLOCKED duplicate stat gain toast")
            return
        }

        lastStatGainMessage = msg
        lastStatGainTime = now

        mainHandler.post {
            showCustomToast(msg)
        }
    }

    private fun showCustomToast(message: String) {
        Log.d("ToastNotifier", "Showing toast: $message")
        val ctx = EduMonApp.appContext
        val inflater = LayoutInflater.from(ctx)
        val layout = inflater.inflate(R.layout.toast_custom, null)

        val text = layout.findViewById<TextView>(R.id.toast_text)
        text.text = message

        Toast(ctx).apply {
            duration = Toast.LENGTH_SHORT
            view = layout
            show()
        }
    }
}