package br.edu.unijorge.niteconnect.attendance.infrastructure;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Holds PostgreSQL session advisory locks across external storage operations. */
@Component
public class JustificationAttachmentOperationLocks {
    public static final long RETENTION_PURGE_KEY = 784629302L;

    private final JdbcTemplate jdbc;

    public JustificationAttachmentOperationLocks(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public <T> Optional<T> tryWithKey(long key, Supplier<T> operation) {
        return jdbc.execute((ConnectionCallback<Optional<T>>) connection -> {
            if (!tryAcquire(connection, key)) return Optional.empty();
            try {
                return Optional.of(Objects.requireNonNull(operation.get()));
            } finally {
                release(connection, key);
            }
        });
    }

    public <T> T withNamedLock(String name, Supplier<T> operation) {
        long key = keyFor(name);
        return jdbc.execute((ConnectionCallback<T>) connection -> {
            acquire(connection, key);
            try {
                return Objects.requireNonNull(operation.get());
            } finally {
                release(connection, key);
            }
        });
    }

    public <T> Optional<T> tryWithNamedLock(String name, Supplier<T> operation) {
        return tryWithKey(keyFor(name), operation);
    }

    private static long keyFor(String name) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(name.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static boolean tryAcquire(Connection connection, long key) throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select pg_try_advisory_lock(?)")) {
            statement.setLong(1, key);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    private static void acquire(Connection connection, long key) throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select pg_advisory_lock(?)")) {
            statement.setLong(1, key);
            try (ResultSet ignored = statement.executeQuery()) {
                // The session lock is held until release() runs on this same connection.
            }
        }
    }

    private static void release(Connection connection, long key) throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select pg_advisory_unlock(?)")) {
            statement.setLong(1, key);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !result.getBoolean(1)) {
                    throw new java.sql.SQLException("Advisory lock was not released");
                }
            }
        } catch (java.sql.SQLException failure) {
            try {
                connection.abort(Runnable::run);
            } catch (java.sql.SQLException abortFailure) {
                failure.addSuppressed(abortFailure);
            }
            throw failure;
        }
    }
}
