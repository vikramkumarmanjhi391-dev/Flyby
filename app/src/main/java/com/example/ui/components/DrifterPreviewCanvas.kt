package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.animation.ActiveDrifterInstance
import com.example.animation.CustomMediaBitmapCache
import com.example.animation.DrifterNativeRenderer
import com.example.animation.DrifterPhysicsEngine
import com.example.model.DrifterCharacter
import com.example.model.MovementPreset
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DeepIndigoSurface
import com.example.ui.theme.ObsidianBg
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Interactive in-app stage preview that simulates a mini phone viewport with the selected
 * DrifterCharacter moving according to the live MovementPreset, speed, size, and opacity.
 */
@Composable
fun LiveDrifterStagePreview(
    character: DrifterCharacter,
    movementPreset: MovementPreset,
    speedMultiplier: Float,
    sizeMultiplier: Float,
    opacity: Float,
    isPaused: Boolean,
    previewBurstTrigger: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    var frameTick by remember { mutableLongStateOf(0L) }
    var stageWidth by remember { mutableFloatStateOf(600f) }
    var stageHeight by remember { mutableFloatStateOf(320f) }

    val customBitmap = remember(character.id, character.customUriString) {
        if (character.isCustom && character.customUriString != null) {
            CustomMediaBitmapCache.loadOrGetBitmap(context, character.customUriString)
        } else {
            null
        }
    }

    val drifterRef = remember(character.id, movementPreset, previewBurstTrigger) {
        mutableStateOf(
            DrifterPhysicsEngine.spawnDrifter(
                character = character,
                movementPreset = movementPreset,
                canvasWidth = stageWidth,
                canvasHeight = stageHeight,
                density = density * 0.78f,
                speedMultiplier = speedMultiplier,
                sizeMultiplier = sizeMultiplier,
                opacity = opacity,
                maxPasses = Int.MAX_VALUE
            ).apply {
                // Start visible inside the preview stage immediately
                x = stageWidth * 0.35f
                y = stageHeight * 0.5f
            }
        )
    }

    // Keep live sliders reflected on the stage instance without resetting its position
    LaunchedEffect(speedMultiplier, sizeMultiplier, opacity) {
        val current = drifterRef.value
        val updated = DrifterPhysicsEngine.spawnDrifter(
            character = character,
            movementPreset = movementPreset,
            canvasWidth = stageWidth,
            canvasHeight = stageHeight,
            density = density * 0.78f,
            speedMultiplier = speedMultiplier,
            sizeMultiplier = sizeMultiplier,
            opacity = opacity,
            maxPasses = Int.MAX_VALUE
        ).apply {
            x = current.x.coerceIn(40f, stageWidth - 40f)
            y = current.y.coerceIn(40f, stageHeight - 40f)
            phase = current.phase
            particles.addAll(current.particles)
        }
        drifterRef.value = updated
    }

    // 30 FPS battery-conscious preview loop
    LaunchedEffect(isPaused, character.id, movementPreset, previewBurstTrigger) {
        if (isPaused) return@LaunchedEffect
        var lastMs = System.currentTimeMillis()
        while (isActive) {
            delay(33L) // ~30 FPS
            val now = System.currentTimeMillis()
            val dt = ((now - lastMs) / 1000f).coerceIn(0.01f, 0.08f)
            lastMs = now

            val d = drifterRef.value
            DrifterPhysicsEngine.updateDrifter(
                drifter = d,
                dtSeconds = dt,
                canvasWidth = stageWidth,
                canvasHeight = stageHeight,
                particleMultiplier = 1.0f
            )
            frameTick = now
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ObsidianBg,
                        DeepIndigoSurface,
                        Color(character.primaryColorHex).copy(alpha = 0.25f)
                    )
                )
            )
            .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read frameTick to trigger redraw
            val tick = frameTick
            if (tick >= 0L) {
                stageWidth = size.width.coerceAtLeast(200f)
                stageHeight = size.height.coerceAtLeast(160f)

                // Draw subtle cyber-grid lines to illustrate screen pass-through
                val gridStep = 36.dp.toPx()
                val gridColor = Color.White.copy(alpha = 0.04f)
                var gx = gridStep
                while (gx < size.width) {
                    drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                    gx += gridStep
                }
                var gy = gridStep
                while (gy < size.height) {
                    drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                    gy += gridStep
                }

                val d = drifterRef.value
                DrifterNativeRenderer.drawDrifter(
                    canvas = drawContext.canvas.nativeCanvas,
                    drifter = d,
                    effectiveOpacity = opacity,
                    customBitmap = customBitmap
                )
            }
        }
    }
}

/**
 * Compact animated thumbnail for Explore cards, Recent chips, and Custom Studio presets.
 */
@Composable
fun DrifterAnimatedThumbnail(
    character: DrifterCharacter,
    modifier: Modifier = Modifier,
    animate: Boolean = true
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var phase by remember { mutableFloatStateOf(0f) }

    val customBitmap = remember(character.id, character.customUriString) {
        if (character.isCustom && character.customUriString != null) {
            CustomMediaBitmapCache.loadOrGetBitmap(context, character.customUriString)
        } else {
            null
        }
    }

    LaunchedEffect(animate, character.id) {
        if (!animate) return@LaunchedEffect
        while (isActive) {
            withFrameNanos { nanos ->
                phase = (nanos / 1_000_000_000f) * 2.2f
            }
            delay(40L) // ~25 FPS for thumbnails to save CPU/battery
        }
    }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(character.primaryColorHex).copy(alpha = 0.38f),
                        CardSurfaceElevated
                    )
                )
            )
    ) {
        val dummyInstance = ActiveDrifterInstance(
            instanceId = 1L,
            character = character,
            movementPreset = character.defaultMovement,
            x = size.width * 0.5f,
            y = size.height * 0.5f,
            vx = 40f,
            vy = 0f,
            baseSizePx = (size.minDimension * 0.72f).coerceAtLeast(32f * density),
            speedMultiplier = 1.0f,
            opacity = 1.0f,
            phase = phase,
            facingRight = true
        )
        DrifterNativeRenderer.drawDrifter(
            canvas = drawContext.canvas.nativeCanvas,
            drifter = dummyInstance,
            effectiveOpacity = 1.0f,
            customBitmap = customBitmap
        )
    }
}
