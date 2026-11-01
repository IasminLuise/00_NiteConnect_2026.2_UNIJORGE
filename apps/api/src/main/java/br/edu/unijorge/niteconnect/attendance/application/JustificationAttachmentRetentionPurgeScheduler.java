package br.edu.unijorge.niteconnect.attendance.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="nite.justification.retention.purge", name="enabled", havingValue="true")
public class JustificationAttachmentRetentionPurgeScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JustificationAttachmentRetentionPurgeScheduler.class);
    private final JustificationAttachmentRetentionPurge purge;
    private final boolean dryRun;

    public JustificationAttachmentRetentionPurgeScheduler(JustificationAttachmentRetentionPurge purge,
            @Value("${nite.justification.retention.purge.dry-run:true}") boolean dryRun) {
        this.purge = purge;
        this.dryRun = dryRun;
    }

    @Scheduled(cron="${nite.justification.retention.purge.cron:0 15 2 * * *}",
            zone="${nite.justification.retention.purge.zone:UTC}")
    public void execute() {
        var result = purge.runScheduled(dryRun);
        LOGGER.info("Justification attachment retention purge run={} mode={} status={} candidates={} skipped={} deleted={} failed={} code={}",
                result.id(), result.mode(), result.status(), result.candidateCount(), result.skippedCount(),
                result.deletedCount(), result.failedCount(), result.resultCode());
    }
}
