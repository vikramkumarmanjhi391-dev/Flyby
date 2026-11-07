package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.animation.DrifterPhysicsEngine
import com.example.model.DrifterCatalog
import com.example.model.DrifterCategory
import com.example.model.MovementPreset
import com.example.overlay.OverlayStateHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify app name and catalog categories`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FlyBy", appName)

        // Ensure all 4 main content categories have built-in original animations
        val animeCount = DrifterCatalog.builtInCharacters.count { it.category == DrifterCategory.ANIME }
        val memeCount = DrifterCatalog.builtInCharacters.count { it.category == DrifterCategory.MEME_PIXEL }
        val cozyCount = DrifterCatalog.builtInCharacters.count { it.category == DrifterCategory.COZY_AESTHETIC }
        val vehicleCount = DrifterCatalog.builtInCharacters.count { it.category == DrifterCategory.VEHICLE_RETRO }

        assertTrue(animeCount >= 5)
        assertTrue(memeCount >= 6)
        assertTrue(cozyCount >= 7)
        assertTrue(vehicleCount >= 5)
    }

    @Test
    fun `verify physics engine moves drifters across all 9 movement presets`() {
        val character = DrifterCatalog.builtInCharacters.first()
        for (preset in MovementPreset.entries) {
            val instance = DrifterPhysicsEngine.spawnDrifter(
                character = character,
                movementPreset = preset,
                canvasWidth = 1080f,
                canvasHeight = 2400f,
                density = 2.75f,
                speedMultiplier = 1.2f,
                sizeMultiplier = 1.0f,
                opacity = 0.8f,
                maxPasses = 3
            )
            val startX = instance.x
            val startY = instance.y

            val alive = DrifterPhysicsEngine.updateDrifter(
                drifter = instance,
                dtSeconds = 0.05f,
                canvasWidth = 1080f,
                canvasHeight = 2400f,
                particleMultiplier = 1.0f
            )
            assertTrue("Drifter should remain alive for preset ${preset.name}", alive)
            assertTrue(
                "Drifter coordinates must advance for preset ${preset.name}",
                instance.x != startX || instance.y != startY
            )
        }
    }

    @Test
    fun `verify untrusted touch opacity query returns valid range`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val maxOpacity = OverlayStateHolder.queryMaxAllowedUntrustedTouchOpacity(context)
        assertTrue(maxOpacity in 0.1f..1.0f)
        assertNotEquals(0f, maxOpacity)
    }
}
