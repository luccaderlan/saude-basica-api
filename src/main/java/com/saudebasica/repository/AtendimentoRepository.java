package com.saudebasica.repository;

import com.saudebasica.domain.Atendimento;
import com.saudebasica.domain.AtendimentoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    List<Atendimento> findByStatus(AtendimentoStatus status);

    List<Atendimento> findByPacienteId(Long pacienteId);

    boolean existsByProfissionalIdAndDataAtendimentoAndStatus(Long profissionalId,
                                                              LocalDateTime dataAtendimento,
                                                              AtendimentoStatus status);
}
