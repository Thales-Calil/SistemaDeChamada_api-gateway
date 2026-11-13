package com.sistemadechamada_apigateway.gateway.security;

import com.sistemadechamada_apigateway.gateway.model.Role;
import com.sistemadechamada_apigateway.gateway.model.UsuarioAutenticado;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.Map;

/**
 * Matriz de autorização (RBAC) das rotas /api/** repassadas aos microsserviços.
 *
 * ADMIN     -> acesso total.
 * PROFESSOR -> leitura de alunos, turmas, professores, disciplinas, chamadas e presenças
 *              (cada microsserviço filtra pela turma do professor usando X-User-Role / X-Ref-Id);
 *              pode abrir, registrar presença, finalizar e excluir chamadas.
 *              Sem cadastro de alunos, turmas, professores, disciplinas ou usuários.
 * ALUNO     -> somente leitura (GET) dos próprios dados: o {id} da URL precisa ser igual ao
 *              referenciaId do token. Qualquer outro caminho ou método é negado.
 *
 * Contrato com o backend: endpoints usados pelo ALUNO
 *   GET /api/alunos/{alunoId}[/**]
 *   GET /api/presencas/aluno/{alunoId}[/**]
 *   GET /api/turmas/aluno/{alunoId}[/**]
 */
public final class RegrasDeAcesso {

    private static final PathPatternParser PARSER = PathPatternParser.defaultInstance;

    private static final List<PathPattern> PROFESSOR_LEITURA = padroes(
            "/api/alunos/**", "/api/turmas/**", "/api/professores/**",
            "/api/disciplinas/**", "/api/chamadas/**", "/api/presencas/**");

    private static final Map<HttpMethod, List<PathPattern>> PROFESSOR_ESCRITA = Map.of(
            HttpMethod.POST, padroes("/api/chamadas", "/api/chamadas/{id}/presencas", "/api/chamadas/{id}/presencas/**"),
            HttpMethod.PUT, padroes("/api/chamadas/{id}/presencas/**", "/api/chamadas/{id}/finalizar"),
            HttpMethod.DELETE, padroes("/api/chamadas/{id}"));

    /** Padrões do ALUNO: a variável {alunoId} precisa ser o próprio referenciaId. */
    private static final List<PathPattern> ALUNO_LEITURA = padroes(
            "/api/alunos/{alunoId}", "/api/alunos/{alunoId}/**",
            "/api/presencas/aluno/{alunoId}", "/api/presencas/aluno/{alunoId}/**",
            "/api/turmas/aluno/{alunoId}", "/api/turmas/aluno/{alunoId}/**");

    private RegrasDeAcesso() {
    }

    public static boolean permitido(UsuarioAutenticado usuario, HttpMethod metodo, String caminho) {
        if (usuario == null || metodo == null || caminho == null || caminhoSuspeito(caminho)) {
            return false;
        }
        PathContainer path = PathContainer.parsePath(caminho);

        if (usuario.role() == Role.ADMIN) {
            return true;
        }
        if (usuario.role() == Role.PROFESSOR) {
            if (HttpMethod.GET.equals(metodo)) {
                return algumCasa(PROFESSOR_LEITURA, path);
            }
            return algumCasa(PROFESSOR_ESCRITA.getOrDefault(metodo, List.of()), path);
        }
        if (usuario.role() == Role.ALUNO) {
            if (!HttpMethod.GET.equals(metodo) || usuario.referenciaId() == null) {
                return false;
            }
            String proprioId = String.valueOf(usuario.referenciaId());
            for (PathPattern padrao : ALUNO_LEITURA) {
                PathPattern.PathMatchInfo info = padrao.matchAndExtract(path);
                if (info != null && proprioId.equals(info.getUriVariables().get("alunoId"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Bloqueia truques de caminho (/api/alunos/../turmas, //, %2F, ;) que poderiam burlar a matriz. */
    private static boolean caminhoSuspeito(String caminho) {
        return caminho.contains("..") || caminho.contains("//") || caminho.contains("%")
                || caminho.contains(";") || caminho.contains("\\");
    }

    private static boolean algumCasa(List<PathPattern> padroes, PathContainer path) {
        return padroes.stream().anyMatch(p -> p.matches(path));
    }

    private static List<PathPattern> padroes(String... valores) {
        return java.util.Arrays.stream(valores).map(PARSER::parse).toList();
    }
}
