package com.example.overlay

import android.content.Context
import android.graphics.Canvas
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import com.example.animation.ActiveDrifterInstance
import com.example.animation.CustomMediaBitmapCache
import com.example.animation.DrifterNativeRenderer
import com.example.animation.DrifterPhysicsEngine
import com.example.model.AnimationQuality
import com.example.model.DrifterCharacter
import com.example.model.MovementPreset
import kotlin.math.hypot

/**
 * Lightweight, hardware-accelerated custom View attached to WindowManager.
 * Uses Choreographer frame pacing (20/30/60 FPS) and automatically halts the frame callback
 * whenever there are zero active drifters or when paused/screen-off to preserve battery.
 */
class OverlayDrifterView(
    context: Context,
    private val onActiveCountChanged: (Int) -> Unit
) : View(context), Choreographer.FrameCallback {

    private val activeDrifters = mutableListOf<ActiveDrifterInstance>()
    private var isLoopRunning = false
    private var isPaused = false
    private var lastFrameTimeNanos = 0L

    var targetFps: Int = 30
        set(value) {
            field = value.coerceIn(15, 60)
        }

    var particleMultiplier: Float = 1.0f
    var effectiveOpacity: Float = 0.78f
    var interactiveModeEnabled: Boolean = false

    fun spawnDrifter(
        character: DrifterCharacter,
        movementPreset: MovementPreset,
        speedMultiplier: Float,
        sizeMultiplier: Float,
        opacity: Float,
        repetitionCount: Int,
        maxSimultaneous: Int
    ) {
        if (character.isCustom && character.customUriString != null) {
            CustomMediaBitmapCache.loadOrGetBitmap(context, character.customUriString)
        }

        // Enforce maximum simultaneous character limit for battery optimization
        while (activeDrifters.size >= maxSimultaneous.coerceIn(1, 6)) {
            activeDrifters.removeAt(0)
        }

        val w = width.takeIf { it > 0 }?.toFloat()
            ?: resources.displayMetrics.widthPixels.toFloat()
        val h = height.takeIf { it > 0 }?.toFloat()
            ?: resources.displayMetrics.heightPixels.toFloat()
        val density = resources.displayMetrics.density

        val instance = DrifterPhysicsEngine.spawnDrifter(
            character = character,
            movementPreset = movementPreset,
            canvasWidth = w,
            canvasHeight = h,
            density = density,
            speedMultiplier = speedMultiplier,
            sizeMultiplier = sizeMultiplier,
            opacity = opacity,
            maxPasses = if (repetitionCount <= 0) Int.MAX_VALUE else repetitionCount
        )
        activeDrifters.add(instance)
        onActiveCountChanged(activeDrifters.size)
        ensureLoopRunning()
        invalidate()
    }

    fun updateLiveParameters(
        movementPreset: MovementPreset,
        speedMultiplier: Float,
        opacity: Float,
        quality: AnimationQuality,
        isBatterySaver: Boolean
    ) {
        targetFps = if (isBatterySaver) 20 else quality.targetFps
        particleMultiplier = if (isBatterySaver) 0.4f else quality.particleMultiplier
        effectiveOpacity = opacity
        activeDrifters.forEach { d ->
            val currentSpeed = hypot(d.vx, d.vy).coerceAtLeast(10f)
            val newSpeed = 115f * resources.displayMetrics.density * speedMultiplier
            val ratio = newSpeed / currentSpeed
            d.vx *= ratio
            d.vy *= ratio
        }
        invalidate()
    }

    fun setPausedState(paused: Boolean) {
        isPaused = paused
        if (paused) {
            stopLoop()
        } else if (activeDrifters.isNotEmpty()) {
            ensureLoopRunning()
        }
        invalidate()
    }

    fun clearAllDrifters() {
        activeDrifters.clear()
        onActiveCountChanged(0)
        stopLoop()
        invalidate()
    }

    fun getActiveCount(): Int = activeDrifters.size

    private fun ensureLoopRunning() {
        if (!isLoopRunning && !isPaused && activeDrifters.isNotEmpty() && isAttachedToWindow) {
            isLoopRunning = true
            lastFrameTimeNanos = System.nanoTime()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun stopLoop() {
        if (isLoopRunning) {
            isLoopRunning = false
            Choreographer.getInstance().removeFrameCallback(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (activeDrifters.isNotEmpty() && !isPaused) {
            ensureLoopRunning()
        }
    }

    override fun onDetachedFromWindow() {
        stopLoop()
        super.onDetachedFromWindow()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isLoopRunning || isPaused || activeDrifters.isEmpty()) {
            isLoopRunning = false
            return
        }

        val minFrameIntervalNanos = 1_000_000_000L / targetFps.coerceIn(15, 60)
        val elapsedNanos = frameTimeNanos - lastFrameTimeNanos

        if (elapsedNanos >= minFrameIntervalNanos) {
            val dtSeconds = (elapsedNanos / 1_000_000_000f).coerceIn(0.005f, 0.08f)
            lastFrameTimeNanos = frameTimeNanos

            val w = width.takeIf { it > 0 }?.toFloat()
                ?: resources.displayMetrics.widthPixels.toFloat()
            val h = height.takeIf { it > 0 }?.toFloat()
                ?: resources.displayMetrics.heightPixels.toFloat()

            val iterator = activeDrifters.iterator()
            var removedAny = false
            while (iterator.hasNext()) {
                val drifter = iterator.next()
                val alive = DrifterPhysicsEngine.updateDrifter(
                    drifter = drifter,
                    dtSeconds = dtSeconds,
                    canvasWidth = w,
                    canvasHeight = h,
                    particleMultiplier = particleMultiplier
                )
                if (!alive) {
                    iterator.remove()
                    removedAny = true
                }
            }

            if (removedAny) {
                onActiveCountChanged(activeDrifters.size)
            }

            invalidate()
        }

        if (activeDrifters.isNotEmpty() && !isPaused) {
            Choreographer.getInstance().postFrameCallback(this)
        } else {
            isLoopRunning = false
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (activeDrifters.isEmpty()) return

        for (i in 0 until activeDrifters.size) {
            val drifter = activeDrifters[i]
            val customBmp = if (drifter.character.isCustom) {
                CustomMediaBitmapCache.getCachedBitmap(drifter.character.customUriString)
            } else {
                null
            }
            DrifterNativeRenderer.drawDrifter(
                canvas = canvas,
                drifter = drifter,
                effectiveOpacity = effectiveOpacity,
                customBitmap = customBmp
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!interactiveModeEnabled || event == null) {
            return false
        }
        if (event.action == MotionEvent.ACTION_DOWN) {
            val tx = event.x
            val ty = event.y
            // If user taps near a drifter in Interactive Mode, give it a playful boost & sparkle burst!
            for (drifter in activeDrifters) {
                val dist = hypot(tx - drifter.x, ty - drifter.y)
                if (dist <= drifter.baseSizePx * 0.9f) {
                    drifter.vx = -drifter.vx * 1.15f
                    drifter.vy = -180f * resources.displayMetrics.density
                    drifter.facingRight = drifter.vx >= 0f
                    invalidate()
                    return true
                }
            }
        }
        return false
    }
}
