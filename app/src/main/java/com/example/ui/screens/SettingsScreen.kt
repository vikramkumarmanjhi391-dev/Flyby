package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AmbientInterval
import com.example.model.AnimationQuality
import com.example.model.FlyByPreferences
import com.example.model.OverlayRuntimeState
import com.example.ui.theme.AuroraMint
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.DeepIndigoSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PlasmaPink
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    preferences: FlyByPreferences,
    runtimeState: OverlayRuntimeState,
    hasOverlayPermission: Boolean,
    compatibilityNotes: List<String>,
    onToggleMasterOverlay: (Boolean) -> Unit,
    onToggleGhostMode: (Boolean) -> Unit,
    onToggleUnlockTrigger: (Boolean) -> Unit,
    onUnlockCooldownChanged: (Int) -> Unit,
    onAmbientIntervalChanged: (AmbientInterval) -> Unit,
    onCustomAmbientMinutesChanged: (Int) -> Unit,
    onAnimationQualityChanged: (AnimationQuality) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSizeChanged: (Float) -> Unit,
    onStartDelayChanged: (Int) -> Unit,
    onRepetitionCountChanged: (Int) -> Unit,
    onToggleGamingFocus: (Boolean) -> Unit,
    onQuietHoursChanged: (Boolean, Int, Int) -> Unit,
    onToggleNotificationControls: (Boolean) -> Unit,
    onResetPreferences: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Settings & Diagnostics",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Overlay engine behavior, smart triggers, battery optimization & compatibility telemetry",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // 1. Master Overlay & Ghost Touch Controls
        item {
            SettingsCard(title = "Overlay Engine & Touch Pass-Through", icon = Icons.Default.Security) {
                SettingsToggleRow(
                    title = "Master Overlay ON / OFF",
                    subtitle = "Runs the FlyBy foreground window above other apps",
                    checked = runtimeState.isServiceRunning && runtimeState.isOverlayWindowAttached,
                    onCheckedChange = onToggleMasterOverlay,
                    testTag = "settings_master_overlay_switch"
                )
                HorizontalDivider(color = BorderSubtle)
                SettingsToggleRow(
                    title = "Ghost Mode (Touch Pass-Through)",
                    subtitle = "Default ON. Uses FLAG_NOT_TOUCHABLE and clamps Android 12+ opacity so touches never block underlying apps.",
                    checked = preferences.ghostMode,
                    onCheckedChange = onToggleGhostMode,
                    testTag = "settings_ghost_mode_switch"
                )
                HorizontalDivider(color = BorderSubtle)
                SettingsToggleRow(
                    title = "Show Pause Action in Notification",
                    subtitle = "Include quick Pause/Resume button alongside Stop FlyBy in the persistent notification",
                    checked = preferences.showNotificationControls,
                    onCheckedChange = onToggleNotificationControls,
                    testTag = "settings_notif_controls_switch"
                )
            }
        }

        // 2. Smart Triggers (Unlock Trigger, Ambient Timer, Gaming Focus, Quiet Hours)
        item {
            SettingsCard(title = "Smart Triggers & Schedules", icon = Icons.Default.Schedule) {
                SettingsToggleRow(
                    title = "Play Animation When Phone Unlocked",
                    subtitle = "Listens for ACTION_USER_PRESENT while FlyBy service is active. Never draws over the lock screen.",
                    checked = preferences.unlockTriggerEnabled,
                    onCheckedChange = onToggleUnlockTrigger,
                    testTag = "settings_unlock_trigger_switch"
                )

                if (preferences.unlockTriggerEnabled) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Unlock Cooldown", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                "${preferences.unlockCooldownSeconds}s cooldown",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                        }
                        Slider(
                            value = preferences.unlockCooldownSeconds.toFloat(),
                            onValueChange = { onUnlockCooldownChanged(it.roundToInt()) },
                            valueRange = 15f..180f,
                            colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                Text(
                    text = "Ambient Timer Interval",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AmbientInterval.entries) { interval ->
                        FilterChip(
                            selected = preferences.ambientInterval == interval,
                            onClick = { onAmbientIntervalChanged(interval) },
                            label = { Text(interval.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricCyan
                            )
                        )
                    }
                }

                if (preferences.ambientInterval == AmbientInterval.CUSTOM) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Custom Timer Interval", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                "Every ${preferences.customAmbientMinutes} min",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonViolet
                            )
                        }
                        Slider(
                            value = preferences.customAmbientMinutes.toFloat(),
                            onValueChange = { onCustomAmbientMinutesChanged(it.roundToInt()) },
                            valueRange = 1f..60f,
                            colors = SliderDefaults.colors(thumbColor = NeonViolet, activeTrackColor = NeonViolet)
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                SettingsToggleRow(
                    title = "Gaming / Focus Mode Pause",
                    subtitle = "Temporarily suspends ambient and unlock animations without requiring Usage Access permission",
                    checked = preferences.gamingFocusMode,
                    onCheckedChange = onToggleGamingFocus,
                    testTag = "settings_gaming_focus_switch"
                )

                HorizontalDivider(color = BorderSubtle)

                SettingsToggleRow(
                    title = "Quiet Hours (${preferences.quietStartHour}:00 – 0${preferences.quietEndHour}:00)",
                    subtitle = "Automatically suppress scheduled ambient fly-bys overnight",
                    checked = preferences.quietHoursEnabled,
                    onCheckedChange = { enabled ->
                        onQuietHoursChanged(enabled, preferences.quietStartHour, preferences.quietEndHour)
                    },
                    testTag = "settings_quiet_hours_switch"
                )
            }
        }

        // 3. Performance, Quality & Default Motion Tuning
        item {
            SettingsCard(title = "Performance & Animation Quality", icon = Icons.Default.BatteryChargingFull) {
                Text(
                    text = "Animation Quality Mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimationQuality.entries.forEach { quality ->
                        val selected = preferences.animationQuality == quality
                        Card(
                            onClick = { onAnimationQualityChanged(quality) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) AuroraMint else BorderSubtle,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .testTag("quality_option_${quality.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) AuroraMint.copy(alpha = 0.14f) else ObsidianBg
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = quality.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) AuroraMint else TextPrimary
                                )
                                Text(
                                    text = quality.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // Default Speed
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Default Animation Speed", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            String.format("%.1fx", preferences.speedMultiplier),
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                    }
                    Slider(
                        value = preferences.speedMultiplier,
                        onValueChange = onSpeedChanged,
                        valueRange = 0.4f..2.5f
                    )
                }

                // Default Size
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Default Character Size", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            String.format("%.1fx", preferences.sizeMultiplier),
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonViolet
                        )
                    }
                    Slider(
                        value = preferences.sizeMultiplier,
                        onValueChange = onSizeChanged,
                        valueRange = 0.5f..2.2f
                    )
                }

                // Start Delay & Passes per Spawn
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Start Delay", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            "${preferences.startDelaySeconds}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberAmber
                        )
                    }
                    Slider(
                        value = preferences.startDelaySeconds.toFloat(),
                        onValueChange = { onStartDelayChanged(it.roundToInt()) },
                        valueRange = 0f..15f
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Screen Passes per Launch", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            if (preferences.repetitionCount == 0) "Continuous" else "${preferences.repetitionCount} passes",
                            style = MaterialTheme.typography.labelMedium,
                            color = AuroraMint
                        )
                    }
                    Slider(
                        value = preferences.repetitionCount.toFloat(),
                        onValueChange = { onRepetitionCountChanged(it.roundToInt()) },
                        valueRange = 0f..10f
                    )
                }
            }
        }

        // 4. REQUIRED DIAGNOSTICS SECTION (Acceptance Criteria 16)
        item {
            SettingsCard(title = "System & Overlay Diagnostics", icon = Icons.Default.BugReport) {
                DiagnosticRow("Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                DiagnosticRow(
                    "Overlay Permission",
                    if (hasOverlayPermission) "GRANTED (Settings.canDrawOverlays = true)" else "DENIED / NOT GRANTED",
                    valueColor = if (hasOverlayPermission) AuroraMint else CyberAmber
                )
                DiagnosticRow(
                    "Overlay Service Status",
                    when {
                        runtimeState.isServiceRunning && runtimeState.isOverlayWindowAttached -> "RUNNING (Window Attached)"
                        runtimeState.isServiceRunning -> "RUNNING (Standby)"
                        else -> "STOPPED"
                    },
                    valueColor = if (runtimeState.isServiceRunning) AuroraMint else TextSecondary
                )
                DiagnosticRow("Current Animation", runtimeState.currentCharacterName)
                DiagnosticRow("Current Movement Preset", runtimeState.currentMovementLabel)
                DiagnosticRow(
                    "Active Quality Mode",
                    "${preferences.animationQuality.label} (${runtimeState.currentFps} FPS target)"
                )
                DiagnosticRow(
                    "Ghost Touch Max Opacity",
                    "${(runtimeState.maxAllowedUntrustedTouchOpacity * 100).roundToInt()}% (Applied: ${(runtimeState.effectiveAppliedOpacity * 100).roundToInt()}%)"
                )
                DiagnosticRow(
                    "Android Battery Saver",
                    if (runtimeState.isPowerSaveMode) "ACTIVE (Throttled to 20 FPS)" else "INACTIVE"
                )
                DiagnosticRow(
                    "Last Trigger Source",
                    "${runtimeState.lastTriggerSource} (${runtimeState.activeDrifterCount} active on screen)"
                )

                if (runtimeState.lastErrorMessage != null) {
                    Text(
                        text = "Last Notice: ${runtimeState.lastErrorMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralAlert
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                Text(
                    text = "Device & OS Compatibility Notes",
                    style = MaterialTheme.typography.titleSmall,
                    color = ElectricCyan
                )
                compatibilityNotes.forEach { note ->
                    Text(
                        text = "• $note",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // 5. Privacy, About & Reset
        item {
            SettingsCard(title = "Privacy & About FlyBy", icon = Icons.Default.PrivacyTip) {
                Text(
                    text = "FlyBy v1.0 — Screen Drifters & Companions",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "• Zero Screen Capture: FlyBy only draws its own animated companion layer and never reads or records your screen contents.\n" +
                        "• Strict Permission Minimization: No camera, microphone, location, contacts, Accessibility Service, or Usage Access permissions are ever requested.\n" +
                        "• Lock Screen Protection: Animations automatically pause when the screen turns off or when the lock screen is active.\n" +
                        "• 100% Local Storage: All custom stickers and preferences stay exclusively on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedButton(
                    onClick = onResetPreferences,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reset_preferences_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Preferences",
                        tint = PlasmaPink
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset All Preferences to Default", color = PlasmaPink)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
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
                Icon(imageVector = icon, contentDescription = title, tint = ElectricCyan)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DeepIndigoSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = valueColor,
            modifier = Modifier.weight(0.58f)
        )
    }
}
