# Apresentação — 02/10/2026

Duração planejada: 7 minutos, com até 3 minutos para perguntas. Entrega principal:
`saude-basica-api`, branch `codex/relatorios-paginacao`. Ensaiar uma vez lendo este
roteiro e outra explicando com suas próprias palavras.

## Preparar antes da apresentação

- Docker Desktop ativo; `docker start postgres-dev`. Não recriar container/volume.
- Informar DB_PASSWORD no ambiente conforme README e iniciar `.\mvnw.cmd spring-boot:run`.
- Abrir Swagger, README, `docs/DECISOES.md` e o código da consulta do relatório.
- Ter `.\mvnw.cmd verify` concluído e as evidências de `docs/VALIDACAO.md` disponíveis.
- Deixar um segundo PowerShell na pasta da API para executar o script de demonstração.
- Usar o período **27/07/2026 a 25/09/2026**, que contém os dez registros existentes.
  A data de hoje não contém esses registros antigos.
- Não executar seed nem operações de criação/cancelamento no banco histórico.

## 0:00–0:45 — Problema e escopo

Dizer: “Evoluí a API de atendimentos com a opção B. A listagem agora tem limite e
metadados para navegação. O relatório mostra totais por status e unidade no período.
A implementação continua em camadas e preserva o banco existente.”

Mostrar as rotas no README. Explicar que foram paginadas todas as listagens existentes:
geral, filtro por status e histórico do paciente.

## 0:45–1:45 — Paginação funcionando

No Swagger ou PowerShell, executar:

```powershell
$p0 = Invoke-RestMethod 'http://localhost:8080/api/atendimentos?page=0&size=3&sort=id,asc'
$p1 = Invoke-RestMethod 'http://localhost:8080/api/atendimentos?page=1&size=3&sort=id,asc'
$p0 | ConvertTo-Json -Depth 5
$p1 | ConvertTo-Json -Depth 5
```

Esperado: página zero IDs 1/2/3 e página um IDs 4/5/6; total 10, quatro páginas.
Apontar `content`, `number`, `totalElements` e `totalPages`.
Dizer: “O total é do conjunto filtrado, não apenas dos três itens desta página.”
Explicar que size vai de 1 a 100 e que a ordem padrão é data decrescente com id como
desempate. Um cliente antigo que lia array deve passar a ler `content`.

## 1:45–2:30 — Filtro e histórico

```powershell
Invoke-RestMethod 'http://localhost:8080/api/atendimentos?status=CONCLUIDO&size=2' | ConvertTo-Json -Depth 5
Invoke-RestMethod 'http://localhost:8080/api/pacientes/1/atendimentos?size=1' | ConvertTo-Json -Depth 5
```

Esperado: seis concluídos no total, dois na página; paciente 1 tem dois atendimentos,
com um na página. Dizer: “O filtro acontece no banco antes do limite. Paciente sem
histórico tem página vazia; paciente que não existe recebe 404.”

## 2:30–3:45 — Relatório

```powershell
Invoke-RestMethod 'http://localhost:8080/api/relatorios/atendimentos?dataInicio=2026-07-27&dataFim=2026-09-25' | ConvertTo-Json -Depth 6
```

Esperado no banco conferido: total 10, CONCLUIDO 6, CANCELADO 2, EM_ATENDIMENTO 2,
AGENDADO 0. UBS SES total 7; UBS Grageru total 3. Status atual, agrupamento pela
unidade do atendimento. O terceiro cadastro de unidade não tem movimento e não aparece.

Dizer: “O relatório inclui os dias inteiros. A consulta usa início inclusivo e início
do dia seguinte exclusivo, evitando perder horários e frações de segundo. Não preciso
percorrer páginas para calcular os totais: eles vêm de uma consulta agrupada.”

## 3:45–4:30 — Erros e período vazio

Mostrar no Swagger (ou usar o script completo abaixo):

```text
/api/atendimentos?size=101                                        -> 400
/api/atendimentos?status=XPTO                                     -> 400
/api/atendimentos/999999999                                       -> 404
/api/relatorios/atendimentos?dataInicio=2026-10-02&dataFim=2026-10-01 -> 400
/api/relatorios/atendimentos?dataInicio=2026-10-01                  -> 400
/api/relatorios/atendimentos?dataInicio=1900-01-01&dataFim=1900-01-01 -> 200, zeros
```

