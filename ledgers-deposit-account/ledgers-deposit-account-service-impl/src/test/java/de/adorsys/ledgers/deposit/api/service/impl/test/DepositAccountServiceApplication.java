package de.adorsys.ledgers.deposit.api.service.impl.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import de.adorsys.ledgers.deposit.api.service.EnableDepositAccountService;
import de.adorsys.ledgers.postings.impl.EnablePostingService;

@SpringBootApplication
@EnableDepositAccountService
@EnablePostingService
@EnableJpaAuditing
public class DepositAccountServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(DepositAccountServiceApplication.class, args);
	}
}
