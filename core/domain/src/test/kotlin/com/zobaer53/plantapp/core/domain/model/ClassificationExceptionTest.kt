package com.zobaer53.plantapp.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ClassificationExceptionTest {

    @Test
    fun `message defaults to the error name`() {
        val e = ClassificationException(DiagnosisError.MODEL_UNAVAILABLE)

        assertEquals("MODEL_UNAVAILABLE", e.message)
        assertNull(e.cause)
    }

    @Test
    fun `keeps an explicit message and cause`() {
        val cause = RuntimeException("decode failed")
        val e = ClassificationException(DiagnosisError.IMAGE_UNREADABLE, "bad jpeg", cause)

        assertEquals(DiagnosisError.IMAGE_UNREADABLE, e.error)
        assertEquals("bad jpeg", e.message)
        assertSame(cause, e.cause)
    }
}
