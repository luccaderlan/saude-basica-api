package com.saudebasica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AtendimentoRequestDTO(

        @NotNull(message = "pacienteId e obrigatorio")
        Long pacienteId,

        @NotNull(message = "profissionalId e obrigatorio")
        Long profissionalId,

        @NotNull(message = "unidadeId e obrigatorio")
        Long unidadeId,

        @NotBlank(message = "descricao nao pode ser vazia")
        String descricao,

        @NotNull(message = "dataAtendimento e obrigatoria")
        LocalDateTime dataAtendimento
) {
}
