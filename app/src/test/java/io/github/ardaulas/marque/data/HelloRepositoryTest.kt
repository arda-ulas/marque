package io.github.ardaulas.marque.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HelloRepositoryTest {
    @Test
    fun `greeting returns the expected text`() {
        assertEquals("Hello from Marque", HelloRepository().greeting())
    }
}
