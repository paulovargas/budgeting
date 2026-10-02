package com.dio.budgeting;

import com.dio.budgeting.application.output.TransactionOutput;
import com.dio.budgeting.domain.Category;
import com.dio.budgeting.domain.Transaction;
import com.dio.budgeting.domain.TransactionId;
import com.dio.budgeting.infrastructure.http.response.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionOutputTest {
    @ParameterizedTest
    @CsvSource({"8000, 80.00", "12533, 125.33", "235, 2.35", "1, 0.01", "0, 0.00",
            "9223372036854775807, 92233720368547758.07"})
    void convertsCentsWithoutLosingPrecision(long cents, String expected) {
        var transaction = new Transaction("Compra", cents, Category.GROCERIES);
        var output = TransactionOutput.from(transaction);
        var response = TransactionResponse.from(output);

        assertThat(response.value()).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void returnsOnlyTheUuidAndPreservesTransactionDetails() {
        var uuid = UUID.fromString("a5ac3e36-67a8-4061-ab68-548504bd39a7");
        var transaction = new Transaction(new TransactionId(uuid), "Mercado", 8000, Category.GROCERIES);
        var response = TransactionResponse.from(TransactionOutput.from(transaction));

        assertThat(response.id()).isEqualTo(uuid.toString());
        assertThat(response.description()).isEqualTo("Mercado");
        assertThat(response.category()).isEqualTo("GROCERIES");
    }
}
