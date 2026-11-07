package com.example.animation

import com.example.model.DrifterCharacter
import com.example.model.MovementPreset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class ParticlePoint(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var alpha: Float,
    var decayRate: Float,
    var colorHex: Long,
    var rotationDeg: Float = 0f
)

class ActiveDrifterInstance(
    val instanceId: Long,
    val character: DrifterCharacter,
    val movementPreset: MovementPreset,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val baseSizePx: Float,
    val speedMultiplier: Float,
    val opacity: Float,
    var phase: Float = Random.nextFloat() * 100f,
    var ageSeconds: Float = 0f,
    var facingRight: Boolean = true,
    var tiltAngleDeg: Float = 0f,
    var completedPasses: Int = 0,
    val maxPasses: Int = 1,
    val particles: MutableList<ParticlePoint> = mutableListOf()
)

object DrifterPhysicsEngine {

    fun spawnDrifter(
        character: DrifterCharacter,
        movementPreset: MovementPreset,
        canvasWidth: Float,
        canvasHeight: Float,
        density: Float,
        speedMultiplier: Float,
        sizeMultiplier: Float,
        opacity: Float,
        maxPasses: Int = 1
    ): ActiveDrifterInstance {
        val w = canvasWidth.coerceAtLeast(300f)
        val h = canvasHeight.coerceAtLeast(300f)
        val baseSizePx = (68f * density * sizeMultiplier).coerceIn(36f * density, 220f * density)
        val baseSpeedPxPerSec = (115f * density * speedMultiplier).coerceIn(35f * density, 480f * density)

        var startX = w * 0.5f
        var startY = h * 0.5f
        var vx = baseSpeedPxPerSec
        var vy = 0f
        var facingRight = true

        val safeTop = h * 0.12f
        val safeBottom = h * 0.85f
        val randomMidY = Random.nextFloat() * (safeBottom - safeTop) + safeTop
        val randomMidX = Random.nextFloat() * (w * 0.76f) + (w * 0.12f)

        when (movementPreset) {
            MovementPreset.LEFT_TO_RIGHT -> {
                startX = -baseSizePx * 0.8f
                startY = randomMidY
                vx = baseSpeedPxPerSec
                vy = 0f
                facingRight = true
            }
            MovementPreset.RIGHT_TO_LEFT -> {
                startX = w + baseSizePx * 0.8f
                startY = randomMidY
                vx = -baseSpeedPxPerSec
                vy = 0f
                facingRight = false
            }
            MovementPreset.TOP_TO_BOTTOM -> {
                startX = randomMidX
                startY = -baseSizePx * 0.8f
                vx = (Random.nextFloat() - 0.5f) * baseSpeedPxPerSec * 0.35f
                vy = baseSpeedPxPerSec * 0.85f
                facingRight = vx >= 0f
            }
            MovementPreset.BOTTOM_TO_TOP -> {
                startX = randomMidX
                startY = h + baseSizePx * 0.8f
                vx = (Random.nextFloat() - 0.5f) * baseSpeedPxPerSec * 0.35f
                vy = -baseSpeedPxPerSec * 0.85f
                facingRight = vx >= 0f
            }
            MovementPreset.DIAGONAL -> {
                val fromLeft = Random.nextBoolean()
                startX = if (fromLeft) -baseSizePx * 0.6f else w + baseSizePx * 0.6f
                startY = h * (0.15f + Random.nextFloat() * 0.35f)
                vx = if (fromLeft) baseSpeedPxPerSec * 0.85f else -baseSpeedPxPerSec * 0.85f
                vy = baseSpeedPxPerSec * 0.48f
                facingRight = fromLeft
            }
            MovementPreset.ZIGZAG -> {
                startX = -baseSizePx * 0.6f
                startY = randomMidY
                vx = baseSpeedPxPerSec * 0.85f
                vy = baseSpeedPxPerSec * 0.6f
                facingRight = true
            }
            MovementPreset.RANDOM_FLOATING -> {
                startX = randomMidX
                startY = randomMidY
                val angle = Random.nextFloat() * 2f * PI.toFloat()
                vx = cos(angle) * baseSpeedPxPerSec * 0.7f
                vy = sin(angle) * baseSpeedPxPerSec * 0.7f
                facingRight = vx >= 0f
            }
            MovementPreset.BOTTOM_EDGE_RUNNING -> {
                val fromLeft = Random.nextBoolean()
                startX = if (fromLeft) -baseSizePx * 0.5f else w + baseSizePx * 0.5f
                startY = h - baseSizePx * 0.95f
                vx = if (fromLeft) baseSpeedPxPerSec * 1.05f else -baseSpeedPxPerSec * 1.05f
                vy = 0f
                facingRight = fromLeft
            }
            MovementPreset.GENTLE_AMBIENT_FLOATING -> {
                startX = randomMidX
                startY = randomMidY
                vx = (if (Random.nextBoolean()) 1f else -1f) * baseSpeedPxPerSec * 0.32f
                vy = (Random.nextFloat() - 0.5f) * baseSpeedPxPerSec * 0.22f
                facingRight = vx >= 0f
            }
        }

        return ActiveDrifterInstance(
            instanceId = System.nanoTime() + Random.nextLong(10000),
            character = character,
            movementPreset = movementPreset,
            x = startX,
            y = startY,
            vx = vx,
            vy = vy,
            baseSizePx = baseSizePx,
            speedMultiplier = speedMultiplier,
            opacity = opacity,
            facingRight = facingRight,
            maxPasses = if (maxPasses <= 0) Int.MAX_VALUE else maxPasses
        )
    }

