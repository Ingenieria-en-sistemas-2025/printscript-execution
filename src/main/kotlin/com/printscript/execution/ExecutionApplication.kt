package com.printscript.execution

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ExecutionApplication

fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    runApplication<ExecutionApplication>(*args)
}
