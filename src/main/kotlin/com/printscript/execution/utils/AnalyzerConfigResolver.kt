package com.printscript.execution.utils

import com.printscript.execution.dto.LintReq
import com.printscript.execution.error.ErrorMapping
import com.printscript.execution.error.ExecException
import org.printscript.analyzer.config.AnalyzerConfig
import org.printscript.analyzer.loader.ConfigFormat
import org.printscript.analyzer.loader.ConfigReader
import org.printscript.analyzer.loader.JsonConfigReader
import org.printscript.analyzer.loader.YamlConfigReader
import org.printscript.common.Failure
import org.printscript.common.Success

object AnalyzerConfigResolver {
    fun resolveAnalyzerConfig(req: LintReq): AnalyzerConfig {
        if (req.configText.isNullOrBlank()) return AnalyzerConfig()

        val fmt = when (req.configFormat?.lowercase()) {
            null, "", "json" -> ConfigFormat.JSON
            "yaml", "yml"    -> ConfigFormat.YAML
            else -> throw IllegalArgumentException("configFormat inválido: ${req.configFormat}") // ver
        }

        val reader: ConfigReader = when (fmt) {
            ConfigFormat.JSON -> JsonConfigReader()
            ConfigFormat.YAML -> YamlConfigReader()
        }

        val bytes = req.configText.toByteArray(Charsets.UTF_8)
        return when (val result = reader.load(bytes.inputStream())) {
            is Success -> result.value
            is Failure -> throw ExecException(
                diagnostic = ErrorMapping.toApiDiagnostic(result.error, "PS-LINT"),
                msg = result.error.message
            )
        }
    }
}