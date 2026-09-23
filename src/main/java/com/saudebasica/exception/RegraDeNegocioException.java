package com.saudebasica.exception;

/** Tratada como HTTP 400 pelo GlobalExceptionHandler. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
