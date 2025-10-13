package com.helldeck.testutil

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.helldeck.data.HelldeckDb
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

/**
 * Base test class for database-related tests
 */
@ExperimentalCoroutinesApi
abstract class DatabaseTest : BaseTest() {

    protected lateinit var database: HelldeckDb
    protected lateinit var context: Context

    @Before
    override fun setUp() {
        super.setUp()
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            HelldeckDb::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    override fun tearDown() {
        super.tearDown()
        database.close()
    }

    /**
     * Helper function to run database test with proper setup
     */
    protected fun runDatabaseTest(test: suspend () -> Unit) {
        runTest {
            test()
        }
    }

    /**
     * Helper function to clear all database tables
     */
    protected suspend fun clearDatabase() {
        database.clearAllTables()
    }
}