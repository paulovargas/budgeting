package com.dio.budgeting.infrastructure.http;

import org.springframework.http.HttpStatus;

public class ApiOperationException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final boolean transactionMayHaveBeenSaved;

    public ApiOperationException(HttpStatus status, String code, String message, boolean transactionMayHaveBeenSaved) {
        super(message);
        this.status = status;
        this.code = code;
        this.transactionMayHaveBeenSaved = transactionMayHaveBeenSaved;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public boolean transactionMayHaveBeenSaved() { return transactionMayHaveBeenSaved; }
}
