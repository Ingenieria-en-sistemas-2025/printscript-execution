package com.printscript.execution.utils

import org.printscript.interpreter.InputProvider

class QueueInputProvider(items: List<String>) : InputProvider {
    private val it = items.iterator()
    override fun read(prompt: String): String = if (it.hasNext()) it.next() else error("No more inputs available for prompt: $prompt")
}