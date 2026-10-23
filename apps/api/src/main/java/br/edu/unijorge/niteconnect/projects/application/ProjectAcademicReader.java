package br.edu.unijorge.niteconnect.projects.application;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import br.edu.unijorge.niteconnect.access.application.ProjectAccess;
import br.edu.unijorge.niteconnect.shared.api.ApiException;

/** Historical membership is checked at the contribution time, independently of the actor's current read permission. */
@Component
public class ProjectAcademicReader {
    private final JdbcTemplate jdbc;private final ProjectAccess access;private final ProjectDirectory projects;
    public ProjectAcademicReader(JdbcTemplate jdbc,ProjectAccess access,ProjectDirectory projects) { this.jdbc=jdbc;this.access=access;this.projects=projects; }
    public record Snapshot(UUID projectId,UUID membershipId,UUID memberId,Instant startedAt,Instant endedAt,UUID linkedBy,long projectVersion) {}
    public Snapshot read(UUID actor,UUID member,UUID project,Instant at) {
        access.requireRead(actor,project);var p=projects.require(project);
        return jdbc.query("select * from nite_connect.project_memberships where project_id=? and member_id=? and started_at<=? and (ended_at is null or ended_at>?) order by started_at desc,id limit 1",
                (r,n)->new Snapshot(project,r.getObject("id",UUID.class),member,r.getTimestamp("started_at").toInstant(),
                        r.getTimestamp("ended_at")==null?null:r.getTimestamp("ended_at").toInstant(),r.getObject("started_by",UUID.class),p.version()),
                project,member,Timestamp.from(at),Timestamp.from(at)).stream().findFirst().orElseThrow(()->ApiException.conflict("Sem vínculo no instante da contribuição"));
    }
}
