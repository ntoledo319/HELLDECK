package com.helldeck.content.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.helldeck.content.data.ContentRepository
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33])
class GameEngineV2IntegrationTest {

    private lateinit var context: Context
    private lateinit var repo: ContentRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repo = ContentRepository(context)
    }

    @Test
    fun `content repository initializes`() {
        repo.initialize()
        assertNotNull(repo)
    }
}
