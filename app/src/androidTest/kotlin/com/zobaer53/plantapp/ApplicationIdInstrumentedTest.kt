package com.zobaer53.plantapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationIdInstrumentedTest {
    @Test
    fun applicationIdIsUnchangedFromV1() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.zobaer53.tomato_leaf_disease_detection_ml_androidapp", context.packageName)
    }
}
