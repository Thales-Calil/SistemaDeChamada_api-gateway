package com.sistemadechamada_apigateway.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "é obrigatório")
    private String username;

    @NotBlank(message = "é obrigatória")
    private String password;
}
