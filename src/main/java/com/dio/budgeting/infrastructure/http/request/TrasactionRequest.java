package com.dio.budgeting.infrastructure.http.request;

import com.dio.budgeting.application.input.PersistTransactionInput;
import com.dio.budgeting.domain.Category;
import java.math.BigDecimal;

public record TrasactionRequest(String description, Category category, BigDecimal amount) {
    public PersistTransactionInput toInput() {
        if (amount == null) {
            throw new IllegalArgumentException("O valor em centavos é obrigatório.");
        }
        try {
            return new PersistTransactionInput(description, amount.longValueExact(), category);
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("O valor deve ser inteiro em centavos e estar dentro do limite permitido.");
        }
    }
}
