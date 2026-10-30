package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.Review;
import br.edu.unijorge.niteconnect.attendance.api.ParticipationGovernanceContracts.*;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.Event;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.EventStatus;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.Modality;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.Period;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.PeriodKind;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.ReviewStatus;
import br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository;
import br.edu.unijorge.niteconnect.attendance.infrastructure.ParticipationGovernanceRepository;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class ParticipationGovernanceService {
    private final ParticipationGovernanceRepository repo;
    private final AttendanceRepository attendance;
    private final ParticipationReviewService reviews;
    private final AccessService access;
    private final AdministrativeTransactions transactions;
    private final Clock clock;

    public ParticipationGovernanceService(ParticipationGovernanceRepository repo, AttendanceRepository attendance,
                                          ParticipationReviewService reviews, AccessService access,
                                          AdministrativeTransactions transactions, Clock clock) {
        this.repo = repo;
        this.attendance = attendance;
        this.reviews = reviews;
        this.access = access;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Transactional
    public CalendarVersion changeCalendar(UUID actor, CalendarChange change) {
        access.requireAdministrator(actor);
        validateWeek(change.weekStart(), change.weekEnd());
        validateHours(change.presentialHoursRelieved(), change.remoteHoursRelieved());
        validateCalendarShape(change);
        transactions.lock();
        int version = repo.currentCalendarVersion(change.weekStart(), change.weekEnd());
        if (version != change.expectedVersion()) throw ApiException.conflict("Calendário alterado; consulte a versão atual");
        if (repo.overlapsCalendar(change.weekStart(), change.weekEnd())) {
            throw ApiException.conflict("A exceção sobrepõe outro intervalo oficial; consolide as alterações em uma única semana");
        }
        validateEventLinks(change);
        return repo.saveCalendar(actor, change, clock.instant());
    }

    @Transactional(readOnly = true)
    public PageResult<CalendarVersion> calendarHistory(UUID actor, LocalDate from, LocalDate to, int page, int size) {
        access.requireAdministrator(actor);
        if ((from == null) != (to == null) || from != null && (from.isAfter(to) || from.equals(LocalDate.MAX) || to.equals(LocalDate.MAX))) {
            throw ApiException.invalid("Informe um intervalo de datas completo e válido");
        }
        return repo.calendarHistory(from, to, page, size);
    }

    @Transactional
    public WeeklyReviewBatchResult reviewWeek(UUID actor, WeeklyReviewBatch batch) {
        access.requireAdministrator(actor);
        validateWeek(batch.weekStart(), batch.weekEnd());
        if (new HashSet<>(batch.items().stream().map(WeeklyReviewItem::periodId).toList()).size() != batch.items().size()) {
            throw ApiException.invalid("O conjunto não pode repetir períodos");
        }
        transactions.lock();
        Instant from = batch.weekStart().atStartOfDay(clock.getZone()).toInstant();
        Instant to = batch.weekEnd().plusDays(1).atStartOfDay(clock.getZone()).toInstant();
        var results = new ArrayList<WeeklyReviewItemResult>();
        for (WeeklyReviewItem item : batch.items()) {
            Period period = attendance.period(item.periodId());
            if (period.kind() != PeriodKind.WORK_LOG || period.endsAt() == null
                    || period.startsAt().isBefore(from) || period.endsAt().isAfter(to)) {
                throw ApiException.invalid("Cada lançamento deve ser um período remoto fechado dentro do intervalo semanal");
            }
            if (period.reviewStatus() != ReviewStatus.PENDENTE) throw ApiException.conflict("O conjunto aceita somente períodos pendentes");
            long resultingVersion = item.expectedVersion();
            if (item.action() == WeeklyReviewAction.APROVAR || item.action() == WeeklyReviewAction.REJEITAR) {
                ReviewStatus status = item.action() == WeeklyReviewAction.APROVAR ? ReviewStatus.APROVADA : ReviewStatus.RECUSADA;
                Period reviewed = reviews.review(actor, item.periodId(), new Review(item.expectedVersion(), status, item.reason()));
                resultingVersion = reviewed.version();
            } else if (period.version() != item.expectedVersion()) {
                throw ApiException.conflict("Lançamento alterado; consulte a versão atual");
            }
            results.add(new WeeklyReviewItemResult(item.periodId(), item.action(), item.expectedVersion(), resultingVersion, item.reason()));
        }
        return repo.createBatch(UUID.randomUUID(), batch.weekStart(), batch.weekEnd(), actor,
                batch.reason(), clock.instant(), results);
    }

    @Transactional(readOnly = true)
    public PageResult<ReviewRequest> reviewRequests(UUID actor, int page, int size) {
        access.requireActive(actor);
        return repo.reviewRequests(actor, page, size);
    }

    @Transactional
    public AdjustmentView requestReplacement(UUID actor, ReplacementRequest request) {
        access.requireActive(actor);
        validateWeek(request.weekStart(), request.weekEnd());
        validateAmount(request.modality(), request.hours());
        return repo.requestReplacement(actor, request, clock.instant());
    }

    @Transactional
    public AdjustmentView decideReplacement(UUID actor, UUID id, ReplacementDecision decision) {
        access.requireAdministrator(actor);
        transactions.lock();
        var current = repo.adjustment(id);
        if (!current.kind().equals("REPOSICAO")) throw ApiException.notFound();
        if (decision.decision() == AdjustmentDecisionType.ACEITAR) {
            if (decision.authorizedModality() == null || decision.authorizedHours() == null || decision.dueOn() == null) {
                throw ApiException.invalid("Modalidade, quantidade e prazo são obrigatórios para autorizar reposição");
            }
            validateAmount(decision.authorizedModality(), decision.authorizedHours());
            if (decision.authorizedHours().compareTo(current.requestedHours()) > 0
                    || !decision.dueOn().isAfter(current.sourceWeekEnd())
                    || decision.authorizedModality() != current.requestedModality()) {
                throw ApiException.invalid("A autorização não pode exceder a solicitação, trocar a modalidade e o prazo deve ser posterior à semana de origem");
            }
        } else if (decision.authorizedModality() != null || decision.authorizedHours() != null || decision.dueOn() != null) {
            throw ApiException.invalid("Decisão recusada não recebe horas, modalidade nem prazo autorizados");
        }
        return repo.decideReplacement(id, decision.expectedVersion(), decision.decision(),
                decision.authorizedModality(), decision.authorizedHours(), decision.dueOn(), actor,
                decision.reason(), clock.instant());
    }

    @Transactional
    public AdjustmentView grantDispensation(UUID actor, DispensationGrant grant) {
        access.requireAdministrator(actor);
        validateWeek(grant.weekStart(), grant.weekEnd());
        validateAmount(grant.modality(), grant.hours());
        transactions.lock();
        return repo.grantDispensation(actor, grant, clock.instant());
    }

    @Transactional(readOnly = true)
    public PageResult<AdjustmentView> adjustments(UUID actor, boolean admin, UUID member, int page, int size) {
        access.requireActive(actor);
        if (admin) {
            access.requireAdministrator(actor);
        } else {
            member = actor;
        }
        return repo.adjustments(member, page, size);
    }

    @Transactional
    public ReplacementAllocation allocateReplacement(UUID actor, UUID adjustmentId, UUID periodId) {
        access.requireAdministrator(actor);
        transactions.lock();
        AdjustmentView adjustment = repo.adjustment(adjustmentId);
        Period period = attendance.period(periodId);
        if (!adjustment.kind().equals("REPOSICAO") || !adjustment.status().equals("AUTORIZADA")
                || !period.memberId().equals(adjustment.memberId()) || period.endsAt() == null
                || period.reviewStatus() != ReviewStatus.APROVADA || period.modality() != adjustment.authorizedModality()) {
            throw ApiException.conflict("A reposição exige período realizado e validado na modalidade autorizada");
        }
        LocalDate performedStart = period.startsAt().atZone(clock.getZone()).toLocalDate();
        LocalDate performedEnd = period.endsAt().atZone(clock.getZone()).toLocalDate();
        if (!performedStart.isAfter(adjustment.sourceWeekEnd()) || performedEnd.isAfter(adjustment.dueOn())) {
            throw ApiException.invalid("O período realizado deve estar após a semana de origem e até o prazo autorizado");
        }
        long duration = ChronoUnit.SECONDS.between(period.startsAt(), period.endsAt());
        long allowed = adjustment.authorizedHours().multiply(BigDecimal.valueOf(3600))
                .setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        if (repo.allocatedSeconds(adjustmentId) + duration > allowed) {
            throw ApiException.conflict("Os períodos associados excedem a quantidade de horas autorizada");
        }
        repo.allocateReplacement(adjustmentId, periodId, actor);
        return repo.allocations(adjustmentId).getLast();
    }

    private void validateEventLinks(CalendarChange change) {
        CalendarExceptionType type = change.type();
        if (type == CalendarExceptionType.CANCELAMENTO_PRESENCIAL || type == CalendarExceptionType.REMARCACAO) {
            Event affected = attendance.event(change.affectedEventId());
            if (affected.status() != EventStatus.CANCELADO || affected.modality() != Modality.PRESENCIAL) {
                throw ApiException.conflict("O encontro afetado precisa estar cancelado e ser presencial");
            }
            LocalDate affectedDate = affected.startsAt().atZone(clock.getZone()).toLocalDate();
            if (affectedDate.isBefore(change.weekStart()) || affectedDate.isAfter(change.weekEnd())) {
                throw ApiException.invalid("O encontro afetado precisa ocorrer na semana institucional da exceção");
            }
            if (type == CalendarExceptionType.REMARCACAO) {
                Event replacement = attendance.event(change.replacementEventId());
                if (replacement.status() == EventStatus.CANCELADO || replacement.modality() != Modality.PRESENCIAL
                        || !replacement.startsAt().isAfter(affected.startsAt())) {
                    throw ApiException.invalid("A nova data deve identificar um encontro presencial não cancelado posterior ao original");
                }
            }
        }
    }

    private static void validateCalendarShape(CalendarChange change) {
        BigDecimal presential = change.presentialHoursRelieved();
        BigDecimal remote = change.remoteHoursRelieved();
        boolean valid = switch (change.type()) {
            case CANCELAMENTO_PRESENCIAL, SUSPENSAO_PRESENCIAL -> presential.compareTo(BigDecimal.valueOf(5)) == 0
                    && remote.signum() == 0 && change.affectedEventId() != null
                    == (change.type() == CalendarExceptionType.CANCELAMENTO_PRESENCIAL)
                    && change.replacementEventId() == null;
            case RECESSO_INTEGRAL -> presential.compareTo(BigDecimal.valueOf(5)) == 0
                    && remote.compareTo(BigDecimal.valueOf(4)) == 0
                    && change.affectedEventId() == null && change.replacementEventId() == null;
            case SUSPENSAO_PARCIAL -> (presential.signum() > 0 || remote.signum() > 0)
                    && change.affectedEventId() == null && change.replacementEventId() == null;
            case REMARCACAO -> presential.signum() == 0 && remote.signum() == 0
                    && change.affectedEventId() != null && change.replacementEventId() != null;
        };
        if (!valid) throw ApiException.invalid("Tipo e horas dispensadas não correspondem à regra institucional");
    }

    private static void validateHours(BigDecimal presential, BigDecimal remote) {
        if (presential == null || remote == null || presential.signum() < 0 || remote.signum() < 0
                || presential.compareTo(BigDecimal.valueOf(5)) > 0 || remote.compareTo(BigDecimal.valueOf(4)) > 0
                || presential.scale() > 2 || remote.scale() > 2) {
            throw ApiException.invalid("Horas dispensadas devem ser não negativas, com até duas casas, e não exceder 5h presenciais/4h remotas");
        }
    }

    private static void validateAmount(Modality modality, BigDecimal hours) {
        if (modality == null || hours == null || hours.signum() <= 0 || hours.scale() > 2
                || hours.compareTo(BigDecimal.valueOf(modality == Modality.PRESENCIAL ? 5 : 4)) > 0) {
            throw ApiException.invalid("Quantidade de horas deve ser positiva e caber na carga da modalidade autorizada");
        }
    }

    private static void validateWeek(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.equals(LocalDate.MAX) || end.equals(LocalDate.MAX)
                || ChronoUnit.DAYS.between(start, end) != 6) {
            throw ApiException.invalid("Informe um intervalo explícito de sete dias consecutivos");
        }
    }
}
