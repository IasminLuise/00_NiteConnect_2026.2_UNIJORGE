package br.edu.unijorge.niteconnect.shared.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Serializes low-volume administrative changes, including cross-module invariants. */
@Component
public class AdministrativeTransactions {
    private final JdbcTemplate jdbc;
    public AdministrativeTransactions(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void lock() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Administrative changes require a transaction");
        }
        jdbc.queryForList("select pg_advisory_xact_lock(784629301)");
    }
}
