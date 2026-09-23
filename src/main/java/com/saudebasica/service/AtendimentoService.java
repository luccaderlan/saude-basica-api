package com.saudebasica.service;

import com.saudebasica.domain.Atendimento;
import com.saudebasica.domain.AtendimentoStatus;
import com.saudebasica.domain.Paciente;
import com.saudebasica.domain.Profissional;
import com.saudebasica.domain.UnidadeSaude;
import com.saudebasica.dto.AtendimentoRequestDTO;
import com.saudebasica.dto.AtendimentoResponseDTO;
import com.saudebasica.exception.RecursoNaoEncontradoException;
import com.saudebasica.exception.RegraDeNegocioException;
import com.saudebasica.repository.AtendimentoRepository;
import com.saudebasica.repository.PacienteRepository;
import com.saudebasica.repository.ProfissionalRepository;
import com.saudebasica.repository.UnidadeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AtendimentoService {

    private static final Logger log = LoggerFactory.getLogger(AtendimentoService.class);

    private final AtendimentoRepository atendimentoRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfissionalRepository profissionalRepository;
    private final UnidadeRepository unidadeRepository;
    private final Clock clock;

    public AtendimentoService(AtendimentoRepository atendimentoRepository,
                              PacienteRepository pacienteRepository,
                              ProfissionalRepository profissionalRepository,
                              UnidadeRepository unidadeRepository,
                              Clock clock) {
        this.atendimentoRepository = atendimentoRepository;
        this.pacienteRepository = pacienteRepository;
        this.profissionalRepository = profissionalRepository;
        this.unidadeRepository = unidadeRepository;
        this.clock = clock;
    }

    @Transactional
    public AtendimentoResponseDTO agendar(AtendimentoRequestDTO dados) {
        validarDataNoFuturo(dados.dataAtendimento());
        validarHorarioLivre(dados.profissionalId(), dados.dataAtendimento());

        Paciente paciente = pacienteRepository.findById(dados.pacienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Paciente nao encontrado com id " + dados.pacienteId()));

        Profissional profissional = profissionalRepository.findById(dados.profissionalId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Profissional nao encontrado com id " + dados.profissionalId()));

        UnidadeSaude unidade = unidadeRepository.findById(dados.unidadeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Unidade de saude nao encontrada com id " + dados.unidadeId()));

        Atendimento atendimento = new Atendimento(
                dados.descricao(),
                dados.dataAtendimento(),
                paciente,
                profissional,
                unidade);

        atendimentoRepository.save(atendimento);
        log.info("Atendimento {} agendado para o profissional {} em {}",
                atendimento.getId(), profissional.getId(), atendimento.getDataAtendimento());

        return AtendimentoResponseDTO.fromEntity(atendimento);
    }

    @Transactional(readOnly = true)
    public List<AtendimentoResponseDTO> listarTodos() {
        return atendimentoRepository.findAll()
                .stream()
                .map(AtendimentoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AtendimentoResponseDTO> listarPorStatus(AtendimentoStatus status) {
        return atendimentoRepository.findByStatus(status)
                .stream()
                .map(AtendimentoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AtendimentoResponseDTO buscarPorId(Long id) {
        return AtendimentoResponseDTO.fromEntity(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<AtendimentoResponseDTO> listarPorPaciente(Long pacienteId) {
        // 404 em vez de lista vazia: distingue "paciente sem historico" de "paciente inexistente"
        if (!pacienteRepository.existsById(pacienteId)) {
            throw new RecursoNaoEncontradoException("Paciente nao encontrado com id " + pacienteId);
        }
        return atendimentoRepository.findByPacienteId(pacienteId)
                .stream()
                .map(AtendimentoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public AtendimentoResponseDTO atualizarStatus(Long id, AtendimentoStatus novoStatus) {
        Atendimento atendimento = buscarEntidade(id);
        validarMudancaDeStatus(atendimento.getStatus());

        AtendimentoStatus statusAnterior = atendimento.getStatus();
        atendimento.setStatus(novoStatus);
        atendimentoRepository.save(atendimento);
        log.info("Atendimento {} mudou de {} para {}", id, statusAnterior, novoStatus);

        return AtendimentoResponseDTO.fromEntity(atendimento);
    }

    @Transactional
    public void cancelar(Long id) {
        Atendimento atendimento = buscarEntidade(id);
        validarMudancaDeStatus(atendimento.getStatus());

        atendimento.setStatus(AtendimentoStatus.CANCELADO);
        atendimentoRepository.save(atendimento);
        log.info("Atendimento {} cancelado", id);
    }

    private Atendimento buscarEntidade(Long id) {
        return atendimentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Atendimento nao encontrado com id " + id));
    }

    private void validarDataNoFuturo(LocalDateTime dataAtendimento) {
        if (dataAtendimento.isBefore(LocalDateTime.now(clock))) {
            throw new RegraDeNegocioException("Nao e possivel agendar atendimento com data no passado.");
        }
    }

    private void validarHorarioLivre(Long profissionalId, LocalDateTime dataAtendimento) {
        boolean horarioOcupado = atendimentoRepository.existsByProfissionalIdAndDataAtendimentoAndStatus(
                profissionalId, dataAtendimento, AtendimentoStatus.AGENDADO);

        if (horarioOcupado) {
            throw new RegraDeNegocioException("Este profissional ja tem um atendimento agendado nesse horario.");
        }
    }

    private void validarMudancaDeStatus(AtendimentoStatus statusAtual) {
        if (statusAtual == AtendimentoStatus.CANCELADO) {
            throw new RegraDeNegocioException("Um atendimento CANCELADO nao pode ter o status alterado.");
        }
        if (statusAtual == AtendimentoStatus.CONCLUIDO) {
            throw new RegraDeNegocioException("Um atendimento CONCLUIDO nao pode ser reaberto.");
        }
    }
}
