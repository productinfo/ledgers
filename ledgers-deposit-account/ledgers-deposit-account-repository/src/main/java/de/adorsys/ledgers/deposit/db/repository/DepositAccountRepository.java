package de.adorsys.ledgers.deposit.db.repository;

import de.adorsys.ledgers.deposit.db.domain.DepositAccount;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface DepositAccountRepository extends PagingAndSortingRepository<DepositAccount, String> {
//	Optional<DepositAccount> findByIban(String iban);

	List<DepositAccount> findByIbanIn(List<String> ibans);
	
	List<DepositAccount> findByIbanStartingWith(String iban);  //TODO fix this!

	List<DepositAccount> findByBranch (String branch);
	
}
