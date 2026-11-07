package com.example.model

enum class MainNavTab(val route: String, val label: String) {
    HOME("home", "Home"),
    EXPLORE("explore", "Explore"),
    STUDIO("studio", "Custom Studio"),
    SETTINGS("settings", "Settings")
}

enum class DrifterCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val badgeColorHex: Long
) {
    ANIME(
        id = "anime",
        title = "Anime-Inspired",
        subtitle = "Chibi spirits, shadow crows & energy trails",
        badgeColorHex = 0xFFFF2A85
    ),
    MEME_PIXEL(
        id = "meme_pixel",
        title = "Meme & Pixel",
        subtitle = "16-bit arcade cats, rainbow dashers & UFOs",
        badgeColorHex = 0xFF00F0FF
    ),
    COZY_AESTHETIC(
        id = "cozy",
        title = "Cozy & Aesthetic",
        subtitle = "Fireflies, butterflies, autumn leaves & stars",
        badgeColorHex = 0xFF00F5D4
    ),
    VEHICLE_RETRO(
        id = "vehicle",
        title = "Vehicle & Retro",
        subtitle = "Micro-choppers, biplanes & starhoppers",
        badgeColorHex = 0xFFFFBE0B
    ),
    FAVORITES(
        id = "favorites",
        title = "Favorites",
        subtitle = "Your pinned screen companions",
        badgeColorHex = 0xFF9D4EDD
    )
}

enum class MovementPreset(
    val id: String,
    val label: String,
    val shortDesc: String
) {
    LEFT_TO_RIGHT("left_to_right", "Left to Right", "Cruises smoothly from left screen edge to right"),
    RIGHT_TO_LEFT("right_to_left", "Right to Left", "Glides from right screen edge across to left"),
    TOP_TO_BOTTOM("top_to_bottom", "Top to Bottom", "Descends gracefully from top status area downward"),
    BOTTOM_TO_TOP("bottom_to_top", "Bottom to Top", "Ascends skyward from bottom edge to top"),
    DIAGONAL("diagonal", "Diagonal Drift", "Sweeps corner-to-corner across the display"),
    ZIGZAG("zigzag", "Zigzag Bounce", "Weaves in a playful sine/zigzag wave across screen"),
    RANDOM_FLOATING("random_floating", "Random Roaming", "Wanders organically with smooth direction shifts"),
    BOTTOM_EDGE_RUNNING("bottom_edge_running", "Bottom Edge Runner", "Patrols along the bottom dock/navigation edge"),
    GENTLE_AMBIENT_FLOATING("gentle_ambient", "Gentle Ambient Float", "Hovers softly with minimal low-distraction sway")
}

enum class AnimationQuality(
    val id: String,
    val label: String,
    val targetFps: Int,
    val maxSimultaneous: Int,
    val particleMultiplier: Float,
    val description: String
) {
    BATTERY_SAVER(
        id = "battery_saver",
        label = "Battery Saver",
        targetFps = 20,
        maxSimultaneous = 2,
        particleMultiplier = 0.4f,
        description = "20 FPS • Max 2 drifters • Minimal particle trails"
    ),
    BALANCED(
        id = "balanced",
        label = "Balanced (Default)",
        targetFps = 30,
        maxSimultaneous = 4,
        particleMultiplier = 1.0f,
        description = "30 FPS • Max 4 drifters • Optimal smoothness & battery"
    ),
    SMOOTH(
        id = "smooth",
        label = "Smooth High-FPS",
        targetFps = 60,
        maxSimultaneous = 6,
        particleMultiplier = 1.5f,
        description = "60 FPS • Max 6 drifters • Full particle effects"
    )
}

enum class AmbientInterval(
    val id: String,
    val label: String,
    val minutes: Int
) {
    OFF("off", "Disabled (Manual Only)", 0),
    EVERY_1_MIN("1m", "Every 1 min (Quick Demo)", 1),
    EVERY_5_MIN("5m", "Every 5 minutes", 5),
    EVERY_15_MIN("15m", "Every 15 minutes", 15),
    EVERY_30_MIN("30m", "Every 30 minutes", 30),
    CUSTOM("custom", "Custom Interval", 10)
}

enum class VisualEffectType(val label: String) {
    FEATHER_DISPERSAL("Feather Dispersal"),
    SMOKE_TRAIL("Smoke Disappearance"),
    SPEED_LINES("Speed Trails"),
    ENERGY_PARTICLES("Energy Particles"),
    RAINBOW_RIBBON("Pixel Rainbow Trail"),
    STARDUST_GLOW("Stardust & Glow"),
    PROPULSION_PUFFS("Propulsion Puffs"),
    FLOATING_AURA("Floating Aura")
}

data class DrifterCharacter(
    val id: String,
    val name: String,
    val subtitle: String,
    val category: DrifterCategory,
    val effectType: VisualEffectType,
    val defaultMovement: MovementPreset,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val accentColorHex: Long,
    val isCustom: Boolean = false,
    val customUriString: String? = null,
    val customMimeType: String? = null
)

data class CustomDrifterPreset(
    val id: String,
    val name: String,
    val uriString: String,
    val mimeType: String,
    val movementPreset: MovementPreset,
    val speedMultiplier: Float,
    val sizeMultiplier: Float,
    val opacity: Float,
    val spawnIntervalSeconds: Int,
    val createdAt: Long
)

data class FlyByPreferences(
    val masterOverlayEnabled: Boolean = false,
    val isPaused: Boolean = false,
    val ghostMode: Boolean = true,
    val selectedCharacterId: String = "chibi_crow",
    val selectedMovementPreset: MovementPreset = MovementPreset.LEFT_TO_RIGHT,
    val speedMultiplier: Float = 1.0f,
    val sizeMultiplier: Float = 1.0f,
    val opacity: Float = 0.78f,
    val startDelaySeconds: Int = 0,
    val repetitionCount: Int = 3, // 0 = continuous while active
    val spawnIntervalSeconds: Int = 12,
    val ambientInterval: AmbientInterval = AmbientInterval.EVERY_15_MIN,
    val customAmbientMinutes: Int = 10,
    val unlockTriggerEnabled: Boolean = false,
    val unlockCooldownSeconds: Int = 45,
    val gamingFocusMode: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 23,
    val quietEndHour: Int = 7,
    val showNotificationControls: Boolean = true,
    val animationQuality: AnimationQuality = AnimationQuality.BALANCED,
    val favoriteIds: Set<String> = setOf("chibi_crow", "rainbow_pixel_cat", "glowing_fireflies"),
    val recentIds: List<String> = listOf("chibi_crow", "rainbow_pixel_cat", "glowing_fireflies", "paper_planes"),
    val customPresets: List<CustomDrifterPreset> = emptyList()
)

data class OverlayRuntimeState(
    val isServiceRunning: Boolean = false,
    val isOverlayWindowAttached: Boolean = false,
    val isPaused: Boolean = false,
    val isScreenOn: Boolean = true,
    val isPowerSaveMode: Boolean = false,
    val activeDrifterCount: Int = 0,
    val currentCharacterName: String = "Chibi Crow",
    val currentMovementLabel: String = "Left to Right",
    val lastTriggerSource: String = "Idle",
    val lastTriggerTimestamp: Long = 0L,
    val lastErrorMessage: String? = null,
    val maxAllowedUntrustedTouchOpacity: Float = 0.8f,
    val effectiveAppliedOpacity: Float = 0.78f,
    val isOpacityClampedForGhostTouch: Boolean = false,
    val currentFps: Int = 30,
    val totalDriftersSpawnedSession: Int = 0
)
