package com.printscript.execution.utils

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class QueueInputProviderTest {

    @Test
    fun `lee inputs en orden`() {
        val provider = QueueInputProvider(listOf("a", "b"))

        assertEquals("a", provider.read("prompt1"))
        assertEquals("b", provider.read("prompt2"))
    }

    @Test
    fun `falla si no hay mas inputs`() {
        val provider = QueueInputProvider(emptyList())

        assertThrows<IllegalStateException> {
            provider.read("prompt")
        }
    }
}
