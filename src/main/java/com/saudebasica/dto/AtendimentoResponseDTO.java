package com.saudebasica.dto;

import com.saudebasica.domain.Atendimento;
import com.saudebasica.domain.AtendimentoStatus;

import java.time.LocalDateTime;

/**
 * Nao expomos a entidade direto: os relacionamentos LAZY fariam o Jackson
 * disparar consultas extras (ou falhar) e a API ficaria presa ao modelo do banco.
 */
public record AtendimentoResponseDTO(
        Long id,
        String descricao,
        AtendimentoStatus status,
        LocalDateTime dataAtendimento,
        String nomePaciente,
        String nomeProfissional,
        String especialidade,
        String nomeUnidade
) {

    public static AtendimentoResponseDTO fromEntity(Atendimento atendimento) {
        return new AtendimentoResponseDTO(
                atendimento.getId(),
                atendimento.getDescricao(),
                atendimento.getStatus(),
                atendimento.getDataAtendimento(),
                atendimento.getPaciente().getNome(),
                atendimento.getProfissional().getNome(),
                atendimento.getProfissional().getEspecialidade(),
                atendimento.getUnidade().getNome()
        );
    }
}
