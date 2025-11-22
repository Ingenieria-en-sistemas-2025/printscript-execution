package com.printscript.execution.domain.language

import com.printscript.execution.domain.diagnostics.ExecException
import com.printscript.execution.domain.diagnostics.diagToDiagnosticDto
import io.printscript.contracts.DiagnosticDto
import io.printscript.contracts.run.RunRes
import org.printscript.common.Failure
import org.printscript.common.Success
import org.printscript.formatter.config.FormatterOptions
import org.printscript.runner.ProgramIo
import org.printscript.runner.helpers.FormatterOptionsLoader
import org.printscript.runner.helpers.QueueInputProvider
import org.printscript.runner.helpers.VersionMapper
import org.printscript.runner.runners.Runner
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream

@Component
class PrintScriptRunnerAdapter(
    @Value("\${execution.languages.printscript.name:printscript}")
    override val language: String = "printscript",
) : LanguageRunnerPort {

    private val logger = LoggerFactory.getLogger(PrintScriptRunnerAdapter::class.java)

    private fun parseVersion(version: String) = VersionMapper.parse(version)

    override fun validate(language: String, version: String, content: String): List<DiagnosticDto> {
        val version = parseVersion(version)
        val io = ProgramIo(source = content)

        return when (val res = Runner.parse(version, io)) {
            is Success -> res.value.map { d ->
                DiagnosticDto(
                    ruleId = d.ruleId,
                    message = d.message,
                    line = d.span.start.line,
                    col = d.span.start.column,
                )
            }
            is Failure -> {
                logger.error("Parse failed: ${res.error.message}")
                throw ExecException(msg = "Parse failed: ${res.error.message}")
            }
        }
    }

    override fun lint(language: String, version: String, content: String, configText: String?): List<DiagnosticDto> {
        val version = parseVersion(version)
        val io = ProgramIo(source = content)
        val configStream = configText?.let { ByteArrayInputStream(it.toByteArray(Charsets.UTF_8)) }

        return when (
            val res = Runner.analyzeWithConfigStream(
                v = version,
                io = io,
                config = configStream,
                onConfigError = { msg -> logger.warn("Analyzer config error: $msg") },
            )
        ) {
            is Success -> res.value.map(::diagToDiagnosticDto)
            is Failure -> {
                logger.error("Analyze failed: ${res.error.message}")
                throw ExecException(msg = "Analyze failed: ${res.error.message}")
            }
        }
    }

    override fun format(language: String, version: String, content: String, configText: String?): String {
        val version = parseVersion(version)
        val io = ProgramIo(source = content)

        val options: FormatterOptions =
            FormatterOptionsLoader.fromBytes(configText?.toByteArray(Charsets.UTF_8))

        return when (val res = Runner.format(version, io, options)) {
            is Success -> res.value
            is Failure -> {
                logger.error("Format failed: ${res.error.message}")
                throw ExecException(msg = "Format failed: ${res.error.message}")
            }
        }
    }

    override fun execute(language: String, version: String, content: String, inputs: List<String>): RunRes {
        val version = parseVersion(version)

        val io = ProgramIo(
            source = content,
            inputProviderOverride = QueueInputProvider(inputs),
        )

        val outputs = mutableListOf<String>()
        val printer: (String) -> Unit = { line ->
            outputs += line
        }

        return when (val res = Runner.execute(version, io, printer, collect = false)) {
            is Success -> RunRes(outputs = outputs)
            is Failure -> {
                logger.error("Execute failed: ${res.error.message}")
                throw ExecException(msg = "Execute failed: ${res.error.message}")
            }
        }
    }
}
