package de.adorsys.ledgers.postings.converter;

import de.adorsys.ledgers.postings.domain.Ledger;
import de.adorsys.ledgers.postings.domain.LedgerAccount;
import de.adorsys.ledgers.postings.domain.LedgerAccountBO;
import de.adorsys.ledgers.postings.domain.LedgerBO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LedgerAccountMapper {
    LedgerAccountBO toLedgerAccountBO(LedgerAccount ledgerAccount);
    LedgerAccount toLedgerAccount(LedgerAccountBO ledgerAccount);
}