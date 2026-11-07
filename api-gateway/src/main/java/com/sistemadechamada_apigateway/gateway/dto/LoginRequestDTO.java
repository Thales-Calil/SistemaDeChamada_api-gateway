package com.sistemadechamada_apigateway.gateway.dto;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String username; // ex: "admin", "professor", "aluno"
    private String password;
    private String userId;   // Opcional para simulação (ex: "1", "105", "65a1f2b3c4d5e6f7a8b9c0d1")
}