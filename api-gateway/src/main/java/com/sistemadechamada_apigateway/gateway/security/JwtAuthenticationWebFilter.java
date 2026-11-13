package com.sistemadechamada_apigateway.gateway.security;

import com.sistemadechamada_apigateway.gateway.model.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Lê o JWT do header Authorization e coloca o usuário no SecurityContext do Spring Security.
 * Roda para TODAS as requisições (rotas dos microsserviços e endpoints locais como /api/auth/me).
 * Requisição sem token segue anônima (o SecurityConfig decide se pode); token inválido/expirado = 401.
 */
@RequiredArgsConstructor
public class JwtAuthenticationWebFilter implements WebFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        Optional<UsuarioAutenticado> usuario = jwtTokenProvider.validar(authHeader.substring(7).trim());
        if (usuario.isEmpty()) {
            return responderTokenInvalido(exchange);
        }

        UsuarioAutenticado u = usuario.get();
        var authentication = new UsernamePasswordAuthenticationToken(
                u, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.role().name())));

        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private Mono<Void> responderTokenInvalido(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] corpo = "{\"status\":401,\"erro\":\"Unauthorized\",\"mensagem\":\"Token inválido ou expirado. Faça login novamente.\"}"
                .getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(corpo)));
    }
}
