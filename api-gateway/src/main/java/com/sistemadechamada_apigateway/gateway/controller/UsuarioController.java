package com.sistemadechamada_apigateway.gateway.controller;

import com.sistemadechamada_apigateway.gateway.dto.AlterarSenhaDTO;
import com.sistemadechamada_apigateway.gateway.dto.CriarUsuarioDTO;
import com.sistemadechamada_apigateway.gateway.dto.UsuarioResponseDTO;
import com.sistemadechamada_apigateway.gateway.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/** Cadastro de logins. Acesso restrito a ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/auth/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public Mono<List<UsuarioResponseDTO>> listar() {
        return usuarioService.listar().map(lista -> lista.stream().map(UsuarioResponseDTO::new).toList());
    }

    @GetMapping("/{id}")
    public Mono<UsuarioResponseDTO> buscar(@PathVariable Long id) {
        return usuarioService.buscarPorId(id).map(UsuarioResponseDTO::new);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UsuarioResponseDTO> criar(@Valid @RequestBody CriarUsuarioDTO dto) {
        return usuarioService.criar(dto).map(UsuarioResponseDTO::new);
    }

    @PutMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> alterarSenha(@PathVariable Long id, @Valid @RequestBody AlterarSenhaDTO dto) {
        return usuarioService.alterarSenha(id, dto.getNovaSenha());
    }

    @PutMapping("/{id}/desativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> desativar(@PathVariable Long id) {
        return usuarioService.alterarAtivo(id, false);
    }

    @PutMapping("/{id}/ativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> ativar(@PathVariable Long id) {
        return usuarioService.alterarAtivo(id, true);
    }
}
