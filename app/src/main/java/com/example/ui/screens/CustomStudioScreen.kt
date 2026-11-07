package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CustomDrifterPreset
import com.example.model.DrifterCatalog
import com.example.model.DrifterCategory
import com.example.model.DrifterCharacter
import com.example.model.FlyByPreferences
import com.example.model.MovementPreset
import com.example.model.VisualEffectType
import com.example.ui.components.DrifterAnimatedThumbnail
import com.example.ui.components.LiveDrifterStagePreview
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.StudioDraftState
import kotlin.math.roundToInt

@Composable
fun CustomStudioScreen(
    preferences: FlyByPreferences,
    studioDraft: StudioDraftState,
    previewBurstCounter: Int,
    onStickerUriPicked: (Uri) -> Unit,
    onUpdateDraft: ((StudioDraftState) -> StudioDraftState) -> Unit,
    onSaveCustomPreset: () -> Unit,
    onLaunchCustomPreset: (CustomDrifterPreset) -> Unit,
    onDeleteCustomPreset: (CustomDrifterPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onStickerUriPicked(uri)
        }
    }

    // Zero-permission Storage Access Framework (SAF) Document Picker with persistable URI access
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onStickerUriPicked(uri)
        }
    }

    val previewCharacter = remember(studioDraft) {
        if (!studioDraft.selectedUriString.isNullOrBlank()) {
            DrifterCharacter(
                id = "studio_draft_preview",
                name = studioDraft.customName.ifBlank { "Custom Sticker Preview" },
                subtitle = "Local Device Sticker • ${studioDraft.detectedMimeType}",
                category = DrifterCategory.FAVORITES,
                effectType = VisualEffectType.STARDUST_GLOW,
                defaultMovement = studioDraft.movementPreset,
                primaryColorHex = 0xFF00F0FF,
                secondaryColorHex = 0xFF9D4EDD,
                accentColorHex = 0xFFFF2A85,
                isCustom = true,
                customUriString = studioDraft.selectedUriString,
                customMimeType = studioDraft.detectedMimeType
            )
        } else {
            DrifterCatalog.builtInCharacters.first()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("custom_studio_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Custom Drifter Studio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Import transparent PNG, WebP, or GIF stickers from your phone. 100% local & private — never uploaded anywhere.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // 1. Media Import & Live Studio Stage Card
        item {
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
                        Column {
                            Text(
                                text = if (studioDraft.selectedUriString != null) {
                                    "LIVE CUSTOM STICKER STAGE"
                                } else {
                                    "STUDIO STAGE (SELECT STICKER BELOW)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                            Text(
                                text = previewCharacter.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = studioDraft.movementPreset.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = AuroraMint
                        )
                    }

                    LiveDrifterStagePreview(
                        character = previewCharacter,
                        movementPreset = studioDraft.movementPreset,
                        speedMultiplier = studioDraft.speedMultiplier,
                        sizeMultiplier = studioDraft.sizeMultiplier,
                        opacity = studioDraft.opacity,
                        isPaused = false,
                        previewBurstTrigger = previewBurstCounter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                    )

                    // Validation Error / Success Banner
                    if (studioDraft.validationError != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CoralAlert.copy(alpha = 0.16f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Validation Error",
                                tint = CoralAlert
                            )
                            Text(
                                text = studioDraft.validationError,
                                style = MaterialTheme.typography.bodySmall,
                                color = CoralAlert
                            )
                        }
                    }

                    if (studioDraft.validationSuccessMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AuroraMint.copy(alpha = 0.15f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Validation Success",
                                tint = AuroraMint
                            )
                            Text(
                                text = studioDraft.validationSuccessMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = AuroraMint
                            )
                        }
                    }

                    // Dual Zero-Permission Pickers (Photo Picker + SAF Document Picker)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("studio_photo_picker_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Pick Sticker from Gallery",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Photo Picker", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = {
                                documentPickerLauncher.launch(
                                    arrayOf("image/png", "image/webp", "image/gif")
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("studio_saf_picker_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Pick File via Document Picker",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Files (PNG/GIF)", color = TextPrimary)
                        }
                    }
                }
            }
        }

        // 2. Preset Builder Controls Card
        item {
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
                    Text(
                        text = "Customize Sticker Preset",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = studioDraft.customName,
                        onValueChange = { name ->
                            onUpdateDraft { it.copy(customName = name) }
                        },
                        label = { Text("Sticker Preset Name") },
                        placeholder = { Text("e.g., Pixel Frog, My Chibi Mascot", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_preset_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )

                    Text(
                        text = "Movement Path",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MovementPreset.entries) { preset ->
                            FilterChip(
                                selected = studioDraft.movementPreset == preset,
                                onClick = { onUpdateDraft { it.copy(movementPreset = preset) } },
                                label = { Text(preset.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonViolet.copy(alpha = 0.25f),
                                    selectedLabelColor = TextPrimary
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
                            Text("Speed", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                String.format("%.1fx", studioDraft.speedMultiplier),
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                        }
                        Slider(
                            value = studioDraft.speedMultiplier,
                            onValueChange = { v -> onUpdateDraft { it.copy(speedMultiplier = v) } },
                            valueRange = 0.4f..2.5f,
                            colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                        )
                    }

                    // Size Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Size", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                String.format("%.1fx", studioDraft.sizeMultiplier),
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonViolet
                            )
                        }
                        Slider(
                            value = studioDraft.sizeMultiplier,
                            onValueChange = { v -> onUpdateDraft { it.copy(sizeMultiplier = v) } },
                            valueRange = 0.5f..2.2f,
                            colors = SliderDefaults.colors(thumbColor = NeonViolet, activeTrackColor = NeonViolet)
                        )
                    }

                    // Opacity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Opacity", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                "${(studioDraft.opacity * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = AuroraMint
                            )
                        }
                        Slider(
                            value = studioDraft.opacity,
                            onValueChange = { v -> onUpdateDraft { it.copy(opacity = v) } },
                            valueRange = 0.2f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = AuroraMint, activeTrackColor = AuroraMint)
                        )
                    }

                    // Appearance Frequency
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Spawn Interval", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                "Every ${studioDraft.spawnIntervalSeconds}s",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberAmber
                            )
                        }
                        Slider(
                            value = studioDraft.spawnIntervalSeconds.toFloat(),
                            onValueChange = { v -> onUpdateDraft { it.copy(spawnIntervalSeconds = v.roundToInt()) } },
                            valueRange = 5f..60f,
                            colors = SliderDefaults.colors(thumbColor = CyberAmber, activeTrackColor = CyberAmber)
                        )
                    }

                    Button(
                        onClick = onSaveCustomPreset,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_custom_preset_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonViolet,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save Custom Preset")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Custom Preset", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // 3. Saved Custom Presets Library
        item {
            Text(
                text = "Your Saved Custom Presets (${preferences.customPresets.size})",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }

        if (preferences.customPresets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepIndigoSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "No custom stickers saved yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pick any PNG, WebP, or GIF image above, tune its flight path, and tap Save Custom Preset.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(preferences.customPresets, key = { it.id }) { preset ->
                val customChar = DrifterCatalog.findById(preset.id, preferences.customPresets)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (preferences.selectedCharacterId == preset.id) ElectricCyan else BorderSubtle,
                            shape = RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrifterAnimatedThumbnail(
                            character = customChar,
                            modifier = Modifier.size(64.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "${preset.movementPreset.label} • ${String.format("%.1fx", preset.speedMultiplier)} speed",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        IconButton(
                            onClick = { onLaunchCustomPreset(preset) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = "Activate custom preset",
                                tint = ElectricCyan
                            )
                        }
                        IconButton(
                            onClick = { onDeleteCustomPreset(preset) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete custom preset",
                                tint = PlasmaPink
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
