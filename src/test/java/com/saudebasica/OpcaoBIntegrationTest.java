package com.saudebasica;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saudebasica.domain.*;
import com.saudebasica.dto.AtendimentoResponseDTO;
import com.saudebasica.repository.AtendimentoRepository;
import com.saudebasica.service.Paginacao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:opcao-b;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false",
        "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=OFF", "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("Paginação e relatórios da API")
class OpcaoBIntegrationTest {
    private static final String LISTA = "/api/atendimentos";
    private static final String RELATORIO = "/api/relatorios/atendimentos";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired AtendimentoRepository repository;
    private Long pacienteId;
    private Long pacienteSemHistoricoId;
    private Long unidade1Id;
    private Long unidade2Id;

    @BeforeEach
    void preparar() {
        var municipio = new Municipio("2800308", "Aracaju");
        em.persist(municipio);
        var u1 = new UnidadeSaude("Mesmo nome", "1234567", municipio);
        var u2 = new UnidadeSaude("Mesmo nome", "7654321", municipio);
        em.persist(u1);
        em.persist(u2);
        // Unidade sem atendimentos nao aparece no relatorio.
        em.persist(new UnidadeSaude("Sem movimento", "3333333", municipio));
        var profissional = new Profissional("Profissional teste", "700000000000001", "Clinica", u1);
        em.persist(profissional);
        var paciente = new Paciente("Paciente teste", "11111111111", LocalDate.of(1990, 1, 1), municipio);
        var outro = new Paciente("Sem historico", "22222222222", LocalDate.of(1991, 1, 1), municipio);
        em.persist(paciente);
        em.persist(outro);
        pacienteId = paciente.getId();
        pacienteSemHistoricoId = outro.getId();
        unidade1Id = u1.getId();
        unidade2Id = u2.getId();
        persistir("2030-01-09T23:59:59", AtendimentoStatus.CONCLUIDO, paciente, profissional, u1);
        persistir("2030-01-10T00:00:00", AtendimentoStatus.AGENDADO, paciente, profissional, u1);
        persistir("2030-01-10T12:00:00", AtendimentoStatus.CONCLUIDO, paciente, profissional, u1);
        persistir("2030-01-10T12:00:00", AtendimentoStatus.CONCLUIDO, paciente, profissional, u1);
        persistir("2030-01-10T23:59:59.999999", AtendimentoStatus.CANCELADO, paciente, profissional, u2);
        persistir("2030-01-11T00:00:00", AtendimentoStatus.EM_ATENDIMENTO, paciente, profissional, u2);
        persistir("2030-01-11T12:00:00", AtendimentoStatus.CONCLUIDO, paciente, profissional, u1);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("Divide as listagens em páginas com ordem fixa e informações da página")
    void devePaginarTodasAsListagensComOrdemEstavelEMetadados() throws Exception {
        for (String rota : List.of(LISTA, "/api/pacientes/" + pacienteId + "/atendimentos")) {
            List<Long> ids = new ArrayList<>();
            for (int pagina = 0; pagina < 4; pagina++) {
                var resultado = mvc.perform(get(rota).param("page", "" + pagina)
                                .param("size", "2").param("sort", "dataAtendimento,asc"))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(7))
                        .andExpect(jsonPath("$.totalPages").value(4))
                        .andExpect(jsonPath("$.number").value(pagina))
                        .andExpect(jsonPath("$.size").value(2))
                        .andExpect(jsonPath("$.first").value(pagina == 0))
                        .andExpect(jsonPath("$.last").value(pagina == 3))
                        .andExpect(jsonPath("$.sort[1]").value("id,asc")).andReturn();
                mapper.readTree(resultado.getResponse().getContentAsString()).get("content")
                        .forEach(item -> ids.add(item.get("id").asLong()));
            }
            assertThat(ids).hasSize(7).doesNotHaveDuplicates().isSorted();
        }
    }

