package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AmbientInterval
import com.example.model.DrifterCatalog
import com.example.model.DrifterCharacter
import com.example.model.FlyByPreferences
import com.example.model.MovementPreset
import com.example.model.OverlayRuntimeState
import com.example.ui.components.DrifterAnimatedThumbnail
import com.example.ui.components.LiveDrifterStagePreview
import com.example.ui.theme.AuroraMint
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.CardSurfaceGlass
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.DeepIndigoSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PlasmaPink
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    preferences: FlyByPreferences,
    runtimeState: OverlayRuntimeState,
    hasOverlayPermission: Boolean,
    previewBurstCounter: Int,
    onRequestOverlayPermission: () -> Unit,
    onToggleMasterOverlay: (Boolean) -> Unit,
    onQuickPlay: () -> Unit,
    onTogglePauseResume: () -> Unit,
    onToggleGamingFocus: (Boolean) -> Unit,
    onSelectCharacter: (DrifterCharacter) -> Unit,
    onSelectMovementPreset: (MovementPreset) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSizeChanged: (Float) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onAmbientIntervalChanged: (AmbientInterval) -> Unit,
    onToggleGhostMode: (Boolean) -> Unit,
    onNavigateToExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCharacter = DrifterCatalog.findById(
        preferences.selectedCharacterId,
        preferences.customPresets
    )
    val recentCharacters = preferences.recentIds.map {
        DrifterCatalog.findById(it, preferences.customPresets)
    }.distinctBy { it.id }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Branding & Master Overlay Toggle Card
        item {
            HeroOverlayCommandCard(
                preferences = preferences,
                runtimeState = runtimeState,
                hasOverlayPermission = hasOverlayPermission,
                onToggleMasterOverlay = onToggleMasterOverlay,
                onQuickPlay = onQuickPlay,
                onTogglePauseResume = onTogglePauseResume
            )
        }

        // 2. Permission Status & Explanation Card
        item {
            PermissionStatusCard(
                hasOverlayPermission = hasOverlayPermission,
                ghostMode = preferences.ghostMode,
                runtimeState = runtimeState,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onToggleGhostMode = onToggleGhostMode
            )
        }

        // 3. Selected Character & Live Stage Preview
        item {
            SelectedCharacterPreviewCard(
                character = selectedCharacter,
                preferences = preferences,
                runtimeState = runtimeState,
                previewBurstCounter = previewBurstCounter,
                onQuickPlay = onQuickPlay,
                onNavigateToExplore = onNavigateToExplore
            )
        }

        // 4. Quick Gaming / Focus Pause Bar
        item {
            GamingFocusQuickCard(
                gamingFocusMode = preferences.gamingFocusMode,
                onToggleGamingFocus = onToggleGamingFocus
            )
        }

        // 5. Movement Presets & Live Motion Tuning
        item {
            MovementAndTuningCard(
                preferences = preferences,
                runtimeState = runtimeState,
                onSelectMovementPreset = onSelectMovementPreset,
                onSpeedChanged = onSpeedChanged,
                onSizeChanged = onSizeChanged,
                onOpacityChanged = onOpacityChanged,
                onAmbientIntervalChanged = onAmbientIntervalChanged
            )
        }

        // 6. Recently Used Animations Carousel
        item {
            RecentlyUsedSection(
                recentCharacters = recentCharacters,
                selectedId = selectedCharacter.id,
                onSelectCharacter = onSelectCharacter,
                onNavigateToExplore = onNavigateToExplore
            )
        }

        // 7. Future Feature Teaser: Send a Fly-By (Clearly labelled Coming Soon per Spec 12)
        item {
            SendFlyByComingSoonCard()
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeroOverlayCommandCard(
    preferences: FlyByPreferences,
    runtimeState: OverlayRuntimeState,
    hasOverlayPermission: Boolean,
    onToggleMasterOverlay: (Boolean) -> Unit,
    onQuickPlay: () -> Unit,
    onTogglePauseResume: () -> Unit
) {
    val isRunning = runtimeState.isServiceRunning && runtimeState.isOverlayWindowAttached
    val statusText = when {
        !hasOverlayPermission -> "PERMISSION REQUIRED"
        preferences.gamingFocusMode -> "GAMING FOCUS PAUSED"
        isRunning && preferences.isPaused -> "OVERLAY PAUSED"
        isRunning -> "OVERLAY ACTIVE • ${runtimeState.activeDrifterCount} DRIFTING"
        else -> "OVERLAY STANDBY"
    }
    val statusColor = when {
        !hasOverlayPermission -> CyberAmber
        preferences.gamingFocusMode -> PlasmaPink
        isRunning && !preferences.isPaused -> AuroraMint
        isRunning && preferences.isPaused -> CyberAmber
        else -> TextSecondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = if (isRunning) {
                        listOf(AuroraMint, ElectricCyan, NeonViolet)
                    } else {
                        listOf(BorderSubtle, NeonViolet.copy(alpha = 0.5f))
                    }
                ),
                shape = RoundedCornerShape(26.dp)
            ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_drifters_1794066726812),
                contentDescription = "FlyBy twilight sky hero illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.36f
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ObsidianBg.copy(alpha = 0.35f),
                                DeepIndigoSurface.copy(alpha = 0.88f),
                                CardSurfaceElevated
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_launcher_icon_1794066727409),
                            contentDescription = "FlyBy Logo",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        )
                        Column {
                            Text(
                                text = "FlyBy",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Screen Drifters & Companions",
                                style = MaterialTheme.typography.bodySmall,
                                color = ElectricCyan
                            )
                        }
                    }

                    // Live Status Pill
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ObsidianBg.copy(alpha = 0.78f))
                            .border(1.dp, statusColor.copy(alpha = 0.6f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Master Overlay ON/OFF Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ObsidianBg.copy(alpha = 0.68f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRunning) "Screen Overlay Engine ON" else "Screen Overlay Engine OFF",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isRunning) {
                                "Drifters float above other apps • Trigger: ${runtimeState.lastTriggerSource}"
                            } else {
                                "Toggle ON to float companions over your other apps"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isRunning,
                        onCheckedChange = onToggleMasterOverlay,
                        modifier = Modifier.testTag("master_overlay_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = AuroraMint,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CardSurfaceGlass
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Play + Pause/Resume Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onQuickPlay,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quick_play_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = "Quick Play Drifter",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasOverlayPermission) "Quick Play Now" else "Preview in Stage",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onTogglePauseResume,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pause_resume_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet)
                    ) {
                        Icon(
                            imageVector = if (preferences.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (preferences.isPaused) "Resume animations" else "Pause animations",
                            tint = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (preferences.isPaused) "Resume" else "Pause",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    hasOverlayPermission: Boolean,
    ghostMode: Boolean,
    runtimeState: OverlayRuntimeState,
    onRequestOverlayPermission: () -> Unit,
    onToggleGhostMode: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasOverlayPermission) CardSurfaceElevated else Color(0xFF2B1C28)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        contentDescription = "Overlay Permission Status",
                        tint = if (hasOverlayPermission) AuroraMint else CyberAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (hasOverlayPermission) {
                                "Overlay Permission Granted"
                            } else {
                                "Display Over Other Apps Required"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = if (hasOverlayPermission) {
                                "FlyBy can draw non-intrusive companions above other apps."
                            } else {
                                "Android requires 'Display over other apps' so companions can drift across your screen while using other apps."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                if (!hasOverlayPermission) {
                    Button(
                        onClick = onRequestOverlayPermission,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("grant_overlay_permission_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberAmber,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Ghost Touch / Pass-through Mode bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianBg.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (ghostMode) Icons.Default.Security else Icons.Default.TouchApp,
                        contentDescription = "Ghost Mode status",
                        tint = if (ghostMode) ElectricCyan else PlasmaPink,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = if (ghostMode) {
                                "Ghost Mode ON (100% Touch Pass-Through)"
                            } else {
                                "Interactive Mode ON (Taps Bounce Drifters)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (ghostMode) ElectricCyan else PlasmaPink
                        )
                        Text(
                            text = if (ghostMode) {
                                "Touches pass straight through to underlying apps. Effective opacity: ${(runtimeState.effectiveAppliedOpacity * 100).roundToInt()}%."
                            } else {
                                "Drifters react to taps. Note: Interactive overlays may intercept touches on some Android versions."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Switch(
                    checked = ghostMode,
                    onCheckedChange = onToggleGhostMode,
                    modifier = Modifier.testTag("home_ghost_mode_switch")
                )
            }
        }
    }
}

@Composable
private fun SelectedCharacterPreviewCard(
    character: DrifterCharacter,
    preferences: FlyByPreferences,
    runtimeState: OverlayRuntimeState,
    previewBurstCounter: Int,
    onQuickPlay: () -> Unit,
    onNavigateToExplore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SELECTED DRIFTER PREVIEW",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${character.category.title} • ${character.effectType.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                OutlinedButton(
                    onClick = onNavigateToExplore,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("change_drifter_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Change", color = ElectricCyan)
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                LiveDrifterStagePreview(
                    character = character,
                    movementPreset = preferences.selectedMovementPreset,
                    speedMultiplier = preferences.speedMultiplier,
                    sizeMultiplier = preferences.sizeMultiplier,
                    opacity = preferences.opacity,
                    isPaused = preferences.isPaused,
                    previewBurstTrigger = previewBurstCounter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                )

                // Top-left telemetry pill on stage
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg.copy(alpha = 0.75f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${runtimeState.currentFps} FPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = AuroraMint
                    )
                    Text(
                        text = "• ${preferences.selectedMovementPreset.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun GamingFocusQuickCard(
    gamingFocusMode: Boolean,
    onToggleGamingFocus: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (gamingFocusMode) PlasmaPink else BorderSubtle,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (gamingFocusMode) Color(0xFF331428) else CardSurfaceElevated
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = "Gaming and Focus Mode",
                    tint = if (gamingFocusMode) PlasmaPink else NeonViolet,
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text(
                        text = "Gaming / Focus Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = if (gamingFocusMode) {
                            "Ambient & unlock triggers suspended. Tap switch to resume."
                        } else {
                            "One-tap suspend for ranked matches or deep study sessions."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Switch(
                checked = gamingFocusMode,
                onCheckedChange = onToggleGamingFocus,
                modifier = Modifier.testTag("gaming_focus_switch")
            )
        }
    }
}

@Composable
private fun MovementAndTuningCard(
    preferences: FlyByPreferences,
    runtimeState: OverlayRuntimeState,
    onSelectMovementPreset: (MovementPreset) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSizeChanged: (Float) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onAmbientIntervalChanged: (AmbientInterval) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Motion Engine Controls",
                    tint = ElectricCyan
                )
                Text(
                    text = "Movement & Frequency Controls",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            Text(
                text = "Movement Path Preset",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(MovementPreset.entries) { preset ->
                    val selected = preset == preferences.selectedMovementPreset
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectMovementPreset(preset) },
                        label = { Text(preset.label) },
                        modifier = Modifier.testTag("movement_chip_${preset.id}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricCyan
                        )
                    )
                }
            }

            // Speed Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Flight Speed", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(
                        String.format("%.1fx", preferences.speedMultiplier),
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                }
                Slider(
                    value = preferences.speedMultiplier,
                    onValueChange = onSpeedChanged,
                    valueRange = 0.4f..2.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricCyan
                    )
                )
            }

            // Size Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Drifter Size", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(
                        String.format("%.1fx", preferences.sizeMultiplier),
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonViolet
                    )
                }
                Slider(
                    value = preferences.sizeMultiplier,
                    onValueChange = onSizeChanged,
                    valueRange = 0.5f..2.2f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonViolet,
                        activeTrackColor = NeonViolet
                    )
                )
            }

            // Opacity Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Overlay Opacity", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(
                        "${(preferences.opacity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = AuroraMint
                    )
                }
                Slider(
                    value = preferences.opacity,
                    onValueChange = onOpacityChanged,
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AuroraMint,
                        activeTrackColor = AuroraMint
                    )
                )
                AnimatedVisibility(visible = runtimeState.isOpacityClampedForGhostTouch) {
                    Text(
                        text = "Android 12+ Ghost Touch Guard: Window opacity auto-clamped to ${(runtimeState.effectiveAppliedOpacity * 100).roundToInt()}% so touches pass through reliably.",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberAmber
                    )
                }
            }

            // Ambient Timer Frequency
            Text(
                text = "Ambient Fly-By Frequency",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AmbientInterval.entries) { interval ->
                    val selected = interval == preferences.ambientInterval
                    FilterChip(
                        selected = selected,
                        onClick = { onAmbientIntervalChanged(interval) },
                        label = { Text(interval.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonViolet.copy(alpha = 0.25f),
                            selectedLabelColor = TextPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentlyUsedSection(
    recentCharacters: List<DrifterCharacter>,
    selectedId: String,
    onSelectCharacter: (DrifterCharacter) -> Unit,
    onNavigateToExplore: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recently Used Drifters",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = "Explore All →",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onNavigateToExplore)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recentCharacters, key = { it.id }) { item ->
                val isSelected = item.id == selectedId
                Card(
                    modifier = Modifier
                        .width(136.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) ElectricCyan else BorderSubtle,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onSelectCharacter(item) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DrifterAnimatedThumbnail(
                            character = item,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.defaultMovement.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SendFlyByComingSoonCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepIndigoSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonViolet.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Coming Soon Feature",
                    tint = NeonViolet
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Send a Fly-By to Friends",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "COMING SOON",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberAmber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberAmber.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "Future cloud relay feature to beam surprise screen drifters to a friend's phone. Currently offline-first for maximum privacy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
