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
        var transaction = transactionRepository.save(
                new Transaction( input.description(), input.amount(), input.category()));

        return TransactionOutput.from(transaction);
    }
}
