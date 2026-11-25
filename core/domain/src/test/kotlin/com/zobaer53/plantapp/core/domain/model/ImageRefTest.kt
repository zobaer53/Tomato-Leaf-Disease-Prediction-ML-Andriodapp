package com.zobaer53.plantapp.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageRefTest {

    @Test
    fun `keeps the value as given`() {
        assertEquals("content://media/1", ImageRef("content://media/1").value)
    }

    @Test
    fun `rejects empty and blank values`() {
        for (blank in listOf("", " ", "\t\n")) {
            assertThrows(IllegalArgumentException::class.java) { ImageRef(blank) }
        }
    }
}
