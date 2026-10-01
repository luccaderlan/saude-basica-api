package com.saudebasica.repository;

import com.saudebasica.domain.Atendimento;
import com.saudebasica.domain.AtendimentoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    List<Atendimento> findByStatus(AtendimentoStatus status);

    @Override
    @EntityGraph(attributePaths = {"paciente", "profissional", "unidade"})
    Page<Atendimento> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"paciente", "profissional", "unidade"})
    Page<Atendimento> findByStatus(AtendimentoStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"paciente", "profissional", "unidade"})
    Page<Atendimento> findByPacienteId(Long pacienteId, Pageable pageable);

    @Query("""
            select a.unidade.id as unidadeId, a.unidade.nome as unidadeNome,
                   a.status as status, count(a.id) as total
            from Atendimento a
            where a.dataAtendimento >= :inicio and a.dataAtendimento < :fimExclusivo
            group by a.unidade.id, a.unidade.nome, a.status
            order by a.unidade.id, a.status
            """)
    List<TotalAtendimentosPorUnidadeStatus> contarPorUnidadeEStatus(
            @Param("inicio") LocalDateTime inicio, @Param("fimExclusivo") LocalDateTime fimExclusivo);

    boolean existsByProfissionalIdAndDataAtendimentoAndStatus(Long profissionalId,
                                                              LocalDateTime dataAtendimento,
                                                              AtendimentoStatus status);
}
