package com.example.model

object DrifterCatalog {

    val builtInCharacters: List<DrifterCharacter> = listOf(
        // ================= A. ANIME-INSPIRED COLLECTION =================
        DrifterCharacter(
            id = "chibi_crow",
            name = "Chibi Crow",
            subtitle = "Midnight familiar with crimson eyes & dispersing dark feathers",
            category = DrifterCategory.ANIME,
            effectType = VisualEffectType.FEATHER_DISPERSAL,
            defaultMovement = MovementPreset.LEFT_TO_RIGHT,
            primaryColorHex = 0xFF2B234A,
            secondaryColorHex = 0xFFFF2A85,
            accentColorHex = 0xFF9D4EDD
        ),
        DrifterCharacter(
            id = "shadow_bird",
            name = "Shadow Spirit Hawk",
            subtitle = "Ethereal twilight falcon leaving mist & smoke disappearance puffs",
            category = DrifterCategory.ANIME,
            effectType = VisualEffectType.SMOKE_TRAIL,
            defaultMovement = MovementPreset.DIAGONAL,
            primaryColorHex = 0xFF4A3080,
            secondaryColorHex = 0xFF00F0FF,
            accentColorHex = 0xFFB8B2D8
        ),
        DrifterCharacter(
            id = "cute_ninja",
            name = "Kuro Chibi Shinobi",
            subtitle = "Mini masked ninja sprinting with cyan chakra speed lines",
            category = DrifterCategory.ANIME,
            effectType = VisualEffectType.SPEED_LINES,
            defaultMovement = MovementPreset.BOTTOM_EDGE_RUNNING,
            primaryColorHex = 0xFF1D2440,
            secondaryColorHex = 0xFF00F0FF,
            accentColorHex = 0xFFFF2A85
        ),
        DrifterCharacter(
            id = "mini_flying_creature",
            name = "Kitsune Sky Wisp",
            subtitle = "Floating celestial fox companion with twin glowing spirit orbs",
            category = DrifterCategory.ANIME,
            effectType = VisualEffectType.FLOATING_AURA,
            defaultMovement = MovementPreset.ZIGZAG,
            primaryColorHex = 0xFFFFEEF8,
            secondaryColorHex = 0xFFFF5C77,
            accentColorHex = 0xFF00F5D4
        ),
        DrifterCharacter(
            id = "fantasy_energy_creature",
            name = "Astra Dragonet",
            subtitle = "Translucent plasma dragonet crackling with neon energy sparks",
            category = DrifterCategory.ANIME,
            effectType = VisualEffectType.ENERGY_PARTICLES,
            defaultMovement = MovementPreset.RANDOM_FLOATING,
            primaryColorHex = 0xFF00F0FF,
            secondaryColorHex = 0xFF9D4EDD,
            accentColorHex = 0xFF00F5D4
        ),

        // ================= B. MEME & PIXEL COLLECTION =================
        DrifterCharacter(
            id = "spinning_pixel_cat",
            name = "Spinning Pixel Cat",
            subtitle = "16-bit tuxedo kitty tumbling through space at maximum vibe",
            category = DrifterCategory.MEME_PIXEL,
            effectType = VisualEffectType.STARDUST_GLOW,
            defaultMovement = MovementPreset.DIAGONAL,
            primaryColorHex = 0xFF28263E,
            secondaryColorHex = 0xFFFFFFFF,
            accentColorHex = 0xFFFFBE0B
        ),
        DrifterCharacter(
            id = "rainbow_pixel_cat",
            name = "Prism Dash Cat",
            subtitle = "Original cosmic berry-tart cat streaming a 6-band pixel rainbow",
            category = DrifterCategory.MEME_PIXEL,
            effectType = VisualEffectType.RAINBOW_RIBBON,
            defaultMovement = MovementPreset.LEFT_TO_RIGHT,
            primaryColorHex = 0xFFFF66B3,
            secondaryColorHex = 0xFF00F0FF,
            accentColorHex = 0xFFFFBE0B
        ),
        DrifterCharacter(
            id = "mini_pixel_ufo",
            name = "8-Bit Abductor",
            subtitle = "Arcade alien saucer with pulsing emerald tractor beam",
            category = DrifterCategory.MEME_PIXEL,
            effectType = VisualEffectType.ENERGY_PARTICLES,
            defaultMovement = MovementPreset.ZIGZAG,
            primaryColorHex = 0xFF00F5D4,
            secondaryColorHex = 0xFF9D4EDD,
            accentColorHex = 0xFFFFBE0B
        ),
        DrifterCharacter(
            id = "retro_pixel_heli",
            name = "Bit-Chopper 88",
            subtitle = "Chiptune-era pixel rescue helicopter with whirling top rotor",
            category = DrifterCategory.MEME_PIXEL,
            effectType = VisualEffectType.PROPULSION_PUFFS,
            defaultMovement = MovementPreset.RIGHT_TO_LEFT,
            primaryColorHex = 0xFFFFBE0B,
            secondaryColorHex = 0xFF00F0FF,
            accentColorHex = 0xFFFF2A85
        ),
        DrifterCharacter(
            id = "funny_flying_toast",
            name = "Winged Cyber Toast",
            subtitle = "Golden buttered pixel toast flapping majestic angel wings",
            category = DrifterCategory.MEME_PIXEL,
            effectType = VisualEffectType.FEATHER_DISPERSAL,
            defaultMovement = MovementPreset.TOP_TO_BOTTOM,
            primaryColorHex = 0xFFE8A24E,
            secondaryColorHex = 0xFFFFF3B0,
            accentColorHex = 0xFFFFFFFF
        ),
        DrifterCharacter(
            id = "pixel_birds",
            name = "Flappy Arcade Flock",
            subtitle = "Duo of chunky pixel songbirds bobbing rhythmically across screen",
            category = DrifterCategory.MEME_PIXEL,
            effectType = VisualEffectType.SPEED_LINES,
            defaultMovement = MovementPreset.LEFT_TO_RIGHT,
            primaryColorHex = 0xFF00F0FF,
            secondaryColorHex = 0xFFFFBE0B,
            accentColorHex = 0xFFFF5C77
        ),

        // ================= C. COZY & AESTHETIC COLLECTION =================
        DrifterCharacter(
            id = "glowing_fireflies",
            name = "Lantern Fireflies",
            subtitle = "Warm golden-mint bioluminescent fireflies drifting in twilight",
            category = DrifterCategory.COZY_AESTHETIC,
            effectType = VisualEffectType.STARDUST_GLOW,
            defaultMovement = MovementPreset.GENTLE_AMBIENT_FLOATING,
            primaryColorHex = 0xFF00F5D4,
            secondaryColorHex = 0xFFFFBE0B,
            accentColorHex = 0xFF9D4EDD
        ),
        DrifterCharacter(
            id = "neon_butterflies",
            name = "Morpho Lume",
            subtitle = "Iridescent violet & cyan butterfly shedding shimmering stardust",
            category = DrifterCategory.COZY_AESTHETIC,
            effectType = VisualEffectType.STARDUST_GLOW,
            defaultMovement = MovementPreset.RANDOM_FLOATING,
            primaryColorHex = 0xFF9D4EDD,
            secondaryColorHex = 0xFF00F0FF,
            accentColorHex = 0xFFFF2A85
        ),
        DrifterCharacter(
            id = "autumn_leaves",
            name = "Maple Drift",
            subtitle = "Swirling amber, terracotta & crimson autumn leaves on a breeze",
            category = DrifterCategory.COZY_AESTHETIC,
            effectType = VisualEffectType.FLOATING_AURA,
            defaultMovement = MovementPreset.TOP_TO_BOTTOM,
            primaryColorHex = 0xFFFF7B3A,
            secondaryColorHex = 0xFFFFBE0B,
            accentColorHex = 0xFFFF2A85
        ),
        DrifterCharacter(
            id = "paper_planes",
            name = "Origami Glider",
            subtitle = "Crisp neon-edged folded paper airplane with dotted wind trail",
            category = DrifterCategory.COZY_AESTHETIC,
            effectType = VisualEffectType.SPEED_LINES,
            defaultMovement = MovementPreset.DIAGONAL,
            primaryColorHex = 0xFFE6F9FF,
            secondaryColorHex = 0xFF00F0FF,
            accentColorHex = 0xFF9D4EDD
        ),
        DrifterCharacter(
            id = "tiny_stars",
            name = "Wishlist Stars",
            subtitle = "Twinkling 4-point pastel shooting stars and cosmic sparkles",
            category = DrifterCategory.COZY_AESTHETIC,
            effectType = VisualEffectType.STARDUST_GLOW,
            defaultMovement = MovementPreset.GENTLE_AMBIENT_FLOATING,
            primaryColorHex = 0xFFFFBE0B,
            secondaryColorHex = 0xFFFF2A85,
            accentColorHex = 0xFF00F0FF
        ),
        DrifterCharacter(
            id = "glowing_particles",
            name = "Lo-Fi Aura Orbs",
            subtitle = "Soft bokeh floating light spheres for calm focus vibes",
            category = DrifterCategory.COZY_AESTHETIC,
            effectType = VisualEffectType.FLOATING_AURA,
            defaultMovement = MovementPreset.BOTTOM_TO_TOP,
            primaryColorHex = 0xFF9D4EDD,
            secondaryColorHex = 0xFF00F5D4,
            accentColorHex = 0xFFFF2A85
        ),
        DrifterCharacter(
            id = "dreamy_clouds",
            name = "Cotton Nebula",
            subtitle = "Sleepy pastel twilight puff cloud with a dangling crescent star",
            category = DrifterCategory.COZY_AESTHETIC,
            effectType = VisualEffectType.FLOATING_AURA,
            defaultMovement = MovementPreset.LEFT_TO_RIGHT,
            primaryColorHex = 0xFFD9C7FF,
            secondaryColorHex = 0xFF9D4EDD,
            accentColorHex = 0xFFFFBE0B
        ),

        // ================= D. VEHICLE & RETRO COLLECTION =================
        DrifterCharacter(
            id = "mini_helicopter",
            name = "Nighthawk Micro-Heli",
            subtitle = "Sleek vector night-ops helicopter with soft searchlight beam",
            category = DrifterCategory.VEHICLE_RETRO,
            effectType = VisualEffectType.PROPULSION_PUFFS,
            defaultMovement = MovementPreset.LEFT_TO_RIGHT,
            primaryColorHex = 0xFF28345A,
            secondaryColorHex = 0xFF00F0FF,
            accentColorHex = 0xFFFFBE0B
        ),
        DrifterCharacter(
            id = "tiny_airplane",
            name = "Sunset Biplane",
            subtitle = "Vintage twin-wing sky cruiser leaving cheerful skywriting puffs",
            category = DrifterCategory.VEHICLE_RETRO,
            effectType = VisualEffectType.PROPULSION_PUFFS,
            defaultMovement = MovementPreset.RIGHT_TO_LEFT,
            primaryColorHex = 0xFFFF5C77,
            secondaryColorHex = 0xFFFFBE0B,
            accentColorHex = 0xFFFFFFFF
        ),
        DrifterCharacter(
            id = "retro_flying_machine",
            name = "DaVinci Ornopter",
            subtitle = "Steampunk brass clockwork airship with flapping mechanical wings",
            category = DrifterCategory.VEHICLE_RETRO,
            effectType = VisualEffectType.SMOKE_TRAIL,
            defaultMovement = MovementPreset.LEFT_TO_RIGHT,
            primaryColorHex = 0xFFD99B4E,
            secondaryColorHex = 0xFF7D5224,
            accentColorHex = 0xFF00F5D4
        ),
        DrifterCharacter(
            id = "animated_ufo",
            name = "Roswell Cruiser",
            subtitle = "Chrome dome flying saucer with rotating neon plasma ring lights",
            category = DrifterCategory.VEHICLE_RETRO,
            effectType = VisualEffectType.ENERGY_PARTICLES,
            defaultMovement = MovementPreset.ZIGZAG,
            primaryColorHex = 0xFF8B9BB4,
            secondaryColorHex = 0xFF00F5D4,
            accentColorHex = 0xFFFF2A85
        ),
        DrifterCharacter(
            id = "small_spaceship",
            name = "Starhopper MK-IV",
            subtitle = "Retro-futuristic star cruiser with flickering cyan ion thruster",
            category = DrifterCategory.VEHICLE_RETRO,
            effectType = VisualEffectType.ENERGY_PARTICLES,
            defaultMovement = MovementPreset.BOTTOM_TO_TOP,
            primaryColorHex = 0xFFE2E8F0,
            secondaryColorHex = 0xFF00F0FF,
            accentColorHex = 0xFFFF5C77
        )
    )

    fun findById(id: String, customPresets: List<CustomDrifterPreset> = emptyList()): DrifterCharacter {
        builtInCharacters.find { it.id == id }?.let { return it }
        customPresets.find { it.id == id }?.let { preset ->
            return DrifterCharacter(
                id = preset.id,
                name = preset.name,
                subtitle = "Custom Studio Sticker • ${preset.movementPreset.label}",
                category = DrifterCategory.FAVORITES,
                effectType = VisualEffectType.STARDUST_GLOW,
                defaultMovement = preset.movementPreset,
                primaryColorHex = 0xFF00F0FF,
                secondaryColorHex = 0xFF9D4EDD,
                accentColorHex = 0xFFFF2A85,
                isCustom = true,
                customUriString = preset.uriString,
                customMimeType = preset.mimeType
            )
        }
        return builtInCharacters.first()
    }
}
