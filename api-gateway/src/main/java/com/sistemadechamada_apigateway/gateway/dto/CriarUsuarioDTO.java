package com.sistemadechamada_apigateway.gateway.dto;

import com.sistemadechamada_apigateway.gateway.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarUsuarioDTO {

    @NotBlank(message = "é obrigatório")
    @Size(min = 3, max = 100, message = "deve ter entre 3 e 100 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "use apenas letras, números, ponto, hífen ou sublinhado")
    private String username;

    @NotBlank(message = "é obrigatória")
    @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
    private String password;

    @NotNull(message = "é obrigatório (ADMIN, PROFESSOR ou ALUNO)")
    private Role role;

    /** Obrigatório para PROFESSOR (id do professor) e ALUNO (id do aluno). */
    private Long referenciaId;
}
