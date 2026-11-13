package com.sistemadechamada_apigateway.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlterarSenhaDTO {

    @NotBlank(message = "é obrigatória")
    @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
    private String novaSenha;
}
