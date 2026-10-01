package com.dio.budgeting.infrastructure.http.response;

import com.dio.budgeting.application.output.TransactionOutput;

public record TransactionResponse(String id, String category, String description, double value) {
    public static TransactionResponse from(TransactionOutput output){
        return new TransactionResponse(
                output.id(),
                output.category(),
                output.description(),
                output.value()
        );
    }
}
