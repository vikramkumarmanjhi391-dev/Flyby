package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AmbientInterval
import com.example.model.AnimationQuality
import com.example.model.CustomDrifterPreset
import com.example.model.FlyByPreferences
import com.example.model.MovementPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flyby_user_prefs")

class FlyByPreferencesRepository(private val context: Context) {

    private object Keys {
        val MASTER_ENABLED = booleanPreferencesKey("master_overlay_enabled")
        val IS_PAUSED = booleanPreferencesKey("is_paused")
        val GHOST_MODE = booleanPreferencesKey("ghost_mode")
        val SELECTED_CHARACTER_ID = stringPreferencesKey("selected_character_id")
        val SELECTED_MOVEMENT = stringPreferencesKey("selected_movement")
        val SPEED_MULTIPLIER = floatPreferencesKey("speed_multiplier")
        val SIZE_MULTIPLIER = floatPreferencesKey("size_multiplier")
        val OPACITY = floatPreferencesKey("opacity")
        val START_DELAY_SEC = intPreferencesKey("start_delay_sec")
        val REPETITION_COUNT = intPreferencesKey("repetition_count")
        val SPAWN_INTERVAL_SEC = intPreferencesKey("spawn_interval_sec")
        val AMBIENT_INTERVAL = stringPreferencesKey("ambient_interval")
        val CUSTOM_AMBIENT_MIN = intPreferencesKey("custom_ambient_min")
        val UNLOCK_TRIGGER = booleanPreferencesKey("unlock_trigger_enabled")
        val UNLOCK_COOLDOWN_SEC = intPreferencesKey("unlock_cooldown_sec")
        val GAMING_FOCUS_MODE = booleanPreferencesKey("gaming_focus_mode")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_START_HOUR = intPreferencesKey("quiet_start_hour")
        val QUIET_END_HOUR = intPreferencesKey("quiet_end_hour")
        val SHOW_NOTIFICATION_CONTROLS = booleanPreferencesKey("show_notification_controls")
        val ANIMATION_QUALITY = stringPreferencesKey("animation_quality")
        val FAVORITE_IDS = stringSetPreferencesKey("favorite_ids")
        val RECENT_IDS_CSV = stringPreferencesKey("recent_ids_csv")
        val CUSTOM_PRESETS_JSON = stringPreferencesKey("custom_presets_json")
    }

    val preferencesFlow: Flow<FlyByPreferences> = context.dataStore.data.map { prefs ->
        val movementId = prefs[Keys.SELECTED_MOVEMENT] ?: MovementPreset.LEFT_TO_RIGHT.id
        val movement = MovementPreset.entries.find { it.id == movementId } ?: MovementPreset.LEFT_TO_RIGHT

        val ambientId = prefs[Keys.AMBIENT_INTERVAL] ?: AmbientInterval.EVERY_15_MIN.id
        val ambient = AmbientInterval.entries.find { it.id == ambientId } ?: AmbientInterval.EVERY_15_MIN

        val qualityId = prefs[Keys.ANIMATION_QUALITY] ?: AnimationQuality.BALANCED.id
        val quality = AnimationQuality.entries.find { it.id == qualityId } ?: AnimationQuality.BALANCED

        val recentCsv = prefs[Keys.RECENT_IDS_CSV] ?: "chibi_crow,rainbow_pixel_cat,glowing_fireflies,paper_planes"
        val recentList = recentCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        val customPresets = parseCustomPresets(prefs[Keys.CUSTOM_PRESETS_JSON] ?: "[]")

        FlyByPreferences(
            masterOverlayEnabled = prefs[Keys.MASTER_ENABLED] ?: false,
            isPaused = prefs[Keys.IS_PAUSED] ?: false,
            ghostMode = prefs[Keys.GHOST_MODE] ?: true,
            selectedCharacterId = prefs[Keys.SELECTED_CHARACTER_ID] ?: "chibi_crow",
            selectedMovementPreset = movement,
            speedMultiplier = (prefs[Keys.SPEED_MULTIPLIER] ?: 1.0f).coerceIn(0.3f, 3.0f),
            sizeMultiplier = (prefs[Keys.SIZE_MULTIPLIER] ?: 1.0f).coerceIn(0.5f, 2.5f),
            opacity = (prefs[Keys.OPACITY] ?: 0.78f).coerceIn(0.15f, 1.0f),
            startDelaySeconds = (prefs[Keys.START_DELAY_SEC] ?: 0).coerceIn(0, 30),
            repetitionCount = (prefs[Keys.REPETITION_COUNT] ?: 3).coerceIn(0, 20),
            spawnIntervalSeconds = (prefs[Keys.SPAWN_INTERVAL_SEC] ?: 12).coerceIn(3, 120),
            ambientInterval = ambient,
            customAmbientMinutes = (prefs[Keys.CUSTOM_AMBIENT_MIN] ?: 10).coerceIn(1, 180),
            unlockTriggerEnabled = prefs[Keys.UNLOCK_TRIGGER] ?: false,
            unlockCooldownSeconds = (prefs[Keys.UNLOCK_COOLDOWN_SEC] ?: 45).coerceIn(10, 600),
            gamingFocusMode = prefs[Keys.GAMING_FOCUS_MODE] ?: false,
            quietHoursEnabled = prefs[Keys.QUIET_HOURS_ENABLED] ?: false,
            quietStartHour = (prefs[Keys.QUIET_START_HOUR] ?: 23).coerceIn(0, 23),
            quietEndHour = (prefs[Keys.QUIET_END_HOUR] ?: 7).coerceIn(0, 23),
            showNotificationControls = prefs[Keys.SHOW_NOTIFICATION_CONTROLS] ?: true,
            animationQuality = quality,
            favoriteIds = prefs[Keys.FAVORITE_IDS] ?: setOf("chibi_crow", "rainbow_pixel_cat", "glowing_fireflies"),
            recentIds = recentList.ifEmpty { listOf("chibi_crow") },
            customPresets = customPresets
        )
    }

