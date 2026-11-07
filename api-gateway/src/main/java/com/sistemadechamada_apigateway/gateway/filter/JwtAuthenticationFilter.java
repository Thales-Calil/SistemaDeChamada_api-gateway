package com.sistemadechamada_apigateway.gateway.filter;


import com.sistemadechamada_apigateway.gateway.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // 1. Endpoints públicos: Login, Swagger e Actuator
        if (path.startsWith("/api/auth") || path.contains("/v3/api-docs") || path.contains("/swagger-ui")) {
            return chain.filter(exchange);
        }

        // 2. Extração do Token no Header Authorization
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        // 3. Validação da assinatura e expiração do JWT
        if (!jwtTokenProvider.validarToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        Claims claims = jwtTokenProvider.extrairClaims(token);
        String role = claims.get("role", String.class);
        String userId = claims.get("userId", String.class);

        // 4. Matriz de Autorização (RBAC) por Perfil

        // ADMIN: Permissão total CRUD em todos os serviços
        if ("ADMIN".equals(role)) {
            return repassarComHeaders(exchange, chain, userId, role);
        }

        // ALUNO: Acesso apenas de leitura aos seus dados. Bloqueia criação/alteração/exclusão.
        if ("ALUNO".equals(role)) {
            if (path.startsWith("/api/professores") || path.startsWith("/api/turmas") || path.startsWith("/api/disciplinas")) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
            if ((path.startsWith("/api/chamadas") || path.startsWith("/api/alunos")) && !"GET".equalsIgnoreCase(method)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }

        // PROFESSOR: Acesso a chamadas e turmas, mas sem permissão de escrita em cadastro base de professores/disciplinas
        if ("PROFESSOR".equals(role)) {
            if ((path.startsWith("/api/professores") || path.startsWith("/api/disciplinas")) && !"GET".equalsIgnoreCase(method)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }

        return repassarComHeaders(exchange, chain, userId, role);
    }

    private Mono<Void> repassarComHeaders(ServerWebExchange exchange, GatewayFilterChain chain, String userId, String role) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -1;
    }
}