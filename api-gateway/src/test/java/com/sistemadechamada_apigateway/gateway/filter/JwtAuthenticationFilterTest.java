package com.sistemadechamada_apigateway.gateway.filter;


import com.sistemadechamada_apigateway.gateway.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtAuthenticationFilter filter;
    private GatewayFilterChain chain;

    private final String secret = "umaChaveSecretaMuitoSeguraParaOSistemaDeChamada123456";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, 3600000L);
        filter = new JwtAuthenticationFilter(jwtTokenProvider);
        chain = Mockito.mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void deveRetornar401QuandoRequisicaoSemToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/turmas").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(chain);
    }

    @Test
    void deveRetornar403QuandoAlunoTentarCriarTurma() {
        String tokenAluno = jwtTokenProvider.gerarToken("1", "ALUNO", "aluno_test");
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/turmas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAluno)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(chain);
    }

    @Test
    void deveInjetarHeadersERepassarQuandoAdminCriarTurma() {
        String tokenAdmin = jwtTokenProvider.gerarToken("999", "ADMIN", "admin_test");
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/turmas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain, times(1)).filter(captor.capture());

        ServerWebExchange exchangeMutada = captor.getValue();
        HttpHeaders headers = exchangeMutada.getRequest().getHeaders();

        assertThat(headers.getFirst("X-User-Id")).isEqualTo("999");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("ADMIN");
    }

    @Test
    void devePermitirAcessoLivreParaEndpointDeLogin() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isNull(); // Não bloqueou com erro
    }
}
