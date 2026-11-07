package com.example.animation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.model.VisualEffectType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure Android Canvas renderer shared between the real WindowManager overlay view
 * and in-app Jetpack Compose previews. Draws all 23 original characters + custom bitmaps
 * and their visual particle effects without allocations in the draw loop.
 */
object DrifterNativeRenderer {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val rectF = RectF()

    private val rainbowColors = intArrayOf(
        0xFFFF2A85.toInt(),
        0xFFFF7B3A.toInt(),
        0xFFFFBE0B.toInt(),
        0xFF00F5D4.toInt(),
        0xFF00F0FF.toInt(),
        0xFF9D4EDD.toInt()
    )

    fun drawDrifter(
        canvas: Canvas,
        drifter: ActiveDrifterInstance,
        effectiveOpacity: Float,
        customBitmap: Bitmap? = null
    ) {
        val alphaMultiplier = (effectiveOpacity * drifter.opacity).coerceIn(0.08f, 1.0f)
        val masterAlpha = (alphaMultiplier * 255).toInt().coerceIn(15, 255)

        // 1. Draw trailing visual effects first
        drawTrailingEffects(canvas, drifter, alphaMultiplier)

        // 2. Transform canvas to character local coordinates
        canvas.save()
        canvas.translate(drifter.x, drifter.y)
        val scaleX = if (drifter.facingRight) 1f else -1f
        canvas.scale(scaleX, 1f)
        canvas.rotate(drifter.tiltAngleDeg)

        val r = drifter.baseSizePx * 0.45f
        val phase = drifter.phase

        if (drifter.character.isCustom && customBitmap != null && !customBitmap.isRecycled) {
            paint.reset()
            paint.isAntiAlias = true
            paint.isFilterBitmap = true
            paint.alpha = masterAlpha
            val aspect = customBitmap.width.toFloat().coerceAtLeast(1f) / customBitmap.height.toFloat().coerceAtLeast(1f)
            val halfW = if (aspect >= 1f) r else r * aspect
            val halfH = if (aspect >= 1f) r / aspect else r
            val bob = sin(phase * 1.5f) * (r * 0.08f)
            rectF.set(-halfW, -halfH + bob, halfW, halfH + bob)
            canvas.drawBitmap(customBitmap, null, rectF, paint)
            canvas.restore()
            return
        }

        when (drifter.character.id) {
            "chibi_crow" -> drawChibiCrow(canvas, r, phase, masterAlpha)
            "shadow_bird" -> drawShadowBird(canvas, r, phase, masterAlpha)
            "cute_ninja" -> drawCuteNinja(canvas, r, phase, masterAlpha)
            "mini_flying_creature" -> drawKitsuneWisp(canvas, r, phase, masterAlpha)
            "fantasy_energy_creature" -> drawAstraDragonet(canvas, r, phase, masterAlpha)
            "spinning_pixel_cat" -> drawSpinningPixelCat(canvas, r, phase, masterAlpha)
            "rainbow_pixel_cat" -> drawRainbowPixelCat(canvas, r, phase, masterAlpha)
            "mini_pixel_ufo" -> drawMiniPixelUfo(canvas, r, phase, masterAlpha)
            "retro_pixel_heli" -> drawRetroPixelHeli(canvas, r, phase, masterAlpha)
            "funny_flying_toast" -> drawWingedToast(canvas, r, phase, masterAlpha)
            "pixel_birds" -> drawPixelBirds(canvas, r, phase, masterAlpha)
            "glowing_fireflies" -> drawGlowingFireflies(canvas, r, phase, masterAlpha)
            "neon_butterflies" -> drawNeonButterfly(canvas, r, phase, masterAlpha)
            "autumn_leaves" -> drawAutumnLeaves(canvas, r, phase, masterAlpha)
            "paper_planes" -> drawPaperPlane(canvas, r, phase, masterAlpha)
            "tiny_stars" -> drawTinyStars(canvas, r, phase, masterAlpha)
            "glowing_particles" -> drawLofiOrbs(canvas, r, phase, masterAlpha)
            "dreamy_clouds" -> drawDreamyCloud(canvas, r, phase, masterAlpha)
            "mini_helicopter" -> drawMiniHelicopter(canvas, r, phase, masterAlpha)
            "tiny_airplane" -> drawSunsetBiplane(canvas, r, phase, masterAlpha)
            "retro_flying_machine" -> drawDaVinciOrnopter(canvas, r, phase, masterAlpha)
            "animated_ufo" -> drawRoswellCruiser(canvas, r, phase, masterAlpha)
            "small_spaceship" -> drawStarhopperSpaceship(canvas, r, phase, masterAlpha)
            else -> drawChibiCrow(canvas, r, phase, masterAlpha)
        }

        canvas.restore()
    }

