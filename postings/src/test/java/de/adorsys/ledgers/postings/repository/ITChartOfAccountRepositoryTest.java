package de.adorsys.ledgers.postings.repository;

import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;

import de.adorsys.ledgers.postings.PostingsApplication;
import de.adorsys.ledgers.postings.domain.ChartOfAccount;
import de.adorsys.ledgers.postings.utils.Ids;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes=PostingsApplication.class)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class})
@DatabaseSetup("ITChartOfAccountRepositoryTest-db-entries.xml")
public class ITChartOfAccountRepositoryTest {
	@Autowired
	private ChartOfAccountRepository chartOfAccountRepository;

	@Test
	public void test_create_coa_ok() {
		LocalDateTime now = LocalDateTime.now();
		ChartOfAccount coa = ChartOfAccount.builder().id(Ids.id()).name("CoA").user("Francis").validFrom(now).build();
		chartOfAccountRepository.save(coa);
	}

	@Test
	public void test_load_coa() {
		ChartOfAccount coa = chartOfAccountRepository.findById("ci8k8PDcTrCsi-F3sT3i-g").orElse(null);
		Assert.assertNotNull(coa);
	}

	@Test(expected=DataIntegrityViolationException.class)
	public void test_create_coa_unique_constrain_violation_name_validFrom() {
		ChartOfAccount coa = chartOfAccountRepository.findById("ci8k8PDcTrCsi-F3sT3i-g").orElse(null);
		Assume.assumeNotNull(coa);
		ChartOfAccount coa2 = ChartOfAccount.builder().id(Ids.id()).name(coa.getName()).user("Francis").validFrom(coa.getValidFrom()).build();
		chartOfAccountRepository.save(coa2);
	}
}