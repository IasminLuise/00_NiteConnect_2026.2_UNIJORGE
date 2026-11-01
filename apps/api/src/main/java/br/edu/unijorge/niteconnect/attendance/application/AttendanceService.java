package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.CheckIn;
import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.CheckOut;
import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.Return;
import br.edu.unijorge.niteconnect.attendance.domain.Attendance.*;
import br.edu.unijorge.niteconnect.attendance.domain.Agenda.Event;
import br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository.ts;

@Service
public class AttendanceService {
    private final AttendanceRepository repo;
    private final AccessService access;
    private final AdministrativeTransactions tx;
    private final AttendanceJournal journal;
    private final Clock clock;

    public AttendanceService(AttendanceRepository repo, AccessService access, AdministrativeTransactions tx,
                             AttendanceJournal journal, Clock clock) {
        this.repo = repo;
        this.access = access;
        this.tx = tx;
        this.journal = journal;
        this.clock = clock;
    }

    @Transactional
    public View checkIn(UUID actor, CheckIn command) {
        tx.lock();
        access.requireActive(actor);
        Instant now = clock.instant();
        repo.qr(command.eventId(), AgendaService.hash(command.payload()), now);
        if (repo.attendance(command.eventId(), actor).isPresent()) {
            throw ApiException.conflict("Presença já registrada; utilize retorno");
        }
        UUID id = repo.createAttendance(command.eventId(), actor);
        Event event = repo.event(command.eventId());
        UUID period = repo.createPeriod(actor, id, event.modality(), now);
        journal.record(actor, actor, "PERIOD", period, 0, "CHECKED_IN", null, repo.period(period),
                "Entrada por sessão temporária");
        journal.record(actor, actor, "ATTENDANCE", id, 0, "CREATED", null, repo.attendance(id),
                "Presença em encontro");
        return view(repo.attendance(id));
    }

    @Transactional
    public View checkOut(UUID actor, CheckOut command) {
        tx.lock();
        access.requireActive(actor);
        Period before = repo.open(actor).orElseThrow(() -> ApiException.conflict("Nenhuma presença aberta"));
        Presence attendance = repo.attendance(before.attendanceId());
        repo.version(attendance.version(), command.expectedVersion());
        Instant now = clock.instant();
        if (!now.isAfter(before.startsAt())) throw ApiException.conflict("Saída deve ser posterior à entrada");
        repo.changed(repo.update("update nite_connect.participation_periods set ends_at=?,version=version+1 where id=? and version=?",
                ts(now), before.id(), before.version()));
        repo.touchAttendance(attendance.id(), attendance.version());
        journal.record(actor, actor, "PERIOD", before.id(), before.version() + 1, "CHECKED_OUT", before,
                repo.period(before.id()), "Saída registrada pelo integrante");
        journal.record(actor, actor, "ATTENDANCE", attendance.id(), attendance.version() + 1, "DEPARTURE", attendance,
                repo.attendance(attendance.id()), "Saída do encontro");
        return view(repo.attendance(attendance.id()));
    }

    @Transactional
    public View returnTo(UUID actor, UUID id, Return command) {
        tx.lock();
        access.requireActive(actor);
        Presence before = own(actor, id);
        repo.version(before.version(), command.expectedVersion());
        Instant now = clock.instant();
        repo.qr(before.eventId(), AgendaService.hash(command.payload()), now);
        if (repo.open(actor).isPresent()) throw ApiException.conflict("Já existe presença aberta");
        UUID period = repo.createPeriod(actor, id, repo.event(before.eventId()).modality(), now);
        repo.touchAttendance(id, before.version());
        journal.record(actor, actor, "PERIOD", period, 0, "RETURNED", null, repo.period(period),
                "Retorno ao encontro");
        journal.record(actor, actor, "ATTENDANCE", id, before.version() + 1, "RETURNED", before,
                repo.attendance(id), "Retorno ao encontro");
        return view(repo.attendance(id));
    }

    public Presence own(UUID actor, UUID id) {
        Presence attendance = repo.attendance(id);
        if (!actor.equals(attendance.memberId())) throw ApiException.notFound();
        return attendance;
    }

    public View view(Presence attendance) {
        var segments = repo.periods(attendance.id()).stream()
                .map(period -> new Segment(period.id(), period.startsAt(), period.endsAt(), period.version()))
                .toList();
        return new View(attendance.id(), attendance.eventId(), attendance.memberId(), attendance.version(), segments);
    }
}