    suspend fun setMasterOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MASTER_ENABLED] = enabled }
    }

    suspend fun setPaused(paused: Boolean) {
        context.dataStore.edit { it[Keys.IS_PAUSED] = paused }
    }

    suspend fun setGhostMode(ghostMode: Boolean) {
        context.dataStore.edit { it[Keys.GHOST_MODE] = ghostMode }
    }

    suspend fun selectCharacter(characterId: String, defaultMovement: MovementPreset? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_CHARACTER_ID] = characterId
            if (defaultMovement != null) {
                prefs[Keys.SELECTED_MOVEMENT] = defaultMovement.id
            }
            val currentRecents = (prefs[Keys.RECENT_IDS_CSV] ?: "chibi_crow,rainbow_pixel_cat,glowing_fireflies")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() && it != characterId }
            val updatedRecents = (listOf(characterId) + currentRecents).take(8)
            prefs[Keys.RECENT_IDS_CSV] = updatedRecents.joinToString(",")
        }
    }

    suspend fun setMovementPreset(preset: MovementPreset) {
        context.dataStore.edit { it[Keys.SELECTED_MOVEMENT] = preset.id }
    }

    suspend fun setSpeedMultiplier(speed: Float) {
        context.dataStore.edit { it[Keys.SPEED_MULTIPLIER] = speed.coerceIn(0.3f, 3.0f) }
    }

    suspend fun setSizeMultiplier(size: Float) {
        context.dataStore.edit { it[Keys.SIZE_MULTIPLIER] = size.coerceIn(0.5f, 2.5f) }
    }

    suspend fun setOpacity(opacity: Float) {
        context.dataStore.edit { it[Keys.OPACITY] = opacity.coerceIn(0.15f, 1.0f) }
    }

    suspend fun setStartDelaySeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.START_DELAY_SEC] = seconds.coerceIn(0, 30) }
    }

    suspend fun setRepetitionCount(count: Int) {
        context.dataStore.edit { it[Keys.REPETITION_COUNT] = count.coerceIn(0, 20) }
    }

    suspend fun setSpawnIntervalSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.SPAWN_INTERVAL_SEC] = seconds.coerceIn(3, 120) }
    }

    suspend fun setAmbientInterval(interval: AmbientInterval) {
        context.dataStore.edit { it[Keys.AMBIENT_INTERVAL] = interval.id }
    }

    suspend fun setCustomAmbientMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.CUSTOM_AMBIENT_MIN] = minutes.coerceIn(1, 180) }
    }

    suspend fun setUnlockTriggerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.UNLOCK_TRIGGER] = enabled }
    }

    suspend fun setUnlockCooldownSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.UNLOCK_COOLDOWN_SEC] = seconds.coerceIn(10, 600) }
    }

    suspend fun setGamingFocusMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GAMING_FOCUS_MODE] = enabled }
    }

    suspend fun setQuietHours(enabled: Boolean, startHour: Int, endHour: Int) {
        context.dataStore.edit {
            it[Keys.QUIET_HOURS_ENABLED] = enabled
            it[Keys.QUIET_START_HOUR] = startHour.coerceIn(0, 23)
            it[Keys.QUIET_END_HOUR] = endHour.coerceIn(0, 23)
        }
    }

    suspend fun setShowNotificationControls(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_NOTIFICATION_CONTROLS] = enabled }
    }

    suspend fun setAnimationQuality(quality: AnimationQuality) {
        context.dataStore.edit { it[Keys.ANIMATION_QUALITY] = quality.id }
    }

    suspend fun toggleFavorite(characterId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_IDS] ?: setOf("chibi_crow", "rainbow_pixel_cat", "glowing_fireflies")
            prefs[Keys.FAVORITE_IDS] = if (current.contains(characterId)) {
                current - characterId
            } else {
                current + characterId
            }
        }
    }

    suspend fun addCustomPreset(preset: CustomDrifterPreset) {
        context.dataStore.edit { prefs ->
            val existing = parseCustomPresets(prefs[Keys.CUSTOM_PRESETS_JSON] ?: "[]")
            val updated = listOf(preset) + existing.filterNot { it.id == preset.id }
            prefs[Keys.CUSTOM_PRESETS_JSON] = serializeCustomPresets(updated)
            prefs[Keys.SELECTED_CHARACTER_ID] = preset.id
            prefs[Keys.SELECTED_MOVEMENT] = preset.movementPreset.id
            prefs[Keys.SPEED_MULTIPLIER] = preset.speedMultiplier
            prefs[Keys.SIZE_MULTIPLIER] = preset.sizeMultiplier
            prefs[Keys.OPACITY] = preset.opacity
            prefs[Keys.SPAWN_INTERVAL_SEC] = preset.spawnIntervalSeconds
        }
    }

    suspend fun deleteCustomPreset(presetId: String) {
        context.dataStore.edit { prefs ->
            val existing = parseCustomPresets(prefs[Keys.CUSTOM_PRESETS_JSON] ?: "[]")
            val updated = existing.filterNot { it.id == presetId }
            prefs[Keys.CUSTOM_PRESETS_JSON] = serializeCustomPresets(updated)
            if (prefs[Keys.SELECTED_CHARACTER_ID] == presetId) {
                prefs[Keys.SELECTED_CHARACTER_ID] = "chibi_crow"
            }
        }
    }

    suspend fun resetAllPreferences() {
        context.dataStore.edit { prefs ->
            val preservedCustom = prefs[Keys.CUSTOM_PRESETS_JSON]
            prefs.clear()
            if (preservedCustom != null) {
                prefs[Keys.CUSTOM_PRESETS_JSON] = preservedCustom
            }
        }
    }

    private fun parseCustomPresets(jsonStr: String): List<CustomDrifterPreset> {
        return try {
            val array = JSONArray(jsonStr)
            val result = mutableListOf<CustomDrifterPreset>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val moveId = obj.optString("movementPreset", MovementPreset.LEFT_TO_RIGHT.id)
                val movement = MovementPreset.entries.find { it.id == moveId } ?: MovementPreset.LEFT_TO_RIGHT
                result.add(
                    CustomDrifterPreset(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        uriString = obj.getString("uriString"),
                        mimeType = obj.optString("mimeType", "image/png"),
                        movementPreset = movement,
                        speedMultiplier = obj.optDouble("speedMultiplier", 1.0).toFloat(),
                        sizeMultiplier = obj.optDouble("sizeMultiplier", 1.0).toFloat(),
                        opacity = obj.optDouble("opacity", 0.78).toFloat(),
                        spawnIntervalSeconds = obj.optInt("spawnIntervalSeconds", 12),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeCustomPresets(list: List<CustomDrifterPreset>): String {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("uriString", item.uriString)
                put("mimeType", item.mimeType)
                put("movementPreset", item.movementPreset.id)
                put("speedMultiplier", item.speedMultiplier.toDouble())
                put("sizeMultiplier", item.sizeMultiplier.toDouble())
                put("opacity", item.opacity.toDouble())
                put("spawnIntervalSeconds", item.spawnIntervalSeconds)
                put("createdAt", item.createdAt)
            }
            array.put(obj)
        }
        return array.toString()
    }
}
