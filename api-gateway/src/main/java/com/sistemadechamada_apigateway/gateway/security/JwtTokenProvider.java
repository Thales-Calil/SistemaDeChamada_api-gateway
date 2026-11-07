package com.sistemadechamada_apigateway.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationInMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:umaChaveSecretaMuitoSeguraParaOSistemaDeChamada123456}") String secret,
            @Value("${jwt.expiration:86400000}") long expirationInMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationInMs = expirationInMs;
    }

    public String gerarToken(String userId, String role, String username) {
        Date agora = new Date();
        Date validade = new Date(agora.getTime() + expirationInMs);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role.toUpperCase())
                .issuedAt(agora)
                .expiration(validade)
                .signWith(secretKey)
                .compact();
    }

    public Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validarToken(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}