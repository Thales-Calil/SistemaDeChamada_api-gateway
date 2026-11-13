package com.sistemadechamada_apigateway.gateway.repository;

import com.sistemadechamada_apigateway.gateway.model.Role;
import com.sistemadechamada_apigateway.gateway.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Acesso JDBC (bloqueante) à tabela usuarios.
 * Sempre chamado pelo UsuarioService dentro de Schedulers.boundedElastic(),
 * para não bloquear as threads reativas do Gateway.
 */
@Repository
@RequiredArgsConstructor
public class UsuarioRepository {

    private static final String COLUNAS = "id, username, senha_hash, role, referencia_id, ativo";

    private static final RowMapper<Usuario> MAPPER = (rs, i) -> Usuario.builder()
            .id(rs.getLong("id"))
            .username(rs.getString("username"))
            .senhaHash(rs.getString("senha_hash"))
            .role(Role.valueOf(rs.getString("role")))
            .referenciaId(rs.getObject("referencia_id") != null ? rs.getLong("referencia_id") : null)
            .ativo(rs.getBoolean("ativo"))
            .build();

    private final JdbcClient jdbcClient;

    public Optional<Usuario> buscarPorUsername(String username) {
        return jdbcClient.sql("SELECT " + COLUNAS + " FROM usuarios WHERE username = :username")
                .param("username", username)
                .query(MAPPER)
                .optional();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return jdbcClient.sql("SELECT " + COLUNAS + " FROM usuarios WHERE id = :id")
                .param("id", id)
                .query(MAPPER)
                .optional();
    }

    public List<Usuario> listarTodos() {
        return jdbcClient.sql("SELECT " + COLUNAS + " FROM usuarios ORDER BY username")
                .query(MAPPER)
                .list();
    }

    public boolean existeUsername(String username) {
        return jdbcClient.sql("SELECT COUNT(1) FROM usuarios WHERE username = :username")
                .param("username", username)
                .query(Integer.class)
                .single() > 0;
    }

    public boolean existeAdmin() {
        return jdbcClient.sql("SELECT COUNT(1) FROM usuarios WHERE role = 'ADMIN'")
                .query(Integer.class)
                .single() > 0;
    }

    public Usuario inserir(Usuario usuario) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                        INSERT INTO usuarios (username, senha_hash, role, referencia_id, ativo)
                        VALUES (:username, :senhaHash, :role, :referenciaId, :ativo)
                        """)
                .param("username", usuario.getUsername())
                .param("senhaHash", usuario.getSenhaHash())
                .param("role", usuario.getRole().name())
                .param("referenciaId", usuario.getReferenciaId())
                .param("ativo", usuario.isAtivo())
                .update(keyHolder, "id");
        usuario.setId(keyHolder.getKeyAs(Number.class).longValue());
        return usuario;
    }

    public int alterarAtivo(Long id, boolean ativo) {
        return jdbcClient.sql("UPDATE usuarios SET ativo = :ativo WHERE id = :id")
                .param("ativo", ativo)
                .param("id", id)
                .update();
    }

    public int alterarSenha(Long id, String senhaHash) {
        return jdbcClient.sql("UPDATE usuarios SET senha_hash = :senhaHash WHERE id = :id")
                .param("senhaHash", senhaHash)
                .param("id", id)
                .update();
    }
}
