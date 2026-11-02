package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.attendance.infrastructure.FrequencyRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/** Removes expired handoff hashes after a short operational retention period. */
@Component
public class FrequencyHandoffRetention {
    private static final Duration RETENTION = Duration.ofHours(24);
    private static final int BATCH_SIZE = 500;

    private final FrequencyRepository repository;
    private final Clock clock;

    public FrequencyHandoffRetention(FrequencyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${nite.frequency.handoff-cleanup-delay-ms:3600000}")
    @Transactional
    public void cleanupExpiredHandoffs() {
        repository.deleteExpiredHandoffsBefore(clock.instant().minus(RETENTION), BATCH_SIZE);
    }
}
