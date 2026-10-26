package br.edu.unijorge.niteconnect.documents.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentInspectionRepository {
    private final JdbcTemplate jdbc;
    public DocumentInspectionRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Inspection(UUID id,UUID documentId,UUID versionId,UUID actorId,String editorKey,
                             String status,Instant expiresAt,Integer lastProviderStatus){}
    public void insert(Inspection item){
        jdbc.update("insert into nite_connect.document_inspection_sessions(id,document_id,version_id,actor_id,editor_key,status,expires_at) values (?,?,?,?,?,?,?)",
                item.id(),item.documentId(),item.versionId(),item.actorId(),item.editorKey(),item.status(),DocumentRepository.ts(item.expiresAt()));
    }
    public Optional<Inspection> find(UUID id){
        return jdbc.query("select * from nite_connect.document_inspection_sessions where id=?",(r,n)->new Inspection(
                r.getObject("id",UUID.class),r.getObject("document_id",UUID.class),r.getObject("version_id",UUID.class),
                r.getObject("actor_id",UUID.class),r.getString("editor_key"),r.getString("status"),
                r.getTimestamp("expires_at").toInstant(),r.getObject("last_provider_status",Integer.class)),id).stream().findFirst();
    }
    public void state(UUID id,String state,Integer providerStatus){
        jdbc.update("update nite_connect.document_inspection_sessions set status=?,last_provider_status=? where id=?",state,providerStatus,id);
    }
}
