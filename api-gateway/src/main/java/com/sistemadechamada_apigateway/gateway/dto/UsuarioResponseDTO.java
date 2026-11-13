package com.sistemadechamada_apigateway.gateway.dto;

import com.sistemadechamada_apigateway.gateway.model.Usuario;
import lombok.Data;

@Data
public class UsuarioResponseDTO {
    private Long id;
    private String username;
    private String role;
    private Long referenciaId;
    private boolean ativo;

    public UsuarioResponseDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.username = usuario.getUsername();
        this.role = usuario.getRole().name();
        this.referenciaId = usuario.getReferenciaId();
        this.ativo = usuario.isAtivo();
    }
}
