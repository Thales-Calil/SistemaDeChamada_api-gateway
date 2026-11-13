package com.sistemadechamada_apigateway.gateway.service;

import com.sistemadechamada_apigateway.gateway.dto.CriarUsuarioDTO;
import com.sistemadechamada_apigateway.gateway.exception.ApiException;
import com.sistemadechamada_apigateway.gateway.model.Role;
import com.sistemadechamada_apigateway.gateway.model.Usuario;
import com.sistemadechamada_apigateway.gateway.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.Callable;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /** Hash usado quando o usuário não existe, para o tempo de resposta não revelar se o login existe. */
    private volatile String hashFicticio;

    /** Valida usuário e senha. Erro sempre genérico (401) para não revelar qual dos dois está errado. */
    public Mono<Usuario> autenticar(String username, String senha) {
        return bloqueante(() -> {
            String login = username.trim().toLowerCase();
            Usuario usuario = usuarioRepository.buscarPorUsername(login).orElse(null);

            if (usuario == null) {
                passwordEncoder.matches(senha, hashFicticio());
                throw credenciaisInvalidas();
            }
            if (!usuario.isAtivo() || !passwordEncoder.matches(senha, usuario.getSenhaHash())) {
                throw credenciaisInvalidas();
            }
            return usuario;
        });
    }

    public Mono<Usuario> criar(CriarUsuarioDTO dto) {
        return bloqueante(() -> {
            String login = dto.getUsername().trim().toLowerCase();

            if (dto.getRole() != Role.ADMIN && dto.getReferenciaId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "referenciaId é obrigatório para " + dto.getRole() + " (id do aluno ou do professor).");
            }
            if (usuarioRepository.existeUsername(login)) {
                throw new ApiException(HttpStatus.CONFLICT, "Já existe um usuário com o username '" + login + "'.");
            }

            Usuario usuario = Usuario.builder()
                    .username(login)
                    .senhaHash(passwordEncoder.encode(dto.getPassword()))
                    .role(dto.getRole())
                    .referenciaId(dto.getRole() == Role.ADMIN ? null : dto.getReferenciaId())
                    .ativo(true)
                    .build();
            try {
                return usuarioRepository.inserir(usuario);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Já existe um login vinculado a este " + dto.getRole() + " (referenciaId " + dto.getReferenciaId() + ").");
            }
        });
    }

    public Mono<List<Usuario>> listar() {
        return bloqueante(usuarioRepository::listarTodos);
    }

    public Mono<Usuario> buscarPorId(Long id) {
        return bloqueante(() -> usuarioRepository.buscarPorId(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário " + id + " não encontrado.")));
    }

    public Mono<Void> alterarAtivo(Long id, boolean ativo) {
        return bloqueante(() -> {
            if (usuarioRepository.alterarAtivo(id, ativo) == 0) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Usuário " + id + " não encontrado.");
            }
            return Boolean.TRUE;
        }).then();
    }

    public Mono<Void> alterarSenha(Long id, String novaSenha) {
        return bloqueante(() -> {
            if (usuarioRepository.alterarSenha(id, passwordEncoder.encode(novaSenha)) == 0) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Usuário " + id + " não encontrado.");
            }
            return Boolean.TRUE;
        }).then();
    }

    private String hashFicticio() {
        if (hashFicticio == null) {
            hashFicticio = passwordEncoder.encode("senha-ficticia-para-tempo-constante");
        }
        return hashFicticio;
    }

    private static ApiException credenciaisInvalidas() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos.");
    }

    /** Executa JDBC (bloqueante) fora das threads do Netty. */
    private static <T> Mono<T> bloqueante(Callable<T> acao) {
        return Mono.fromCallable(acao).subscribeOn(Schedulers.boundedElastic());
    }
}
