package com.printscript.execution.logs

import com.newrelic.api.agent.NewRelic
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // se ejecuta primero que todos
class CorrelationIdFilter : OncePerRequestFilter() { // solo una vez por request

    companion object {
        const val CORRELATION_ID_KEY = "correlation-id"
        const val CORRELATION_ID_HEADER = "X-Correlation-Id"
    }

    public override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val correlationId = request.getHeader(CORRELATION_ID_HEADER) ?: UUID.randomUUID().toString() // si viene lo usa, sino crea.

        MDC.put(CORRELATION_ID_KEY, correlationId) // mapped diagnostic context -> todos los logs que se hagan dentro de este request van a incluir ese ID automáticamente

        NewRelic.addCustomParameter(CORRELATION_ID_KEY, correlationId)

        response.setHeader(CORRELATION_ID_HEADER, correlationId) // para trazabilidad

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(CORRELATION_ID_KEY) // limpia el contexto para que otro request no herede este correlation id
        }
    }
}
