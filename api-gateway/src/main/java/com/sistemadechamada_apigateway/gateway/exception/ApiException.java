package com.sistemadechamada_apigateway.gateway.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Erro de negócio com status HTTP explícito (evita o 500 genérico). */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }
}
