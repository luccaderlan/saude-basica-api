# Saúde Básica API — opção B do projeto final

API REST em Java 21 / Spring Boot 3.3.5, PostgreSQL e Spring Data JPA.
A opção B acrescenta paginação em todas as listagens existentes e relatório de
atendimentos por status e unidade. Mantém as regras de agendamento da fase 6.

## Executar no banco existente

Pré-requisitos: JDK 21, Docker Desktop ativo e banco `saude_basica` com o esquema
já criado. Na pasta desta API, no PowerShell:

```powershell
docker start postgres-dev
$credencialBanco = Read-Host 'Senha do PostgreSQL' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $credencialBanco).Password
# Opcionais: DB_URL e DB_USER; padrões locais: localhost:5432/saude_basica e postgres.
.\mvnw.cmd spring-boot:run
```

A senha fica no ambiente da sessão, sem aparecer no comando ou em arquivo versionado.
Ao encerrar a sessão de trabalho: `Remove-Item Env:DB_PASSWORD`.
Dev e prod usam `ddl-auto=validate`: a inicialização verifica o esquema, sem modificá-lo.
O comando da preparação com override `--spring.jpa.hibernate.ddl-auto=validate`
continua válido. Não reexecute `db/seed.sql` no banco histórico.

Para um ambiente **novo e descartável**, crie primeiro o banco. É possível iniciar
uma vez com `-Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=update`
para criar as tabelas, parar a API e carregar `db/seed.sql` uma única vez. Esse
procedimento não faz parte da demonstração e não deve ser aplicado ao banco existente.
Não há mudanças de esquema nesta entrega, portanto não há migration a executar.

