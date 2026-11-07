package com.example.overlay

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FlyByPreferencesRepository
import com.example.model.AmbientInterval
import com.example.model.DrifterCatalog
import com.example.model.FlyByPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Real Android Overlay Engine Service using WindowManager and TYPE_APPLICATION_OVERLAY.
 * Supports Ghost Mode (FLAG_NOT_TOUCHABLE + Android 12+ untrusted touch opacity compliance),
 * Ambient Timer, Unlock Trigger (ACTION_USER_PRESENT with cooldown), Screen Off pause,
 * Battery Saver throttling, and persistent notification Stop/Pause actions.
 */
class FlyByOverlayService : Service() {

    companion object {
        const val ACTION_START_OVERLAY = "com.example.flyby.ACTION_START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.example.flyby.ACTION_STOP_OVERLAY"
        const val ACTION_QUICK_PLAY = "com.example.flyby.ACTION_QUICK_PLAY"
        const val ACTION_TOGGLE_PAUSE = "com.example.flyby.ACTION_TOGGLE_PAUSE"
        const val ACTION_CLEAR_DRIFTERS = "com.example.flyby.ACTION_CLEAR_DRIFTERS"

        private const val NOTIFICATION_CHANNEL_ID = "flyby_overlay_channel"
        private const val NOTIFICATION_ID = 4201

        fun startServiceOrQuickPlay(context: Context, action: String = ACTION_START_OVERLAY) {
            if (!OverlayStateHolder.canDrawOverlays(context)) {
                OverlayStateHolder.updateState {
                    it.copy(
                        lastErrorMessage = "Display Over Other Apps permission is required before launching the overlay."
                    )
                }
                return
            }
            val intent = Intent(context, FlyByOverlayService::class.java).apply {
                this.action = action
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                OverlayStateHolder.updateState {
                    it.copy(
                        lastErrorMessage = "Could not start Foreground Service: ${e.localizedMessage}"
                    )
                }
            }
        }

        fun stopOverlayService(context: Context) {
            val intent = Intent(context, FlyByOverlayService::class.java).apply {
                action = ACTION_STOP_OVERLAY
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                context.stopService(Intent(context, FlyByOverlayService::class.java))
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var preferencesRepository: FlyByPreferencesRepository
    private var windowManager: WindowManager? = null
    private var overlayView: OverlayDrifterView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var currentPrefs: FlyByPreferences = FlyByPreferences()
    private var ambientTimerJob: Job? = null
    private var spawnSeriesJob: Job? = null
    private var isScreenInteractive: Boolean = true
    private var lastUnlockTriggerEpochMs: Long = 0L
    private var isReceiverRegistered: Boolean = false

    private val systemEventsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenInteractive = false
                    overlayView?.clearAllDrifters()
                    overlayView?.setPausedState(true)
                    OverlayStateHolder.updateState {
                        it.copy(
                            isScreenOn = false,
                            activeDrifterCount = 0,
                            lastTriggerSource = "Paused (Screen Off)"
                        )
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenInteractive = true
                    val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    val isLocked = keyguard?.isKeyguardLocked == true
                    // Do not show animations over the lock screen
                    if (!isLocked && !currentPrefs.isPaused && !currentPrefs.gamingFocusMode) {
                        overlayView?.setPausedState(false)
                    }
                    OverlayStateHolder.updateState {
                        it.copy(isScreenOn = true)
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    isScreenInteractive = true
                    if (!currentPrefs.isPaused && !currentPrefs.gamingFocusMode) {
                        overlayView?.setPausedState(false)
                    }
                    handleDeviceUnlocked()
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    val powerSave = OverlayStateHolder.isBatterySaverActive(this@FlyByOverlayService)
                    OverlayStateHolder.updateState {
                        it.copy(isPowerSaveMode = powerSave)
                    }
                    applyCurrentPreferencesToWindow(currentPrefs)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = FlyByPreferencesRepository(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        createNotificationChannel()
        startForegroundCompat(buildNotification(currentPrefs))
        registerDynamicSystemReceivers()

        OverlayStateHolder.updateState {
            it.copy(
                isServiceRunning = true,
                isPowerSaveMode = OverlayStateHolder.isBatterySaverActive(this),
                maxAllowedUntrustedTouchOpacity = OverlayStateHolder.queryMaxAllowedUntrustedTouchOpacity(this),
                lastErrorMessage = null
            )
        }

        // Observe DataStore preferences and keep WindowManager & timers synchronized
        serviceScope.launch {
            var isFirstLoad = true
            preferencesRepository.preferencesFlow.collectLatest { prefs ->
                val prevAmbient = currentPrefs.ambientInterval
                val prevCustomMin = currentPrefs.customAmbientMinutes
                val prevCharId = currentPrefs.selectedCharacterId
                currentPrefs = prefs

                if (!OverlayStateHolder.canDrawOverlays(this@FlyByOverlayService)) {
                    OverlayStateHolder.updateState {
                        it.copy(
                            lastErrorMessage = "Overlay permission was revoked in system settings."
                        )
                    }
                    stopAndCleanup()
                    return@collectLatest
                }

                ensureOverlayWindowAttached(prefs)
                applyCurrentPreferencesToWindow(prefs)
                updateNotification(prefs)

                if (isFirstLoad) {
                    isFirstLoad = false
                    if (!prefs.isPaused && !prefs.gamingFocusMode) {
                        triggerDrifterWave("Overlay Started", prefs)
                    }
                    restartAmbientTimer(prefs)
                } else {
                    if (prevCharId != prefs.selectedCharacterId && !prefs.isPaused && !prefs.gamingFocusMode) {
                        triggerDrifterWave("Character Switched", prefs)
                    }
                    if (prevAmbient != prefs.ambientInterval || prevCustomMin != prefs.customAmbientMinutes) {
                        restartAmbientTimer(prefs)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                serviceScope.launch {
                    preferencesRepository.setMasterOverlayEnabled(false)
                    stopAndCleanup()
                }
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_PAUSE -> {
                serviceScope.launch {
                    val newPaused = !currentPrefs.isPaused
                    preferencesRepository.setPaused(newPaused)
                    if (newPaused) {
                        overlayView?.setPausedState(true)
                    } else {
                        overlayView?.setPausedState(false)
                        triggerDrifterWave("Resumed", currentPrefs)
                    }
                }
            }
            ACTION_CLEAR_DRIFTERS -> {
                spawnSeriesJob?.cancel()
                overlayView?.clearAllDrifters()
            }
            ACTION_QUICK_PLAY -> {
                if (OverlayStateHolder.canDrawOverlays(this)) {
                    ensureOverlayWindowAttached(currentPrefs)
                    triggerDrifterWave("Manual Quick Play", currentPrefs, ignoreFocusAndQuiet = true)
                }
            }
            ACTION_START_OVERLAY, null -> {
                if (!OverlayStateHolder.canDrawOverlays(this)) {
                    stopAndCleanup()
                    return START_NOT_STICKY
                }
                ensureOverlayWindowAttached(currentPrefs)
            }
        }
        return START_STICKY
    }

    private fun ensureOverlayWindowAttached(prefs: FlyByPreferences) {
        if (overlayView != null) return
        if (!OverlayStateHolder.canDrawOverlays(this)) return

        val wm = windowManager ?: return
        val flags = buildWindowFlags(prefs.ghostMode)
        val maxTouchOpacity = OverlayStateHolder.queryMaxAllowedUntrustedTouchOpacity(this)
        val safeWindowAlpha = if (prefs.ghostMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Keep window opacity strictly below Android 12+ untrusted touch threshold
            prefs.opacity.coerceAtMost((maxTouchOpacity * 0.95f).coerceAtMost(0.79f))
        } else {
            prefs.opacity
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            alpha = safeWindowAlpha
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val view = OverlayDrifterView(this) { activeCount ->
            OverlayStateHolder.updateState {
                it.copy(activeDrifterCount = activeCount)
            }
        }

        try {
            wm.addView(view, params)
            overlayView = view
            layoutParams = params
            OverlayStateHolder.updateState {
                it.copy(
                    isOverlayWindowAttached = true,
                    maxAllowedUntrustedTouchOpacity = maxTouchOpacity,
                    effectiveAppliedOpacity = safeWindowAlpha,
                    isOpacityClampedForGhostTouch = prefs.ghostMode && prefs.opacity > safeWindowAlpha,
                    lastErrorMessage = null
                )
            }
        } catch (e: Exception) {
            OverlayStateHolder.updateState {
                it.copy(
                    isOverlayWindowAttached = false,
                    lastErrorMessage = "Failed to attach overlay window: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun buildWindowFlags(ghostMode: Boolean): Int {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (ghostMode) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }
        return flags
    }

    private fun applyCurrentPreferencesToWindow(prefs: FlyByPreferences) {
        val view = overlayView ?: return
        val params = layoutParams ?: return
        val wm = windowManager ?: return

        val maxTouchOpacity = OverlayStateHolder.queryMaxAllowedUntrustedTouchOpacity(this)
        val safeWindowAlpha = if (prefs.ghostMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            prefs.opacity.coerceAtMost((maxTouchOpacity * 0.95f).coerceAtMost(0.79f))
        } else {
            prefs.opacity
        }

        val desiredFlags = buildWindowFlags(prefs.ghostMode)
        val flagsChanged = params.flags != desiredFlags
        val alphaChanged = kotlin.math.abs(params.alpha - safeWindowAlpha) > 0.01f

        if (flagsChanged || alphaChanged) {
            params.flags = desiredFlags
            params.alpha = safeWindowAlpha
            try {
                if (view.isAttachedToWindow) {
                    wm.updateViewLayout(view, params)
                }
            } catch (_: Exception) {
            }
        }

        val batterySaver = OverlayStateHolder.isBatterySaverActive(this)
        val effectiveFps = if (batterySaver) 20 else prefs.animationQuality.targetFps
        val character = DrifterCatalog.findById(prefs.selectedCharacterId, prefs.customPresets)

        view.interactiveModeEnabled = !prefs.ghostMode
        view.updateLiveParameters(
            movementPreset = prefs.selectedMovementPreset,
            speedMultiplier = prefs.speedMultiplier,
            opacity = safeWindowAlpha,
            quality = prefs.animationQuality,
            isBatterySaver = batterySaver
        )

        val shouldSuspend = prefs.isPaused || prefs.gamingFocusMode || !isScreenInteractive
        view.setPausedState(shouldSuspend)
        if (prefs.gamingFocusMode) {
            view.clearAllDrifters()
        }

        OverlayStateHolder.updateState {
            it.copy(
                isPaused = prefs.isPaused,
                currentCharacterName = character.name,
                currentMovementLabel = prefs.selectedMovementPreset.label,
                maxAllowedUntrustedTouchOpacity = maxTouchOpacity,
                effectiveAppliedOpacity = safeWindowAlpha,
                isOpacityClampedForGhostTouch = prefs.ghostMode && prefs.opacity > safeWindowAlpha,
                currentFps = effectiveFps,
                isPowerSaveMode = batterySaver
            )
        }
    }

    private fun triggerDrifterWave(
        source: String,
        prefs: FlyByPreferences,
        ignoreFocusAndQuiet: Boolean = false
    ) {
        if (!isScreenInteractive) return

        // Never show animations over the lock screen
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguard?.isKeyguardLocked == true) return

        if (!ignoreFocusAndQuiet) {
            if (prefs.isPaused || prefs.gamingFocusMode) return
            if (OverlayStateHolder.isWithinQuietHours(prefs.quietHoursEnabled, prefs.quietStartHour, prefs.quietEndHour)) {
                OverlayStateHolder.updateState {
                    it.copy(lastTriggerSource = "Skipped (Quiet Hours Active)")
                }
                return
            }
        }

        val view = overlayView ?: return
        val character = DrifterCatalog.findById(prefs.selectedCharacterId, prefs.customPresets)
        val batterySaver = OverlayStateHolder.isBatterySaverActive(this)
        val maxSimultaneous = if (batterySaver) 2 else prefs.animationQuality.maxSimultaneous

        spawnSeriesJob?.cancel()
        spawnSeriesJob = serviceScope.launch {
            if (prefs.startDelaySeconds > 0 && source != "Character Switched") {
                delay(prefs.startDelaySeconds * 1000L)
            }
            if (!isActive) return@launch

            view.setPausedState(false)
            view.spawnDrifter(
                character = character,
                movementPreset = prefs.selectedMovementPreset,
                speedMultiplier = prefs.speedMultiplier,
                sizeMultiplier = prefs.sizeMultiplier,
                opacity = 1.0f, // Window alpha already handles master opacity
                repetitionCount = prefs.repetitionCount.coerceAtLeast(1),
                maxSimultaneous = maxSimultaneous
            )

            OverlayStateHolder.updateState {
                it.copy(
                    lastTriggerSource = source,
                    lastTriggerTimestamp = System.currentTimeMillis(),
                    totalDriftersSpawnedSession = it.totalDriftersSpawnedSession + 1
                )
            }
        }
    }

    private fun restartAmbientTimer(prefs: FlyByPreferences) {
        ambientTimerJob?.cancel()
        if (prefs.ambientInterval == AmbientInterval.OFF) return

        val intervalMinutes = if (prefs.ambientInterval == AmbientInterval.CUSTOM) {
            prefs.customAmbientMinutes.coerceAtLeast(1)
        } else {
            prefs.ambientInterval.minutes.coerceAtLeast(1)
        }

        ambientTimerJob = serviceScope.launch {
            while (isActive) {
                delay(intervalMinutes * 60_000L)
                if (isScreenInteractive && !currentPrefs.isPaused && !currentPrefs.gamingFocusMode) {
                    triggerDrifterWave("Ambient Timer (${intervalMinutes}m)", currentPrefs)
                }
            }
        }
    }

    private fun handleDeviceUnlocked() {
        if (!currentPrefs.unlockTriggerEnabled) return
        if (currentPrefs.isPaused || currentPrefs.gamingFocusMode) return

        val now = System.currentTimeMillis()
        val cooldownMs = currentPrefs.unlockCooldownSeconds.coerceAtLeast(10) * 1000L
        if (now - lastUnlockTriggerEpochMs < cooldownMs) {
            OverlayStateHolder.updateState {
                it.copy(lastTriggerSource = "Unlock Trigger (Cooldown Active)")
            }
            return
        }
        lastUnlockTriggerEpochMs = now
        triggerDrifterWave("Phone Unlocked Trigger", currentPrefs)
    }

    private fun registerDynamicSystemReceivers() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            systemEventsReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        isReceiverRegistered = true
    }

    private fun unregisterDynamicSystemReceivers() {
        if (!isReceiverRegistered) return
        try {
            unregisterReceiver(systemEventsReceiver)
        } catch (_: Exception) {
        }
        isReceiverRegistered = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(prefs: FlyByPreferences): Notification {
        val character = DrifterCatalog.findById(prefs.selectedCharacterId, prefs.customPresets)
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FlyByOverlayService::class.java).apply {
            action = ACTION_STOP_OVERLAY
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, FlyByOverlayService::class.java).apply {
            action = ACTION_TOGGLE_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            102,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusSummary = when {
            prefs.gamingFocusMode -> "Gaming / Focus Pause Active"
            prefs.isPaused -> "Overlay Paused • Tap Resume"
            prefs.ghostMode -> "${character.name} • Ghost Mode (Touch Pass-Through)"
            else -> "${character.name} • Interactive Mode"
        }

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(statusSummary)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_stop_action),
                stopPendingIntent
            )

        if (prefs.showNotificationControls) {
            val pauseResumeLabel = if (prefs.isPaused) {
                getString(R.string.notification_resume_action)
            } else {
                getString(R.string.notification_pause_action)
            }
            builder.addAction(
                android.R.drawable.ic_media_pause,
                pauseResumeLabel,
                pausePendingIntent
            )
        }

        return builder.build()
    }

    private fun updateNotification(prefs: FlyByPreferences) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, buildNotification(prefs))
    }

    private fun startForegroundCompat(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            OverlayStateHolder.updateState {
                it.copy(lastErrorMessage = "Foreground service start notice: ${e.localizedMessage}")
            }
        }
    }

    private fun removeOverlayWindow() {
        val view = overlayView
        overlayView = null
        layoutParams = null
        if (view != null) {
            view.clearAllDrifters()
            try {
                if (view.isAttachedToWindow) {
                    windowManager?.removeViewImmediate(view)
                }
            } catch (_: Exception) {
                try {
                    windowManager?.removeView(view)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun stopAndCleanup() {
        ambientTimerJob?.cancel()
        spawnSeriesJob?.cancel()
        unregisterDynamicSystemReceivers()
        removeOverlayWindow()
        OverlayStateHolder.updateState {
            it.copy(
                isServiceRunning = false,
                isOverlayWindowAttached = false,
                activeDrifterCount = 0,
                lastTriggerSource = "Stopped"
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        ambientTimerJob?.cancel()
        spawnSeriesJob?.cancel()
        unregisterDynamicSystemReceivers()
        removeOverlayWindow()
        serviceScope.cancel()
        OverlayStateHolder.updateState {
            it.copy(
                isServiceRunning = false,
                isOverlayWindowAttached = false,
                activeDrifterCount = 0
            )
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
