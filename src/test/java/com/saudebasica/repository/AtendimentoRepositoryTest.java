package com.saudebasica.repository;

import com.saudebasica.domain.Atendimento;
import com.saudebasica.domain.AtendimentoStatus;
import com.saudebasica.domain.Municipio;
import com.saudebasica.domain.Paciente;
import com.saudebasica.domain.Profissional;
import com.saudebasica.domain.UnidadeSaude;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AtendimentoRepositoryTest {

    private static final LocalDateTime HORARIO = LocalDateTime.of(2030, 1, 10, 9, 0);

    @Autowired
    private AtendimentoRepository atendimentoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Paciente paciente;
    private Profissional profissional;
    private UnidadeSaude unidade;

    @BeforeEach
    void setUp() {
        Municipio municipio = entityManager.persist(new Municipio("2800308", "Aracaju"));
        unidade = entityManager.persist(new UnidadeSaude("USF Centro", "1234567", municipio));
        profissional = entityManager.persist(
                new Profissional("Dra. Ana Lima", "700000000000001", "Clinica Geral", unidade));
        paciente = entityManager.persist(
                new Paciente("Joao da Silva", "11111111111", LocalDate.of(1990, 3, 12), municipio));
    }

    @Test
    void findByStatusDeveRetornarApenasAtendimentosDoStatusInformado() {
        persistirAtendimento("Consulta 1", HORARIO, AtendimentoStatus.AGENDADO);
        persistirAtendimento("Consulta 2", HORARIO.plusHours(1), AtendimentoStatus.AGENDADO);
        persistirAtendimento("Consulta 3", HORARIO.plusHours(2), AtendimentoStatus.CONCLUIDO);
        persistirAtendimento("Consulta 4", HORARIO.plusHours(3), AtendimentoStatus.CANCELADO);
        entityManager.flush();

        List<Atendimento> agendados = atendimentoRepository.findByStatus(AtendimentoStatus.AGENDADO);

        assertThat(agendados)
                .hasSize(2)
                .extracting(Atendimento::getStatus)
                .containsOnly(AtendimentoStatus.AGENDADO);
    }

    @Test
    void findByStatusDeveRetornarListaVaziaQuandoNenhumAtendimentoTiverOStatus() {
        persistirAtendimento("Consulta 1", HORARIO, AtendimentoStatus.AGENDADO);
        entityManager.flush();

        assertThat(atendimentoRepository.findByStatus(AtendimentoStatus.CONCLUIDO)).isEmpty();
    }

    @Test
    void deveDetectarHorarioOcupadoApenasParaAtendimentoAgendado() {
        persistirAtendimento("Consulta agendada", HORARIO, AtendimentoStatus.AGENDADO);
        persistirAtendimento("Consulta cancelada", HORARIO.plusHours(1), AtendimentoStatus.CANCELADO);
        entityManager.flush();

        Long profissionalId = profissional.getId();

        assertThat(atendimentoRepository.existsByProfissionalIdAndDataAtendimentoAndStatus(
                profissionalId, HORARIO, AtendimentoStatus.AGENDADO)).isTrue();
        assertThat(atendimentoRepository.existsByProfissionalIdAndDataAtendimentoAndStatus(
                profissionalId, HORARIO.plusHours(1), AtendimentoStatus.AGENDADO)).isFalse();
    }

    private void persistirAtendimento(String descricao, LocalDateTime data, AtendimentoStatus status) {
        Atendimento atendimento = new Atendimento(descricao, data, paciente, profissional, unidade);
        atendimento.setStatus(status);
        entityManager.persist(atendimento);
    }
}
