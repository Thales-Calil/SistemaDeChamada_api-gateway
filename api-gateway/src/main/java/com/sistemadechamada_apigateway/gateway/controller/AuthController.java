package com.sistemadechamada_apigateway.gateway.controller;

import com.sistemadechamada_apigateway.gateway.dto.LoginRequestDTO;
import com.sistemadechamada_apigateway.gateway.dto.LoginResponseDTO;
import com.sistemadechamada_apigateway.gateway.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponseDTO>> login(@RequestBody LoginRequestDTO request) {
        String username = request.getUsername() != null ? request.getUsername().toLowerCase() : "aluno";
        String role;
        String userId;

        // Regra de geração de tokens conforme o usuário fornecido
        if (username.contains("admin")) {
            role = "ADMIN";
            userId = request.getUserId() != null ? request.getUserId() : "999";
        } else if (username.contains("prof")) {
            role = "PROFESSOR";
            userId = request.getUserId() != null ? request.getUserId() : "105";
        } else {
            role = "ALUNO";
            userId = request.getUserId() != null ? request.getUserId() : "65a1f2b3c4d5e6f7a8b9c0d1";
        }

        String token = jwtTokenProvider.gerarToken(userId, role, username);
        LoginResponseDTO response = new LoginResponseDTO(token, "Bearer", userId, role);

        return Mono.just(ResponseEntity.ok(response));
    }
}
