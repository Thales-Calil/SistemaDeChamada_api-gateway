package com.sistemadechamada_apigateway.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String tokenType;
    private String userId;
    private String role;
}
