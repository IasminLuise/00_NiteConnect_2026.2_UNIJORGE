package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository;
import br.edu.unijorge.niteconnect.audit.application.AuditTrail;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;

@Component
public class ParticipationJournal {
    private final AttendanceRepository repo; private final AuditTrail audit; private final ObjectMapper json;
    public ParticipationJournal(AttendanceRepository repo,AuditTrail audit,ObjectMapper json) { this.repo=repo;this.audit=audit;this.json=json; }
    @Transactional(propagation=Propagation.MANDATORY)
    public void record(UUID actor,UUID member,String type,UUID id,long version,String action,Object before,Object after,String reason) {
        repo.update("""
            insert into nite_connect.participation_events(id,entity_type,entity_id,member_id,actor_id,action,entity_version,before_value,after_value,reason)
            values (?,?,?,?,?,?,?,?,?,?)
            """,UUID.randomUUID(),type,id,member,actor,action,version,before==null?null:json.writeValueAsString(before),json.writeValueAsString(after),reason);
        audit.recordEntity(actor,type,id,action,null,"version="+version,"Operação de participação: "+action);
    }
}
