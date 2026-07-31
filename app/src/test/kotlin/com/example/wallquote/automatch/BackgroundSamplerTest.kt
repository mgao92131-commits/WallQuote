package com.example.wallquote.automatch

import android.graphics.Bitmap
import android.graphics.Color
import com.example.wallquote.domain.model.BackgroundSpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P4-003: Auto Match sampling must apply the photo's `dimAmount` to sampled pixels so the
 * suggestion matches what is actually rendered on top of the dimmed photo, not the raw image.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackgroundSamplerTest {

    private val sampler = DefaultBackgroundSampler()

    private fun whiteBitmap(): Bitmap {
        // Fill via setPixel rather than Canvas.drawColor: Robolectric's shadow Canvas does not
        // reliably rasterize onto the backing Bitmap's pixel buffer, but setPixel/getPixel are
        // backed directly by the shadow bitmap's pixel array.
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (y in 0 until 64) {
            for (x in 0 until 64) {
                bitmap.setPixel(x, y, Color.WHITE)
            }
        }
        return bitmap
    }

    private fun photoSpec(dimAmount: Float) =
        BackgroundSpec.Photo(assetId = "asset-1", dimAmount = dimAmount)

    @Test
    fun samplePhoto_zeroDim_matchesUndimmedLuminance() = runTest {
        val sample = sampler.sample(photoSpec(dimAmount = 0f), whiteBitmap())
        // Pure white has relative luminance 1.0.
        assertEquals(1f, sample.averageLuminance, 0.02f)
    }

    @Test
    fun samplePhoto_higherDim_producesLowerLuminance() = runTest {
        val noDim = sampler.sample(photoSpec(dimAmount = 0f), whiteBitmap())
        val halfDim = sampler.sample(photoSpec(dimAmount = 0.5f), whiteBitmap())
        val fullDim = sampler.sample(photoSpec(dimAmount = 1f), whiteBitmap())

        assertTrue(noDim.averageLuminance > halfDim.averageLuminance)
        assertTrue(halfDim.averageLuminance > fullDim.averageLuminance)
    }

    @Test
    fun samplePhoto_fullDim_isEffectivelyBlack() = runTest {
        val sample = sampler.sample(photoSpec(dimAmount = 1f), whiteBitmap())
        assertEquals(0f, sample.averageLuminance, 0.02f)
    }

    @Test
    fun samplePhoto_dimAmountIsPartOfCacheKey_soDifferentDimsAreNotConflated() = runTest {
        // Same source bitmap, two different dim amounts: results must differ (P4-004 relies on
        // dimAmount being part of the auto-match cache/staleness key).
        val bitmap = whiteBitmap()
        val low = sampler.sample(photoSpec(dimAmount = 0.1f), bitmap)
        val high = sampler.sample(photoSpec(dimAmount = 0.9f), bitmap)
        assertTrue(low.averageLuminance > high.averageLuminance)
    }

    @Test
    fun sampleSolid_doesNotApplyDim() = runTest {
        // Solid/gradient backgrounds have no dimAmount concept; sampling must reflect the raw
        // color regardless of any (unrelated) photo dim state.
        val sample = sampler.sample(BackgroundSpec.Solid("#FFFFFF"), photoBitmap = null)
        assertEquals(1f, sample.averageLuminance, 0.02f)
    }
}
