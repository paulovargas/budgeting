package com.dio.budgeting.infrastructure.http.request;

import com.dio.budgeting.application.PersistTransactionUseCase;
import com.dio.budgeting.application.input.PersistTransactionInput;
import com.dio.budgeting.domain.Category;

public record TrasactionRequest(String description, Category category, long amount) {
    public PersistTransactionInput toInput() {
        return new PersistTransactionInput( description(), amount, category);
    }
}
