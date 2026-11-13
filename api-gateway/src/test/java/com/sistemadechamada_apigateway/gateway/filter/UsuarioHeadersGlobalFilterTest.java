package com.sistemadechamada_apigateway.gateway.filter;

import com.sistemadechamada_apigateway.gateway.model.Role;
import com.sistemadechamada_apigateway.gateway.model.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioHeadersGlobalFilterTest {

    @Test
    void deveSubstituirHeadersForjadosPelosDoToken() {
        MockServerHttpRequest original = MockServerHttpRequest.get("/api/alunos/42")
                .header("X-User-Role", "ADMIN")       // tentativa de se passar por ADMIN
                .header("X-Ref-Id", "999")
                .build();
        UsuarioAutenticado aluno = new UsuarioAutenticado(3L, "aluno", Role.ALUNO, 42L);

        HttpHeaders headers = UsuarioHeadersGlobalFilter.montarRequest(original, aluno).getHeaders();

        assertThat(headers.get("X-User-Role")).containsExactly("ALUNO");
        assertThat(headers.get("X-Ref-Id")).containsExactly("42");
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("3");
        assertThat(headers.getFirst("X-Username")).isEqualTo("aluno");
    }

    @Test
    void semUsuarioRemoveHeadersDeIdentidade() {
        MockServerHttpRequest original = MockServerHttpRequest.get("/v3/api-docs/aluno-service")
                .header("X-User-Role", "ADMIN")
                .build();

        ServerHttpRequest resultado = UsuarioHeadersGlobalFilter.montarRequest(original, null);

        assertThat(resultado.getHeaders().containsKey("X-User-Role")).isFalse();
    }

    @Test
    void adminNaoRecebeRefId() {
        MockServerHttpRequest original = MockServerHttpRequest.get("/api/turmas").build();
        UsuarioAutenticado admin = new UsuarioAutenticado(1L, "admin", Role.ADMIN, null);

        HttpHeaders headers = UsuarioHeadersGlobalFilter.montarRequest(original, admin).getHeaders();

        assertThat(headers.getFirst("X-User-Role")).isEqualTo("ADMIN");
        assertThat(headers.containsKey("X-Ref-Id")).isFalse();
    }
}
