package com.sistemadechamada_apigateway.gateway.controller;

import com.sistemadechamada_apigateway.gateway.dto.LoginRequestDTO;
import com.sistemadechamada_apigateway.gateway.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class AuthControllerTest {

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider("umaChaveSecretaMuitoSeguraParaOSistemaDeChamada123456", 3600000L);
        AuthController authController = new AuthController(jwtTokenProvider);
        webTestClient = WebTestClient.bindToController(authController).build();
    }

    @Test
    void deveRealizarLoginERetornarJwtToken() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("prof_alberto");
        request.setUserId("105");

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.role").isEqualTo("PROFESSOR")
                .jsonPath("$.userId").isEqualTo("105");
    }
}