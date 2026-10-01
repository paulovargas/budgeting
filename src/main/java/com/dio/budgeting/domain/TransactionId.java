package com.dio.budgeting.domain;

import java.util.UUID;

public record TransactionId() {
    public TransactionId() {
        this(UUID.randomUUID());
    }
}
