package com.dio.budgeting.application;

import com.dio.budgeting.application.input.PersistTransactionInput;
import com.dio.budgeting.application.output.TransactionOutput;
import com.dio.budgeting.domain.Category;
import com.dio.budgeting.domain.Transaction;
import com.dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class PersistTransactionUseCase {
    private final TransactionRepository transactionRepository;

    public PersistTransactionUseCase(TransactionRepository transactionRepository){
        this.transactionRepository = transactionRepository;
    }

    @Tool(name = "persistTransaction", description = "Persiste uma nova transação financeira")
    public TransactionOutput execute(PersistTransactionInput input){
        if (input == null || input.description() == null || input.description().isBlank()) {
            throw new IllegalArgumentException("A descrição da transação é obrigatória.");
        }
        if (input.description().length() > 255) {
            throw new IllegalArgumentException("A descrição deve ter no máximo 255 caracteres.");
        }
        if (input.amount() <= 0) {
            throw new IllegalArgumentException("O valor deve ser positivo e informado em centavos.");
        }
        if (input.category() == null) {
            throw new IllegalArgumentException("A categoria é obrigatória.");
        }
        var transaction = transactionRepository.save(
                new Transaction( input.description(), input.amount(), input.category()));

        return TransactionOutput.from(transaction);
    }
}
