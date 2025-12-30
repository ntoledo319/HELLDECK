package com.helldeck.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.engine.TemplateEngine
import com.helldeck.content.model.v2.TemplateV2
import com.helldeck.content.util.SeededRng
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateEngineTest {

    private lateinit var context: Context
    private lateinit var engine: TemplateEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        engine = TemplateEngine(ContentRepository(context), SeededRng(42))
    }

    @Test
    fun `fill returns text unchanged when no slots`() {
        val t = TemplateV2(
            id = "t1",
            game = "TEST",
            family = "test",
            spice = 1,
            locality = 1,
            text = "Hello world",
            max_words = 10,
        )
        val out = engine.fill(t, TemplateEngine.Context())
        assertEquals("Hello world", out.text)
    }
}
