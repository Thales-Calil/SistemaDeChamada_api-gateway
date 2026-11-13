package com.sistemadechamada_apigateway.gateway.filter;

import com.sistemadechamada_apigateway.gateway.model.UsuarioAutenticado;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * BFF de identidade: repassa aos microsserviços QUEM é o usuário, para que cada serviço
 * filtre os dados (aluno vê só os próprios dados, professor só a sua turma).
 *
 * Headers enviados aos microsserviços:
 *   X-User-Id   -> id do login (tabela usuarios do Gateway)
 *   X-User-Role -> ADMIN | PROFESSOR | ALUNO
 *   X-Ref-Id    -> id do aluno (aluno-service) ou do professor (turma-service); ausente para ADMIN
 *   X-Username  -> nome de login
 *
 * Os mesmos headers vindos do cliente são SEMPRE removidos antes, para ninguém se passar por outro usuário.
 * A autenticação e a autorização em si são feitas no SecurityConfig (Spring Security).
 */
@Component
public class UsuarioHeadersGlobalFilter implements GlobalFilter, Ordered {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_REF_ID = "X-Ref-Id";
    public static final String HEADER_USERNAME = "X-Username";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(SecurityContext::getAuthentication)
                .filter(auth -> auth.getPrincipal() instanceof UsuarioAutenticado)
                .map(auth -> Optional.of((UsuarioAutenticado) auth.getPrincipal()))
                .defaultIfEmpty(Optional.empty())
                .flatMap(usuario -> chain.filter(exchange.mutate()
                        .request(montarRequest(exchange.getRequest(), usuario.orElse(null)))
                        .build()));
    }

    static ServerHttpRequest montarRequest(ServerHttpRequest original, UsuarioAutenticado usuario) {
        return original.mutate().headers(headers -> {
            headers.remove(HEADER_USER_ID);
            headers.remove(HEADER_USER_ROLE);
            headers.remove(HEADER_REF_ID);
            headers.remove(HEADER_USERNAME);

            if (usuario != null) {
                headers.set(HEADER_USER_ID, String.valueOf(usuario.usuarioId()));
                headers.set(HEADER_USER_ROLE, usuario.role().name());
                headers.set(HEADER_USERNAME, usuario.username());
                if (usuario.referenciaId() != null) {
                    headers.set(HEADER_REF_ID, String.valueOf(usuario.referenciaId()));
                }
            }
        }).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
