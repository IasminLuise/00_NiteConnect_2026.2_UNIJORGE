package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.attendance.domain.Participation.Modality;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.Period;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.PeriodKind;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.ReviewStatus;
import br.edu.unijorge.niteconnect.attendance.application.WeeklyParticipationRules;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class WeeklyParticipationRulesTest {
    private static final UUID MEMBER = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-10-26T00:00:00Z");
    private static final Instant END = Instant.parse("2026-11-02T00:00:00Z");

    @Test
    void separatesRegisteredPendingValidatedAndRefusedHoursByModality() {
        var periods = List.of(
                period(Modality.PRESENCIAL, 1, 4, ReviewStatus.APROVADA),
                period(Modality.REMOTA, 1, 2, ReviewStatus.APROVADA),
                period(Modality.REMOTA, 2, 3, ReviewStatus.PENDENTE),
                period(Modality.REMOTA, 3, 4, ReviewStatus.RECUSADA));

        var result = WeeklyParticipationRules.calculate(periods, START, END, 5 * 3600L, 4 * 3600L, 0, 0);

        assertThat(result.presential()).isEqualTo(new WeeklyParticipationRules.ModalitySummary(5 * 3600L, 3 * 3600L,
                0, 3 * 3600L, 0, 0, 2 * 3600L));
        assertThat(result.remote()).isEqualTo(new WeeklyParticipationRules.ModalitySummary(4 * 3600L, 3 * 3600L,
                1 * 3600L, 1 * 3600L, 1 * 3600L, 0, 3 * 3600L));
    }

    @Test
    void clipsAtRangeAndSubtractsOnlyTheMatchingModalitiesObligation() {
        var periods = List.of(period(Modality.REMOTA, -1, 1, ReviewStatus.APROVADA));

        var result = WeeklyParticipationRules.calculate(periods, START, END, 5 * 3600L, 4 * 3600L, 2 * 3600L, 0);

        assertThat(result.remote().validatedSeconds()).isEqualTo(3600L);
        assertThat(result.presential().requiredSeconds()).isEqualTo(3 * 3600L);
        assertThat(result.remote().requiredSeconds()).isEqualTo(4 * 3600L);
        assertThat(result.presential().remainingSeconds()).isEqualTo(3 * 3600L);
    }

    private static Period period(Modality modality, long startHour, long endHour, ReviewStatus status) {
        boolean presential=modality==Modality.PRESENCIAL;
        return new Period(UUID.randomUUID(), MEMBER, presential?UUID.randomUUID():null,
                presential?PeriodKind.ATTENDANCE:PeriodKind.WORK_LOG, modality,
                START.plusSeconds(startHour * 3600), START.plusSeconds(endHour * 3600), "Registro de teste", null, null,
                "DECLARATION", status, null, null, null, 0);
    }
}
