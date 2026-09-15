package io.github.ardaulas.marque.ui.common

import io.github.ardaulas.marque.R
import io.github.ardaulas.marque.core.result.DataError
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class DataErrorMessagesTest {
    @Test
    fun `every error maps to its own string resource`() {
        assertEquals(R.string.error_network, DataError.Network.messageRes())
        assertEquals(R.string.error_timeout, DataError.Timeout.messageRes())
        assertEquals(R.string.error_http, DataError.Http(503).messageRes())
        assertEquals(R.string.error_serialization, DataError.Serialization.messageRes())
        assertEquals(R.string.error_unknown, DataError.Unknown.messageRes())
    }

    @Test
    fun `only Http carries a format argument, the status code`() {
        assertArrayEquals(arrayOf<Any>(503), DataError.Http(503).messageArgs())
        assertArrayEquals(emptyArray<Any>(), DataError.Network.messageArgs())
        assertArrayEquals(emptyArray<Any>(), DataError.Timeout.messageArgs())
        assertArrayEquals(emptyArray<Any>(), DataError.Serialization.messageArgs())
        assertArrayEquals(emptyArray<Any>(), DataError.Unknown.messageArgs())
    }
}
