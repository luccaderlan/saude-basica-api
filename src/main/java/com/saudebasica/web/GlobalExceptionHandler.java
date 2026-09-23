package com.saudebasica.web;

import com.saudebasica.exception.RecursoNaoEncontradoException;
import com.saudebasica.exception.RegraDeNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        ErroResposta corpo = ErroResposta.de(404, "Nao encontrado", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(corpo);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResposta> tratarRegraDeNegocio(RegraDeNegocioException ex) {
        ErroResposta corpo = ErroResposta.de(400, "Regra de negocio", ex.getMessage());
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(MethodArgumentNotValidException ex) {
        String mensagens = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErroResposta corpo = ErroResposta.de(400, "Dados invalidos", mensagens);
        return ResponseEntity.badRequest().body(corpo);
    }

    /** Ex: ?status=XPTO, valor que nao existe no enum. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> tratarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        ErroResposta corpo = ErroResposta.de(400, "Parametro invalido",
                "Valor invalido para o parametro '" + ex.getName() + "'.");
        return ResponseEntity.badRequest().body(corpo);
    }
}