Maven Wrapper: Maven 3.9.11. Plugin de build Spring Boot 3.3.6 preservado para
corrigir a inicialização em caminhos com acentos no Windows
([issue #43051](https://github.com/spring-projects/spring-boot/issues/43051)).
As dependências da aplicação continuam em 3.3.5.

Swagger: [localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
OpenAPI: [localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs).

## Rotas

| Método | Rota | Comportamento |
|---|---|---|
| POST | `/api/atendimentos` | Agendar, 201; dados/regras inválidos, 400 |
| GET | `/api/atendimentos` | Listagem paginada, 200 |
| GET | `/api/atendimentos?status=CONCLUIDO` | Filtro antes da paginação, 200 |
| GET | `/api/atendimentos/{id}` | Buscar, 200 / 404 |
| PATCH | `/api/atendimentos/{id}/status` | Alterar status, 200 / 400 / 404 |
| DELETE | `/api/atendimentos/{id}` | Cancelar logicamente, 204 / 400 / 404 |
| GET | `/api/pacientes/{id}/atendimentos` | Histórico paginado, 200 / 404 |
| GET | `/api/relatorios/atendimentos?dataInicio=2026-07-27&dataFim=2026-09-25` | Totais por status e unidade no período, 200 / 400 |

POST recebe `pacienteId`, `profissionalId`, `unidadeId`, `descricao` e
`dataAtendimento` (ex.: `2030-01-10T09:00:00`). PATCH recebe
`{"status":"CONCLUIDO"}`. O DELETE altera status para CANCELADO; não remove a linha.
Agendamento no passado, conflito de profissional/horário AGENDADO, mudança de
CANCELADO e reabertura de CONCLUIDO continuam bloqueados.

## Contrato da paginação

As três listagens aceitam os mesmos parâmetros:

| Parâmetro | Padrão | Valores |
|---|---|---|
| `page` | `0` | Inteiro >= 0; primeira página é zero |
| `size` | `20` | Inteiro de 1 a 100; acima do limite retorna 400 |
| `sort` | `dataAtendimento,desc` | Um campo (`id`, `dataAtendimento` ou `status`) e direção `asc`/`desc` |

Datas/status iguais são desempatados por `id,asc`. Se o campo principal for
`id`, vale apenas a direção pedida. Use um único parâmetro `sort`.
A ordenação de status segue os nomes gravados como texto; não é uma ordem clínica.

```powershell
Invoke-RestMethod 'http://localhost:8080/api/atendimentos?page=0&size=3&sort=id,asc'
Invoke-RestMethod 'http://localhost:8080/api/atendimentos?status=CONCLUIDO&page=1&size=2'
Invoke-RestMethod 'http://localhost:8080/api/pacientes/1/atendimentos?size=1'
```

Exemplo de metadados, com o conteúdo omitido apenas neste exemplo:

```json
{
  "content": [],
  "number": 0,
  "size": 3,
  "totalElements": 10,
  "totalPages": 4,
  "first": true,
  "last": false,
  "sort": ["id,asc"]
}
```

`content` contém os mesmos DTOs de atendimento usados anteriormente, com id,
descrição, status, data e nomes. `totalElements`/`totalPages` respeitam o filtro,
sem se limitar aos registros da página. Página além do fim retorna 200 com
`content: []` e o total preservado. Paciente existente sem histórico retorna
página vazia; paciente inexistente retorna 404.

**Mudança de contrato:** antes essas rotas devolviam um array na raiz. Agora
clientes devem ler `resposta.content` e navegar pelos metadados. Não há frontend
neste projeto para migrar. Não serializamos `PageImpl` diretamente.

## Contrato do relatório

`dataInicio` e `dataFim` são obrigatórias, no formato ISO `AAAA-MM-DD`, com anos de 0001 a 9999.
Usamos `dataAtendimento`, incluindo todos os horários dos dias inicial e final:
`>= início às 00:00` e `< dia seguinte ao fim às 00:00`. Data inválida,
ausente ou intervalo invertido retorna 400 com `ErroResposta` em JSON.

```json
{
  "dataInicio": "2026-07-27",
  "dataFim": "2026-09-25",
  "total": 10,
  "porStatus": {"AGENDADO": 0, "EM_ATENDIMENTO": 2, "CONCLUIDO": 6, "CANCELADO": 2},
  "porUnidade": [
    {"unidadeId": 1, "unidadeNome": "Exemplo ilustrativo", "total": 10,
     "porStatus": {"AGENDADO": 0, "EM_ATENDIMENTO": 2, "CONCLUIDO": 6, "CANCELADO": 2}}
  ]
}
```

O exemplo de unidade é ilustrativo; consulte o endpoint para a distribuição real.
Todos os quatro status aparecem, inclusive com zero. Unidades são identificadas
por **id e nome**, ordenadas por id; aparecem somente as que têm atendimentos no
período. O agrupamento usa a unidade do atendimento. Período vazio: 200, total
zero, quatro status zero e `porUnidade: []`. O relatório não é paginado nem depende
do tamanho das páginas das listagens. Mostra o **status atual** dos atendimentos
na data selecionada; não reconstrói o status histórico de cada transição.

## Testar e demonstrar

```powershell
.\mvnw.cmd verify
.\scripts\demonstrar-opcao-b.ps1
```

Os testes usam Mockito/H2; não precisam do PostgreSQL nem de DB_PASSWORD.
A suíte mantém os 12 testes da fase 6 e adiciona testes de integração HTTP,
JPA, serviço, validação, período, agrupamentos e consultas por página.
O workflow `.github/workflows/ci.yml` executa `verify` com Java 21 em pushes e PRs.

A demonstração usa a API em execução e somente GETs. Os defaults do script
correspondem ao banco conferido em 01/10/2026; podem ser sobrescritos com
`-BaseUrl`, `-DataInicio`, `-DataFim`, `-PacienteId`. O dia 01/10 não contém os
atendimentos antigos: usar só a data de hoje produziria um relatório vazio.


## Arquitetura

`web` recebe parâmetros e entrega JSON; `service` valida regras e monta DTOs;
`repository` aplica filtros/paginação/agregação no banco; `domain` representa
as entidades; `dto` define o contrato de entrada/saída; `exception` e
`GlobalExceptionHandler` padronizam os erros; `config` fornece o Clock.
Conversão das entidades ocorre dentro da transação do serviço, com
`open-in-view=false`. As listagens carregam paciente, profissional e unidade
com `@EntityGraph` somente de relacionamentos ManyToOne.
