package com.printscript.execution.error

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice // intercepta excepciones
class RestErrors {

    @ExceptionHandler(IllegalArgumentException::class)
    fun badReq(ex: IllegalArgumentException) = ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError(error = ex.message ?: "bad request"))

    @ExceptionHandler(ExecException::class)
    fun exec(ex: ExecException) = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(
            ApiError(
                error = ex.message ?: "code error",
                diagnostic = ex.diagnostic,
            ),
        )

    @ExceptionHandler(Exception::class)
    fun boom(ex: Exception) = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError(error = ex.message ?: "internal error"))
}
