package com.sistemadechamada_apigateway.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                // 1. Desabilita proteção CSRF para APIs REST sem sessão
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // 2. Desabilita formulários padrão de login e autenticação básica HTTP
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                // 3. Regras de rotas e preflight (CORS)
                .authorizeExchange(exchanges -> exchanges
                        // Libera todas as requisições de pré-checagem CORS (OPTIONS)
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Libera endpoints públicos de autenticação e documentação
                        .pathMatchers("/api/auth/**", "/api/alunos/login", "/v3/api-docs/**", "/swagger-ui/**").permitAll()

                        // Libera o tráfego geral para que os filtros customizados do Gateway (ou os microsserviços) processem o JWT
                        .anyExchange().permitAll()
                )
                .build();
    }
}