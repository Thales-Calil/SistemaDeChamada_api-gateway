package com.sistemadechamada_apigateway.gateway.controller;

import com.sistemadechamada_apigateway.gateway.dto.LoginRequestDTO;
import com.sistemadechamada_apigateway.gateway.exception.ApiException;
import com.sistemadechamada_apigateway.gateway.exception.GlobalExceptionHandler;
import com.sistemadechamada_apigateway.gateway.model.Role;
import com.sistemadechamada_apigateway.gateway.model.Usuario;
import com.sistemadechamada_apigateway.gateway.security.JwtTokenProvider;
import com.sistemadechamada_apigateway.gateway.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private UsuarioService usuarioService;
    private JwtTokenProvider jwtTokenProvider;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        usuarioService = mock(UsuarioService.class);
        jwtTokenProvider = new JwtTokenProvider("segredo-de-teste-com-mais-de-32-caracteres-123", 3600000L);
        webTestClient = WebTestClient
                .bindToController(new AuthController(usuarioService, jwtTokenProvider))
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginComSenhaCorretaRetornaToken() {
        Usuario prof = Usuario.builder().id(7L).username("prof.alberto").role(Role.PROFESSOR)
                .referenciaId(105L).ativo(true).build();
        when(usuarioService.autenticar("prof.alberto", "senha-correta")).thenReturn(Mono.just(prof));

        webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequestDTO("prof.alberto", "senha-correta"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.role").isEqualTo("PROFESSOR")
                .jsonPath("$.referenciaId").isEqualTo(105)
                .jsonPath("$.expiresIn").isEqualTo(3600);
    }

    @Test
    void loginComSenhaErradaRetorna401() {
        when(usuarioService.autenticar("admin", "errada"))
                .thenReturn(Mono.error(new ApiException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos.")));

        webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequestDTO("admin", "errada"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.mensagem").isEqualTo("Usuário ou senha inválidos.");
    }

    @Test
    void loginSemSenhaRetorna400() {
        webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"admin\"}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
