package com.printscript.snippets.redis.events

import com.printscript.execution.dto.FormatterOptionsDto
import com.printscript.execution.redis.DomainEvent
import java.util.UUID

data class SnippetsFormattingRulesUpdated(val correlationalId: String, val snippetId: UUID, val language: String, val version: String, val configText: String?, val configFormat: String?, val options: FormatterOptionsDto?, val attempt: Int = 0) : DomainEvent
