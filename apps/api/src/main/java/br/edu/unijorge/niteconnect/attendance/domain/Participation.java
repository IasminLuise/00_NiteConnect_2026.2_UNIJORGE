package br.edu.unijorge.niteconnect.attendance.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Participation {
    private Participation() {}
    public enum Modality { PRESENCIAL, REMOTA }
    public enum EventKind { ENCONTRO, EXTRAORDINARIO, FERIADO, EVENTO }
    public enum EventStatus { AGENDADO, ABERTO, ENCERRADO, CANCELADO }
    public enum ReviewStatus { PENDENTE, APROVADA, RECUSADA }
    public enum Decision { ACEITA, RECUSADA, COMPLEMENTACAO_SOLICITADA }
    public enum PeriodKind { ATTENDANCE, WORK_LOG }
    public record Event(UUID id, String title, String description, EventKind kind, Modality modality,
                        Instant startsAt, Instant endsAt, EventStatus status, long version) {}
    public record Period(UUID id, UUID memberId, UUID attendanceId, PeriodKind kind, Modality modality,
                         Instant startsAt, Instant endsAt, String description, UUID activityId, UUID evidenceId,
                         String origin, ReviewStatus reviewStatus, UUID reviewedBy, Instant reviewedAt,
                         String reviewReason, long version) {}
    public record Attendance(UUID id, UUID eventId, UUID memberId, long version) {}
    public record AttendanceView(UUID id, UUID eventId, UUID memberId, long version, List<Period> periods) {}
    public record Justification(UUID id, UUID memberId, UUID eventId, String reason, String status,
                                String decisionReason, UUID decidedBy, Instant decidedAt, long version) {}
    public record Correction(UUID id, UUID memberId, UUID periodId, UUID eventId, Instant proposedStart,
                             Instant proposedEnd, String reason, String status, String decisionReason,
                             UUID decidedBy, Instant decidedAt, long version) {}
    public record History(UUID id, String entityType, UUID entityId, UUID memberId, UUID actorId,
                          String action, long entityVersion, String beforeValue, String afterValue,
                          String reason, Instant occurredAt) {}
    public record SessionPayload(UUID eventId, String payload, Instant expiresAt, long version) {}
    public record ExpectedParticipant(UUID memberId, String memberName, boolean expected, boolean checkedIn, UUID attendanceId) {}
    public record ExpectedEvent(UUID id, String title, String description, EventKind kind, Modality modality,
                                Instant startsAt, Instant endsAt, EventStatus status,
                                boolean expected, boolean checkedIn, UUID attendanceId) {}
    public record TimelineItem(UUID id, UUID eventId, String eventTitle, EventKind eventKind,
                               EventStatus eventStatus, PeriodKind kind, Modality modality,
                               Instant startsAt, Instant endsAt, Long durationSeconds,
                               String description, UUID activityId, UUID evidenceId, String origin,
                               ReviewStatus reviewStatus, String periodStatus, UUID replacementAdjustmentId, Instant reviewedAt,
                               String reviewReason, long version) {}
}
