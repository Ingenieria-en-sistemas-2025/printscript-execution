package com.printscript.execution.model

import com.printscript.execution.application.ExecutionService
import com.printscript.execution.application.ExecutionServiceImpl
import com.printscript.execution.domain.RunTestCaseDto
import com.printscript.execution.domain.RunTestsReq
import com.printscript.execution.domain.RunTestsRes
import com.printscript.execution.domain.SingleTestResultDto
import com.printscript.execution.domain.SummaryDto
import com.printscript.execution.web.ExecutionController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.printscript.contracts.formatter.FormatReq
import io.printscript.contracts.formatter.FormatRes
import io.printscript.contracts.linting.LintReq
import io.printscript.contracts.linting.LintRes
import io.printscript.contracts.parse.ParseReq
import io.printscript.contracts.parse.ParseRes
import io.printscript.contracts.run.RunReq
import io.printscript.contracts.run.RunRes
import io.printscript.contracts.tests.RunSingleTestReq
import io.printscript.contracts.tests.RunSingleTestRes
import org.springframework.http.HttpStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ExecutionControllerTest {

    private val service = mockk<ExecutionService>()
    private val controller = ExecutionController(service)

    @Test
    fun `ping devuelve 204`() {
        val res = controller.ping()
        assertEquals(HttpStatus.NO_CONTENT, res.statusCode)
        assertEquals(null, res.body)
    }

    @Test
    fun `parse delega en service parse`() {
        val req = ParseReq(language = "printscript", version = "1.1", content = "let a: number = 1;")
        val expected = ParseRes(valid = true, diagnostics = emptyList())

        every { service.parse(req) } returns expected

        val res = controller.parse(req)

        assertEquals(expected, res)
        verify(exactly = 1) { service.parse(req) }
    }

    @Test
    fun `lint delega en service lint`() {
        val req =
            LintReq(language = "printscript", version = "1.1", content = "code", configText = null, configFormat = null)
        val expected = LintRes(violations = emptyList())

        every { service.lint(req) } returns expected

        val res = controller.lint(req)

        assertEquals(expected, res)
        verify(exactly = 1) { service.lint(req) }
    }

    @Test
    fun `format delega en service formatContent`() {
        val req = FormatReq(
            language = "printscript",
            version = "1.1",
            content = "code",
            configText = null,
            configFormat = null,
            options = null,
        )
        val expected = FormatRes(formattedContent = "formatted")

        every { service.formatContent(req) } returns expected

        val res = controller.formatContent(req)

        assertEquals(expected, res)
        verify(exactly = 1) { service.formatContent(req) }
    }

    @Test
    fun `run delega en service execute`() {
        val req = RunReq(
            language = "printscript",
            version = "1.1",
            content = "println(1);",
            inputs = listOf("1"),
        )
        val expected = RunRes(outputs = listOf("1"))

        every { service.execute(req) } returns expected

        val res = controller.execute(req)

        assertEquals(expected, res)
        verify(exactly = 1) { service.execute(req) }
    }

    @Test
    fun `runTest delega en service runSingleTest`() {
        val singleReq = RunSingleTestReq(
            language = "printscript",
            version = "1.1",
            content = "println(1);",
            inputs = listOf("1"),
            expectedOutputs = listOf("1"),
            options = null,
        )

        val expected = RunSingleTestRes(
            status = "PASS",
            actual = listOf("1"),
            mismatchAt = null,
            diagnostic = null,
        )

        every { service.runSingleTest(singleReq) } returns expected

        val res = controller.runTest(singleReq)

        assertEquals(expected, res)
        verify(exactly = 1) { service.runSingleTest(singleReq) }
    }
}
