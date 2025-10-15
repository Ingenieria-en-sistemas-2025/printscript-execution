package com.printscript.execution

import com.printscript.execution.dto.FormatReq
import com.printscript.execution.dto.FormatRes
import com.printscript.execution.dto.LintReq
import com.printscript.execution.dto.LintRes
import com.printscript.execution.dto.ParseReq
import com.printscript.execution.dto.ParseRes
import com.printscript.execution.dto.RunReq
import com.printscript.execution.dto.RunRes
import org.printscript.ast.StatementStream
import org.printscript.common.Result
import org.printscript.common.Version
import org.printscript.runner.LanguageWiring
import org.printscript.runner.LanguageWiringFactory
import org.printscript.runner.ProgramIo
import org.printscript.cli.CliSupport
import org.printscript.common.LabeledError
import org.printscript.token.TokenStream


class ExecutionService : Service {

    // helpers
    private fun toVersion(version: String): Version {
        return CliSupport.resolveVersion(version)
    }

//    private fun createWiring(version: String, language: String, content: String) {
//        val io: ProgramIo = ProgramIo(content)
//        val wiring: LanguageWiring = LanguageWiringFactory.forVersion(toVersion(req.version))
//
//    }

    override fun parse(req: ParseReq): ParseRes {
        val io: ProgramIo = ProgramIo(req.content)
        val wiring: LanguageWiring = LanguageWiringFactory.forVersion(toVersion(req.version))
        val result: StatementStream = wiring.parser.parse(wiring.tokenStreamFromReader(io.openReader()))





    }

    override fun lint(req: LintReq): LintRes {

    }

    override fun execute(req: RunReq): RunRes {

    }

    override fun formatContent(req: FormatReq): FormatRes {

    }
}