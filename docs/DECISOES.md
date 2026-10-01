# Decisões da opção B e defesa do projeto

## O problema resolvido

Uma listagem sem limite cresce junto com o banco: carrega mais linhas, relacionamentos
e JSON. A paginação limita o trabalho por requisição. O relatório permite responder
quantos atendimentos ocorreram no período, em quais unidades e com quais status,
sem baixar todas as páginas para somar no cliente.

## Fluxo e responsabilidade de cada camada

1. `AtendimentoController` e `PacienteController` recebem `page`, `size`, `sort`
   e o filtro. `RelatorioController` converte datas ISO para LocalDate.
2. `AtendimentoService` valida paginação por `Paginacao`, confere a existência do
   paciente e converte as entidades em DTOs dentro da transação.
3. `AtendimentoRepository` executa consultas paginadas e uma consulta JPQL com
   GROUP BY. `TotalAtendimentosPorUnidadeStatus` é a projeção da agregação.
4. `RelatorioService` valida o período e transforma as contagens por unidade/status
   em totais globais e por unidade. Preenche status ausentes com zero.
5. Os DTOs definem o JSON; `GlobalExceptionHandler` traduz erros para 400/404.

O controller não contém consultas SQL nem regras de domínio. O repositório não
conhece HTTP. O service coordena a regra e o acesso aos dados. Isso permite testar
as regras separadamente e mudar o contrato HTTP sem alterar as entidades.

## Por que Pageable no banco?

`PageRequest` informa offset, limite e ordenação ao Spring Data JPA. O repositório
retorna `Page`, que contém conteúdo e contagens. Fazer `findAll().stream().skip()`
continuaria carregando todos os registros, apesar de devolver poucos ao cliente.
O teste de estatísticas verifica uma página de dois registros com duas consultas:
conteúdo com os relacionamentos e COUNT. Após limpar o EntityManager, o DTO ainda
pode ser construído, comprovando que não depende de consultas LAZY por registro.

O tamanho padrão é 20, com máximo 100. Retornar 400 acima do limite deixa o erro
explícito. `page` começa em zero para seguir PageRequest. São aceitos somente
`id`, `dataAtendimento` e `status` para ordenar: isso evita expor caminhos internos
do modelo e transformar propriedades desconhecidas em erro de servidor.

## Por que desempatar por id?

Datas e status se repetem. Sem desempate, o banco pode devolver registros empatados
em ordens diferentes, causando repetição/omissão quando se passa à próxima página.
O id é único: `dataAtendimento,desc` torna-se `dataAtendimento DESC, id ASC`.
O teste atravessa as páginas com datas iguais e verifica os IDs e a ordem.

Isso garante uma ordem total **para o mesmo conjunto de dados**. Atualizações ou
inserções entre requisições podem deslocar páginas com offset. Em alto volume,
consideraria paginação por cursor, baseada em data/id, e avaliaria o custo do COUNT.
Não alegar que offset oferece uma fotografia imutável durante navegação concorrente.

## Por que DTO de página?

`PaginaDTO` controla nomes e tipos de `content`, `number`, `size`, `totalElements`,
`totalPages`, `first`, `last` e `sort`. Evita que o JSON dependa dos detalhes internos
do PageImpl/framework. O DTO de atendimento existente continua sendo usado dentro
de `content`; não serializamos entidades JPA.

Houve uma quebra explícita do contrato das três listagens: array na raiz passa a
objeto com `content`. O README explica a adaptação necessária. Os outros endpoints
mantêm seus contratos. Um cliente antigo precisaria ser atualizado; não há frontend
neste repositório. Versionaria a rota se precisasse sustentar clientes antigos.

## Por que EntityGraph?

Cada DTO usa nomes de paciente, profissional e unidade. LAZY sem plano de carregamento
pode gerar consultas adicionais por registro, o chamado N+1. O EntityGraph carrega
as três relações ManyToOne junto à página. Essas relações não multiplicam as linhas
do atendimento. Não incluí coleções OneToMany no grafo: joins de coleções podem
multiplicar linhas e prejudicar a paginação. O DTO é montado na transação; o projeto
mantém `open-in-view=false`, sem depender da sessão durante a serialização JSON.

