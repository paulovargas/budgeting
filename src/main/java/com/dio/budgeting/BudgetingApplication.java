package com.dio.budgeting;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BudgetingApplication {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder){
        return builder.build();
    }

	public static void main(String[] args) {

        SpringApplication.run(BudgetingApplication.class, args);
	}

}
