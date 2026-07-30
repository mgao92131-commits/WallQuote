package com.example.wallquote.data.background

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.wallquote.domain.background.BackgroundAssetId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun commitAndDeleteLifecycle() = runBlocking {
        val stagedFile = File(context.cacheDir, "background_staging/draft_test.tmp").also {
            it.parentFile?.mkdirs()
            Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).compress(
                Bitmap.CompressFormat.JPEG,
                90,
                it.outputStream(),
            )
        }
        // Use import path via temporary content-like file by staging manually then commit.
        val staged = com.example.wallquote.domain.background.StagedBackgroundAsset(
            draftId = "d1",
            stagingToken = "draft_test",
        )
        assertTrue(stagedFile.exists())
        val assetId = store.commit(staged)
        assertTrue(store.exists(assetId))
        assertFalse(stagedFile.exists())
        store.delete(assetId)
        assertFalse(store.exists(assetId))
    }

    @Test
    fun orphanCleanupKeepsReferenced() = runBlocking {
        val formal = File(context.filesDir, "backgrounds").also { it.mkdirs() }
        val keep = File(formal, "bg_keep.jpg").also { it.writeText("x") }
        val drop = File(formal, "bg_drop.jpg").also {
            it.writeText("y")
            it.setLastModified(System.currentTimeMillis() - 2 * 60 * 60 * 1000)
        }
        store.cleanupOrphans(setOf("bg_keep"))
        assertTrue(keep.exists())
        assertFalse(drop.exists())
        store.delete(BackgroundAssetId("bg_keep"))
    }
}
