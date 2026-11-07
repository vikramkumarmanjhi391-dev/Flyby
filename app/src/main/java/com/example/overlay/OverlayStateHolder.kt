package com.example.overlay

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.example.model.OverlayRuntimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar

/**
 * Singleton state holder synchronizing real-time overlay service status,
 * Android 12+ untrusted touch opacity limits, and active drifter telemetry
 * between [FlyByOverlayService] and the Jetpack Compose UI.
 */
object OverlayStateHolder {

    private val _runtimeState = MutableStateFlow(OverlayRuntimeState())
    val runtimeState: StateFlow<OverlayRuntimeState> = _runtimeState.asStateFlow()

    fun updateState(reducer: (OverlayRuntimeState) -> OverlayRuntimeState) {
        _runtimeState.update(reducer)
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Queries Android 12+ (API 31+) InputManager.maximumObscuringOpacityForTouch
     * to guarantee Ghost Mode touch pass-through compliance.
     */
    fun queryMaxAllowedUntrustedTouchOpacity(context: Context): Float {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return try {
                val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
                val maxOpacity = inputManager?.maximumObscuringOpacityForTouch ?: 0.8f
                if (maxOpacity in 0.1f..1.0f) maxOpacity else 0.8f
            } catch (e: Exception) {
                0.8f
            }
        }
        return 1.0f
    }

    fun isBatterySaverActive(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isPowerSaveMode == true
    }

    fun isWithinQuietHours(enabled: Boolean, startHour: Int, endHour: Int): Boolean {
        if (!enabled) return false
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (startHour <= endHour) {
            currentHour in startHour until endHour
        } else {
            // Spans midnight (e.g. 23:00 to 07:00)
            currentHour >= startHour || currentHour < endHour
        }
    }
}
