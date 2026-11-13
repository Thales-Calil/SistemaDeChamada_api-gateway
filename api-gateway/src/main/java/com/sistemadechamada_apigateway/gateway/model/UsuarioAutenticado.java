package com.sistemadechamada_apigateway.gateway.model;

/**
 * Dados do usuário extraídos de um JWT válido. Fica no SecurityContext
 * e é usado para autorização e para os headers repassados aos microsserviços.
 */
public record UsuarioAutenticado(Long usuarioId, String username, Role role, Long referenciaId) {
}
