package com.saudebasica.service;

import com.saudebasica.domain.Atendimento;
import com.saudebasica.domain.AtendimentoStatus;
import com.saudebasica.domain.Municipio;
import com.saudebasica.domain.Paciente;
import com.saudebasica.domain.Profissional;
import com.saudebasica.domain.UnidadeSaude;
import com.saudebasica.dto.AtendimentoRequestDTO;
import com.saudebasica.dto.AtendimentoResponseDTO;
import com.saudebasica.exception.RegraDeNegocioException;
import com.saudebasica.repository.AtendimentoRepository;
import com.saudebasica.repository.PacienteRepository;
import com.saudebasica.repository.ProfissionalRepository;
import com.saudebasica.repository.UnidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtendimentoServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 23, 9, 0);
    private static final LocalDateTime AMANHA = AGORA.plusDays(1);
    private static final LocalDateTime ONTEM = AGORA.minusDays(1);

    private static final Long PACIENTE_ID = 1L;
    private static final Long PROFISSIONAL_ID = 2L;
    private static final Long UNIDADE_ID = 3L;
    private static final Long ATENDIMENTO_ID = 10L;

    @Mock
    private AtendimentoRepository atendimentoRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private ProfissionalRepository profissionalRepository;
    @Mock
    private UnidadeRepository unidadeRepository;

    private AtendimentoService service;

    private Paciente paciente;
    private Profissional profissional;
    private UnidadeSaude unidade;

    @BeforeEach
    void setUp() {
        Clock relogioFixo = Clock.fixed(AGORA.atZone(FUSO).toInstant(), FUSO);
        service = new AtendimentoService(atendimentoRepository, pacienteRepository,
                profissionalRepository, unidadeRepository, relogioFixo);

        Municipio municipio = new Municipio("2800308", "Aracaju");
        unidade = new UnidadeSaude("USF Centro", "1234567", municipio);
        unidade.setId(UNIDADE_ID);
        profissional = new Profissional("Dra. Ana Lima", "700000000000001", "Clinica Geral", unidade);
        profissional.setId(PROFISSIONAL_ID);
        paciente = new Paciente("Joao da Silva", "11111111111", LocalDate.of(1990, 3, 12), municipio);
        paciente.setId(PACIENTE_ID);
    }

    @Test
    void deveAgendarAtendimentoQuandoDataForFutura() {
        // Arrange
        AtendimentoRequestDTO pedido = pedidoPara(AMANHA);
        when(atendimentoRepository.existsByProfissionalIdAndDataAtendimentoAndStatus(
                PROFISSIONAL_ID, AMANHA, AtendimentoStatus.AGENDADO)).thenReturn(false);
        when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(PROFISSIONAL_ID)).thenReturn(Optional.of(profissional));
        when(unidadeRepository.findById(UNIDADE_ID)).thenReturn(Optional.of(unidade));

        // Act
        AtendimentoResponseDTO resposta = service.agendar(pedido);

        // Assert
        assertThat(resposta.status()).isEqualTo(AtendimentoStatus.AGENDADO);
        assertThat(resposta.dataAtendimento()).isEqualTo(AMANHA);
        assertThat(resposta.nomePaciente()).isEqualTo("Joao da Silva");
        assertThat(resposta.nomeProfissional()).isEqualTo("Dra. Ana Lima");
        verify(atendimentoRepository).save(any(Atendimento.class));
    }

    @Test
    void deveLancarExcecaoQuandoDataAtendimentoForNoPassado() {
        AtendimentoRequestDTO pedido = pedidoPara(ONTEM);

        assertThatThrownBy(() -> service.agendar(pedido))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("data no passado");

        verify(atendimentoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoProfissionalTiverConflitoDeHorario() {
        AtendimentoRequestDTO pedido = pedidoPara(AMANHA);
        when(atendimentoRepository.existsByProfissionalIdAndDataAtendimentoAndStatus(
                PROFISSIONAL_ID, AMANHA, AtendimentoStatus.AGENDADO)).thenReturn(true);

        assertThatThrownBy(() -> service.agendar(pedido))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ja tem um atendimento agendado");

        verify(atendimentoRepository, never()).save(any());
    }

    @Test
    void naoDevePermitirMudarStatusDeAtendimentoCancelado() {
        Atendimento cancelado = atendimentoComStatus(AtendimentoStatus.CANCELADO);
        when(atendimentoRepository.findById(ATENDIMENTO_ID)).thenReturn(Optional.of(cancelado));

        assertThatThrownBy(() -> service.atualizarStatus(ATENDIMENTO_ID, AtendimentoStatus.AGENDADO))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CANCELADO");

        assertThat(cancelado.getStatus()).isEqualTo(AtendimentoStatus.CANCELADO);
        verify(atendimentoRepository, never()).save(any());
    }

    @Test
    void naoDevePermitirReabrirAtendimentoConcluido() {
        Atendimento concluido = atendimentoComStatus(AtendimentoStatus.CONCLUIDO);
        when(atendimentoRepository.findById(ATENDIMENTO_ID)).thenReturn(Optional.of(concluido));

        assertThatThrownBy(() -> service.atualizarStatus(ATENDIMENTO_ID, AtendimentoStatus.EM_ATENDIMENTO))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CONCLUIDO");

        verify(atendimentoRepository, never()).save(any());
    }

    @Test
    void deveMudarStatusQuandoAtendimentoEstiverAgendado() {
        Atendimento agendado = atendimentoComStatus(AtendimentoStatus.AGENDADO);
        when(atendimentoRepository.findById(ATENDIMENTO_ID)).thenReturn(Optional.of(agendado));

        AtendimentoResponseDTO resposta = service.atualizarStatus(ATENDIMENTO_ID, AtendimentoStatus.EM_ATENDIMENTO);

        assertThat(resposta.status()).isEqualTo(AtendimentoStatus.EM_ATENDIMENTO);
        verify(atendimentoRepository).save(agendado);
    }

    private AtendimentoRequestDTO pedidoPara(LocalDateTime data) {
        return new AtendimentoRequestDTO(PACIENTE_ID, PROFISSIONAL_ID, UNIDADE_ID, "Consulta de rotina", data);
    }

    private Atendimento atendimentoComStatus(AtendimentoStatus status) {
        Atendimento atendimento = new Atendimento("Consulta de rotina", AMANHA, paciente, profissional, unidade);
        atendimento.setId(ATENDIMENTO_ID);
        atendimento.setStatus(status);
        return atendimento;
    }
}
