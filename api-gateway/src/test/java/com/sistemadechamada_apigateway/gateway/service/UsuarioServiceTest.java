package com.sistemadechamada_apigateway.gateway.service;

import com.sistemadechamada_apigateway.gateway.dto.CriarUsuarioDTO;
import com.sistemadechamada_apigateway.gateway.exception.ApiException;
import com.sistemadechamada_apigateway.gateway.model.Role;
import com.sistemadechamada_apigateway.gateway.model.Usuario;
import com.sistemadechamada_apigateway.gateway.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    private UsuarioRepository repository;
    private PasswordEncoder encoder;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        encoder = new BCryptPasswordEncoder(4); // custo baixo só para o teste ser rápido
        service = new UsuarioService(repository, encoder);
    }

    @Test
    void autenticaComSenhaCorreta() {
        Usuario u = Usuario.builder().id(1L).username("admin").senhaHash(encoder.encode("senha123!"))
                .role(Role.ADMIN).ativo(true).build();
        when(repository.buscarPorUsername("admin")).thenReturn(Optional.of(u));

        StepVerifier.create(service.autenticar("Admin", "senha123!")).expectNext(u).verifyComplete();
    }

    @Test
    void recusaSenhaErradaUsuarioInexistenteOuInativo() {
        Usuario inativo = Usuario.builder().id(2L).username("inativo").senhaHash(encoder.encode("senha123!"))
                .role(Role.ALUNO).referenciaId(5L).ativo(false).build();
        Usuario ativo = Usuario.builder().id(1L).username("admin").senhaHash(encoder.encode("senha123!"))
                .role(Role.ADMIN).ativo(true).build();
        when(repository.buscarPorUsername("admin")).thenReturn(Optional.of(ativo));
        when(repository.buscarPorUsername("inativo")).thenReturn(Optional.of(inativo));
        when(repository.buscarPorUsername("ninguem")).thenReturn(Optional.empty());

        StepVerifier.create(service.autenticar("admin", "errada")).expectError(ApiException.class).verify();
        StepVerifier.create(service.autenticar("inativo", "senha123!")).expectError(ApiException.class).verify();
        StepVerifier.create(service.autenticar("ninguem", "x")).expectError(ApiException.class).verify();
    }

    @Test
    void alunoSemReferenciaNaoPodeSerCriado() {
        CriarUsuarioDTO dto = new CriarUsuarioDTO("joao", "senha12345", Role.ALUNO, null);

        StepVerifier.create(service.criar(dto)).expectError(ApiException.class).verify();
        verify(repository, never()).inserir(any());
    }

    @Test
    void criaUsuarioComSenhaEmHash() {
        CriarUsuarioDTO dto = new CriarUsuarioDTO("Joao", "senha12345", Role.ALUNO, 42L);
        when(repository.existeUsername("joao")).thenReturn(false);
        when(repository.inserir(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        StepVerifier.create(service.criar(dto))
                .expectNextMatches(u -> u.getId() == 10L
                        && u.getUsername().equals("joao")
                        && !u.getSenhaHash().equals("senha12345")
                        && encoder.matches("senha12345", u.getSenhaHash()))
                .verifyComplete();
    }
}
