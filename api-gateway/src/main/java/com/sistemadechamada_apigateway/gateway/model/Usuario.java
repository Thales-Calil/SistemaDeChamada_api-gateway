package com.sistemadechamada_apigateway.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    private Long id;
    private String username;
    private String senhaHash;
    private Role role;
    /** Id do aluno (aluno-service) ou do professor (turma-service). Nulo para ADMIN. */
    private Long referenciaId;
    private boolean ativo;
}
