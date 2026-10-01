package com.dio.budgeting.application;

import com.dio.budgeting.application.output.TransactionOutput;
import com.dio.budgeting.domain.Category;
import com.dio.budgeting.domain.Transaction;
import com.dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class PersistTransactionUseCase {
    private final TransactionRepository transactionRepository;

    public PersistTransactionUseCase(TransactionRepository transactionRepository){
        this.transactionRepository = transactionRepository;
    }

    public TransactionOutput execute(String description, long amout, Category category){
        var transaction = transactionRepository.save(new Transaction(description, amout, category));

        return TransactionOutput.from(transaction);
    }
}
