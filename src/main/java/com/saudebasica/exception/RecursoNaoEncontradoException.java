package com.saudebasica.exception;

/** Tratada como HTTP 404 pelo GlobalExceptionHandler. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
