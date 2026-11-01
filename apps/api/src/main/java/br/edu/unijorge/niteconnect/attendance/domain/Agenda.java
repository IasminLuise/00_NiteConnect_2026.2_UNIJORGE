package br.edu.unijorge.niteconnect.attendance.domain;

import java.time.Instant;
import java.util.UUID;

public final class Agenda {
    private Agenda() {}

    public enum Modality { PRESENCIAL, REMOTA }
    public enum EventKind { ENCONTRO, EXTRAORDINARIO, FERIADO, EVENTO }
    public enum EventStatus { AGENDADO, ABERTO, ENCERRADO, CANCELADO }

    public record Event(UUID id, String title, String description, EventKind kind, Modality modality,
                        Instant startsAt, Instant endsAt, EventStatus status, long version) {}
    public record SessionPayload(UUID eventId, String payload, Instant expiresAt, long version) {}
    public record ExpectedParticipant(UUID memberId, String memberName, boolean expected, boolean checkedIn, UUID attendanceId) {}
    public record ExpectedEvent(UUID id, String title, String description, EventKind kind, Modality modality,
                                Instant startsAt, Instant endsAt, EventStatus status,
                                boolean expected, boolean checkedIn, UUID attendanceId) {}
}
