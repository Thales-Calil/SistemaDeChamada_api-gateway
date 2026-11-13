package com.sistemadechamada_apigateway.gateway.config;

import com.sistemadechamada_apigateway.gateway.model.Role;
import com.sistemadechamada_apigateway.gateway.model.Usuario;
import com.sistemadechamada_apigateway.gateway.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro ADMIN quando a tabela ainda não tem nenhum.
 * A senha vem da variável ADMIN_INITIAL_PASSWORD (nunca do código).
 * Depois do primeiro acesso, troque a senha por PUT /api/auth/usuarios/{id}/senha
 * e pode remover a variável do App Service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInicialSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin-inicial.username:admin}")
    private String username;

    @Value("${app.admin-inicial.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existeAdmin()) {
            return;
        }
        if (password == null || password.length() < 8) {
            log.warn("Nenhum ADMIN cadastrado e ADMIN_INITIAL_PASSWORD ausente (ou com menos de 8 caracteres). "
                    + "Ninguém conseguirá cadastrar usuários até que a variável seja definida.");
            return;
        }
        usuarioRepository.inserir(Usuario.builder()
                .username(username.trim().toLowerCase())
                .senhaHash(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .ativo(true)
                .build());
        log.info("Usuário ADMIN inicial '{}' criado.", username);
    }
}
