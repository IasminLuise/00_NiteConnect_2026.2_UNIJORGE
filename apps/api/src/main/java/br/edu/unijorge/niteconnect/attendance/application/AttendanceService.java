package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.CheckIn;
import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.CheckOut;
import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.Return;
import br.edu.unijorge.niteconnect.attendance.domain.Attendance.*;
import br.edu.unijorge.niteconnect.attendance.domain.Frequency.Confirmation;
import br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AttendanceService {
    private final AttendanceRepository repo;
    private final FrequencyService frequency;

    public AttendanceService(AttendanceRepository repo, FrequencyService frequency) {
        this.repo = repo;
        this.frequency = frequency;
    }

    @Transactional
    public Confirmation checkIn(UUID actor, CheckIn command) {
        return frequency.confirmLegacy(actor, command.eventId(), command.payload());
    }

    @Transactional
    public View checkOut(UUID actor, CheckOut command) {
        throw ApiException.gone("LEGACY_SEGMENT_FLOW_DISABLED");
    }

    @Transactional
    public View returnTo(UUID actor, UUID id, Return command) {
        throw ApiException.gone("LEGACY_SEGMENT_FLOW_DISABLED");
    }

    public View view(Presence attendance) {
        var segments = repo.segments(attendance.id()).stream()
                .map(segment -> new Segment(segment.id(), segment.startsAt(), segment.endsAt(), segment.version()))
                .toList();
        return new View(attendance.id(), attendance.eventId(), attendance.memberId(), attendance.version(), segments);
    }
}
