package com.printscript.execution.web

import com.printscript.execution.domain.diagnostics.ApiError
import com.printscript.execution.domain.diagnostics.ExecException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice // intercepta excepciones
class RestErrors {

    private val logger = LoggerFactory.getLogger(RestErrors::class.java)

    @ExceptionHandler(IllegalArgumentException::class)
    fun badReq(ex: IllegalArgumentException): ResponseEntity<ApiError> {
        logger.warn("Bad request: {}", ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError(error = ex.message ?: "bad request"))
    }

    @ExceptionHandler(ExecException::class)
    fun exec(ex: ExecException): ResponseEntity<ApiError> {
        logger.info("ExecException: {}", ex.message)
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(
                ApiError(
                    error = ex.message ?: "code error",
                    diagnostic = ex.diagnostic,
                ),
            )
    }

    @ExceptionHandler(Exception::class)
    fun boom(ex: Exception): ResponseEntity<ApiError> {
        logger.error("Unexpected error in Execution service", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError(error = ex.message ?: "internal error"))
    }
}
