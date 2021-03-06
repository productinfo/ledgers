package de.adorsys.ledgers.data.upload.model;

import de.adorsys.ledgers.middleware.api.domain.account.AccountDetailsTO;
import de.adorsys.ledgers.middleware.api.domain.um.UserTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadedData {
    private List<UserTO> users = new ArrayList<>();
    private Map<String, AccountDetailsTO> details = new HashMap<>(); // k-> IBAN, v->Details
    private Map<String, AccountBalance> balances = new HashMap<>();  // k-> IBAN, v->Balance

    public UploadedData() {
    }

    public UploadedData(List<UserTO> users, Map<String, AccountDetailsTO> details, Map<String, AccountBalance> balances) {
        this.users = users;
        this.details = details;
        this.balances = balances;
    }

    public List<UserTO> getUsers() {
        return users;
    }

    public void setUsers(List<UserTO> users) {
        this.users = users;
    }

    public Map<String, AccountDetailsTO> getDetails() {
        return details;
    }

    public void setDetails(Map<String, AccountDetailsTO> details) {
        this.details = details;
    }

    public Map<String, AccountBalance> getBalances() {
        return balances;
    }

    public void setBalances(Map<String, AccountBalance> balances) {
        this.balances = balances;
    }
}
