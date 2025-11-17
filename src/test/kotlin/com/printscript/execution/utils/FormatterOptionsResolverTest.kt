package com.printscript.execution.utils

import com.printscript.execution.domain.format.FormatterOptionsResolver
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.printscript.contracts.formatter.FormatReq
import io.printscript.contracts.formatter.FormatRes
import io.printscript.contracts.formatter.FormatterOptionsDto
import org.printscript.formatter.config.FormatterOptions
import org.printscript.runner.helpers.FormatterOptionsLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FormatterOptionsResolverTest {

    @Test
    fun `si no hay options devuelve config base`() {
        val req = FormatReq(
            language = "printscript",
            version = "1.1",
            content = "let a:number=1;",
            configText = null,
            configFormat = null,
            options = null,
        )

        val opts = FormatterOptionsResolver.resolve(req)

        // No sabemos los valores exactos, pero al menos verificamos que no es null
        assertNotNull(opts)
        assertTrue(opts is FormatterOptions)
    }

    @Test
    fun `options sobreescriben campos de la config base`() {
        val dto = FormatterOptionsDto(
            spaceBeforeColonInDecl = true,
            spaceAfterColonInDecl = true,
            spaceAroundAssignment = false,
            blankLinesAfterPrintln = 2,
            indentSpaces = 4,
            mandatorySingleSpaceSeparation = true,
            ifBraceBelowLine = true,
            ifBraceSameLine = false,
        )

        val req = FormatReq(
            language = "printscript",
            version = "1.1",
            content = "let a:number=1;",
            configText = "{}", // base mínima
            configFormat = "json",
            options = dto,
        )

        val opts = FormatterOptionsResolver.resolve(req)

        assertEquals(true, opts.spaceBeforeColonInDecl)
        assertEquals(true, opts.spaceAfterColonInDecl)
        assertEquals(false, opts.spaceAroundAssignment)
        assertEquals(2, opts.blankLinesAfterPrintln)
        assertEquals(4, opts.indentSpaces)
        assertEquals(true, opts.mandatorySingleSpaceSeparation)
        assertEquals(true, opts.ifBraceBelowLine)
        assertEquals(false, opts.ifBraceSameLine)
    }

    @Test
    fun `cuando base no es FormatterConfig usa wrapper que mezcla options y base`() {
        mockkObject(FormatterOptionsLoader)
        try {
            val base = object : FormatterOptions {
                override val spaceBeforeColonInDecl: Boolean = false
                override val spaceAfterColonInDecl: Boolean = false
                override val spaceAroundAssignment: Boolean = true
                override val blankLinesAfterPrintln: Int = 0
                override val indentSpaces: Int = 2
                override val mandatorySingleSpaceSeparation: Boolean = false
                override val ifBraceBelowLine: Boolean = false
                override val ifBraceSameLine: Boolean = true
            }

            every { FormatterOptionsLoader.fromBytes(any()) } returns base

            // DTO con algunos campos nulos (para verificar fallback a base)
            val dto = FormatterOptionsDto(
                spaceBeforeColonInDecl = null, // usa base
                spaceAfterColonInDecl = true, // override
                spaceAroundAssignment = null, // usa base
                blankLinesAfterPrintln = 5, // override
                indentSpaces = null, // usa base
                mandatorySingleSpaceSeparation = true, // override
                ifBraceBelowLine = null, // usa base
                ifBraceSameLine = false, // override
            )

            val req = FormatReq(
                language = "printscript",
                version = "1.1",
                content = "let a:number=1;",
                configText = "ignored",
                configFormat = "json",
                options = dto,
            )

            val opts = FormatterOptionsResolver.resolve(req)

            assertEquals(false, opts.spaceBeforeColonInDecl) // base
            assertEquals(true, opts.spaceAfterColonInDecl) // override
            assertEquals(true, opts.spaceAroundAssignment) // base
            assertEquals(5, opts.blankLinesAfterPrintln) // override
            assertEquals(2, opts.indentSpaces) // base
            assertEquals(true, opts.mandatorySingleSpaceSeparation) // override
            assertEquals(false, opts.ifBraceBelowLine) // base
            assertEquals(false, opts.ifBraceSameLine) // override
        } finally {
            unmockkObject(FormatterOptionsLoader)
        }
    }
}
