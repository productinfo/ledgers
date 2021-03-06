package de.adorsys.ledgers.middleware.rest.mockbank;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import de.adorsys.ledgers.middleware.LedgersMiddlewareRestApplication;
import de.adorsys.ledgers.middleware.api.domain.um.BearerTokenTO;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = LedgersMiddlewareRestApplication.class, webEnvironment=SpringBootTest.WebEnvironment.MOCK)
@WebAppConfiguration
@ActiveProfiles("h2")
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class,
	DbUnitTestExecutionListener.class })
@DatabaseTearDown(value = { "MiddlewareServiceImplIT-db-delete.xml" }, type = DatabaseOperation.DELETE_ALL)
public class AppManagementResourceIT {
	ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private WebApplicationContext wac;
	private MockMvc mockMvc;
	private BearerTokenTO bearerToken;
	@Before
	public void before() throws Exception {
		this.mockMvc = MockMvcBuilders
				.webAppContextSetup(this.wac)
				.apply(springSecurity())
				.build();
		String payload = mapper.writeValueAsString(AdminPayload.adminPayload());
        MvcResult mvcResult = this.mockMvc.perform(
        		MockMvcRequestBuilders.post("/management/app/admin")
        			.contentType(MediaType.APPLICATION_JSON)
        			.content(payload))
        			.andExpect(MockMvcResultMatchers.status().isOk())
        			.andExpect(MockMvcResultMatchers.content().string(StringContains.containsString("."))).andReturn();
        String bearerTokenString = mvcResult.getResponse().getContentAsString();
        bearerToken = new ObjectMapper().readValue(bearerTokenString, BearerTokenTO.class);
	}

	@Test
    public void givenInitURI_whenMockMVC_thenReturnsVoid() throws Exception {

		this.mockMvc.perform(
        		MockMvcRequestBuilders.post("/management/app/init")
        			.header("Authorization", "Bearer " + bearerToken.getAccess_token()))
        			.andExpect(MockMvcResultMatchers.status().isOk());
	}
}