    private fun drawTrailingEffects(
        canvas: Canvas,
        drifter: ActiveDrifterInstance,
        alphaMultiplier: Float
    ) {
        val effect = drifter.character.effectType
        val r = drifter.baseSizePx * 0.45f

        if (effect == VisualEffectType.RAINBOW_RIBBON) {
            val dir = if (drifter.facingRight) -1f else 1f
            val bandHeight = (r * 0.9f) / rainbowColors.size
            val segments = 6
            val segWidth = r * 0.28f
            for (seg in 0 until segments) {
                val waveY = sin(drifter.phase * 2f - seg * 0.7f) * (r * 0.14f)
                val segAlpha = ((1f - seg / segments.toFloat()) * alphaMultiplier * 210).toInt().coerceIn(0, 255)
                val left = drifter.x + dir * (r * 0.4f + (seg + 1) * segWidth)
                val right = drifter.x + dir * (r * 0.4f + seg * segWidth)
                val minX = minOf(left, right)
                val maxX = maxOf(left, right)
                for (cIdx in rainbowColors.indices) {
                    paint.style = Paint.Style.FILL
                    paint.color = rainbowColors[cIdx]
                    paint.alpha = segAlpha
                    val top = drifter.y - r * 0.45f + cIdx * bandHeight + waveY
                    canvas.drawRect(minX, top, maxX, top + bandHeight + 1f, paint)
                }
            }
        }

        drifter.particles.forEach { p ->
            val pAlpha = (p.alpha * alphaMultiplier * 255).toInt().coerceIn(0, 255)
            if (pAlpha <= 4) return@forEach
            paint.style = Paint.Style.FILL
            paint.color = p.colorHex.toInt()
            paint.alpha = pAlpha

            when (effect) {
                VisualEffectType.FEATHER_DISPERSAL -> {
                    canvas.save()
                    canvas.translate(p.x, p.y)
                    canvas.rotate(p.rotationDeg)
                    rectF.set(-p.size * 1.3f, -p.size * 0.45f, p.size * 1.3f, p.size * 0.45f)
                    canvas.drawOval(rectF, paint)
                    canvas.restore()
                }
                VisualEffectType.SPEED_LINES -> {
                    strokePaint.color = p.colorHex.toInt()
                    strokePaint.alpha = pAlpha
                    strokePaint.strokeWidth = (p.size * 0.45f).coerceAtLeast(2f)
                    canvas.drawLine(p.x - p.size * 2.2f, p.y, p.x + p.size * 2.2f, p.y, strokePaint)
                }
                VisualEffectType.SMOKE_TRAIL, VisualEffectType.PROPULSION_PUFFS -> {
                    canvas.drawCircle(p.x, p.y, p.size * 1.25f, paint)
                }
                else -> {
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                }
            }
        }
    }

    // 1. Chibi Crow
    private fun drawChibiCrow(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        val wingFlap = sin(phase * 3.2f) * r * 0.55f
        // Aura glow
        paint.style = Paint.Style.FILL
        paint.color = 0xFF9D4EDD.toInt()
        paint.alpha = (alpha * 0.25f).toInt()
        canvas.drawCircle(0f, 0f, r * 1.15f, paint)

        // Back wing
        paint.color = 0xFF1A1433.toInt()
        paint.alpha = alpha
        path.reset()
        path.moveTo(-r * 0.1f, 0f)
        path.quadTo(-r * 0.6f, -r * 0.8f - wingFlap, -r * 0.9f, -wingFlap * 0.7f)
        path.quadTo(-r * 0.4f, r * 0.2f, -r * 0.1f, 0f)
        canvas.drawPath(path, paint)

        // Plump Chibi Body
        paint.color = 0xFF282048.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.65f, -r * 0.55f, r * 0.65f, r * 0.55f)
        canvas.drawOval(rectF, paint)

