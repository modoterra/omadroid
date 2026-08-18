package com.omadroid.clock

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object AlarmTone {
    private var player: MediaPlayer? = null

    @Synchronized
    fun start(context: Context) {
        if (player?.isPlaying == true) {
            return
        }
        stop()
        val uri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (uri != null) {
            try {
                player =
                    MediaPlayer().apply {
                        setDataSource(context, uri)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build(),
                        )
                        isLooping = true
                        prepare()
                        start()
                    }
            } catch (_: Exception) {
                player?.release()
                player = null
            }
        }
        vibrate(context)
    }

    @Synchronized
    fun stop() {
        try {
            player?.stop()
        } catch (_: Exception) {
            // already stopped
        }
        player?.release()
        player = null
    }

    private fun vibrate(context: Context) {
        val vibrator =
            if (Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        if (vibrator == null || !vibrator.hasVibrator()) {
            return
        }
        val pattern = longArrayOf(0L, 500L, 400L, 500L)
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    fun stopVibrate(context: Context) {
        val vibrator =
            if (Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        vibrator?.cancel()
    }
}
