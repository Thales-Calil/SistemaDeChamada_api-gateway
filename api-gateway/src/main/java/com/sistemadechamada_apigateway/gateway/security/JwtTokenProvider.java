package com.sistemadechamada_apigateway.gateway.security;

import com.sistemadechamada_apigateway.gateway.model.Role;
import com.sistemadechamada_apigateway.gateway.model.Usuario;
import com.sistemadechamada_apigateway.gateway.model.UsuarioAutenticado;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenProvider {

    private static final String ISSUER = "sistema-chamadas-api-gateway";
    private static final int TAMANHO_MINIMO_SEGREDO = 32; // 256 bits, exigido pelo HS256

    private final SecretKey secretKey;
    private final long expirationInMs;

    /**
     * O segredo vem SOMENTE da variável de ambiente JWT_SECRET (sem valor padrão no código).
     * Se estiver ausente ou curto demais, a aplicação falha ao iniciar.
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:28800000}") long expirationInMs) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET não configurado. Defina a variável de ambiente no App Service.");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException("JWT_SECRET deve ter pelo menos " + TAMANHO_MINIMO_SEGREDO + " caracteres.");
        }
        this.secretKey = Keys.hmacShaKeyFor(bytes);
        this.expirationInMs = expirationInMs;
    }

    public String gerarToken(Usuario usuario) {
        Date agora = new Date();
        Date validade = new Date(agora.getTime() + expirationInMs);

        var builder = Jwts.builder()
                .issuer(ISSUER)
                .subject(usuario.getUsername())
                .claim("userId", usuario.getId())
                .claim("role", usuario.getRole().name())
                .issuedAt(agora)
                .expiration(validade);

        if (usuario.getReferenciaId() != null) {
            builder.claim("refId", usuario.getReferenciaId());
        }
        // HS256 fixo: é o algoritmo que os microsserviços validam (com chave longa o jjwt escolheria HS384/HS512)
        return builder.signWith(secretKey, Jwts.SIG.HS256).compact();
    }

    /** Valida assinatura, emissor e expiração. Retorna vazio se o token for inválido. */
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Role role = Role.valueOf(claims.get("role", String.class));
            Long userId = paraLong(claims.get("userId"));
            Long refId = paraLong(claims.get("refId"));

            if (userId == null || (role != Role.ADMIN && refId == null)) {
                return Optional.empty();
            }
            return Optional.of(new UsuarioAutenticado(userId, claims.getSubject(), role, refId));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public long getExpirationInSeconds() {
        return expirationInMs / 1000;
    }

    private static Long paraLong(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number n) return n.longValue();
        return Long.valueOf(valor.toString());
    }
}
