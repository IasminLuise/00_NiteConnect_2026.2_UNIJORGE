package br.edu.unijorge.niteconnect.attendance.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Attendance {
    private Attendance() {}

    public record Presence(UUID id, UUID eventId, UUID memberId, long version) {}
    public record Period(UUID id, UUID memberId, UUID attendanceId, Instant startsAt, Instant endsAt, long version) {}
    public record Segment(UUID id, Instant startsAt, Instant endsAt, long version) {}
    public record View(UUID id, UUID eventId, UUID memberId, long version, List<Segment> periods) {}
}
