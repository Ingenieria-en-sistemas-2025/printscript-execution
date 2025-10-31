package com.printscript.execution.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@Profile("!test")
class SecurityConfig(
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val issuer: String,
    @Value("\${auth0.audience}")
    private val audience: String,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .authorizeHttpRequests {
            it
                // (no necesita auth)
                .requestMatchers(GET, "/ping").permitAll()
                .requestMatchers(POST, "/parse").permitAll()
                .requestMatchers(POST, "/lint").permitAll()
                .requestMatchers(POST, "/format").permitAll()
                .requestMatchers(POST, "/run").permitAll()
                .requestMatchers(POST, "/run-tests").permitAll()
                .requestMatchers(POST, "/run-test").permitAll()
                .anyRequest().authenticated()
        }
        .oauth2ResourceServer { rs ->
            rs.jwt { jwt -> jwt.jwtAuthenticationConverter(permissionsConverter()) }
        }
        .csrf { it.disable() }
        .cors { it.disable() }
        .build()

    private fun permissionsConverter(): Converter<Jwt, AbstractAuthenticationToken> {
        val base = JwtGrantedAuthoritiesConverter().apply { setAuthorityPrefix("SCOPE_") }
        return Converter { jwt ->
            val authorities = base.convert(jwt)?.toMutableSet()

            val perms = jwt.getClaimAsStringList("permissions") ?: emptyList()
            authorities?.addAll(perms.map { SimpleGrantedAuthority("SCOPE_$it") })

            JwtAuthenticationToken(jwt, authorities, jwt.subject)
        }
    }

    @Bean
    fun jwtDecoder(tokenValidator: OAuth2TokenValidator<Jwt>): JwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuer).build().apply {
        setJwtValidator(tokenValidator)
    }

    @Bean
    fun tokenValidator(): OAuth2TokenValidator<Jwt> {
        val withIssuer: OAuth2TokenValidator<Jwt> = JwtValidators.createDefaultWithIssuer(issuer)

        val audienceClaimValidator = JwtClaimValidator<List<String>>("aud") { audList ->
            audList != null && audience in audList
        }

        return DelegatingOAuth2TokenValidator(withIssuer, audienceClaimValidator)
    }
}
