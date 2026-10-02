package com.saudebasica.repository;

import com.saudebasica.domain.AtendimentoStatus;

/** Projecao: o banco devolve contagens, sem carregar os atendimentos. */
public interface TotalAtendimentosPorUnidadeStatus {
    Long getUnidadeId();
    String getUnidadeNome();
    AtendimentoStatus getStatus();
    long getTotal();
}