    /**
     * Advances a drifter's position and particle system by [dtSeconds].
     * Returns `true` if the drifter is still alive on screen, or `false` if it has completed all passes.
     */
    fun updateDrifter(
        drifter: ActiveDrifterInstance,
        dtSeconds: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        particleMultiplier: Float
    ): Boolean {
        val dt = dtSeconds.coerceIn(0.001f, 0.1f)
        val w = canvasWidth.coerceAtLeast(200f)
        val h = canvasHeight.coerceAtLeast(200f)
        val margin = drifter.baseSizePx * 1.1f

        drifter.ageSeconds += dt
        drifter.phase += dt * (3.5f + drifter.speedMultiplier * 2.2f)

        when (drifter.movementPreset) {
            MovementPreset.LEFT_TO_RIGHT -> {
                drifter.x += drifter.vx * dt
                drifter.y += sin(drifter.phase * 1.3f) * (drifter.baseSizePx * 0.35f) * dt
                drifter.facingRight = true
                drifter.tiltAngleDeg = sin(drifter.phase) * 6f
                if (drifter.x > w + margin) {
                    drifter.completedPasses++
                    if (drifter.completedPasses >= drifter.maxPasses) return false
                    drifter.x = -margin * 0.8f
                    drifter.y = h * (0.15f + Random.nextFloat() * 0.68f)
                }
            }
            MovementPreset.RIGHT_TO_LEFT -> {
                drifter.x += drifter.vx * dt
                drifter.y += sin(drifter.phase * 1.3f) * (drifter.baseSizePx * 0.35f) * dt
                drifter.facingRight = false
                drifter.tiltAngleDeg = -sin(drifter.phase) * 6f
                if (drifter.x < -margin) {
                    drifter.completedPasses++
                    if (drifter.completedPasses >= drifter.maxPasses) return false
                    drifter.x = w + margin * 0.8f
                    drifter.y = h * (0.15f + Random.nextFloat() * 0.68f)
                }
            }
            MovementPreset.TOP_TO_BOTTOM -> {
                drifter.x += sin(drifter.phase * 1.1f) * (drifter.baseSizePx * 0.85f) * dt
                drifter.y += drifter.vy * dt
                drifter.facingRight = cos(drifter.phase * 1.1f) >= 0f
                drifter.tiltAngleDeg = sin(drifter.phase * 1.1f) * 14f
                if (drifter.y > h + margin) {
                    drifter.completedPasses++
                    if (drifter.completedPasses >= drifter.maxPasses) return false
                    drifter.y = -margin * 0.8f
                    drifter.x = w * (0.15f + Random.nextFloat() * 0.7f)
                }
            }
            MovementPreset.BOTTOM_TO_TOP -> {
                drifter.x += sin(drifter.phase * 1.0f) * (drifter.baseSizePx * 0.65f) * dt
                drifter.y += drifter.vy * dt
                drifter.facingRight = cos(drifter.phase) >= 0f
                drifter.tiltAngleDeg = sin(drifter.phase) * 8f
                if (drifter.y < -margin) {
                    drifter.completedPasses++
                    if (drifter.completedPasses >= drifter.maxPasses) return false
                    drifter.y = h + margin * 0.8f
                    drifter.x = w * (0.15f + Random.nextFloat() * 0.7f)
                }
            }
            MovementPreset.DIAGONAL -> {
                drifter.x += drifter.vx * dt
                drifter.y += drifter.vy * dt
                drifter.facingRight = drifter.vx >= 0f
                drifter.tiltAngleDeg = if (drifter.facingRight) 15f else -15f
                if (drifter.x > w + margin || drifter.x < -margin || drifter.y > h + margin) {
                    drifter.completedPasses++
                    if (drifter.completedPasses >= drifter.maxPasses) return false
                    val fromLeft = Random.nextBoolean()
                    drifter.x = if (fromLeft) -margin * 0.7f else w + margin * 0.7f
                    drifter.y = h * (0.1f + Random.nextFloat() * 0.35f)
                    val speed = kotlin.math.hypot(drifter.vx, drifter.vy)
                    drifter.vx = if (fromLeft) speed * 0.84f else -speed * 0.84f
                    drifter.vy = speed * 0.52f
                }
            }
            MovementPreset.ZIGZAG -> {
                drifter.x += drifter.vx * dt
                val waveVy = cos(drifter.phase * 1.6f) * kotlin.math.abs(drifter.vx) * 0.95f
                drifter.y = (drifter.y + waveVy * dt).coerceIn(drifter.baseSizePx * 0.6f, h - drifter.baseSizePx * 0.6f)
                drifter.facingRight = drifter.vx >= 0f
                drifter.tiltAngleDeg = (atan2(waveVy, kotlin.math.abs(drifter.vx)) * (180f / PI.toFloat())).coerceIn(-28f, 28f)
                if (drifter.x > w + margin) {
                    drifter.completedPasses++
                    if (drifter.completedPasses >= drifter.maxPasses) return false
                    drifter.x = -margin * 0.7f
                }
            }
            MovementPreset.RANDOM_FLOATING -> {
                drifter.vx += sin(drifter.phase * 0.7f) * 18f * dt
                drifter.vy += cos(drifter.phase * 0.9f) * 18f * dt
                drifter.x += drifter.vx * dt
                drifter.y += drifter.vy * dt
                val pad = drifter.baseSizePx * 0.65f
                if (drifter.x < pad) {
                    drifter.x = pad
                    drifter.vx = kotlin.math.abs(drifter.vx)
                } else if (drifter.x > w - pad) {
                    drifter.x = w - pad
                    drifter.vx = -kotlin.math.abs(drifter.vx)
                }
                if (drifter.y < pad) {
                    drifter.y = pad
                    drifter.vy = kotlin.math.abs(drifter.vy)
                } else if (drifter.y > h - pad) {
                    drifter.y = h - pad
                    drifter.vy = -kotlin.math.abs(drifter.vy)
                }
                drifter.facingRight = drifter.vx >= 0f
                drifter.tiltAngleDeg = sin(drifter.phase) * 9f
                if (drifter.ageSeconds > 16f * drifter.maxPasses.coerceAtMost(10)) {
                    return false
                }
            }
            MovementPreset.BOTTOM_EDGE_RUNNING -> {
                drifter.x += drifter.vx * dt
                val bounce = kotlin.math.abs(sin(drifter.phase * 2.6f)) * (drifter.baseSizePx * 0.16f)
                drifter.y = (h - drifter.baseSizePx * 0.85f) - bounce
                drifter.facingRight = drifter.vx >= 0f
                drifter.tiltAngleDeg = sin(drifter.phase * 2.6f) * 5f
                if (drifter.x > w + margin || drifter.x < -margin) {
                    drifter.completedPasses++
                    if (drifter.completedPasses >= drifter.maxPasses) return false
                    drifter.vx = -drifter.vx
                    drifter.x = drifter.x.coerceIn(-margin * 0.5f, w + margin * 0.5f)
                }
            }
            MovementPreset.GENTLE_AMBIENT_FLOATING -> {
                drifter.x += drifter.vx * dt + sin(drifter.phase * 0.6f) * 12f * dt
                drifter.y += drifter.vy * dt + cos(drifter.phase * 0.8f) * 10f * dt
                val pad = drifter.baseSizePx * 0.6f
                if (drifter.x < pad || drifter.x > w - pad) drifter.vx = -drifter.vx
                if (drifter.y < pad || drifter.y > h - pad) drifter.vy = -drifter.vy
                drifter.x = drifter.x.coerceIn(pad, w - pad)
                drifter.y = drifter.y.coerceIn(pad, h - pad)
                drifter.facingRight = drifter.vx >= 0f
                drifter.tiltAngleDeg = sin(drifter.phase * 0.5f) * 5f
                if (drifter.ageSeconds > 20f * drifter.maxPasses.coerceAtMost(10)) {
                    return false
                }
            }
        }

        // Update trailing particles
        val iterator = drifter.particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.alpha -= p.decayRate * dt
            p.rotationDeg += 90f * dt
            if (p.alpha <= 0.02f) {
                iterator.remove()
            }
        }