        // Tail feathers
        path.reset()
        path.moveTo(-r * 0.55f, r * 0.05f)
        path.lineTo(-r * 1.05f, -r * 0.2f)
        path.lineTo(-r * 0.95f, r * 0.1f)
        path.lineTo(-r * 1.08f, r * 0.35f)
        path.close()
        canvas.drawPath(path, paint)

        // Front Wing
        paint.color = 0xFF392E66.toInt()
        paint.alpha = alpha
        path.reset()
        path.moveTo(0f, r * 0.05f)
        path.quadTo(-r * 0.45f, -r * 0.75f + wingFlap, -r * 0.85f, wingFlap)
        path.quadTo(-r * 0.35f, r * 0.35f, 0f, r * 0.05f)
        canvas.drawPath(path, paint)

        // Golden Beak
        paint.color = 0xFFFFBE0B.toInt()
        paint.alpha = alpha
        path.reset()
        path.moveTo(r * 0.55f, -r * 0.12f)
        path.lineTo(r * 0.95f, 0f)
        path.lineTo(r * 0.55f, r * 0.15f)
        path.close()
        canvas.drawPath(path, paint)

        // Glowing Crimson Eye
        paint.color = 0xFFFF2A85.toInt()
        paint.alpha = alpha
        canvas.drawCircle(r * 0.32f, -r * 0.12f, r * 0.16f, paint)
        paint.color = Color.WHITE
        paint.alpha = alpha
        canvas.drawCircle(r * 0.36f, -r * 0.16f, r * 0.06f, paint)
    }

    // 2. Shadow Spirit Hawk
    private fun drawShadowBird(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        val flap = sin(phase * 2.8f) * r * 0.65f
        paint.style = Paint.Style.FILL
        paint.color = 0xFF00F0FF.toInt()
        paint.alpha = (alpha * 0.28f).toInt()
        canvas.drawCircle(0f, 0f, r * 1.1f, paint)

        // Swept Spirit Wings
        paint.color = 0xFF4A3080.toInt()
        paint.alpha = alpha
        path.reset()
        path.moveTo(r * 0.7f, 0f)
        path.lineTo(-r * 0.2f, -r * 0.85f - flap * 0.6f)
        path.lineTo(-r * 0.95f, -r * 0.2f - flap * 0.3f)
        path.lineTo(-r * 0.5f, 0f)
        path.lineTo(-r * 0.95f, r * 0.2f + flap * 0.3f)
        path.lineTo(-r * 0.2f, r * 0.85f + flap * 0.6f)
        path.close()
        canvas.drawPath(path, paint)

        // Neon Cyan Crest & Eye
        paint.color = 0xFF00F0FF.toInt()
        paint.alpha = alpha
        canvas.drawCircle(r * 0.35f, -r * 0.06f, r * 0.12f, paint)
    }

    // 3. Cute Chibi Ninja
    private fun drawCuteNinja(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        val runBob = abs(sin(phase * 3.5f)) * r * 0.15f
        // Fluttering Scarf
        strokePaint.color = 0xFFFF2A85.toInt()
        strokePaint.alpha = alpha
        strokePaint.strokeWidth = r * 0.22f
        path.reset()
        path.moveTo(-r * 0.2f, -r * 0.15f - runBob)
        path.quadTo(-r * 0.7f, -r * 0.45f + sin(phase * 4f) * r * 0.25f, -r * 1.15f, -r * 0.1f + cos(phase * 4f) * r * 0.25f)
        canvas.drawPath(path, strokePaint)

        // Ninja Body & Head
        paint.style = Paint.Style.FILL
        paint.color = 0xFF1D2440.toInt()
        paint.alpha = alpha
        canvas.drawCircle(0f, -r * 0.2f - runBob, r * 0.52f, paint)
        rectF.set(-r * 0.38f, r * 0.15f - runBob, r * 0.38f, r * 0.78f - runBob)
        canvas.drawRoundRect(rectF, r * 0.2f, r * 0.2f, paint)

        // Eye slit visor
        paint.color = 0xFFFFE5D9.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.15f, -r * 0.32f - runBob, r * 0.45f, -r * 0.05f - runBob)
        canvas.drawRoundRect(rectF, r * 0.12f, r * 0.12f, paint)

        // Determined Eyes & Cyan Headband
        paint.color = 0xFF00F0FF.toInt()
        paint.alpha = alpha
        canvas.drawCircle(r * 0.22f, -r * 0.18f - runBob, r * 0.09f, paint)
    }

    // 4. Kitsune Sky Wisp
    private fun drawKitsuneWisp(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        val sway = sin(phase * 2.2f) * r * 0.2f
        // Twin spirit orb tails
        paint.style = Paint.Style.FILL
        paint.color = 0xFF00F5D4.toInt()
        paint.alpha = (alpha * 0.75f).toInt()
        canvas.drawCircle(-r * 0.85f, -r * 0.25f + sway, r * 0.28f, paint)
        canvas.drawCircle(-r * 0.75f, r * 0.32f - sway, r * 0.24f, paint)

        // White Fox Head/Body
        paint.color = 0xFFFFF5FA.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.55f, -r * 0.45f, r * 0.55f, r * 0.45f)
        canvas.drawOval(rectF, paint)

        // Fox Ears with Crimson Tips
        path.reset()
        path.moveTo(-r * 0.25f, -r * 0.35f)
        path.lineTo(-r * 0.45f, -r * 0.95f)
        path.lineTo(r * 0.05f, -r * 0.42f)
        path.close()
        canvas.drawPath(path, paint)

        path.reset()
        path.moveTo(r * 0.15f, -r * 0.35f)
        path.lineTo(r * 0.38f, -r * 0.95f)
        path.lineTo(r * 0.48f, -r * 0.32f)
        path.close()
        canvas.drawPath(path, paint)

        // Crimson markings & eye
        paint.color = 0xFFFF2A85.toInt()
        paint.alpha = alpha
        canvas.drawCircle(r * 0.28f, -r * 0.08f, r * 0.1f, paint)
    }

    // 5. Astra Dragonet
    private fun drawAstraDragonet(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        // Serpentine wave segments
        for (i in 5 downTo 0) {
            val offset = -i * r * 0.26f
            val waveY = sin(phase * 2.6f - i * 0.6f) * r * 0.28f
            val segRadius = r * (0.45f - i * 0.05f).coerceAtLeast(0.14f)
            paint.style = Paint.Style.FILL
            paint.color = if (i % 2 == 0) 0xFF00F0FF.toInt() else 0xFF9D4EDD.toInt()
            paint.alpha = (alpha * (1f - i * 0.1f)).toInt().coerceIn(20, 255)
            canvas.drawCircle(offset, waveY, segRadius, paint)
        }
        // Dragonet Horns & Eye
        paint.color = 0xFFFFBE0B.toInt()
        paint.alpha = alpha
        canvas.drawCircle(r * 0.18f, sin(phase * 2.6f) * r * 0.28f - r * 0.08f, r * 0.11f, paint)
    }

    // 6. Spinning Pixel Cat
    private fun drawSpinningPixelCat(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        canvas.save()
        canvas.rotate((phase * 95f) % 360f)
        drawPixelCatHead(canvas, r, alpha, 0xFF28263E.toInt(), 0xFFFFBE0B.toInt())
        canvas.restore()
    }

    // 7. Prism Dash Cat (Original Pixel Rainbow Cat)
    private fun drawRainbowPixelCat(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        val bob = sin(phase * 3f) * r * 0.1f
        // Frosted Berry Tart Body
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFFB5D8.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.7f, -r * 0.5f + bob, r * 0.35f, r * 0.5f + bob)
        canvas.drawRoundRect(rectF, r * 0.15f, r * 0.15f, paint)

        paint.color = 0xFFFF2A85.toInt()
        paint.alpha = alpha
        canvas.drawCircle(-r * 0.4f, -r * 0.2f + bob, r * 0.09f, paint)
        canvas.drawCircle(-r * 0.1f, r * 0.18f + bob, r * 0.09f, paint)
        canvas.drawCircle(r * 0.1f, -r * 0.15f + bob, r * 0.09f, paint)

        // Cat Head at front
        canvas.save()
        canvas.translate(r * 0.42f, bob * 0.6f)
        drawPixelCatHead(canvas, r * 0.68f, alpha, 0xFF3D375E.toInt(), 0xFF00F0FF.toInt())
        canvas.restore()
    }

    private fun drawPixelCatHead(canvas: Canvas, r: Float, alpha: Int, furColor: Int, eyeColor: Int) {
        paint.style = Paint.Style.FILL
        paint.color = furColor
        paint.alpha = alpha
        // Blocky pixel head
        canvas.drawRect(-r * 0.6f, -r * 0.45f, r * 0.6f, r * 0.5f, paint)
        // Left & Right Pixel Ears
        canvas.drawRect(-r * 0.6f, -r * 0.78f, -r * 0.22f, -r * 0.45f, paint)
        canvas.drawRect(r * 0.22f, -r * 0.78f, r * 0.6f, -r * 0.45f, paint)
        // Pixel Eyes
        paint.color = eyeColor
        paint.alpha = alpha
        canvas.drawRect(-r * 0.38f, -r * 0.15f, -r * 0.14f, r * 0.1f, paint)
        canvas.drawRect(r * 0.14f, -r * 0.15f, r * 0.38f, r * 0.1f, paint)
        // Pink pixel nose
        paint.color = 0xFFFF2A85.toInt()
        paint.alpha = alpha
        canvas.drawRect(-r * 0.1f, r * 0.12f, r * 0.1f, r * 0.26f, paint)
    }

    // 8. 8-Bit Abductor UFO
    private fun drawMiniPixelUfo(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        // Tractor Beam Cone
        paint.style = Paint.Style.FILL
        paint.color = 0xFF00F5D4.toInt()
        paint.alpha = ((0.25f + 0.15f * sin(phase * 3f)) * alpha).toInt()
        path.reset()
        path.moveTo(-r * 0.3f, r * 0.15f)
        path.lineTo(r * 0.3f, r * 0.15f)
        path.lineTo(r * 0.75f, r * 1.05f)
        path.lineTo(-r * 0.75f, r * 1.05f)
        path.close()
        canvas.drawPath(path, paint)

        // Glass Dome
        paint.color = 0xFF00F0FF.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.5f, -r * 0.55f, r * 0.5f, r * 0.1f)
        canvas.drawArc(rectF, 180f, 180f, true, paint)

        // Saucer Body
        paint.color = 0xFF9D4EDD.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.95f, -r * 0.18f, r * 0.95f, r * 0.28f)
        canvas.drawOval(rectF, paint)

        // Blinking Lights
        paint.color = 0xFFFFBE0B.toInt()
        paint.alpha = alpha
        for (i in -1..1) {
            canvas.drawCircle(i * r * 0.45f, r * 0.05f, r * 0.11f, paint)
        }
    }

    // 9. Bit-Chopper 88
    private fun drawRetroPixelHeli(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        drawMiniHelicopter(canvas, r, phase, alpha)
    }

    // 10. Winged Cyber Toast
    private fun drawWingedToast(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        val flap = sin(phase * 3.2f) * r * 0.45f
        // White Angel Wings
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.alpha = alpha
        rectF.set(-r * 0.95f, -r * 0.55f - flap, -r * 0.2f, r * 0.1f)
        canvas.drawOval(rectF, paint)
        rectF.set(r * 0.2f, -r * 0.55f - flap, r * 0.95f, r * 0.1f)
        canvas.drawOval(rectF, paint)

        // Toast Crust & Crumb
        paint.color = 0xFFD9822B.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.52f, -r * 0.52f, r * 0.52f, r * 0.58f)
        canvas.drawRoundRect(rectF, r * 0.18f, r * 0.18f, paint)

        paint.color = 0xFFF5CBA7.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.4f, -r * 0.4f, r * 0.4f, r * 0.46f)
        canvas.drawRoundRect(rectF, r * 0.12f, r * 0.12f, paint)

        // Melting Golden Butter Pat
        paint.color = 0xFFFFBE0B.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.18f, -r * 0.18f, r * 0.18f, r * 0.18f)
        canvas.drawRoundRect(rectF, r * 0.05f, r * 0.05f, paint)
    }

    // 11. Flappy Arcade Flock
    private fun drawPixelBirds(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        val bob1 = sin(phase * 3.4f) * r * 0.2f
        val bob2 = cos(phase * 3.4f) * r * 0.2f
        drawSinglePixelBird(canvas, r * 0.25f, -r * 0.15f + bob1, r * 0.55f, alpha, 0xFF00F0FF.toInt())
        drawSinglePixelBird(canvas, -r * 0.45f, r * 0.25f + bob2, r * 0.42f, alpha, 0xFFFFBE0B.toInt())
    }

    private fun drawSinglePixelBird(canvas: Canvas, cx: Float, cy: Float, size: Float, alpha: Int, color: Int) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = alpha
        canvas.drawCircle(cx, cy, size, paint)
        paint.color = Color.WHITE
        paint.alpha = alpha
        canvas.drawCircle(cx + size * 0.35f, cy - size * 0.2f, size * 0.32f, paint)
        paint.color = 0xFFFF2A85.toInt()
        paint.alpha = alpha
        canvas.drawRect(cx + size * 0.6f, cy - size * 0.05f, cx + size * 1.15f, cy + size * 0.35f, paint)
    }

    // 12. Lantern Fireflies
    private fun drawGlowingFireflies(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        for (i in 0..2) {
            val angle = phase * 1.4f + i * 2.1f
            val fx = cos(angle) * r * 0.48f
            val fy = sin(angle * 1.3f) * r * 0.42f
            val pulse = 0.65f + 0.35f * sin(phase * 3f + i)
            paint.style = Paint.Style.FILL
            paint.color = if (i % 2 == 0) 0xFF00F5D4.toInt() else 0xFFFFBE0B.toInt()
            paint.alpha = (alpha * 0.32f * pulse).toInt()
            canvas.drawCircle(fx, fy, r * 0.48f * pulse, paint)
            paint.alpha = alpha
            canvas.drawCircle(fx, fy, r * 0.16f, paint)
        }
    }

    // 13. Morpho Lume Butterfly
    private fun drawNeonButterfly(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        val wingScale = (0.35f + 0.65f * abs(sin(phase * 2.8f)))
        canvas.save()
        canvas.scale(1f, wingScale)

        paint.style = Paint.Style.FILL
        paint.color = 0xFF9D4EDD.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.85f, -r * 0.85f, r * 0.15f, 0f)
        canvas.drawOval(rectF, paint)
        rectF.set(-r * 0.85f, 0f, r * 0.15f, r * 0.85f)
        canvas.drawOval(rectF, paint)

        paint.color = 0xFF00F0FF.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.15f, -r * 0.95f, r * 0.85f, 0f)
        canvas.drawOval(rectF, paint)
        rectF.set(-r * 0.15f, 0f, r * 0.85f, r * 0.95f)
        canvas.drawOval(rectF, paint)

        canvas.restore()
        // Center body
        paint.color = Color.WHITE
        paint.alpha = alpha
        rectF.set(-r * 0.55f, -r * 0.08f, r * 0.55f, r * 0.08f)
        canvas.drawRoundRect(rectF, r * 0.08f, r * 0.08f, paint)
    }

    // 14. Maple Drift Autumn Leaves
    private fun drawAutumnLeaves(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        canvas.save()
        canvas.rotate(sin(phase * 1.5f) * 35f)
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFF7B3A.toInt()
        paint.alpha = alpha
        path.reset()
        path.moveTo(r * 0.85f, 0f)
        path.lineTo(r * 0.2f, -r * 0.35f)
        path.lineTo(r * 0.35f, -r * 0.8f)
        path.lineTo(-r * 0.15f, -r * 0.35f)
        path.lineTo(-r * 0.7f, -r * 0.5f)
        path.lineTo(-r * 0.45f, 0f)
        path.lineTo(-r * 0.7f, r * 0.5f)
        path.lineTo(-r * 0.15f, r * 0.35f)
        path.lineTo(r * 0.35f, r * 0.8f)
        path.lineTo(r * 0.2f, r * 0.35f)
        path.close()
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    // 15. Origami Paper Plane
    private fun drawPaperPlane(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        val hover = sin(phase * 2f) * r * 0.08f
        paint.style = Paint.Style.FILL
        paint.color = 0xFFE6F9FF.toInt()
        paint.alpha = alpha
        path.reset()
        path.moveTo(r * 0.95f, hover)
        path.lineTo(-r * 0.85f, -r * 0.55f + hover)
        path.lineTo(-r * 0.45f, hover)
        path.lineTo(-r * 0.85f, r * 0.55f + hover)
        path.close()
        canvas.drawPath(path, paint)

        // Center Fold Shadow
        paint.color = 0xFF00F0FF.toInt()
        paint.alpha = (alpha * 0.85f).toInt()
        path.reset()
        path.moveTo(r * 0.95f, hover)
        path.lineTo(-r * 0.45f, hover)
        path.lineTo(-r * 0.65f, r * 0.28f + hover)
        path.close()
        canvas.drawPath(path, paint)
    }

    // 16. Wishlist Stars
    private fun drawTinyStars(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        drawFourPointStar(canvas, 0f, 0f, r * 0.85f, alpha, 0xFFFFBE0B.toInt())
        val pulse = 0.6f + 0.4f * sin(phase * 3f)
        drawFourPointStar(canvas, -r * 0.65f, -r * 0.45f, r * 0.42f * pulse, alpha, 0xFFFF2A85.toInt())
    }

    private fun drawFourPointStar(canvas: Canvas, cx: Float, cy: Float, radius: Float, alpha: Int, color: Int) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = alpha
        path.reset()
        path.moveTo(cx, cy - radius)
        path.quadTo(cx, cy, cx + radius, cy)
        path.quadTo(cx, cy, cx, cy + radius)
        path.quadTo(cx, cy, cx - radius, cy)
        path.quadTo(cx, cy, cx, cy - radius)
        path.close()
        canvas.drawPath(path, paint)
    }

    // 17. Lo-Fi Aura Orbs
    private fun drawLofiOrbs(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        val pulse = 0.85f + 0.15f * sin(phase * 2f)
        paint.style = Paint.Style.FILL
        paint.color = 0xFF9D4EDD.toInt()
        paint.alpha = (alpha * 0.35f).toInt()
        canvas.drawCircle(0f, 0f, r * 1.1f * pulse, paint)

        paint.color = 0xFF00F5D4.toInt()
        paint.alpha = (alpha * 0.7f).toInt()
        canvas.drawCircle(r * 0.15f, -r * 0.1f, r * 0.62f * pulse, paint)

        paint.color = Color.WHITE
        paint.alpha = alpha
        canvas.drawCircle(0f, 0f, r * 0.28f, paint)
    }

    // 18. Cotton Nebula Dreamy Cloud
    private fun drawDreamyCloud(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        paint.style = Paint.Style.FILL
        paint.color = 0xFFD9C7FF.toInt()
        paint.alpha = alpha
        canvas.drawCircle(-r * 0.42f, r * 0.08f, r * 0.42f, paint)
        canvas.drawCircle(0f, -r * 0.14f, r * 0.52f, paint)
        canvas.drawCircle(r * 0.45f, r * 0.08f, r * 0.38f, paint)
        rectF.set(-r * 0.45f, 0f, r * 0.48f, r * 0.48f)
        canvas.drawRect(rectF, paint)

        // Dangling Golden Star
        val swing = sin(phase * 2f) * r * 0.15f
        drawFourPointStar(canvas, swing, r * 0.82f, r * 0.25f, alpha, 0xFFFFBE0B.toInt())
    }

    // 19. Nighthawk Micro-Heli
    private fun drawMiniHelicopter(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        // Tail Boom
        strokePaint.color = 0xFF00F0FF.toInt()
        strokePaint.alpha = alpha
        strokePaint.strokeWidth = r * 0.14f
        canvas.drawLine(-r * 0.95f, -r * 0.05f, -r * 0.1f, -r * 0.05f, strokePaint)

        // Cabin
        paint.style = Paint.Style.FILL
        paint.color = 0xFF28345A.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.45f, -r * 0.42f, r * 0.65f, r * 0.38f)
        canvas.drawRoundRect(rectF, r * 0.35f, r * 0.35f, paint)

        // Windshield
        paint.color = 0xFF00F0FF.toInt()
        paint.alpha = alpha
        rectF.set(0f, -r * 0.32f, r * 0.55f, r * 0.12f)
        canvas.drawRoundRect(rectF, r * 0.2f, r * 0.2f, paint)

        // Spinning Top Rotor
        val rotorSpan = r * 1.05f * cos(phase * 9f)
        strokePaint.color = 0xFFFFBE0B.toInt()
        strokePaint.alpha = alpha
        strokePaint.strokeWidth = r * 0.1f
        canvas.drawLine(-rotorSpan, -r * 0.58f, rotorSpan, -r * 0.58f, strokePaint)
    }

    // 20. Sunset Biplane
    private fun drawSunsetBiplane(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFF5C77.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.85f, -r * 0.22f, r * 0.65f, r * 0.22f)
        canvas.drawRoundRect(rectF, r * 0.2f, r * 0.2f, paint)

        // Upper & Lower Biplane Wings
        paint.color = 0xFFFFBE0B.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.35f, -r * 0.52f, r * 0.45f, -r * 0.32f)
        canvas.drawRoundRect(rectF, r * 0.08f, r * 0.08f, paint)
        rectF.set(-r * 0.35f, r * 0.28f, r * 0.45f, r * 0.48f)
        canvas.drawRoundRect(rectF, r * 0.08f, r * 0.08f, paint)

        // Nose Propeller
        val propH = r * 0.48f * abs(sin(phase * 10f)).coerceAtLeast(0.25f)
        paint.color = Color.WHITE
        paint.alpha = alpha
        rectF.set(r * 0.65f, -propH, r * 0.78f, propH)
        canvas.drawOval(rectF, paint)
    }

    // 21. DaVinci Ornopter
    private fun drawDaVinciOrnopter(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        val wingY = sin(phase * 3f) * r * 0.55f
        // Clockwork Brass Hull
        paint.style = Paint.Style.FILL
        paint.color = 0xFFD99B4E.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.65f, -r * 0.25f, r * 0.65f, r * 0.35f)
        canvas.drawOval(rectF, paint)

        // Mechanical Flapping Wing
        strokePaint.color = 0xFF00F5D4.toInt()
        strokePaint.alpha = alpha
        strokePaint.strokeWidth = r * 0.1f
        canvas.drawLine(0f, -r * 0.15f, -r * 0.55f, -r * 0.45f - wingY, strokePaint)
        canvas.drawLine(0f, -r * 0.15f, r * 0.55f, -r * 0.45f - wingY, strokePaint)
    }

    // 22. Roswell Cruiser UFO
    private fun drawRoswellCruiser(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        drawMiniPixelUfo(canvas, r, phase, alpha)
    }

    // 23. Starhopper MK-IV Spaceship
    private fun drawStarhopperSpaceship(canvas: Canvas, r: Float, phase: Float, alpha: Int) {
        // Thruster Flame
        val flameLen = r * (0.45f + 0.25f * abs(sin(phase * 6f)))
        paint.style = Paint.Style.FILL
        paint.color = 0xFF00F0FF.toInt()
        paint.alpha = alpha
        path.reset()
        path.moveTo(-r * 0.55f, -r * 0.18f)
        path.lineTo(-r * 0.55f - flameLen, 0f)
        path.lineTo(-r * 0.55f, r * 0.18f)
        path.close()
        canvas.drawPath(path, paint)

        // Swept Deltawing Fins
        paint.color = 0xFFFF2A85.toInt()
        paint.alpha = alpha
        path.reset()
        path.moveTo(r * 0.2f, 0f)
        path.lineTo(-r * 0.7f, -r * 0.65f)
        path.lineTo(-r * 0.5f, 0f)
        path.lineTo(-r * 0.7f, r * 0.65f)
        path.close()
        canvas.drawPath(path, paint)

        // Fuselage
        paint.color = 0xFFE2E8F0.toInt()
        paint.alpha = alpha
        rectF.set(-r * 0.6f, -r * 0.26f, r * 0.85f, r * 0.26f)
        canvas.drawOval(rectF, paint)

        // Cockpit Canopy
        paint.color = 0xFF00F0FF.toInt()
        paint.alpha = alpha
        rectF.set(0f, -r * 0.16f, r * 0.5f, r * 0.16f)
        canvas.drawOval(rectF, paint)
    }
}
