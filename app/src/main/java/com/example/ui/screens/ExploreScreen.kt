package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DrifterCatalog
import com.example.model.DrifterCategory
import com.example.model.DrifterCharacter
import com.example.model.FlyByPreferences
import com.example.ui.components.DrifterAnimatedThumbnail
import com.example.ui.theme.AuroraMint
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.DeepIndigoSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PlasmaPink
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ExploreScreen(
    preferences: FlyByPreferences,
    selectedCategory: DrifterCategory,
    onSelectCategory: (DrifterCategory) -> Unit,
    onSelectCharacter: (DrifterCharacter, Boolean) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val customCharacters = remember(preferences.customPresets) {
        preferences.customPresets.map { preset ->
            DrifterCatalog.findById(preset.id, preferences.customPresets)
        }
    }

    val allCharacters = remember(customCharacters) {
        DrifterCatalog.builtInCharacters + customCharacters
    }

    val filteredCharacters = remember(selectedCategory, searchQuery, preferences.favoriteIds, allCharacters) {
        allCharacters.filter { character ->
            val matchesQuery = searchQuery.isBlank() ||
                character.name.contains(searchQuery, ignoreCase = true) ||
                character.subtitle.contains(searchQuery, ignoreCase = true) ||
                character.effectType.label.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                DrifterCategory.FAVORITES -> preferences.favoriteIds.contains(character.id) || character.isCustom
                else -> character.category == selectedCategory
            }
            matchesQuery && matchesCategory
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("explore_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Explore Collection",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "23 original procedural companions + your Custom Studio stickers",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Search Field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search crows, pixel cats, fireflies, UFOs...", color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search animations",
                        tint = ElectricCyan
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = CardSurfaceElevated,
                    unfocusedContainerColor = CardSurfaceElevated
                )
            )
        }

        // Category Folder Cards
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(DrifterCategory.entries) { category ->
                    val isSelected = category == selectedCategory
                    val accent = Color(category.badgeColorHex)
                    Card(
                        modifier = Modifier
                            .width(172.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) accent else BorderSubtle,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onSelectCategory(category) }
                            .testTag("category_folder_${category.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) accent.copy(alpha = 0.16f) else CardSurfaceElevated
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            )
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = category.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (filteredCharacters.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No matching drifters in this folder",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap the heart icon on any animation card to pin it to Favorites, or clear your search filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredCharacters, key = { it.id }) { character ->
                ExploreCharacterCard(
                    character = character,
                    isSelected = character.id == preferences.selectedCharacterId,
                    isFavorite = preferences.favoriteIds.contains(character.id),
                    onSelectOnly = { onSelectCharacter(character, false) },
                    onDeployNow = { onSelectCharacter(character, true) },
                    onToggleFavorite = { onToggleFavorite(character.id) }
                )
            }
        }

        // Future Feature Teaser: Premium Packs (Clearly labelled Coming Soon per Spec 12)
        item {
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
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Coming Soon Packs",
                        tint = CyberAmber,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Community & Artist Theme Packs",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "COMING SOON",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "All 23 original animations & Custom Studio imports are 100% unlocked with zero ads in this release.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ExploreCharacterCard(
    character: DrifterCharacter,
    isSelected: Boolean,
    isFavorite: Boolean,
    onSelectOnly: () -> Unit,
    onDeployNow: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val accent = Color(character.secondaryColorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) ElectricCyan else BorderSubtle,
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("drifter_card_${character.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrifterAnimatedThumbnail(
                    character = character,
                    modifier = Modifier.size(84.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = character.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        if (isSelected) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = AuroraMint,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AuroraMint.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = character.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FX: ${character.effectType.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                        Text(
                            text = "• ${character.defaultMovement.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("favorite_btn_${character.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) PlasmaPink else TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSelectOnly,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("preview_select_${character.id}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Preview Drifter",
                        modifier = Modifier.size(18.dp),
                        tint = ElectricCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview", color = ElectricCyan)
                }

                Button(
                    onClick = onDeployNow,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("deploy_drifter_${character.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonViolet,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = "Launch Drifter",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch Fly-By")
                }
            }
        }
    }
}
