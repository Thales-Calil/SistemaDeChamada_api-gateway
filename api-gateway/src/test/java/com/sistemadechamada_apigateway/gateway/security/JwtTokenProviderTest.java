package com.sistemadechamada_apigateway.gateway.security;

import com.sistemadechamada_apigateway.gateway.model.Role;
import com.sistemadechamada_apigateway.gateway.model.Usuario;
import com.sistemadechamada_apigateway.gateway.model.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres-123";
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SEGREDO, 3600000L);
    }

    private static Usuario usuario(Long id, String username, Role role, Long ref) {
        return Usuario.builder().id(id).username(username).role(role).referenciaId(ref).ativo(true).build();
    }

    @Test
    void deveGerarEValidarTokenComTodosOsDados() {
        String token = provider.gerarToken(usuario(7L, "prof.alberto", Role.PROFESSOR, 105L));

        UsuarioAutenticado u = provider.validar(token).orElseThrow();

        assertThat(u.usuarioId()).isEqualTo(7L);
        assertThat(u.username()).isEqualTo("prof.alberto");
        assertThat(u.role()).isEqualTo(Role.PROFESSOR);
        assertThat(u.referenciaId()).isEqualTo(105L);
    }

    @Test
    void adminNaoPrecisaDeReferencia() {
        String token = provider.gerarToken(usuario(1L, "admin", Role.ADMIN, null));
        assertThat(provider.validar(token)).isPresent();
    }

    @Test
    void deveRejeitarTokenMalformado() {
        assertThat(provider.validar("eyJhbGciOiJIUzI1NiJ9.token.invalido")).isEmpty();
    }

    @Test
    void deveRejeitarTokenAssinadoComOutraChave() {
        JwtTokenProvider outro = new JwtTokenProvider("outra-chave-totalmente-diferente-com-32-chars", 3600000L);
        String token = outro.gerarToken(usuario(1L, "admin", Role.ADMIN, null));
        assertThat(provider.validar(token)).isEmpty();
    }

    @Test
    void deveRejeitarTokenExpirado() {
        JwtTokenProvider expira = new JwtTokenProvider(SEGREDO, -1000L);
        String token = expira.gerarToken(usuario(1L, "admin", Role.ADMIN, null));
        assertThat(provider.validar(token)).isEmpty();
    }

    @Test
    void naoDeveIniciarSemSegredoOuComSegredoCurto() {
        assertThatThrownBy(() -> new JwtTokenProvider("", 1000L)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtTokenProvider("curto", 1000L)).isInstanceOf(IllegalStateException.class);
    }
}