    @Test
    @DisplayName("Filtra por status antes de paginar e conta apenas os atendimentos filtrados")
    void deveFiltrarAntesDePaginarEContarApenasOStatus() throws Exception {
        mvc.perform(get(LISTA).param("status", "CONCLUIDO").param("size", "2").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.content[1].status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.totalElements").value(4)).andExpect(jsonPath("$.totalPages").value(2));
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @DisplayName("Ordena na direção escolhida e desempata pelo ID")
    @ValueSource(strings = {"dataAtendimento,desc", "status,asc", "status,desc"})
    void deveRespeitarDirecaoEDesempatarPorId(String sort) throws Exception {
        List<JsonNode> itens = new ArrayList<>();
        for (int pagina = 0; pagina < 4; pagina++) {
            var resultado = mvc.perform(get(LISTA).param("sort", sort).param("size", "2")
                            .param("page", "" + pagina))
                    .andExpect(status().isOk()).andReturn();
            mapper.readTree(resultado.getResponse().getContentAsString()).get("content").forEach(itens::add);
        }
        java.util.Comparator<JsonNode> comparator;
        if (sort.startsWith("status")) {
            comparator = java.util.Comparator.comparing(item -> item.get("status").asText());
        } else {
            comparator = java.util.Comparator.comparing(item ->
                    LocalDateTime.parse(item.get("dataAtendimento").asText()));
        }
        if (sort.endsWith("desc")) { comparator = comparator.reversed(); }
        comparator = comparator.thenComparingLong(item -> item.get("id").asLong());
        assertThat(itens).hasSize(7).isSortedAccordingTo(comparator);
        assertThat(itens.stream().map(item -> item.get("id").asLong()).toList()).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Retorna uma página vazia além do limite e mantém o total de atendimentos")
    void deveRetornarPaginaVaziaForaDoLimitePreservandoTotal() throws Exception {
        mvc.perform(get(LISTA).param("size", "2").param("page", "99"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(7)).andExpect(jsonPath("$.number").value(99));
    }

    @Test
    @DisplayName("Retorna histórico vazio para paciente sem atendimentos e 404 para paciente inexistente")
    void deveDistinguirPacienteSemHistoricoDePacienteInexistente() throws Exception {
        mvc.perform(get("/api/pacientes/" + pacienteSemHistoricoId + "/atendimentos"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0)).andExpect(jsonPath("$.totalPages").value(0));
        mvc.perform(get("/api/pacientes/999999999/atendimentos"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Usa os valores padrão da paginação e permite ordenar pelo ID")
    void deveAplicarPadroesDaPaginacaoEPermitirOrdenacaoPorId() throws Exception {
        mvc.perform(get(LISTA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0)).andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.sort[0]").value("dataAtendimento,desc"));
        mvc.perform(get(LISTA).param("size", "100").param("sort", "id,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sort.length()").value(1))
                .andExpect(jsonPath("$.sort[0]").value("id,desc"));
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @DisplayName("Retorna 400 para parâmetros inválidos na listagem")
    @ValueSource(strings = {"page=-1", "page=abc", "size=0", "size=-1", "size=101", "size=abc",
            "sort=nome,asc", "sort=id,xpto", "sort=id", "sort=id,asc,status", "status=XPTO"})
    void deveRejeitarParametrosInvalidosNaListagem(String parametro) throws Exception {
        String[] partes = parametro.split("=", 2);
        mvc.perform(get(LISTA).param(partes[0], partes[1])).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.mensagem").isString());
    }

    @Test
    @DisplayName("Valida a paginação no histórico do paciente e na listagem filtrada")
    void deveValidarPaginacaoTambemNoHistoricoENoFiltro() throws Exception {
        mvc.perform(get("/api/pacientes/" + pacienteId + "/atendimentos").param("size", "101"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get(LISTA).param("status", "CONCLUIDO").param("page", "-1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Inclui o dia inteiro no relatório, exclui o dia seguinte e mostra zero nos status sem atendimentos")
    void relatorioDeveIncluirDiaInteiroExcluirDiaSeguinteEPreencherZeros() throws Exception {
        mvc.perform(get(RELATORIO).param("dataInicio", "2030-01-10").param("dataFim", "2030-01-10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.porStatus.AGENDADO").value(1))
                .andExpect(jsonPath("$.porStatus.CONCLUIDO").value(2))
                .andExpect(jsonPath("$.porStatus.CANCELADO").value(1))
                .andExpect(jsonPath("$.porStatus.EM_ATENDIMENTO").value(0));
    }

    @Test
    @DisplayName("Separa unidades com o mesmo nome pelo ID e usa a unidade do atendimento")
    void relatorioDeveSepararUnidadesPorIdEUsarUnidadeDoAtendimento() throws Exception {
        mvc.perform(get(RELATORIO).param("dataInicio", "2030-01-10").param("dataFim", "2030-01-11"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.porStatus.CONCLUIDO").value(3))
                .andExpect(jsonPath("$.porStatus.EM_ATENDIMENTO").value(1))
                .andExpect(jsonPath("$.porUnidade.length()").value(2))
                .andExpect(jsonPath("$.porUnidade[0].unidadeId").value(unidade1Id))
                .andExpect(jsonPath("$.porUnidade[0].total").value(4))
                .andExpect(jsonPath("$.porUnidade[0].porStatus.CANCELADO").value(0))
                .andExpect(jsonPath("$.porUnidade[1].unidadeId").value(unidade2Id))
                .andExpect(jsonPath("$.porUnidade[1].total").value(2))
                .andExpect(jsonPath("$.porUnidade[1].porStatus.CANCELADO").value(1));
    }

    @Test
    @DisplayName("Retorna 200 e os quatro status com zero quando o relatório está vazio")
    void relatorioVazioDeveRetornar200EQuatroStatusComZero() throws Exception {
        mvc.perform(get(RELATORIO).param("dataInicio", "2040-01-01").param("dataFim", "2040-01-31"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.porUnidade").isEmpty())
                .andExpect(jsonPath("$.porStatus.AGENDADO").value(0))
                .andExpect(jsonPath("$.porStatus.CONCLUIDO").value(0))
                .andExpect(jsonPath("$.porStatus.CANCELADO").value(0))
                .andExpect(jsonPath("$.porStatus.EM_ATENDIMENTO").value(0));
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @DisplayName("Retorna 400 para data inicial inválida ou maior que a final")
    @ValueSource(strings = {"2030-02-30", "10-01-2030", "xpto", "2030-01-12", "+999999999-12-31"})
    void relatorioDeveRejeitarDataInvalidaOuPeriodoInvertido(String dataInicio) throws Exception {
        mvc.perform(get(RELATORIO).param("dataInicio", dataInicio).param("dataFim", "2030-01-11"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @DisplayName("Exige as datas inicial e final para gerar o relatório")
    @ValueSource(strings = {"dataInicio", "dataFim"})
    void relatorioDeveExigirAmbasAsDatas(String parametro) throws Exception {
        mvc.perform(get(RELATORIO).param(parametro, "2030-01-10"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Parametro obrigatorio"));
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @DisplayName("Retorna 400 para data final inválida ou fora do limite aceito")
    @ValueSource(strings = {"2030-02-30", "+999999999-12-31", "0000-01-01"})
    void relatorioDeveRejeitarDataFimInvalidaOuForaDaFaixaSuportada(String dataFim) throws Exception {
        mvc.perform(get(RELATORIO).param("dataInicio", "2030-01-01").param("dataFim", dataFim))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Carrega apenas a página e os dados relacionados usando duas consultas ao banco")
    void deveCarregarSomentePaginaERelacionamentosSemConsultasPorRegistro() {
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var page = repository.findAll(Paginacao.criar(0, 2, "id,asc"));
        em.clear();
        assertThat(page.getContent().stream().map(AtendimentoResponseDTO::fromEntity).toList()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(7);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2); // conteudo com joins + count
    }

    @Test
    @DisplayName("Mostra a rota do relatório e os parâmetros de paginação na documentação da API")
    void devePublicarNovoContratoNoOpenApi() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        JsonNode docs = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(docs.path("paths").has(RELATORIO)).isTrue();
        assertThat(docs.path("paths").path(LISTA).path("get").path("parameters").toString())
                .contains("page", "size", "sort");
    }

    private void persistir(String data, AtendimentoStatus status, Paciente paciente,
                          Profissional profissional, UnidadeSaude unidade) {
        var a = new Atendimento("Teste de limite", LocalDateTime.parse(data), paciente, profissional, unidade);
        a.setStatus(status);
        em.persist(a);
    }
}
