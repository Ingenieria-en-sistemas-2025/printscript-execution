package com.printscript.execution

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertTrue

@SpringBootTest(properties = ["streams.enabled=false"])
class ExecutionApplicationTests {

    @Test
    fun contextLoads() {
        assertTrue(true)
    }
}
