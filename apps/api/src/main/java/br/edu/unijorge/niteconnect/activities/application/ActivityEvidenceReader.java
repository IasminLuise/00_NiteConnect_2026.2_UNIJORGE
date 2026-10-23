package br.edu.unijorge.niteconnect.activities.application;

import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import br.edu.unijorge.niteconnect.activities.infrastructure.*;
import br.edu.unijorge.niteconnect.projects.application.ProjectAcademicReader;
import br.edu.unijorge.niteconnect.shared.api.ApiException;

@Component
public class ActivityEvidenceReader {
    private final EvidenceRepository evidence;private final ActivityRepository activities;private final ActivityPolicy access;private final JdbcTemplate jdbc;private final ProjectAcademicReader projects;
    public ActivityEvidenceReader(EvidenceRepository evidence,ActivityRepository activities,ActivityPolicy access,JdbcTemplate jdbc,ProjectAcademicReader projects) { this.evidence=evidence;this.activities=activities;this.access=access;this.jdbc=jdbc;this.projects=projects; }
    public record Snapshot(UUID id,UUID activityId,UUID memberId,UUID projectId,String revision,UUID authorId,String status,Instant occurredAt,Map<String,String> attributes) {}
    public Snapshot read(UUID actor,UUID member,UUID sourceId,Instant from,Instant to) {
        var e=evidence.require(sourceId);var a=activities.require(e.activityId());access.read(actor,a);
        if(!member.equals(e.authorId())) throw ApiException.forbidden();
        if(e.withdrawnAt()!=null || e.objectId()!=null && !"AVAILABLE".equals(e.objectState())) throw ApiException.conflict("Evidência retirada ou arquivo indisponível");
        if(e.createdAt().isBefore(from) || !e.createdAt().isBefore(to)) throw ApiException.conflict("Contribuição fora do período");
        if(e.assignmentId()==null || !Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from nite_connect.activity_assignments where id=? and activity_id=? and member_id=? and started_at<=? and (ended_at is null or ended_at>?))",Boolean.class,
                e.assignmentId(),a.id(),member,java.sql.Timestamp.from(e.createdAt()),java.sql.Timestamp.from(e.createdAt())))) throw ApiException.conflict("Contribuição sem atribuição individual verificável");
        Map<String,String> data=new TreeMap<>();data.put("activityId",a.id().toString());data.put("title",a.title());data.put("assignmentId",e.assignmentId().toString());data.put("type",e.type());
        if(e.content()!=null) data.put("content",e.content());
        // External links are references only; do not fetch them or persist bearer/signed URL query parameters.
        if(e.url()!=null) data.put("externalReferencePresent","true");
        if(e.sha256()!=null) data.put("sha256",e.sha256());
        if(e.correctsEvidenceId()!=null) data.put("correctsEvidenceId",e.correctsEvidenceId().toString());
        String projectRevision="";
        if(a.projectId()!=null) { var p=projects.read(actor,member,a.projectId(),e.createdAt());projectRevision=":"+p.projectVersion()+":"+p.membershipId()+":"+p.endedAt();data.put("membershipId",p.membershipId().toString()); }
        return new Snapshot(e.id(),a.id(),member,a.projectId(),a.version()+":"+e.id()+":"+e.objectState()+projectRevision,e.authorId(),a.status().name(),e.createdAt(),Map.copyOf(data));
    }
}
