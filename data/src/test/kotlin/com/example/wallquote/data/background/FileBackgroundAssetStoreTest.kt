package com.example.wallquote.data.background

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.wallquote.domain.background.BackgroundAssetId
import com.example.wallquote.domain.background.BackgroundValidation
import com.example.wallquote.domain.background.StagedBackgroundAsset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FileBackgroundAssetStoreTest {

    private lateinit var context: Context
    private lateinit var store: FileBackgroundAssetStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = FileBackgroundAssetStore(context)
    }

    @Test
    fun prepareKeepsStagingAndCreatesFormal() {
        runBlocking {
            val token = "draft_01234567-89ab-cdef-0123-456789abcdef"
            val stagedFile = File(context.cacheDir, "background_staging/$token.tmp").also {
                it.parentFile?.mkdirs()
                Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    it.outputStream(),
                )
            }
            val staged = StagedBackgroundAsset(draftId = "d1", stagingToken = token)
            assertTrue(stagedFile.exists())
            val assetId = store.prepareFormalAsset(staged)
            assertTrue(BackgroundValidation.isValidAssetId(assetId.value))
            assertTrue(store.exists(assetId))
            assertTrue(stagedFile.exists())
            store.discardStaging(staged)
            assertFalse(stagedFile.exists())
            store.delete(assetId)
            assertFalse(store.exists(assetId))
        }
    }

    @Test
    fun invalidAssetIdResolveReturnsNull() {
        runBlocking {
            assertNull(store.resolvePath(BackgroundAssetId("../escape")))
            assertNull(store.resolvePath(BackgroundAssetId("bg_short")))
        }
    }

    @Test
    fun invalidStagingTokenRejected() {
        runBlocking {
            try {
                store.prepareFormalAsset(StagedBackgroundAsset("d", "not-a-token"))
                fail("expected failure")
            } catch (_: BackgroundImportException) {
            }
        }
    }

    @Test
    fun orphanCleanupKeepsReferenced() {
        runBlocking {
            val formal = File(context.filesDir, "backgrounds").also { it.mkdirs() }
            val keepId = "bg_bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
            val dropId = "bg_cccccccccccccccccccccccccccccccc"
            val keep = File(formal, "$keepId.jpg").also { it.writeText("x") }
            val drop = File(formal, "$dropId.jpg").also {
                it.writeText("y")
                it.setLastModified(System.currentTimeMillis() - 2 * 60 * 60 * 1000)
            }
            store.cleanupOrphans(setOf(keepId))
            assertTrue(keep.exists())
            assertFalse(drop.exists())
            store.delete(BackgroundAssetId(keepId))
        }
    }

    @Test
    fun missingStagingThrowsWithoutCreatingFormal() {
        runBlocking {
            val token = "draft_11111111-2222-3333-4444-555555555555"
            val before = File(context.filesDir, "backgrounds").also { it.mkdirs() }
                .listFiles()
                ?.map { it.name }
                ?.toSet()
                .orEmpty()
            try {
                store.prepareFormalAsset(StagedBackgroundAsset("d", token))
                fail("expected staging_missing")
            } catch (error: BackgroundImportException) {
                assertEquals("staging_missing", error.message)
            }
            val after = File(context.filesDir, "backgrounds")
                .listFiles()
                ?.map { it.name }
                ?.toSet()
                .orEmpty()
            assertEquals(before, after)
        }
    }
}
