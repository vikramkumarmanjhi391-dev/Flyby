package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.animation.CustomMediaBitmapCache
import com.example.data.FlyByPreferencesRepository
import com.example.model.AmbientInterval
import com.example.model.AnimationQuality
import com.example.model.CustomDrifterPreset
import com.example.model.DrifterCatalog
import com.example.model.DrifterCategory
import com.example.model.DrifterCharacter
import com.example.model.FlyByPreferences
import com.example.model.MainNavTab
import com.example.model.MovementPreset
import com.example.model.OverlayRuntimeState
import com.example.overlay.FlyByOverlayService
import com.example.overlay.OverlayStateHolder
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudioDraftState(
    val selectedUriString: String? = null,
    val detectedMimeType: String = "image/png",
    val customName: String = "",
    val movementPreset: MovementPreset = MovementPreset.LEFT_TO_RIGHT,
    val speedMultiplier: Float = 1.0f,
    val sizeMultiplier: Float = 1.0f,
    val opacity: Float = 0.78f,
    val spawnIntervalSeconds: Int = 12,
    val validationError: String? = null,
    val validationSuccessMessage: String? = null
)

class FlyByViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FlyByPreferencesRepository(application.applicationContext)

    val preferences: StateFlow<FlyByPreferences> = repository.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FlyByPreferences()
    )

    val runtimeState: StateFlow<OverlayRuntimeState> = OverlayStateHolder.runtimeState

    private val _currentTab = MutableStateFlow(MainNavTab.HOME)
    val currentTab: StateFlow<MainNavTab> = _currentTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow(DrifterCategory.ANIME)
    val selectedCategory: StateFlow<DrifterCategory> = _selectedCategory.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(
        OverlayStateHolder.canDrawOverlays(application.applicationContext)
    )
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _previewBurstCounter = MutableStateFlow(0)
    val previewBurstCounter: StateFlow<Int> = _previewBurstCounter.asStateFlow()

    private val _studioDraft = MutableStateFlow(StudioDraftState())
    val studioDraft: StateFlow<StudioDraftState> = _studioDraft.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    init {
        refreshSystemDiagnostics()
    }

    fun selectTab(tab: MainNavTab) {
        _currentTab.value = tab
        refreshSystemDiagnostics()
    }

    fun selectExploreCategory(category: DrifterCategory) {
        _selectedCategory.value = category
    }

    fun dismissBannerMessage() {
        _statusBannerMessage.value = null
    }

    fun refreshSystemDiagnostics() {
        val ctx = getApplication<Application>().applicationContext
        val canDraw = OverlayStateHolder.canDrawOverlays(ctx)
        _hasOverlayPermission.value = canDraw
        OverlayStateHolder.updateState {
            it.copy(
                isPowerSaveMode = OverlayStateHolder.isBatterySaverActive(ctx),
                maxAllowedUntrustedTouchOpacity = OverlayStateHolder.queryMaxAllowedUntrustedTouchOpacity(ctx)
            )
        }
    }

    fun buildOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun toggleMasterOverlay(enabled: Boolean) {
        val ctx = getApplication<Application>().applicationContext
        refreshSystemDiagnostics()
        if (enabled && !_hasOverlayPermission.value) {
            _statusBannerMessage.value =
                "Grant 'Display over other apps' permission first so FlyBy can float drifters across your screen."
            return
        }
        viewModelScope.launch {
            repository.setMasterOverlayEnabled(enabled)
            if (enabled) {
                repository.setPaused(false)
                FlyByOverlayService.startServiceOrQuickPlay(ctx, FlyByOverlayService.ACTION_START_OVERLAY)
                _statusBannerMessage.value = "FlyBy overlay engine started! Drifters are now active over other apps."
            } else {
                FlyByOverlayService.stopOverlayService(ctx)
                _statusBannerMessage.value = "FlyBy overlay stopped and window removed cleanly."
            }
        }
    }

    fun triggerQuickPlay() {
        val ctx = getApplication<Application>().applicationContext
        _previewBurstCounter.value += 1
        refreshSystemDiagnostics()

        if (_hasOverlayPermission.value) {
            viewModelScope.launch {
                repository.setMasterOverlayEnabled(true)
                repository.setPaused(false)
                FlyByOverlayService.startServiceOrQuickPlay(ctx, FlyByOverlayService.ACTION_QUICK_PLAY)
                _statusBannerMessage.value = "Launched drifter onto screen overlay!"
            }
        } else {
            _statusBannerMessage.value =
                "Playing in-app preview! Grant Overlay Permission to launch over other apps."
        }
    }

    fun togglePauseResume() {
        val ctx = getApplication<Application>().applicationContext
        val currentlyPaused = preferences.value.isPaused
        viewModelScope.launch {
            repository.setPaused(!currentlyPaused)
            if (runtimeState.value.isServiceRunning) {
                FlyByOverlayService.startServiceOrQuickPlay(ctx, FlyByOverlayService.ACTION_TOGGLE_PAUSE)
            }
        }
    }

    fun selectCharacter(character: DrifterCharacter, launchImmediately: Boolean = false) {
        viewModelScope.launch {
            repository.selectCharacter(character.id, character.defaultMovement)
            _previewBurstCounter.value += 1
            if (launchImmediately && _hasOverlayPermission.value) {
                val ctx = getApplication<Application>().applicationContext
                repository.setMasterOverlayEnabled(true)
                repository.setPaused(false)
                FlyByOverlayService.startServiceOrQuickPlay(ctx, FlyByOverlayService.ACTION_QUICK_PLAY)
                _statusBannerMessage.value = "${character.name} is now drifting across your screen!"
            } else {
                _statusBannerMessage.value = "Selected ${character.name} (${character.defaultMovement.label})"
            }
        }
    }

    fun toggleFavorite(characterId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(characterId)
        }
    }

    fun setMovementPreset(preset: MovementPreset) {
        viewModelScope.launch {
            repository.setMovementPreset(preset)
            _previewBurstCounter.value += 1
        }
    }

    fun setSpeedMultiplier(speed: Float) {
        viewModelScope.launch {
            repository.setSpeedMultiplier(speed)
        }
    }

    fun setSizeMultiplier(size: Float) {
        viewModelScope.launch {
            repository.setSizeMultiplier(size)
        }
    }

    fun setOpacity(opacity: Float) {
        viewModelScope.launch {
            repository.setOpacity(opacity)
        }
    }

    fun setStartDelaySeconds(seconds: Int) {
        viewModelScope.launch {
            repository.setStartDelaySeconds(seconds)
        }
    }

    fun setRepetitionCount(count: Int) {
        viewModelScope.launch {
            repository.setRepetitionCount(count)
        }
    }

    fun setSpawnIntervalSeconds(seconds: Int) {
        viewModelScope.launch {
            repository.setSpawnIntervalSeconds(seconds)
        }
    }

    fun setGhostMode(ghostMode: Boolean) {
        viewModelScope.launch {
            repository.setGhostMode(ghostMode)
        }
    }

    fun setAmbientInterval(interval: AmbientInterval) {
        viewModelScope.launch {
            repository.setAmbientInterval(interval)
        }
    }

    fun setCustomAmbientMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setCustomAmbientMinutes(minutes)
        }
    }

    fun setUnlockTriggerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setUnlockTriggerEnabled(enabled)
        }
    }

    fun setUnlockCooldownSeconds(seconds: Int) {
        viewModelScope.launch {
            repository.setUnlockCooldownSeconds(seconds)
        }
    }

    fun setGamingFocusMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setGamingFocusMode(enabled)
            _statusBannerMessage.value = if (enabled) {
                "Gaming / Focus Mode ON — Ambient & unlock animations suspended."
            } else {
                "Gaming / Focus Mode OFF — Animations resumed."
            }
        }
    }

    fun setQuietHours(enabled: Boolean, startHour: Int, endHour: Int) {
        viewModelScope.launch {
            repository.setQuietHours(enabled, startHour, endHour)
        }
    }

    fun setShowNotificationControls(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowNotificationControls(enabled)
        }
    }

    fun setAnimationQuality(quality: AnimationQuality) {
        viewModelScope.launch {
            repository.setAnimationQuality(quality)
        }
    }

    fun resetPreferences() {
        viewModelScope.launch {
            repository.resetAllPreferences()
            _statusBannerMessage.value = "All preferences reset to factory defaults."
        }
    }

    // ==================== CUSTOM STUDIO ====================

    fun onCustomStickerPicked(uri: Uri) {
        val ctx = getApplication<Application>().applicationContext
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            ctx.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (_: Exception) {
            // Photo picker URIs may not require or support takePersistableUriPermission
        }

        val validation = CustomMediaBitmapCache.validateMediaUri(ctx, uri)
        if (!validation.isValid) {
            _studioDraft.value = _studioDraft.value.copy(
                validationError = validation.errorMessage,
                validationSuccessMessage = null
            )
            return
        }

        _studioDraft.value = _studioDraft.value.copy(
            selectedUriString = uri.toString(),
            detectedMimeType = validation.mimeType,
            customName = _studioDraft.value.customName.ifBlank { validation.displayName },
            validationError = null,
            validationSuccessMessage = "Loaded ${validation.displayName} (${validation.mimeType.uppercase()})"
        )
    }

    fun updateStudioDraft(reducer: (StudioDraftState) -> StudioDraftState) {
        _studioDraft.value = reducer(_studioDraft.value)
    }

    fun saveCustomStudioPreset() {
        val draft = _studioDraft.value
        val uriStr = draft.selectedUriString
        if (uriStr.isNullOrBlank()) {
            _studioDraft.value = draft.copy(
                validationError = "Please select a PNG, WebP, or GIF image from your device first."
            )
            return
        }
        val cleanName = draft.customName.trim().ifEmpty { "Custom Drifter" }
        val newPreset = CustomDrifterPreset(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            uriString = uriStr,
            mimeType = draft.detectedMimeType,
            movementPreset = draft.movementPreset,
            speedMultiplier = draft.speedMultiplier,
            sizeMultiplier = draft.sizeMultiplier,
            opacity = draft.opacity,
            spawnIntervalSeconds = draft.spawnIntervalSeconds,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.addCustomPreset(newPreset)
            _studioDraft.value = StudioDraftState(
                validationSuccessMessage = "Saved '$cleanName' and set as active drifter!"
            )
            _statusBannerMessage.value = "Saved custom preset '$cleanName'!"
        }
    }

    fun deleteCustomPreset(preset: CustomDrifterPreset) {
        CustomMediaBitmapCache.evict(preset.uriString)
        viewModelScope.launch {
            repository.deleteCustomPreset(preset.id)
            _statusBannerMessage.value = "Deleted custom preset '${preset.name}'."
        }
    }

    fun getDeviceCompatibilityNotes(): List<String> {
        val notes = mutableListOf<String>()
        val sdk = Build.VERSION.SDK_INT
        val rt = runtimeState.value
        if (sdk >= Build.VERSION_CODES.S) {
            val maxPct = (rt.maxAllowedUntrustedTouchOpacity * 100).toInt()
            notes.add(
                "Android 12+ (API $sdk) Untrusted Touch Protection active: System maximum pass-through opacity is $maxPct%. FlyBy automatically clamps Ghost Mode window opacity (${(rt.effectiveAppliedOpacity * 100).toInt()}%) so underlying apps receive 100% of touches."
            )
        } else {
            notes.add("Android API $sdk: Standard FLAG_NOT_TOUCHABLE pass-through supported at all opacity levels.")
        }
        notes.add(
            "Unlock Trigger (ACTION_USER_PRESENT) is dynamically registered while the FlyBy Foreground Service is running, complying with Android 8.0+ background execution limits."
        )
        notes.add(
            "Lock Screen Safety: FlyBy inspects KeyguardManager and automatically hides animations on the lock screen and when the display turns off."
        )
        if (rt.isPowerSaveMode) {
            notes.add("Android Battery Saver is currently ON: Frame rate automatically throttled to 20 FPS and max simultaneous drifters capped at 2.")
        }
        return notes
    }
}
