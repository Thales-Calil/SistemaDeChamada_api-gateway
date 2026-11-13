package com.sistemadechamada_apigateway.gateway.security;

import com.sistemadechamada_apigateway.gateway.model.Role;
import com.sistemadechamada_apigateway.gateway.model.UsuarioAutenticado;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.*;

class RegrasDeAcessoTest {

    private final UsuarioAutenticado admin = new UsuarioAutenticado(1L, "admin", Role.ADMIN, null);
    private final UsuarioAutenticado professor = new UsuarioAutenticado(2L, "prof", Role.PROFESSOR, 105L);
    private final UsuarioAutenticado aluno = new UsuarioAutenticado(3L, "aluno", Role.ALUNO, 42L);

    @Test
    void adminPodeTudo() {
        assertThat(RegrasDeAcesso.permitido(admin, POST, "/api/turmas")).isTrue();
        assertThat(RegrasDeAcesso.permitido(admin, DELETE, "/api/alunos/10")).isTrue();
    }

    @Test
    void professorLeEGerenciaChamadas() {
        assertThat(RegrasDeAcesso.permitido(professor, GET, "/api/alunos")).isTrue();
        assertThat(RegrasDeAcesso.permitido(professor, GET, "/api/presencas/turma/3")).isTrue();
        assertThat(RegrasDeAcesso.permitido(professor, POST, "/api/chamadas")).isTrue();
        assertThat(RegrasDeAcesso.permitido(professor, POST, "/api/chamadas/9/presencas")).isTrue();
        assertThat(RegrasDeAcesso.permitido(professor, POST, "/api/chamadas/9/presencas/lote")).isTrue();
        assertThat(RegrasDeAcesso.permitido(professor, PUT, "/api/chamadas/9/presencas/42")).isTrue();
        assertThat(RegrasDeAcesso.permitido(professor, PUT, "/api/chamadas/9/finalizar")).isTrue();
        assertThat(RegrasDeAcesso.permitido(professor, DELETE, "/api/chamadas/9")).isTrue();
    }

    @Test
    void professorNaoFazCadastros() {
        assertThat(RegrasDeAcesso.permitido(professor, POST, "/api/alunos")).isFalse();
        assertThat(RegrasDeAcesso.permitido(professor, PUT, "/api/alunos/42")).isFalse();
        assertThat(RegrasDeAcesso.permitido(professor, POST, "/api/turmas")).isFalse();
        assertThat(RegrasDeAcesso.permitido(professor, POST, "/api/professores")).isFalse();
        assertThat(RegrasDeAcesso.permitido(professor, DELETE, "/api/disciplinas/1")).isFalse();
    }

    @Test
    void alunoSoLeOsProprioDados() {
        assertThat(RegrasDeAcesso.permitido(aluno, GET, "/api/alunos/42")).isTrue();
        assertThat(RegrasDeAcesso.permitido(aluno, GET, "/api/presencas/aluno/42")).isTrue();
        assertThat(RegrasDeAcesso.permitido(aluno, GET, "/api/turmas/aluno/42")).isTrue();
    }

    @Test
    void alunoNaoVeDadosDeOutrosNemEscreve() {
        assertThat(RegrasDeAcesso.permitido(aluno, GET, "/api/alunos")).isFalse();
        assertThat(RegrasDeAcesso.permitido(aluno, GET, "/api/alunos/43")).isFalse();
        assertThat(RegrasDeAcesso.permitido(aluno, GET, "/api/presencas/aluno/43")).isFalse();
        assertThat(RegrasDeAcesso.permitido(aluno, GET, "/api/turmas")).isFalse();
        assertThat(RegrasDeAcesso.permitido(aluno, POST, "/api/chamadas/9/presencas")).isFalse();
        assertThat(RegrasDeAcesso.permitido(aluno, PUT, "/api/alunos/42")).isFalse();
    }

    @Test
    void bloqueiaTruquesDeCaminho() {
        assertThat(RegrasDeAcesso.permitido(aluno, GET, "/api/alunos/42/../../turmas")).isFalse();
        assertThat(RegrasDeAcesso.permitido(aluno, GET, "/api/alunos/42%2F..%2F43")).isFalse();
        assertThat(RegrasDeAcesso.permitido(professor, POST, "/api//alunos")).isFalse();
        assertThat(RegrasDeAcesso.permitido(aluno, GET, "/api/alunos/42;x=1")).isFalse();
    }
}