        // Spawn new trailing particle based on quality multiplier
        val maxParticles = (16 * particleMultiplier).toInt().coerceAtLeast(4)
        if (drifter.particles.size < maxParticles && Random.nextFloat() < 0.45f * particleMultiplier) {
            val dirSign = if (drifter.facingRight) -1f else 1f
            val colors = longArrayOf(
                drifter.character.primaryColorHex,
                drifter.character.secondaryColorHex,
                drifter.character.accentColorHex
            )
            drifter.particles.add(
                ParticlePoint(
                    x = drifter.x + dirSign * drifter.baseSizePx * 0.25f + (Random.nextFloat() - 0.5f) * drifter.baseSizePx * 0.3f,
                    y = drifter.y + (Random.nextFloat() - 0.5f) * drifter.baseSizePx * 0.35f,
                    vx = dirSign * (15f + Random.nextFloat() * 35f),
                    vy = (Random.nextFloat() - 0.5f) * 30f,
                    size = (drifter.baseSizePx * (0.06f + Random.nextFloat() * 0.08f)).coerceAtLeast(3f),
                    alpha = 0.85f,
                    decayRate = 0.8f + Random.nextFloat() * 0.7f,
                    colorHex = colors[Random.nextInt(colors.size)],
                    rotationDeg = Random.nextFloat() * 360f
                )
            )
        }

        return true
    }
}
