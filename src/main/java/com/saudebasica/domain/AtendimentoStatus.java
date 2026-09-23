package com.saudebasica.domain;

/**
 * Os estados possiveis de um atendimento.
 * Fluxo normal: AGENDADO -> EM_ATENDIMENTO -> CONCLUIDO.
 * A qualquer momento antes de concluir ele pode virar CANCELADO.
 */
public enum AtendimentoStatus {
    AGENDADO,
    EM_ATENDIMENTO,
    CONCLUIDO,
    CANCELADO
}
