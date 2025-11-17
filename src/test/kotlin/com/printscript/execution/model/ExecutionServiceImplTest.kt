package com.printscript.execution.model

import com.printscript.execution.application.ExecutionServiceImpl
import com.printscript.execution.application.FormatUseCase
import com.printscript.execution.application.LintUseCase
import com.printscript.execution.application.ParseUseCase
import com.printscript.execution.application.RunTestsUseCase
import com.printscript.execution.application.RunUseCase
import com.printscript.execution.domain.RunTestCaseDto
import com.printscript.execution.domain.RunTestsReq
import com.printscript.execution.domain.diagnostics.ExecException
import com.printscript.execution.domain.language.DefaultLanguageRunnerRegistry
import com.printscript.execution.domain.language.LanguageRunnerPort
import com.printscript.execution.domain.language.LanguageRunnerRegistry
import com.printscript.execution.domain.language.PrintScriptRunnerAdapter
import io.printscript.contracts.formatter.FormatReq
import io.printscript.contracts.linting.LintReq
import io.printscript.contracts.parse.ParseReq
import io.printscript.contracts.run.RunReq
import io.printscript.contracts.tests.RunSingleTestReq
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExecutionServiceImplTest {

    private val parseUseCase = ParseUseCase(DefaultLanguageRunnerRegistry(listOf(PrintScriptRunnerAdapter())))
    private val lintUseCase = LintUseCase(DefaultLanguageRunnerRegistry(listOf(PrintScriptRunnerAdapter())))
    private val formatUseCase = FormatUseCase(DefaultLanguageRunnerRegistry(listOf(PrintScriptRunnerAdapter())))
    private val runUseCase = RunUseCase(DefaultLanguageRunnerRegistry(listOf(PrintScriptRunnerAdapter())))
    private val runTestsUseCase = RunTestsUseCase(DefaultLanguageRunnerRegistry(listOf(PrintScriptRunnerAdapter())), runUseCase)

    private val service = ExecutionServiceImpl(
        parseUseCase = parseUseCase,
        lintUseCase = lintUseCase,
        formatUseCase = formatUseCase,
        runUseCase = runUseCase,
        runTestsUseCase = runTestsUseCase,
    )

    @Test
    fun `parse falla si el lenguaje no es printscript`() {
        val req = ParseReq(
            language = "typescript",
            version = "1.1",
            content = "console.log(1);",
        )

        assertThrows<ExecException> {
            service.parse(req)
        }
    }

    @Test
    fun `lint falla si el lenguaje no es printscript`() {
        val req = LintReq(
            language = "javascript",
            version = "1.1",
            content = "code",
            configText = null,
            configFormat = null,
        )

        assertThrows<ExecException> {
            service.lint(req)
        }
    }

    @Test
    fun `execute falla si el lenguaje no es printscript`() {
        val req = RunReq(
            language = "js",
            version = "1.1",
            content = "console.log(1);",
            inputs = null,
        )

        assertThrows<ExecException> {
            service.execute(req)
        }
    }

    @Test
    fun `runTests con contenido vacio y sin expectedOutputs marca PASS`() {
        val req = RunTestsReq(
            language = "printscript",
            version = "1.1",
            content = "", // programa vacío -> sin salidas
            testCases = listOf(
                RunTestCaseDto(
                    inputs = emptyList(),
                    expectedOutputs = emptyList(),
                ),
            ),
            options = null,
        )

        val res = service.runTests(req)

        assertEquals(1, res.summary.total)
        assertEquals(1, res.summary.passed)
        assertEquals("PASS", res.results.first().status)
        assertEquals(emptyList<String>(), res.results.first().expected)
        assertEquals(emptyList<String>(), res.results.first().actual)
        assertNull(res.results.first().mismatchAt)
    }

    @Test
    fun `runTests marca FAIL cuando expectedOutputs difieren de actual`() {
        val req = RunTestsReq(
            language = "printscript",
            version = "1.1",
            content = "", // programa vacío -> actual = []
            testCases = listOf(
                RunTestCaseDto(
                    inputs = emptyList(),
                    expectedOutputs = listOf("1"), // no coincide
                ),
            ),
            options = null,
        )

        val res = service.runTests(req)

        assertEquals(1, res.summary.total)
        // nadie pasa
        assertEquals(0, res.summary.passed)
        val result = res.results.first()
        assertEquals("FAIL", result.status)
        assertEquals(listOf("1"), result.expected)
        assertEquals(emptyList<String>(), result.actual)
        // evaluateOutputs devuelve n = min(expected, actual) = 0
        assertEquals(0, result.mismatchAt)
    }

    @Test
    fun `parse ok para codigo printscript valido`() {
        val req = ParseReq(
            language = "printscript",
            version = "1.1",
            content = "let a: number = 1;",
        )

        val res = service.parse(req)

        assertTrue(res.valid)
        assertTrue(res.diagnostics.isEmpty())
    }

    @Test
    fun `parse devuelve diagnostics para codigo invalido`() {
        val req = ParseReq(
            language = "printscript",
            version = "1.1",
            content = "let : number = 1;",
        )

        val res = service.parse(req)

        assertFalse(res.valid)
        assertTrue(res.diagnostics.isNotEmpty())
    }

    @Test
    fun `execute devuelve outputs para println simple`() {
        val req = RunReq(
            language = "printscript",
            version = "1.1",
            content = """println("hola");""",
            inputs = emptyList(),
        )

        val res = service.execute(req)

        assertEquals(listOf("hola"), res.outputs)
    }

    @Test
    fun `execute lanza ExecException en error de runtime`() {
        val req = RunReq(
            language = "printscript",
            version = "1.1",
            // sintácticamente válido, pero usa variable no declarada
            content = """
                let a: number = b;
                println(a);
            """.trimIndent(),
            inputs = emptyList(),
        )

        assertThrows<ExecException> {
            service.execute(req)
        }
    }

    @Test
    fun `formatContent formatea codigo simple`() {
        val req = FormatReq(
            language = "printscript",
            version = "1.1",
            content = "let a:number=1;",
            configText = """{}""",
            configFormat = "json",
            options = null,
        )

        val res = service.formatContent(req)

        // al menos esperamos que agregue espacios razonables
        assertTrue(res.formattedContent.contains("let a"))
    }

    @Test
    fun `runTests devuelve ERROR cuando hay error de sintaxis al inicio`() {
        val req = RunTestsReq(
            language = "printscript",
            version = "1.1",
            content = "let : number = 1;",
            testCases = listOf(
                RunTestCaseDto(
                    inputs = emptyList(),
                    expectedOutputs = emptyList(),
                ),
            ),
            options = null,
        )

        val res = service.runTests(req)

        assertEquals(1, res.summary.total)
        assertEquals(0, res.summary.passed)
        val result = res.results.first()
        assertEquals("ERROR", result.status)
        assertEquals("PS-SYNTAX", result.diagnostic?.ruleId)
    }

    @Test
    fun `runSingleTest wrapea a runTests y devuelve el resultado del unico test`() {
        val req = RunSingleTestReq(
            language = "printscript",
            version = "1.1",
            content = """println("hola");""",
            inputs = emptyList(),
            expectedOutputs = listOf("hola"),
            options = null,
        )

        val res = service.runSingleTest(req)

        assertEquals("PASS", res.status)
        assertEquals(listOf("hola"), res.actual)
        assertNull(res.mismatchAt)
        assertNull(res.diagnostic)
    }
}
