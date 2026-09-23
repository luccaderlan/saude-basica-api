package com.saudebasica.dto;

import com.saudebasica.domain.AtendimentoStatus;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusDTO(

        @NotNull(message = "status e obrigatorio")
        AtendimentoStatus status
) {
}
