package com.sistemadechamada_apigateway.gateway.controller;

import com.sistemadechamada_apigateway.gateway.dto.LoginRequestDTO;
import com.sistemadechamada_apigateway.gateway.dto.LoginResponseDTO;
import com.sistemadechamada_apigateway.gateway.model.UsuarioAutenticado;
import com.sistemadechamada_apigateway.gateway.security.JwtTokenProvider;
import com.sistemadechamada_apigateway.gateway.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtTokenProvider jwtTokenProvider;

    /** Login real (1.1): confere usuário e senha (BCrypt) na tabela usuarios. */
    @PostMapping("/login")
    public Mono<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return usuarioService.autenticar(request.getUsername(), request.getPassword())
                .map(usuario -> new LoginResponseDTO(
                        jwtTokenProvider.gerarToken(usuario),
                        "Bearer",
                        jwtTokenProvider.getExpirationInSeconds(),
                        usuario.getId(),
                        usuario.getUsername(),
                        usuario.getRole().name(),
                        usuario.getReferenciaId()));
    }

    /** Dados do usuário logado (o front usa para decidir quais abas e botões mostrar). */
    @GetMapping("/me")
    public Mono<UsuarioAutenticado> me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return Mono.just(usuario);
    }
}
