package com.saudebasica.web;

import java.time.LocalDateTime;

public record ErroResposta(
        LocalDateTime momento,
        int status,
        String erro,
        String mensagem
) {
    public static ErroResposta de(int status, String erro, String mensagem) {
        return new ErroResposta(LocalDateTime.now(), status, erro, mensagem);
    }
}
