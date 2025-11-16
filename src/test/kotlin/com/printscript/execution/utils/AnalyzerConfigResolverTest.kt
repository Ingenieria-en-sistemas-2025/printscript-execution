package com.printscript.execution.utils

import com.printscript.execution.error.ExecException
import io.printscript.contracts.linting.LintReq
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.printscript.analyzer.config.AnalyzerConfig
import kotlin.test.Test

class AnalyzerConfigResolverTest {

    @Test
    fun `si configText es null devuelve AnalyzerConfig vacio`() {
        val req = LintReq(
            language = "printscript",
            version = "1.1",
            content = "code",
            configText = null,
            configFormat = null,
        )

        val cfg = AnalyzerConfigResolver.resolveAnalyzerConfig(req)

        assertTrue(cfg is AnalyzerConfig)
    }

    @Test
    fun `configFormat invalido tira IllegalArgumentException`() {
        val req = LintReq(
            language = "printscript",
            version = "1.1",
            content = "code",
            configText = "{}",
            configFormat = "xml", // no soportado
        )

        assertThrows(IllegalArgumentException::class.java) {
            AnalyzerConfigResolver.resolveAnalyzerConfig(req)
        }
    }

    @Test
    fun `configText invalido dispara ExecException`() {
        val req = LintReq(
            language = "printscript",
            version = "1.1",
            content = "code",
            configText = "{not-json", // debería hacer fallar el loader
            configFormat = "json",
        )

        assertThrows(ExecException::class.java) {
            AnalyzerConfigResolver.resolveAnalyzerConfig(req)
        }
    }
}
