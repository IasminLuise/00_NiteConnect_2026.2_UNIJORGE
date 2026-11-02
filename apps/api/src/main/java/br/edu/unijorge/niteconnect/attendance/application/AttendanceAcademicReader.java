package br.edu.unijorge.niteconnect.attendance.application;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository;
import br.edu.unijorge.niteconnect.attendance.infrastructure.FrequencyRepository;
import br.edu.unijorge.niteconnect.attendance.domain.Attendance.AttendanceSegment;
import br.edu.unijorge.niteconnect.shared.api.ApiException;

@Component
public class AttendanceAcademicReader {
    private final AttendanceRepository repo;private final AccessService access;private final FrequencyRepository frequency;
    public AttendanceAcademicReader(AttendanceRepository repo,AccessService access,FrequencyRepository frequency) { this.repo=repo;this.access=access;this.frequency=frequency; }
    public record Snapshot(UUID id,UUID memberId,String revision,UUID authorId,UUID validatedBy,String status,Instant occurredAt,Instant validatedAt,Map<String,String> attributes) {}
    public Snapshot read(UUID actor,UUID member,UUID sourceId,Instant from,Instant to) {
        access.requireActive(actor);if(!actor.equals(member)) access.requireAdministrator(actor);
        AttendanceSegment p=repo.segment(sourceId);if(!member.equals(p.memberId())) throw ApiException.forbidden();
        if (p.attendanceId() == null) {
            throw ApiException.conflict("Origem não é presença registrada em encontro");
        }
        if(p.startsAt().compareTo(to)>=0 || p.endsAt()!=null && p.endsAt().compareTo(from)<=0) throw ApiException.conflict("Período fora do semestre");
        var attendance = repo.attendance(p.attendanceId());
        var event = repo.event(attendance.eventId());
        if (event.status().name().equals("CANCELADO")) throw ApiException.conflict("Encontro cancelado");
        Map<String,String> data=new TreeMap<>();
        data.put("kind","PRESENCA_ENCONTRO");
        data.put("eventId",event.id().toString());
        data.put("eventTitle",event.title());
        data.put("eventKind",event.kind().name());
        data.put("modality",event.modality().name());
        data.put("startsAt",p.startsAt().toString());
        data.put("open",Boolean.toString(p.endsAt()==null));
        if(p.endsAt()!=null) data.put("endsAt",p.endsAt().toString());
        String revision = attendance.version()+":"+event.version();
        return new Snapshot(p.id(),member,revision,member,null,"REGISTRADA",p.startsAt(),null,Map.copyOf(data));
    }

    public Snapshot readConfirmation(UUID actor,UUID member,UUID sourceId,Instant from,Instant to) {
        access.requireActive(actor);if(!actor.equals(member)) access.requireAdministrator(actor);
        var p=frequency.confirmationEvidence(sourceId).orElseThrow(ApiException::notFound);
        if(!member.equals(p.memberId())) throw ApiException.forbidden();
        if(!"PRESENTE".equals(p.participantStatus())) throw ApiException.conflict("Origem não é confirmação de presença vigente");
        if("CANCELADA".equals(p.listStatus()) || "CANCELADO".equals(p.eventStatus())) throw ApiException.conflict("Reunião cancelada");
        if(p.confirmedAt().isBefore(from) || !p.confirmedAt().isBefore(to)) throw ApiException.conflict("Confirmação fora do semestre");
        Map<String,String> data=new TreeMap<>();
        data.put("kind","PRESENCA_FREQUENCIA");
        data.put("eventId",p.eventId().toString());
        data.put("eventTitle",p.title());
        data.put("eventKind",p.kind());
        data.put("modality",p.modality());
        data.put("listId",p.listId().toString());
        data.put("startsAt",p.startsAt().toString());
        data.put("confirmedAt",p.confirmedAt().toString());
        String revision=p.attendanceVersion()+":"+p.memberVersion();
        return new Snapshot(p.attendanceId(),member,revision,member,null,"REGISTRADA",p.confirmedAt(),null,Map.copyOf(data));
    }
}
