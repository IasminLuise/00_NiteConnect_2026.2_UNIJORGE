package br.edu.unijorge.niteconnect.attendance.application;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository;
import br.edu.unijorge.niteconnect.shared.api.ApiException;

@Component
public class AttendanceAcademicReader {
    private final AttendanceRepository repo;private final AccessService access;
    public AttendanceAcademicReader(AttendanceRepository repo,AccessService access) { this.repo=repo;this.access=access; }
    public record Snapshot(UUID id,UUID memberId,String revision,UUID authorId,UUID validatedBy,String status,Instant occurredAt,Instant validatedAt,Map<String,String> attributes) {}
    public Snapshot read(UUID actor,UUID member,UUID sourceId,Instant from,Instant to) {
        access.requireActive(actor);if(!actor.equals(member)) access.requireAdministrator(actor);
        var p=repo.period(sourceId);if(!member.equals(p.memberId())) throw ApiException.forbidden();
        if(p.startsAt().compareTo(to)>=0 || p.endsAt()!=null && p.endsAt().compareTo(from)<=0) throw ApiException.conflict("Período fora do semestre");
        String eventRevision="";
        if(p.attendanceId()!=null) { var a=repo.attendance(p.attendanceId());var e=repo.event(a.eventId());
            if(e.status().name().equals("CANCELADO")) throw ApiException.conflict("Encontro cancelado");eventRevision=":"+e.version(); }
        Map<String,String> data=new TreeMap<>();data.put("kind",p.kind().name());data.put("modality",p.modality().name());data.put("startsAt",p.startsAt().toString());
        data.put("open",Boolean.toString(p.endsAt()==null));
        if(p.endsAt()!=null) { data.put("endsAt",p.endsAt().toString());Instant start=p.startsAt().isBefore(from)?from:p.startsAt(),end=p.endsAt().isAfter(to)?to:p.endsAt();
            data.put("registeredSeconds",Long.toString(Duration.between(start,end).getSeconds()));
            if(p.reviewStatus().name().equals("APROVADA")) data.put("validatedSeconds",Long.toString(Duration.between(start,end).getSeconds())); }
        return new Snapshot(p.id(),member,p.version()+eventRevision,member,p.reviewedBy(),p.reviewStatus().name(),p.startsAt(),p.reviewedAt(),Map.copyOf(data));
    }
}
