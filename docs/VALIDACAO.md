# Evidências da opção B — 01/10/2026

Validação feita no Windows com JDK 21, Maven Wrapper 3.9.11 e PostgreSQL 16 do
container existente `postgres-dev`. A data da apresentação é 02/10/2026.

## Build e testes

Comando: `.\mvnw.cmd -B verify`.
Resultado final em 01/10/2026, 19:28 (America/Sao_Paulo): **BUILD SUCCESS**;
**49 testes, 0 falhas, 0 erros, 0 ignorados**. JAR executável gerado.
Os relatórios locais estão em `target/surefire-reports/`; o log resumido da execução
está em `target/verify-opcao-b.log` (arquivos de build não versionados).

| Classe | Casos executados | Cobertura |
|---|---:|---|
| AtendimentoServiceTest | 6 | Regras de agendamento e mudança de status anteriores |
| AtendimentoControllerTest | 3 | 201, body inválido 400, recurso inexistente 404 anteriores |
| AtendimentoRepositoryTest | 3 | Filtro e conflito de horário anteriores, com H2 explicitamente isolado |
| RelatorioServiceTest | 2 | Período invertido e data fora da faixa rejeitados antes de consultar banco |
| OpcaoBIntegrationTest | 35 | HTTP, serviços, JPA, paginação, agregações, ordenação, erros, DTOs e OpenAPI |

Os 12 casos anteriores foram preservados. A configuração do DataJpaTest passou a
criar/destruir o schema somente no H2 do perfil test, pois dev agora usa validate.

Na nova integração foram verificados:

- Travessia de todas as páginas da lista e do histórico, sem IDs repetidos, inclusive com datas iguais.
- Ordenação por data/status em ambas as direções relevantes e desempate ascendente por id.
- Filtro aplicado antes de paginar, total filtrado, defaults e limite de tamanho.
- Página além do fim com total preservado; paciente sem histórico 200 e inexistente 404.
- Parâmetros numéricos, sort e status inválidos 400; validação nas três listagens.
- Primeiro instante do período, fração do último segundo e exclusão do início do dia seguinte.
- Datas obrigatórias, calendário inválido, intervalo invertido e faixa de ano suportada.
- Unidades de nomes iguais distintas por id; unidade do atendimento diferente da do profissional.
- Quatro status presentes, inclusive zeros; unidade sem movimento omitida; período vazio 200.
- Uma página com dois registros precisa de duas consultas (conteúdo com joins e COUNT),
  e seus DTOs podem ser montados após limpar o EntityManager, sem consultar relações LAZY.
- Contrato publicado no OpenAPI.

## Verificação no PostgreSQL

A API foi iniciada pelo wrapper na porta 18080, com `ddl-auto=validate` e conexões
configuradas via JDBC com `default_transaction_read_only=on`. A senha foi lida da
configuração existente para o ambiente do processo, sem impressão ou gravação.

O script `.\scripts\demonstrar-opcao-b.ps1 -BaseUrl http://localhost:18080` passou.
Foram feitos somente GETs, sem executar seed, POST, PATCH, DELETE ou migrations.

| Consulta | Resultado observado |
|---|---|
| Página 0, size 3, id asc | 200; IDs 1, 2, 3; total 10 |
| Página 1, size 3, id asc | 200; IDs 4, 5, 6; total 10 |
| CONCLUIDO, size 2 | 200; dois registros na página; total 6 |
| Histórico do paciente 1, size 1 | 200; um registro na página; total 2 |
| Relatório 27/07/2026 a 25/09/2026 | 200; total 10; AGENDADO 0, EM_ATENDIMENTO 2, CONCLUIDO 6, CANCELADO 2 |
| Unidade 1 — UBS SES | Total 7; CONCLUIDO 4, EM_ATENDIMENTO 2, CANCELADO 1, AGENDADO 0 |
| Unidade 2 — UBS Grageru | Total 3; CONCLUIDO 2, CANCELADO 1, outros zero |
| Relatório de 01/01/1900 | 200; total/status zero e unidades vazias |
| size=101 / status=XPTO | 400 com JSON de erro |
| Atendimento 999999999 | 404 com JSON de erro |
| Período invertido / 30/02 / dataFim ausente | 400 com JSON de erro |
| OpenAPI | 200; relatório e parâmetros de paginação presentes |

## Preservação de dados

Antes das verificações, foram contadas as cinco tabelas em transação READ ONLY:
2 municípios, 3 unidades, 4 profissionais, 6 pacientes, 10 atendimentos.
O intervalo real de dataAtendimento é 27/07/2026 a 25/09/2026.

A impressão digital combina representações ordenadas de todas as linhas das cinco
tabelas, com MD5, sem exibir os cadastros. É uma checagem de igualdade do conteúdo
na mesma instância entre as duas leituras; não é backup nem garantia criptográfica.

Antes: `59d01b6e79a0b2dd01e666fa84aa44e2`.
Depois: `59d01b6e79a0b2dd01e666fa84aa44e2`, idêntica ao valor inicial. As contagens permaneceram 2/3/4/6/10. O JAR final foi iniciado após encerrar a execução pelo wrapper, e o script completo de demonstração passou novamente; os registros persistiram após reiniciar a API. O processo temporário foi encerrado ao terminar a verificação.
Para repetir no PowerShell:

```powershell
Get-Content -Raw db/verificar-dados.sql | docker exec -i postgres-dev psql -U postgres -d saude_basica -X -A
```

## Limites

H2 e Mockito não substituem todas as características do PostgreSQL. A verificação
real cobre os endpoints de leitura da opção B e a inicialização/empacotamento.
Os endpoints de escrita antigos foram cobertos pelos testes existentes, mas não
foram executados no banco histórico nesta entrega. Não foi feita uma medição de
carga ou de desempenho em grande volume. O resultado da CI remota é independente
deste build local e deve ser conferido no PR.
