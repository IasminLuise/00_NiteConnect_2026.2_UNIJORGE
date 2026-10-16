package br.edu.unijorge.niteconnect.activities.infrastructure;

import br.edu.unijorge.niteconnect.activities.application.EvidenceObjectStore.Stored;
import br.edu.unijorge.niteconnect.shared.api.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class EvidenceRepository {
    private final JdbcTemplate jdbc;
    private static final String SELECT="select e.*,o.filename,o.content_type,o.size_bytes,o.sha256,o.object_key,o.state as object_state from nite_connect.activity_evidence e left join nite_connect.evidence_objects o on o.id=e.object_id";
    private static final RowMapper<Evidence> EVIDENCE=(r,n)->new Evidence(r.getObject("id",UUID.class),r.getObject("activity_id",UUID.class),
            r.getObject("author_id",UUID.class),r.getObject("assignment_id",UUID.class),r.getString("type"),r.getString("content"),r.getString("url"),
            r.getObject("corrects_evidence_id",UUID.class),r.getTimestamp("created_at").toInstant(),instant(r.getTimestamp("withdrawn_at")),
            r.getObject("object_id",UUID.class),r.getString("filename"),r.getString("content_type"),r.getLong("size_bytes"),r.getString("sha256"),r.getObject("object_key",UUID.class),r.getString("object_state"));
    private static final RowMapper<Receipt> RECEIPT=(r,n)->new Receipt(r.getObject("id",UUID.class),r.getObject("activity_id",UUID.class),r.getObject("author_id",UUID.class),r.getObject("object_key",UUID.class),r.getString("state"),r.getTimestamp("updated_at").toInstant());
    public EvidenceRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static Instant instant(Timestamp t) { return t==null?null:t.toInstant(); }
    public Evidence require(UUID id) { return jdbc.query(SELECT+" where e.id=?",EVIDENCE,id).stream().findFirst().orElseThrow(ApiException::notFound); }
    public Evidence create(UUID actor,UUID activity,UUID assignment,String type,String content,String url,UUID correction,UUID object) {
        UUID id=UUID.randomUUID();jdbc.update("insert into nite_connect.activity_evidence(id,activity_id,author_id,assignment_id,type,content,url,corrects_evidence_id,object_id) values (?,?,?,?,?,?,?,?,?)",id,activity,actor,assignment,type,content,url,correction,object);return require(id);
    }
    public void withdraw(UUID actor,UUID id,String reason) {
        if(jdbc.update("update nite_connect.activity_evidence set withdrawn_at=clock_timestamp(),withdrawn_by=?,withdraw_reason=? where id=? and withdrawn_at is null",actor,reason,id)!=1) throw ApiException.conflict("Evidência já retirada");
    }
    public PageResult<Evidence> list(UUID activity,UUID author,int page,int size) {
        PageResult.validate(page,size);String where=activity!=null?" where e.activity_id=?":" where e.author_id=?";UUID target=activity!=null?activity:author;
        return new PageResult<>(jdbc.query(SELECT+where+" order by e.created_at desc,e.id limit ? offset ?",EVIDENCE,target,size,(long)page*size),page,size,
                jdbc.queryForObject("select count(*) from nite_connect.activity_evidence e"+where,Long.class,target));
    }
    public Receipt begin(UUID actor,UUID activity,String filename) {
        UUID id=UUID.randomUUID(),key=UUID.randomUUID();
        jdbc.update("insert into nite_connect.evidence_objects(id,activity_id,author_id,object_key,filename,state) values (?,?,?,?,?,'PENDING')",id,activity,actor,key,filename);return receipt(id);
    }
    public Receipt receipt(UUID id) { return jdbc.query("select * from nite_connect.evidence_objects where id=?",RECEIPT,id).stream().findFirst().orElseThrow(ApiException::notFound); }
    public void available(UUID id,Stored stored) {
        if(jdbc.update("update nite_connect.evidence_objects set filename=?,content_type=?,size_bytes=?,sha256=?,state='AVAILABLE',updated_at=clock_timestamp() where id=? and state='PENDING'",stored.filename(),stored.contentType(),stored.size(),stored.sha256(),id)!=1) throw ApiException.conflict("Upload não está pendente");
    }
    public boolean fail(UUID id) { return jdbc.update("update nite_connect.evidence_objects set state='FAILED',updated_at=clock_timestamp() where id=? and state='PENDING'",id)==1; }
    public Optional<Receipt> byKey(UUID key) { return jdbc.query("select * from nite_connect.evidence_objects where object_key=?",RECEIPT,key).stream().findFirst(); }
    public List<UUID> stalePendingKeys(Instant cutoff) {
        return jdbc.query("select object_key from nite_connect.evidence_objects where state='PENDING' and updated_at<? order by updated_at,id",
                (r,n)->r.getObject(1,UUID.class),Timestamp.from(cutoff));
    }
    public record Receipt(UUID id,UUID activityId,UUID authorId,UUID objectKey,String state,Instant updatedAt) {}
    public record Evidence(UUID id,UUID activityId,UUID authorId,UUID assignmentId,String type,String content,String url,UUID correctsEvidenceId,
                           Instant createdAt,Instant withdrawnAt,UUID objectId,String filename,String contentType,long size,String sha256,UUID objectKey,String objectState) {}
}
