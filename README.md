

Problemas encontrados: 
1. Ao instalar o vitejs, dava problema de versão por ser a versão mais recente não carregando os módulos ao testar, a solução encontrada foi dar um downgrade de versão nos remotes para 7.3.0 do vite e para 5.1.4 do react. 
2. Ao tentar utilizar o TypeScript como linguagem, havia conflito com o module federation, assim ao usar o federation, o código não entendia o que era o método.

Comandos para atualizar e rodar após alterar:

npm run build:remotes
npm run start

## API Gateway - configuração no Azure (App Service app-api-gateway-chamadas)

Variáveis de ambiente obrigatórias (Configuração > Configurações do aplicativo):

| Variável | Exemplo / observação |
|---|---|
| `GATEWAY_DB_URL` | `jdbc:sqlserver://<servidor>.database.windows.net:1433;database=<banco>;encrypt=true;trustServerCertificate=false;loginTimeout=30;` |
| `GATEWAY_DB_USER` | usuário do Azure SQL |
| `GATEWAY_DB_PASSWORD` | senha do Azure SQL |
| `JWT_SECRET` | texto aleatório com 32+ caracteres (o mesmo valor será usado pelos microsserviços) |
| `ADMIN_INITIAL_PASSWORD` | senha do primeiro admin (8+ caracteres). Usada só se ainda não existir ADMIN |
| `ALUNO_SERVICE_URL`, `CHAMADA_SERVICE_URL`, `TURMA_SERVICE_URL` | URLs dos microsserviços |

Opcionais: `ADMIN_INITIAL_USERNAME` (padrão `admin`), `JWT_EXPIRATION_MS` (padrão 8h).

A tabela `usuarios` é criada automaticamente pelo Flyway na primeira subida.

### Endpoints de autenticação

- `POST /api/auth/login` - `{ "username", "password" }` -> token, role, referenciaId, expiresIn
- `GET /api/auth/me` - dados do usuário logado
- `GET|POST /api/auth/usuarios`, `PUT /api/auth/usuarios/{id}/senha|ativar|desativar` - somente ADMIN.
  Para PROFESSOR e ALUNO, `referenciaId` = id do professor (turma-service) ou do aluno (aluno-service).

### Headers repassados aos microsserviços

`X-User-Id`, `X-User-Role` (ADMIN/PROFESSOR/ALUNO), `X-Ref-Id` (id do aluno/professor), `X-Username`.
Headers com esses nomes enviados pelo cliente são descartados.