## Por que início inclusivo e fim exclusivo?

O usuário informa dias, mas a coluna guarda data e hora. Para 10/01 a 11/01:

```text
2030-01-10 00:00:00 <= dataAtendimento < 2030-01-12 00:00:00
```

Usar `<= 11/01 00:00` excluiria quase todo o dia final. Fixar o fim em 23:59:59
excluiria frações de segundo. O próximo início de dia resolve ambos os problemas.
Os testes incluem o início exato, 23:59:59.999999 e o início exato do dia seguinte.
A consulta compara diretamente a coluna: não aplica função DATE nela.

A aplicação existente usa LocalDateTime e timestamp sem fuso: são horários civis
locais do projeto. Não converto dados antigos para UTC nesta evolução. Se o produto
passar a atender múltiplos fusos, precisará de uma decisão de modelagem e migração.

## Por que uma agregação por unidade e status?

O banco retorna `unidadeId`, `unidadeNome`, `status`, `count(id)`. Não precisa carregar
pacientes, descrições ou profissionais. A memória do service cresce com o número de
combinações unidade/status, em vez de com todos os atendimentos.

Os totais globais são somados a partir desse único resultado. Assim o total geral,
a soma dos status e a soma das unidades representam a mesma consulta, mesmo quando
há alterações concorrentes. A resposta sempre contém os quatro status; uma ausência
significa zero, não um campo ausente. Só aparecem unidades com movimento no período.

Unidades com nomes iguais continuam distintas pelo id. É usada a unidade registrada
no **atendimento**, mesmo se o profissional estiver cadastrado em outra unidade.
Isso respeita o dado histórico e o significado do evento; não corrige cadastros
indiscriminadamente. O relatório conta todos os status, inclusive cancelados e
agendados. Para contar somente realizados, consulte `porStatus.CONCLUIDO`.
O status apresentado é o atual: o sistema não tem tabela de histórico de transições.

## Por que não há migration ou índice novo?

Não foi necessário alterar tabela, coluna, constraint ou dados. Por isso não há
migration nesta entrega. O perfil dev passou a usar validate e a senha vem do
ambiente. Os dados existentes, inclusive descrições nulas, são preservados.
Novos agendamentos continuam sujeitos à validação do DTO.

Com dez atendimentos, não há evidência para justificar um índice novo na entrega.
Em produção, mediria com EXPLAIN ANALYZE e avaliaria índices como
`(data_atendimento, id)`, `(status, data_atendimento, id)` e
`(paciente_id, data_atendimento, id)`, considerando também custo de escrita e
seletividade. Qualquer alteração futura de esquema deve ter migration revisada.

## O que os testes comprovam — e seus limites

Mockito cobre regras isoladas e rejeição do período antes da consulta.
DataJpaTest cobre consultas derivadas. SpringBootTest com MockMvc e H2 integra
controller, binding de parâmetros, serviço, repository, DTO e tratamento de erros.
Os dados dos testes são criados no H2 e revertidos por transação.

O script de demonstração complementa o H2 com o PostgreSQL real, usando somente GETs.
A verificação de impressão digital lê as cinco tabelas antes/depois, sem divulgar
os cadastros. Não testamos POST/PATCH/DELETE no banco histórico nesta opção B.
O projeto ainda não implementa autenticação, exportação ou histórico de status;
essas funcionalidades não fazem parte da opção B. A CI foi adicionada para repetir
`verify` com Java 21; o resultado remoto deve ser observado no PR.

## Referências técnicas

- [Spring Data JPA: query methods, Query e EntityGraph](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)
- [Spring Data JPA: Pageable e Page](https://docs.spring.io/spring-data/jpa/docs/3.1.10/reference/html/)
- [PostgreSQL 16: tipos de data e hora](https://www.postgresql.org/docs/16/datatype-datetime.html)

As referências sustentam as escolhas técnicas; não são uma entrega de análise de
um projeto externo da fase 7. Esta entrega concentra-se na opção B da fase 8.
