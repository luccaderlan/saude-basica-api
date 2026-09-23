# Saúde Básica API — Projetos 5 e 6 do roadmap

API REST do Sistema de Atendimentos, conectada ao PostgreSQL.
Spring Boot 3.3.5 · Java 21 · Spring Data JPA · PostgreSQL · JUnit 5 + Mockito + H2 (testes).

---

## 1. Subir o banco

Se o container da Fase 4 já existe, é só ligar:

```bash
docker start postgres-dev
```

Se não existe:

```bash
docker run --name postgres-dev \
  -e POSTGRES_PASSWORD=senha123 \
  -p 5432:5432 \
  -v pgdata:/var/lib/postgresql/data \
  --restart unless-stopped \
  -d postgres:16
```

O banco `saude_basica` precisa existir. Se ainda não criou, no DBeaver:

```sql
CREATE DATABASE saude_basica;
```

> As credenciais vêm das variáveis de ambiente `DB_URL`, `DB_USER` e `DB_PASSWORD`.
> Se nenhuma for definida, o perfil `dev` usa o padrão do container acima (`postgres` / `senha123`).

---

## 2. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

(No Windows: `mvnw.cmd spring-boot:run`)

Na primeira execução o Hibernate cria as 5 tabelas sozinho (`ddl-auto: update`).
Quando aparecer `Started Application in X seconds`, a API está no ar em `http://localhost:8080`.

---

## 3. Popular com dados de teste

Sem paciente e profissional cadastrados, não dá para agendar nada.
Abra `db/seed.sql` no DBeaver (conectado em `saude_basica`) e execute — **uma vez só**.

> Se você já tem os dados da Fase 4 no banco, pode pular este passo.

---

## 4. Endpoints

| Método | Rota | O que faz | Status |
|---|---|---|---|
| POST | `/api/atendimentos` | agenda um atendimento | 201 |
| GET | `/api/atendimentos` | lista todos | 200 |
| GET | `/api/atendimentos?status=AGENDADO` | filtra por status | 200 |
| GET | `/api/atendimentos/{id}` | busca por id | 200 / 404 |
| PATCH | `/api/atendimentos/{id}/status` | muda o status | 200 / 400 |
| DELETE | `/api/atendimentos/{id}` | cancela | 204 |
| GET | `/api/pacientes/{id}/atendimentos` | histórico do paciente | 200 / 404 |

Documentação automática (bônus do roadmap): **http://localhost:8080/swagger-ui.html**

### Exemplo de body do POST

```json
{
  "pacienteId": 1,
  "profissionalId": 1,
  "unidadeId": 1,
  "descricao": "Consulta de rotina",
  "dataAtendimento": "2026-12-01T09:00:00"
}
```

### Exemplo de body do PATCH

```json
{ "status": "CONCLUIDO" }
```

---

## 5. Regras de negócio implementadas

1. Não é possível agendar atendimento com data no passado → **400**
2. Atendimento `CANCELADO` não pode ter o status alterado → **400**
3. Atendimento `CONCLUIDO` não pode ser reaberto → **400**
4. Um profissional não pode ter dois atendimentos `AGENDADO` no mesmo horário → **400**

Todo erro sai em JSON, nunca como stack trace:

```json
{
  "momento": "2026-09-16T00:57:52.098",
  "status": 400,
  "erro": "Regra de negocio",
  "mensagem": "Nao e possivel agendar atendimento com data no passado."
}
```

---

## 6. Testes

```bash
./mvnw test
```

(No Windows: `mvnw.cmd test`)

Não precisa do PostgreSQL: os testes de repositório usam H2 em memória e os demais usam mocks.

| Classe | Tipo | O que cobre |
|---|---|---|
| `AtendimentoServiceTest` | Unitário (Mockito) | data no passado, conflito de horário, cancelado/concluído imutáveis, caminhos felizes |
| `AtendimentoControllerTest` | `@WebMvcTest` | 404 ao buscar id inexistente, 400 com body inválido, 201 ao agendar |
| `AtendimentoRepositoryTest` | `@DataJpaTest` (H2) | `findByStatus` e a consulta de conflito de horário |

---

## 7. Estrutura de pacotes

```
com.saudebasica
├── domain/        entidades JPA (o que existe no banco)
├── repository/    interfaces JpaRepository (acesso ao banco)
├── service/       regras de negócio
├── dto/           o que a API recebe e devolve
├── web/           controllers REST + tratamento de erro
├── exception/     exceções próprias do sistema
└── config/        beans de configuração (ex: Clock)
```

O fluxo de uma requisição é sempre o mesmo:

```
Postman → Controller → Service → Repository → PostgreSQL
                          ↓
                      DTO → JSON
```
