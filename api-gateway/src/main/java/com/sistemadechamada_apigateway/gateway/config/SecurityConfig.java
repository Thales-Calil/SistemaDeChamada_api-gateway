package com.sistemadechamada_apigateway.gateway.config;

import com.sistemadechamada_apigateway.gateway.model.UsuarioAutenticado;
import com.sistemadechamada_apigateway.gateway.security.JwtAuthenticationWebFilter;
import com.sistemadechamada_apigateway.gateway.security.JwtTokenProvider;
import com.sistemadechamada_apigateway.gateway.security.RegrasDeAcesso;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS (1.5): a ÚNICA fonte das origens permitidas é o application.yml
     * (spring.cloud.gateway.globalcors). O globalcors sozinho só vale para as rotas
     * repassadas aos microsserviços; aqui ele também passa a valer para os endpoints
     * do próprio Gateway (/api/auth/**), sem duplicar a lista em código.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(GlobalCorsProperties globalCorsProperties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        globalCorsProperties.getCorsConfigurations().forEach(source::registerCorsConfiguration);
        return source;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            JwtTokenProvider jwtTokenProvider,
                                                            CorsConfigurationSource corsConfigurationSource) {
        return http
                // API REST sem sessão: sem CSRF, sem formulário, sem basic, sem sessão salva
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Validação do JWT para todas as requisições
                .addFilterAt(new JwtAuthenticationWebFilter(jwtTokenProvider), SecurityWebFiltersOrder.AUTHENTICATION)

                .authorizeExchange(ex -> ex
                        // Preflight CORS
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Login é o único endpoint de negócio público
                        .pathMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // Actuator (1.3): somente health é público; o resto não existe/é negado
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .pathMatchers("/actuator/**").denyAll()

                        // Documentação Swagger
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/webjars/**",
                                "/v3/api-docs", "/v3/api-docs/**").permitAll()

                        // Gestão de usuários: somente ADMIN
                        .pathMatchers("/api/auth/usuarios", "/api/auth/usuarios/**").hasRole("ADMIN")
                        .pathMatchers("/api/auth/me").authenticated()

                        // Rotas dos microsserviços: matriz RBAC por perfil (1.6)
                        .pathMatchers("/api/**").access(regrasDeAcessoPorPerfil())

                        // Qualquer outra coisa é negada
                        .anyExchange().denyAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, e) -> responderErro(exchange, HttpStatus.UNAUTHORIZED,
                                "Autenticação necessária. Envie o header Authorization: Bearer <token>."))
                        .accessDeniedHandler((exchange, e) -> responderErro(exchange, HttpStatus.FORBIDDEN,
                                "Seu perfil não tem permissão para esta operação."))
                )
                .build();
    }

    private ReactiveAuthorizationManager<AuthorizationContext> regrasDeAcessoPorPerfil() {
        return (authentication, context) -> authentication
                .filter(Authentication::isAuthenticated)
                .map(auth -> {
                    var request = context.getExchange().getRequest();
                    boolean permitido = auth.getPrincipal() instanceof UsuarioAutenticado usuario
                            && RegrasDeAcesso.permitido(usuario, request.getMethod(),
                                    request.getPath().pathWithinApplication().value());
                    return new AuthorizationDecision(permitido);
                })
                .defaultIfEmpty(new AuthorizationDecision(false));
    }

    private static Mono<Void> responderErro(ServerWebExchange exchange, HttpStatus status, String mensagem) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"status\":" + status.value() + ",\"erro\":\"" + status.getReasonPhrase()
                + "\",\"mensagem\":\"" + mensagem + "\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8))));
    }
}