Apontar o JSON de erro com status/mensagem. Dizer: “Ausência de dados é um resultado
válido; parâmetros inválidos são um erro do pedido.”

## 4:30–5:45 — Código e decisões

Abrir `AtendimentoRepository.contarPorUnidadeEStatus`, `RelatorioService.atendimentos`
e `Paginacao.criar`. Explicar o WHERE e o GROUP BY, sem ler todas as linhas.

Dizer: “O controller trata HTTP. O service valida e monta a resposta. O repository
faz a consulta. Uso DTO para definir o contrato JSON e preservar o encapsulamento.
O EntityGraph carrega as relações ManyToOne necessárias à página, evitando N+1.
Os totais globais e por unidade usam o mesmo resultado agregado.”

## 5:45–7:00 — Evidências e conclusão da entrega

Mostrar `docs/VALIDACAO.md` e o resumo do Maven. Os 12 testes anteriores continuam
presentes. Os novos testes atravessam páginas, verificam datas empatadas, limites
do período, zeros, unidades de mesmo nome, erros e ausência de consultas por item.
Mostrar o PR e a CI, conforme seu estado real naquele momento.

Dizer: “Além dos testes isolados em H2, demonstrei as leituras no PostgreSQL real.
Não reexecutei seed e comparei a impressão digital das cinco tabelas. A inicialização
usa validate, e nesta verificação as conexões eram somente leitura.”

Concluir: “A entrega resolve o escopo da opção B. Para crescer, eu mediria as consultas,
avaliaria índices e paginação por cursor. Autenticação e exportação seriam novas etapas.”

## Alternativa: demonstração completa automática

```powershell
.\scripts\demonstrar-opcao-b.ps1
```

O script confirma páginas distintas, filtro, histórico, somas do relatório, zeros,
400/404 e OpenAPI. Só usa GETs. Se a API usar outra porta:

```powershell
.\scripts\demonstrar-opcao-b.ps1 -BaseUrl 'http://localhost:18080'
```

## Perguntas prováveis e respostas curtas

| Pergunta | Resposta que você pode defender |
|---|---|
| Por que não paginar com stream? | Porque o banco continuaria devolvendo todas as linhas; Pageable aplica limite/offset na consulta. |
| Por que Page em vez de Slice? | Quero mostrar o total e o número de páginas. Isso custa um COUNT; Slice seria melhor se só precisasse saber se há próxima página. |
| Por que id no sort? | Data e status repetem. O id único desempata e estabiliza a ordem no mesmo conjunto de dados. |
| Por que o relatório não é paginado? | Ele devolve contagens por grupo, não os atendimentos individuais. Seus totais independem das páginas. |
| O total inclui cancelados? | Sim, representa todos os atendimentos do período. Realizados/concluídos são a contagem de CONCLUIDO. |
| O relatório reconstrói o status no passado? | Não. Filtra a data do atendimento e mostra seu status atual; não existe histórico de transições no modelo. |
| Unidades com mesmo nome se misturam? | Não, o agrupamento inclui id e nome; os testes verificam esse caso. |
| Por que não corrigiu os registros antigos? | A opção B não exige isso. O relatório usa a unidade do evento e aceita descrições antigas nulas na leitura. |
| H2 garante o PostgreSQL? | Não sozinho. Por isso validei os endpoints de leitura contra o PostgreSQL e comparei os dados antes/depois. |
| Onde está a migration? | Esta mudança só acrescenta consultas e DTOs; não altera esquema. Mudanças futuras de esquema devem ter migrations. |
| Dá para usar em produção hoje? | A funcionalidade está validada no escopo do estágio. Autenticação, concorrência e desempenho em grande volume exigem trabalho adicional. |
| Como você sabe que não alterou dados? | GETs, validate, conexões somente leitura na validação e impressão digital igual das cinco tabelas. |

## Se algo falhar ao vivo

Se não houver conexão, conferir Docker e DB_PASSWORD sem imprimir senha. Se a porta
8080 estiver ocupada, iniciar com `-Dspring-boot.run.arguments=--server.port=18080`
e adaptar BaseUrl. Não reinstalar ferramentas ou repetir seed durante a apresentação.
Usar as evidências salvas e explicar o limite do que conseguiu demonstrar ao vivo.
