package com.dio.budgeting.application.input;

import com.dio.budgeting.domain.Category;

public record PersistTransactionInput(String description, long amount, Category category) {

}
