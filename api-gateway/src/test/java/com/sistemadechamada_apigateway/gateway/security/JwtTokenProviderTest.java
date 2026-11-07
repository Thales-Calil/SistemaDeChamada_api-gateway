package com.sistemadechamada_apigateway.gateway.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String segredoValido = "umaChaveSecretaMuitoSeguraParaOSistemaDeChamada123456";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(segredoValido, 3600000L); // 1 hora de expiração
    }

    @Test
    void deveGerarEValidarTokenComSucesso() {
        String token = jwtTokenProvider.gerarToken("105", "PROFESSOR", "prof_alberto");

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validarToken(token)).isTrue();
    }

    @Test
    void deveExtrairClaimsCorretamente() {
        String token = jwtTokenProvider.gerarToken("999", "ADMIN", "admin_master");

        Claims claims = jwtTokenProvider.extrairClaims(token);

        assertThat(claims.getSubject()).isEqualTo("admin_master");
        assertThat(claims.get("userId", String.class)).isEqualTo("999");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void deveRejeitarTokenInvalido() {
        String tokenInvalido = "eyJhbGciOiJIUzI1NiJ9.token.invalido.mock";

        assertThat(jwtTokenProvider.validarToken(tokenInvalido)).isFalse();
    }
}