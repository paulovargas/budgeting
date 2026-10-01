package com.dio.budgeting.application;

import com.dio.budgeting.application.input.PersistTransactionInput;
import com.dio.budgeting.application.output.TransactionOutput;
import com.dio.budgeting.domain.Category;
import com.dio.budgeting.domain.Transaction;
import com.dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListTransactionsByCategoryUseCase {
    private final TransactionRepository transactionRepository;

    public ListTransactionsByCategoryUseCase(TransactionRepository transactionRepository){
        this.transactionRepository = transactionRepository;
    }

    public List<TransactionOutput> execute(Category category){
        return transactionRepository.findAllByCategory(category).stream().map(TransactionOutput::from).toList();
    }
}
