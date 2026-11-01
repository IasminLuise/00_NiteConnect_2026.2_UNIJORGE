package br.edu.unijorge.niteconnect.attendance.domain;

import java.time.LocalDate;

public final class JustificationAttachmentRules {
    public static final int MINIMUM_DELETION_GRACE_DAYS = 30;
    public static final int MAX_ATTACHMENTS_PER_JUSTIFICATION = 5;
    public static final int MAX_ATTACHMENTS_PER_MEMBER = 10;
    public static final long MAX_BYTES_PER_ATTACHMENT = 5_000_000L;
    public static final long MAX_BYTES_PER_JUSTIFICATION =
            MAX_ATTACHMENTS_PER_JUSTIFICATION * MAX_BYTES_PER_ATTACHMENT;
    public static final long MAX_BYTES_PER_MEMBER = MAX_ATTACHMENTS_PER_MEMBER * MAX_BYTES_PER_ATTACHMENT;

    private JustificationAttachmentRules() {}

    public static int effectiveDeletionGraceDays(int configuredDays) {
        return Math.max(configuredDays, MINIMUM_DELETION_GRACE_DAYS);
    }

    public static LocalDate deletionDeadline(LocalDate retentionExpiresOn, int configuredDays) {
        return retentionExpiresOn.plusDays(effectiveDeletionGraceDays(configuredDays));
    }

    public static boolean hasApprovedDeletionGrace(int configuredDays) {
        return configuredDays == MINIMUM_DELETION_GRACE_DAYS;
    }
}
