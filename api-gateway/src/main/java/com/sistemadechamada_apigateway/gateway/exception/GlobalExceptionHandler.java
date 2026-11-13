package com.sistemadechamada_apigateway.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/** Respostas de erro padronizadas para os endpoints locais do Gateway (/api/auth/**). */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> tratarApiException(ApiException ex) {
        return corpo(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(WebExchangeBindException ex) {
        String detalhes = ex.getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return corpo(HttpStatus.BAD_REQUEST, detalhes);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<Map<String, Object>> tratarCorpoInvalido(ServerWebInputException ex) {
        return corpo(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> tratarStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return corpo(status, ex.getReason() != null ? ex.getReason() : status.getReasonPhrase());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> tratarInesperado(Exception ex) {
        log.error("Erro inesperado no Gateway", ex);
        return corpo(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no Gateway: " + ex.getClass().getSimpleName());
    }

    private ResponseEntity<Map<String, Object>> corpo(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "erro", status.getReasonPhrase(),
                "mensagem", mensagem == null ? "" : mensagem
        ));
    }
}
