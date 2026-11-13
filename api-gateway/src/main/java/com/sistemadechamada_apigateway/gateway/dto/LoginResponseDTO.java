package com.sistemadechamada_apigateway.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String tokenType;
    /** Validade do token em segundos (o front usa para voltar ao login quando expirar). */
    private long expiresIn;
    private Long userId;
    private String username;
    private String role;
    /** Id do aluno ou do professor vinculado ao login (nulo para ADMIN). */
    private Long referenciaId;
}
