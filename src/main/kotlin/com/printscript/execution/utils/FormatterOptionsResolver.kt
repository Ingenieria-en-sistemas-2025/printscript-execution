package com.printscript.execution.utils

import com.printscript.execution.dto.FormatReq
import com.printscript.execution.dto.FormatterOptionsDto
import org.printscript.formatter.config.FormatterConfig
import org.printscript.formatter.config.FormatterOptions
import org.printscript.runner.helpers.FormatterOptionsLoader

object FormatterOptionsResolver {
    fun resolve(req: FormatReq): FormatterOptions {

        val base: FormatterOptions = FormatterOptionsLoader.fromBytes(
            req.configText?.toByteArray(Charsets.UTF_8)
        )

        val options: FormatterOptionsDto = req.options ?: return base

        return when (base) {
            is FormatterConfig -> base.copy(
                spaceBeforeColonInDecl        = options.spaceBeforeColonInDecl ?: base.spaceBeforeColonInDecl,
                spaceAfterColonInDecl         = options.spaceAfterColonInDecl ?: base.spaceAfterColonInDecl,
                spaceAroundAssignment         = options.spaceAroundAssignment ?: base.spaceAroundAssignment,
                blankLinesAfterPrintln        = options.blankLinesAfterPrintln ?: base.blankLinesAfterPrintln,
                indentSpaces                  = options.indentSpaces ?: base.indentSpaces,
                mandatorySingleSpaceSeparation= options.mandatorySingleSpaceSeparation ?: base.mandatorySingleSpaceSeparation,
                ifBraceBelowLine              = options.ifBraceBelowLine ?: base.ifBraceBelowLine,
                ifBraceSameLine               = options.ifBraceSameLine ?: base.ifBraceSameLine
            )
            else -> object : FormatterOptions by base {
                override val spaceBeforeColonInDecl        = options.spaceBeforeColonInDecl ?: base.spaceBeforeColonInDecl
                override val spaceAfterColonInDecl         = options.spaceAfterColonInDecl ?: base.spaceAfterColonInDecl
                override val spaceAroundAssignment         = options.spaceAroundAssignment ?: base.spaceAroundAssignment
                override val blankLinesAfterPrintln        = options.blankLinesAfterPrintln ?: base.blankLinesAfterPrintln
                override val indentSpaces                  = options.indentSpaces ?: base.indentSpaces
                override val mandatorySingleSpaceSeparation= options.mandatorySingleSpaceSeparation ?: base.mandatorySingleSpaceSeparation
                override val ifBraceBelowLine              = options.ifBraceBelowLine ?: base.ifBraceBelowLine
                override val ifBraceSameLine               = options.ifBraceSameLine ?: base.ifBraceSameLine
            }
        }

    }
}