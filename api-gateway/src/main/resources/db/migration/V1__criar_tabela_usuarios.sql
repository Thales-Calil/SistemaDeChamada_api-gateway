-- Tabela de usuários do API Gateway (autenticação)
-- role: ADMIN | PROFESSOR | ALUNO
-- referencia_id: id do aluno (aluno-service) ou do professor (turma-service). NULL para ADMIN.
CREATE TABLE usuarios (
    id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    username       NVARCHAR(100)  NOT NULL,
    senha_hash     NVARCHAR(100)  NOT NULL,
    role           NVARCHAR(20)   NOT NULL,
    referencia_id  BIGINT         NULL,
    ativo          BIT            NOT NULL CONSTRAINT df_usuarios_ativo DEFAULT 1,
    criado_em      DATETIME2      NOT NULL CONSTRAINT df_usuarios_criado_em DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_usuarios_username UNIQUE (username),
    CONSTRAINT ck_usuarios_role CHECK (role IN ('ADMIN', 'PROFESSOR', 'ALUNO')),
    CONSTRAINT ck_usuarios_referencia CHECK (role = 'ADMIN' OR referencia_id IS NOT NULL)
);

-- Impede dois logins apontando para o mesmo aluno/professor
CREATE UNIQUE INDEX uq_usuarios_role_referencia
    ON usuarios (role, referencia_id)
    WHERE referencia_id IS NOT NULL;
