package com.helldeck.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.content.db.HelldeckDb
import com.helldeck.content.db.TemplateExposureEntity
import com.helldeck.content.db.TemplateStatEntity
import com.helldeck.data.PlayerEntity
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ExportImportTest {

    private lateinit var context: Context
    private lateinit var db: HelldeckDb

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = HelldeckDb.get(context)
        runBlocking {
            db.templateStatDao().deleteAll()
            db.templateExposureDao().deleteAll()
            db.players().deleteAll()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            db.templateStatDao().deleteAll()
            db.templateExposureDao().deleteAll()
            db.players().deleteAll()
        }
    }

    @Test
    fun `brainpack export and import round trip`() = runBlocking {
        val playerDao = db.players()
        val statDao = db.templateStatDao()
        val exposureDao = db.templateExposureDao()

        val player = PlayerEntity(id = "player-1", name = "Tester", avatar = "😀")
        playerDao.upsert(player)
        statDao.upsert(TemplateStatEntity(templateId = "template-1", visits = 3, rewardSum = 2.5))
        exposureDao.insert(TemplateExposureEntity(templateId = "template-1", timestamp = 1234L))

        val exportResult = kotlin.runCatching { ExportImport.exportBrainpack(context, "test_brainpack.zip") }
        exportResult.exceptionOrNull()?.let { throw it }
        val uri = exportResult.getOrNull()
        assertNotNull("Export returned null", uri)
        val exportFile = File(uri!!.path!!)
        assertTrue(exportFile.exists())

        // Clear tables then import
        playerDao.deleteAll()
        statDao.deleteAll()
        exposureDao.deleteAll()

        val result = ExportImport.importBrainpack(context, uri)
        if (result !is ImportResult.Success) {
            val message = when (result) {
                is ImportResult.Failure -> result.error
                ImportResult.Cancelled -> "cancelled"
                else -> "unknown"
            }
            fail("Import failed: $message")
        }
        val success = result as ImportResult.Success
        assertEquals(1, success.playersImported)

        val restoredPlayers = playerDao.getAllSnapshot()
        assertEquals(1, restoredPlayers.size)
        assertEquals("Tester", restoredPlayers.first().name)

        val restoredStats = statDao.getAll()
        assertEquals(1, restoredStats.size)
    }
}
