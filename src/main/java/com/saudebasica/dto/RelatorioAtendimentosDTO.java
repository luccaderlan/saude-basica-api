package com.saudebasica.dto;

import com.saudebasica.domain.AtendimentoStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record RelatorioAtendimentosDTO(LocalDate dataInicio, LocalDate dataFim, long total,
                                       Map<AtendimentoStatus, Long> porStatus,
                                       List<TotalUnidadeDTO> porUnidade) {
    public record TotalUnidadeDTO(Long unidadeId, String unidadeNome, long total,
                                  Map<AtendimentoStatus, Long> porStatus) { }
}
