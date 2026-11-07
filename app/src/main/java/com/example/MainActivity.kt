package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.DrifterCatalog
import com.example.model.MainNavTab
import com.example.ui.screens.CustomStudioScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AuroraMint
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DeepIndigoSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FlyByTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.FlyByViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlyByTheme {
                FlyByRootApp()
            }
        }
    }
}

@Composable
fun FlyByRootApp(
    viewModel: FlyByViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val runtimeState by viewModel.runtimeState.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()
    val previewBurstCounter by viewModel.previewBurstCounter.collectAsStateWithLifecycle()
    val studioDraft by viewModel.studioDraft.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    // Refresh overlay permission & Battery Saver status whenever returning to app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemDiagnostics()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // BackHandler on secondary tabs returns to HOME
    BackHandler(enabled = currentTab != MainNavTab.HOME) {
        viewModel.selectTab(MainNavTab.HOME)
    }

    // Runtime notification permission launcher for Android 13+ foreground service notification
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Overlay works regardless of notification permission, though notification helps stop/pause
    }

    // System Overlay Permission settings launcher
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemDiagnostics()
    }

    val openOverlaySettings = {
        try {
            val intent = viewModel.buildOverlayPermissionIntent(context)
            overlayPermissionLauncher.launch(intent)
        } catch (_: Exception) {
            try {
                overlayPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            } catch (_: Exception) {
            }
        }
    }

    val handleMasterOverlayToggle: (Boolean) -> Unit = { enable ->
        if (enable && !hasOverlayPermission) {
            openOverlaySettings()
        } else {
            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            viewModel.toggleMasterOverlay(enable)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavigationRail) {
                    FlyByBottomNavigationBar(
                        currentTab = currentTab,
                        onSelectTab = viewModel::selectTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    FlyBySideNavigationRail(
                        currentTab = currentTab,
                        onSelectTab = viewModel::selectTab
                    )
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // Dismissible status banner
                    AnimatedVisibility(
                        visible = statusBannerMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        statusBannerMessage?.let { msg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CardSurfaceElevated)
                                    .border(1.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AuroraMint,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = viewModel::dismissBannerMessage,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss message",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentTab) {
                            MainNavTab.HOME -> {
                                HomeScreen(
                                    preferences = preferences,
                                    runtimeState = runtimeState,
                                    hasOverlayPermission = hasOverlayPermission,
                                    previewBurstCounter = previewBurstCounter,
                                    onRequestOverlayPermission = openOverlaySettings,
                                    onToggleMasterOverlay = handleMasterOverlayToggle,
                                    onQuickPlay = viewModel::triggerQuickPlay,
                                    onTogglePauseResume = viewModel::togglePauseResume,
                                    onToggleGamingFocus = viewModel::setGamingFocusMode,
                                    onSelectCharacter = { char ->
                                        viewModel.selectCharacter(char, launchImmediately = false)
                                    },
                                    onSelectMovementPreset = viewModel::setMovementPreset,
                                    onSpeedChanged = viewModel::setSpeedMultiplier,
                                    onSizeChanged = viewModel::setSizeMultiplier,
                                    onOpacityChanged = viewModel::setOpacity,
                                    onAmbientIntervalChanged = viewModel::setAmbientInterval,
                                    onToggleGhostMode = viewModel::setGhostMode,
                                    onNavigateToExplore = {
                                        viewModel.selectTab(MainNavTab.EXPLORE)
                                    }
                                )
                            }
                            MainNavTab.EXPLORE -> {
                                ExploreScreen(
                                    preferences = preferences,
                                    selectedCategory = selectedCategory,
                                    onSelectCategory = viewModel::selectExploreCategory,
                                    onSelectCharacter = { char, deployNow ->
                                        viewModel.selectCharacter(char, launchImmediately = deployNow)
                                    },
                                    onToggleFavorite = viewModel::toggleFavorite
                                )
                            }
                            MainNavTab.STUDIO -> {
                                CustomStudioScreen(
                                    preferences = preferences,
                                    studioDraft = studioDraft,
                                    previewBurstCounter = previewBurstCounter,
                                    onStickerUriPicked = viewModel::onCustomStickerPicked,
                                    onUpdateDraft = viewModel::updateStudioDraft,
                                    onSaveCustomPreset = viewModel::saveCustomStudioPreset,
                                    onLaunchCustomPreset = { preset ->
                                        val char = DrifterCatalog.findById(preset.id, preferences.customPresets)
                                        viewModel.selectCharacter(char, launchImmediately = true)
                                    },
                                    onDeleteCustomPreset = viewModel::deleteCustomPreset
                                )
                            }
                            MainNavTab.SETTINGS -> {
                                SettingsScreen(
                                    preferences = preferences,
                                    runtimeState = runtimeState,
                                    hasOverlayPermission = hasOverlayPermission,
                                    compatibilityNotes = viewModel.getDeviceCompatibilityNotes(),
                                    onToggleMasterOverlay = handleMasterOverlayToggle,
                                    onToggleGhostMode = viewModel::setGhostMode,
                                    onToggleUnlockTrigger = viewModel::setUnlockTriggerEnabled,
                                    onUnlockCooldownChanged = viewModel::setUnlockCooldownSeconds,
                                    onAmbientIntervalChanged = viewModel::setAmbientInterval,
                                    onCustomAmbientMinutesChanged = viewModel::setCustomAmbientMinutes,
                                    onAnimationQualityChanged = viewModel::setAnimationQuality,
                                    onSpeedChanged = viewModel::setSpeedMultiplier,
                                    onSizeChanged = viewModel::setSizeMultiplier,
                                    onStartDelayChanged = viewModel::setStartDelaySeconds,
                                    onRepetitionCountChanged = viewModel::setRepetitionCount,
                                    onToggleGamingFocus = viewModel::setGamingFocusMode,
                                    onQuietHoursChanged = viewModel::setQuietHours,
                                    onToggleNotificationControls = viewModel::setShowNotificationControls,
                                    onResetPreferences = viewModel::resetPreferences
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlyByBottomNavigationBar(
    currentTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit
) {
    NavigationBar(
        containerColor = DeepIndigoSurface,
        contentColor = TextPrimary,
        tonalElevation = 8.dp,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .border(width = 1.dp, color = BorderSubtle)
    ) {
        MainNavTab.entries.forEach { tab ->
            val selected = tab == currentTab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = getNavTabIcon(tab, selected),
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier.testTag("nav_tab_${tab.route}"),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ObsidianBg,
                    selectedTextColor = ElectricCyan,
                    indicatorColor = ElectricCyan,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}

@Composable
private fun FlyBySideNavigationRail(
    currentTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit
) {
    NavigationRail(
        containerColor = DeepIndigoSurface,
        contentColor = TextPrimary
    ) {
        MainNavTab.entries.forEach { tab ->
            val selected = tab == currentTab
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = getNavTabIcon(tab, selected),
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier.testTag("nav_rail_${tab.route}"),
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = ObsidianBg,
                    selectedTextColor = ElectricCyan,
                    indicatorColor = ElectricCyan,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}

private fun getNavTabIcon(tab: MainNavTab, selected: Boolean): ImageVector {
    return when (tab) {
        MainNavTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
        MainNavTab.EXPLORE -> if (selected) Icons.Filled.Explore else Icons.Outlined.Explore
        MainNavTab.STUDIO -> if (selected) Icons.Filled.AutoFixHigh else Icons.Outlined.AutoFixHigh
        MainNavTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
    }
}
