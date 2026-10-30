package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.attendance.domain.Participation.Modality;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.Period;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.ReviewStatus;
import java.time.Instant;
import java.util.List;

/** Pure weekly aggregation; required hours and dispensation are kept modality-specific. */
public final class WeeklyParticipationRules {
    private WeeklyParticipationRules() {}

    public record ModalitySummary(long requiredSeconds, long registeredSeconds, long pendingSeconds,
                                  long validatedSeconds, long refusedSeconds, long openPeriods,
                                  long remainingSeconds) {}
    public record Summary(ModalitySummary presential, ModalitySummary remote) {}

    public static Summary calculate(List<Period> periods, Instant from, Instant to,
                                    long requiredPresentialSeconds, long requiredRemoteSeconds,
                                    long releasedPresentialSeconds, long releasedRemoteSeconds) {
        if (periods == null || from == null || to == null || !to.isAfter(from)
                || requiredPresentialSeconds < 0 || requiredRemoteSeconds < 0
                || releasedPresentialSeconds < 0 || releasedRemoteSeconds < 0
                || releasedPresentialSeconds > requiredPresentialSeconds
                || releasedRemoteSeconds > requiredRemoteSeconds) {
            throw new IllegalArgumentException("Intervalo, carga horária ou dispensa inválidos");
        }
        var presential = new MutableSummary(Math.subtractExact(requiredPresentialSeconds, releasedPresentialSeconds));
        var remote = new MutableSummary(Math.subtractExact(requiredRemoteSeconds, releasedRemoteSeconds));
        for (Period period : periods) {
            MutableSummary target = period.modality() == Modality.PRESENCIAL ? presential : remote;
            if (period.endsAt() == null) {
                target.openPeriods++;
                continue;
            }
            long seconds = ParticipationRules.clippedSeconds(period.startsAt(), period.endsAt(), from, to);
            target.registeredSeconds = Math.addExact(target.registeredSeconds, seconds);
            if (period.reviewStatus() == ReviewStatus.APROVADA) target.validatedSeconds = Math.addExact(target.validatedSeconds, seconds);
            else if (period.reviewStatus() == ReviewStatus.PENDENTE) target.pendingSeconds = Math.addExact(target.pendingSeconds, seconds);
            else target.refusedSeconds = Math.addExact(target.refusedSeconds, seconds);
        }
        return new Summary(presential.toRecord(), remote.toRecord());
    }

    private static final class MutableSummary {
        private final long requiredSeconds;
        private long registeredSeconds;
        private long pendingSeconds;
        private long validatedSeconds;
        private long refusedSeconds;
        private long openPeriods;

        private MutableSummary(long requiredSeconds) { this.requiredSeconds = requiredSeconds; }

        private ModalitySummary toRecord() {
            return new ModalitySummary(requiredSeconds, registeredSeconds, pendingSeconds, validatedSeconds,
                    refusedSeconds, openPeriods, Math.max(0, requiredSeconds - validatedSeconds));
        }
    }
}
