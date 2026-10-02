package com.dio.budgeting.infrastructure.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ApiError(int status, String code, String message, boolean transactionMayHaveBeenSaved) {}

    @ExceptionHandler(ApiOperationException.class)
    public ResponseEntity<ApiError> operation(ApiOperationException error) {
        return response(error.status(), error.code(), error.getMessage(), error.transactionMayHaveBeenSaved());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> validation(IllegalArgumentException error) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_INPUT", error.getMessage(), false);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> malformed(Exception error) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Verifique o corpo, os parâmetros e a categoria (GROCERIES, PHARMA ou AUTO).", false);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> tooLarge(MaxUploadSizeExceededException error) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "AUDIO_TOO_LARGE", "O áudio deve ter no máximo 10 MB.", false);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> media(HttpMediaTypeNotSupportedException error) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Content-Type não suportado.", false);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> notAcceptable(HttpMediaTypeNotAcceptableException error) {
        return response(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE",
                "O cabeçalho Accept não permite o formato de resposta deste endpoint.", false);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception error) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Não foi possível concluir a operação. Consulte as transações antes de reenviar uma criação.", true);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message, boolean mayBeSaved) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(new ApiError(status.value(), code, message, mayBeSaved));
    }
}
