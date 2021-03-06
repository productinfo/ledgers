package de.adorsys.ledgers.mockbank.simple.service;

import java.util.ArrayList;
import java.util.List;

import de.adorsys.ledgers.middleware.api.domain.account.AccountDetailsTO;
import de.adorsys.ledgers.middleware.api.domain.um.BearerTokenTO;
import de.adorsys.ledgers.middleware.api.domain.um.UserRoleTO;
import de.adorsys.ledgers.middleware.api.domain.um.UserTO;

public class UserContext {
	private UserTO user;
	private BearerTokenTO accessToken;
	private UserRoleTO role;
	private List<AccountDetailsTO> accessibleAccounts = new ArrayList<>();

	public UserContext(UserTO user, BearerTokenTO accessToken, UserRoleTO role) {
		this.user = user;
		this.accessToken = accessToken;
		this.role = role;
	}

	public UserContext(UserTO user) {
		this.user = user;
	}

	public UserContext(UserTO user, BearerTokenTO accessToken) {
		this.user = user;
		this.accessToken = accessToken;
		this.role = accessToken.getAccessTokenObject().getRole();
	}
	
	public BearerTokenTO getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(BearerTokenTO accessToken) {
		this.accessToken = accessToken;
	}

	public UserRoleTO getRole() {
		return role;
	}

	public void setRole(UserRoleTO role) {
		this.role = role;
	}

	public UserTO getUser() {
		return user;
	}

	public void setUser(UserTO user) {
		this.user = user;
	}

	public List<AccountDetailsTO> getAccessibleAccounts() {
		return accessibleAccounts;
	}

	public void setAccessibleAccounts(List<AccountDetailsTO> accessibleAccounts) {
		this.accessibleAccounts = accessibleAccounts;
	}
}
