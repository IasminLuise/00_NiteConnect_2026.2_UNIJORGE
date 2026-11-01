package br.edu.unijorge.niteconnect.attendance.infrastructure;

import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentContracts.Context;
import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentContracts.RetentionPolicyEventView;
import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentContracts.RetentionPolicyView;
import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentContracts.RetentionHoldEventView;
import br.edu.unijorge.niteconnect.attendance.domain.JustificationAttachmentRules;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JustificationAttachmentRepository {
    private final JdbcTemplate jdbc;
    public JustificationAttachmentRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record RetentionPolicy(UUID id, int version, int retentionMonths, int deletionGraceDays,
                                  String anchorRule, UUID createdBy, Instant createdAt,
                                  String creationReason, UUID supersedesId) {}
    public record Attachment(UUID id, UUID justificationId, UUID memberId, UUID retentionPolicyId,
                             UUID objectKey, Context context, String filename, String contentType,
                             Long sizeBytes, String sha256, String state, UUID uploadedBy,
                             Instant createdAt, Instant deletedAt, long version) {}
    public record AttachmentUsage(long count, long sizeBytes) {}
    public record Classification(UUID id, UUID justificationId, int version, LocalDate semesterEndOn,
                                 String sourceReference, String reason, UUID createdBy, Instant createdAt) {}
    public record Hold(UUID id, UUID attachmentId, String basis, String reason, UUID actorId, Instant occurredAt) {}

    private static final org.springframework.jdbc.core.RowMapper<RetentionPolicy> POLICY = (rs, row) -> new RetentionPolicy(
            rs.getObject("id", UUID.class), rs.getInt("version"), rs.getInt("retention_months"),
            rs.getInt("deletion_grace_days"), rs.getString("anchor_rule"), rs.getObject("created_by", UUID.class),
            rs.getTimestamp("created_at").toInstant(), rs.getString("creation_reason"), rs.getObject("supersedes_id", UUID.class));
    private static final org.springframework.jdbc.core.RowMapper<RetentionPolicyEventView> POLICY_EVENT = (rs, row) ->
            new RetentionPolicyEventView(rs.getObject("id", UUID.class), rs.getString("event_type"),
                    rs.getObject("actor_id", UUID.class), rs.getTimestamp("occurred_at").toInstant(),
                    rs.getString("reason"), rs.getString("source_reference"),
                    rs.getString("backup_handling_reference"), rs.getString("sensitive_data_handling_reference"));
    private static final org.springframework.jdbc.core.RowMapper<Attachment> ATTACHMENT = (rs, row) -> new Attachment(
            rs.getObject("id", UUID.class), rs.getObject("justification_id", UUID.class), rs.getObject("member_id", UUID.class),
            rs.getObject("retention_policy_id", UUID.class), rs.getObject("object_key", UUID.class),
            Context.valueOf(rs.getString("context")), rs.getString("filename"), rs.getString("content_type"),
            rs.getObject("size_bytes", Long.class), rs.getString("sha256"), rs.getString("state"),
            rs.getObject("uploaded_by", UUID.class), rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("deleted_at") == null ? null : rs.getTimestamp("deleted_at").toInstant(), rs.getLong("version"));
    private static final org.springframework.jdbc.core.RowMapper<Classification> CLASSIFICATION = (rs, row) ->
            new Classification(rs.getObject("id", UUID.class), rs.getObject("justification_id", UUID.class),
                    rs.getInt("version"), rs.getObject("semester_end_on", LocalDate.class), rs.getString("source_reference"),
                    rs.getString("reason"), rs.getObject("created_by", UUID.class), rs.getTimestamp("created_at").toInstant());

    public boolean hasPendingPolicyProposal() {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(select 1 from nite_connect.justification_attachment_retention_policies p
                  where (select event_type from nite_connect.justification_attachment_retention_events e
                         where e.policy_id=p.id order by e.sequence desc limit 1)='CRIADA')
                """, Boolean.class));
    }
    public int nextPolicyVersion() {
        return jdbc.queryForObject("select coalesce(max(version),0)+1 from nite_connect.justification_attachment_retention_policies", Integer.class);
    }
    public boolean hasValidatedPolicy() {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(select 1 from nite_connect.justification_attachment_retention_policies p
                  where (select event_type from nite_connect.justification_attachment_retention_events e
                         where e.policy_id=p.id order by e.sequence desc limit 1)='VALIDADA')
                """, Boolean.class));
    }
    public Optional<RetentionPolicy> activeRetentionPolicy() {
        return jdbc.query("""
                select p.* from nite_connect.justification_attachment_retention_policies p
                where (select event_type from nite_connect.justification_attachment_retention_events e
                       where e.policy_id=p.id order by e.sequence desc limit 1)='VALIDADA'
                order by p.version desc limit 1
                """, POLICY).stream().findFirst();
    }
    public boolean wasPolicyValidated(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(select 1 from nite_connect.justification_attachment_retention_events
                  where policy_id=? and event_type='VALIDADA')
                """, Boolean.class, id));
    }
    public RetentionPolicy policy(UUID id) {
        return jdbc.query("select * from nite_connect.justification_attachment_retention_policies where id=?", POLICY, id)
                .stream().findFirst().orElseThrow(ApiException::notFound);
    }
    public String policyStatus(UUID id) {
        return jdbc.query("""
                select event_type from nite_connect.justification_attachment_retention_events
                where policy_id=? order by sequence desc limit 1
                """, (rs, row) -> rs.getString(1), id).stream().findFirst().orElseThrow(ApiException::notFound);
    }
    public Optional<RetentionPolicy> latestPolicy() {
        return jdbc.query("select * from nite_connect.justification_attachment_retention_policies order by version desc limit 1", POLICY)
                .stream().findFirst();
    }
    public List<RetentionPolicyEventView> policyEvents(UUID policyId) {
        return jdbc.query("""
                select id,event_type,actor_id,occurred_at,reason,source_reference,backup_handling_reference,sensitive_data_handling_reference
                from nite_connect.justification_attachment_retention_events where policy_id=? order by sequence
                """, POLICY_EVENT, policyId);
    }
    public RetentionPolicyView policyView(UUID id) {
        RetentionPolicy p = policy(id);
        return new RetentionPolicyView(p.id(), p.version(), p.retentionMonths(), p.deletionGraceDays(), p.anchorRule(),
                p.createdBy(), p.createdAt(), p.creationReason(), policyStatus(id), policyEvents(id));
    }
    public void createPolicy(RetentionPolicy p) {
        jdbc.update("""
                insert into nite_connect.justification_attachment_retention_policies(
                  id,version,retention_months,deletion_grace_days,anchor_rule,created_by,created_at,creation_reason,supersedes_id)
                values (?,?,?,?,?,?,?,?,?)
                """, p.id(), p.version(), p.retentionMonths(), p.deletionGraceDays(), p.anchorRule(), p.createdBy(),
                Timestamp.from(p.createdAt()), p.creationReason(), p.supersedesId());
    }
    public void appendPolicyEvent(UUID id, UUID policyId, String type, UUID actor, Instant at, String reason,
                                  String sourceReference, String backupReference, String sensitiveReference) {
        jdbc.update("""
                insert into nite_connect.justification_attachment_retention_events(
                  id,policy_id,event_type,actor_id,occurred_at,reason,source_reference,backup_handling_reference,sensitive_data_handling_reference)
                values (?,?,?,?,?,?,?,?,?)
                """, id, policyId, type, actor, Timestamp.from(at), reason, sourceReference, backupReference, sensitiveReference);
    }
    public PageResult<RetentionPolicyView> policyHistory(int page, int size) {
        PageResult.validate(page, size);
        long total = jdbc.queryForObject("select count(*) from nite_connect.justification_attachment_retention_policies", Long.class);
        List<UUID> ids = jdbc.query("select id from nite_connect.justification_attachment_retention_policies order by version desc,id limit ? offset ?",
                (rs, row) -> rs.getObject(1, UUID.class), size, (long) page * size);
        List<RetentionPolicyView> rows = ids.stream().map(this::policyView).toList();
        return new PageResult<>(rows, page, size, total);
    }

    public boolean justificationExists(UUID justificationId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from nite_connect.absence_justifications where id=?)",
                Boolean.class, justificationId));
    }
    public boolean justificationOwned(UUID justificationId, UUID memberId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from nite_connect.absence_justifications where id=? and member_id=?)",
                Boolean.class, justificationId, memberId));
    }
    public String justificationStatus(UUID id) {
        return jdbc.query("select status from nite_connect.absence_justifications where id=?", (rs, row) -> rs.getString(1), id)
                .stream().findFirst().orElseThrow(ApiException::notFound);
    }
    public Instant justificationDecidedAt(UUID id) {
        List<Instant> decidedAt = jdbc.query("select decided_at from nite_connect.absence_justifications where id=?", (rs, row) -> {
            Timestamp timestamp = rs.getTimestamp(1);
            return timestamp == null ? null : timestamp.toInstant();
        }, id);
        if (decidedAt.isEmpty()) throw ApiException.notFound();
        return decidedAt.getFirst();
    }
    public int nextClassificationVersion(UUID justificationId) {
        return jdbc.queryForObject("select coalesce(max(version),0)+1 from nite_connect.absence_justification_retention_classifications where justification_id=?",
                Integer.class, justificationId);
    }
    public void classify(UUID justificationId, int version, LocalDate semesterEndOn, String sourceReference,
                         String reason, UUID actor, Instant at) {
        jdbc.update("""
                insert into nite_connect.absence_justification_retention_classifications(
                  id,justification_id,version,semester_end_on,source_reference,reason,created_by,created_at)
                values (?,?,?,?,?,?,?,?)
                """, UUID.randomUUID(), justificationId, version, semesterEndOn, sourceReference, reason, actor, Timestamp.from(at));
    }
    public Optional<Classification> classification(UUID justificationId) {
        return jdbc.query("""
                select * from nite_connect.absence_justification_retention_classifications
                where justification_id=? order by version desc limit 1
                """, CLASSIFICATION, justificationId).stream().findFirst();
    }

    public Attachment beginUpload(UUID id, UUID justificationId, UUID memberId, UUID policyId, Context context,
                                  String safeFilename, long reservedSizeBytes, UUID actor, Instant at) {
        jdbc.update("""
                insert into nite_connect.absence_justification_attachments(
                  id,justification_id,member_id,retention_policy_id,object_key,context,filename,size_bytes,state,uploaded_by,created_at)
                values (?,?,?,?,?,?,?,?, 'PENDENTE',?,?)
                """, id, justificationId, memberId, policyId, id, context.name(), safeFilename, reservedSizeBytes,
                actor, Timestamp.from(at));
        attachmentEvent(id, actor, "UPLOAD_INICIADO", at, null);
        return attachment(id);
    }
    public List<Attachment> pendingUploads(UUID memberId) {
        if (memberId == null) {
            return jdbc.query("select * from nite_connect.absence_justification_attachments where state='PENDENTE' order by created_at,id",
                    ATTACHMENT);
        }
        return jdbc.query("select * from nite_connect.absence_justification_attachments where member_id=? and state='PENDENTE' order by created_at,id",
                ATTACHMENT, memberId);
    }
    public boolean hasPendingDeletionForJustification(UUID justificationId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(select 1 from nite_connect.absence_justification_attachments
                  where justification_id=? and state='EXCLUSAO_PENDENTE')
                """, Boolean.class, justificationId));
    }
    public void makeAvailable(UUID id, UUID actor, String filename, String contentType, long size, String sha256, Instant at) {
        jdbc.update("""
                update nite_connect.absence_justification_attachments
                set state='DISPONIVEL',filename=?,content_type=?,size_bytes=?,sha256=?,version=version+1
                where id=? and state='PENDENTE'
                """, filename, contentType, size, sha256, id);
        attachmentEvent(id, actor, "UPLOAD_DISPONIVEL", at, null);
    }
    public boolean failUpload(UUID id, UUID actor, Instant at) {
        int changed = jdbc.update("""
                update nite_connect.absence_justification_attachments set state='FALHA',size_bytes=null,version=version+1
                where id=? and state='PENDENTE'
                """, id);
        if (changed == 1) attachmentEvent(id, actor, "UPLOAD_FALHOU", at, null);
        return changed == 1;
    }
    public Attachment attachment(UUID id) {
        return jdbc.query("select * from nite_connect.absence_justification_attachments where id=?", ATTACHMENT, id)
                .stream().findFirst().orElseThrow(ApiException::notFound);
    }
    public List<Attachment> attachments(UUID justificationId) {
        return jdbc.query("select * from nite_connect.absence_justification_attachments "
                        + "where justification_id=? and state in ('PENDENTE','DISPONIVEL','EXCLUSAO_PENDENTE') order by created_at,id",
                ATTACHMENT, justificationId);
    }
    public AttachmentUsage attachmentUsageForJustification(UUID justificationId) {
        return attachmentUsage("justification_id", justificationId);
    }
    public AttachmentUsage attachmentUsageForMember(UUID memberId) {
        return attachmentUsage("member_id", memberId);
    }
    private AttachmentUsage attachmentUsage(String column, UUID id) {
        if (!column.equals("justification_id") && !column.equals("member_id")) {
            throw new IllegalArgumentException("Coluna de quota não permitida");
        }
        return jdbc.queryForObject("""
                select count(*) as attachment_count,
                  coalesce(sum(case when state='PENDENTE' then coalesce(size_bytes,?) else size_bytes end),0)::bigint as size_bytes
                from nite_connect.absence_justification_attachments
                where %s=? and state in ('PENDENTE','DISPONIVEL','EXCLUSAO_PENDENTE')
                """.formatted(column),
                (rs, row) -> new AttachmentUsage(rs.getLong("attachment_count"), rs.getLong("size_bytes")),
                JustificationAttachmentRules.MAX_BYTES_PER_ATTACHMENT, id);
    }
    public void attachmentEvent(UUID attachmentId, UUID actor, String type, Instant at, String reason) {
        jdbc.update("""
                insert into nite_connect.absence_justification_attachment_events(id,attachment_id,event_type,actor_id,occurred_at,reason)
                values (?,?,?,?,?,?)
                """, UUID.randomUUID(), attachmentId, type, actor, Timestamp.from(at), reason);
    }
    public void markDeletePending(Attachment a, UUID actor, String reason, Instant at) {
        int changed = jdbc.update("""
                update nite_connect.absence_justification_attachments set state='EXCLUSAO_PENDENTE',version=version+1
                where id=? and version=? and state='DISPONIVEL'
                """, a.id(), a.version());
        if (changed != 1) throw ApiException.conflict("Comprovante alterado; consulte novamente");
        attachmentEvent(a.id(), actor, "EXCLUSAO_INICIADA", at, reason);
    }
    public void restoreAfterDeleteFailure(Attachment a, UUID actor, String reason, Instant at) {
        jdbc.update("update nite_connect.absence_justification_attachments set state='DISPONIVEL',version=version+1 where id=? and state='EXCLUSAO_PENDENTE'", a.id());
        attachmentEvent(a.id(), actor, "EXCLUSAO_FALHOU", at, reason);
    }
    public void finishDelete(Attachment a, UUID actor, String reason, Instant at) {
        int changed = jdbc.update("""
                update nite_connect.absence_justification_attachments
                set state='REMOVIDO',object_key=null,filename=null,content_type=null,size_bytes=null,sha256=null,deleted_at=?,version=version+1
                where id=? and state='EXCLUSAO_PENDENTE'
                """, Timestamp.from(at), a.id());
        if (changed != 1) throw ApiException.conflict("Comprovante alterado; consulte novamente");
        attachmentEvent(a.id(), actor, "EXCLUIDO", at, reason);
    }
    public boolean hasActiveHold(UUID attachmentId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(select 1 from nite_connect.absence_justification_attachment_retention_holds h
                  where h.attachment_id=? and h.event_type='APLICADO' and not exists(
                    select 1 from nite_connect.absence_justification_attachment_retention_holds r
                    where r.hold_id=h.hold_id and r.event_type='LIBERADO'))
                """, Boolean.class, attachmentId));
    }
    public Hold createHold(UUID attachmentId, String basis, String reason, UUID actor, Instant at) {
        UUID holdId = UUID.randomUUID();
        insertHoldEvent(holdId, attachmentId, "APLICADO", actor, at, basis, reason);
        return new Hold(holdId, attachmentId, basis, reason, actor, at);
    }
    public Hold hold(UUID attachmentId, UUID holdId) {
        return jdbc.query("""
                select h.hold_id,h.attachment_id,h.basis,h.reason,h.actor_id,h.occurred_at
                from nite_connect.absence_justification_attachment_retention_holds h
                where h.attachment_id=? and h.hold_id=? and h.event_type='APLICADO'
                  and not exists(select 1 from nite_connect.absence_justification_attachment_retention_holds r
                    where r.hold_id=h.hold_id and r.event_type='LIBERADO')
                order by h.sequence limit 1
                """, (rs, row) -> new Hold(rs.getObject("hold_id", UUID.class), rs.getObject("attachment_id", UUID.class),
                rs.getString("basis"), rs.getString("reason"), rs.getObject("actor_id", UUID.class), rs.getTimestamp("occurred_at").toInstant()),
                attachmentId, holdId).stream().findFirst().orElseThrow(ApiException::notFound);
    }

    public List<RetentionHoldEventView> holdHistory(UUID attachmentId) {
        return jdbc.query("""
                select event_id,hold_id,attachment_id,event_type,actor_id,occurred_at,basis,reason
                from nite_connect.absence_justification_attachment_retention_holds
                where attachment_id=? order by sequence,event_id
                """, (rs, row) -> new RetentionHoldEventView(rs.getObject("event_id", UUID.class),
                rs.getObject("hold_id", UUID.class), rs.getObject("attachment_id", UUID.class),
                rs.getString("event_type"), rs.getObject("actor_id", UUID.class),
                rs.getTimestamp("occurred_at").toInstant(), rs.getString("basis"), rs.getString("reason")), attachmentId);
    }
    public void releaseHold(UUID holdId, UUID attachmentId, UUID actor, String reason, Instant at) {
        insertHoldEvent(holdId, attachmentId, "LIBERADO", actor, at, null, reason);
    }
    private void insertHoldEvent(UUID holdId, UUID attachmentId, String type, UUID actor, Instant at, String basis, String reason) {
        jdbc.update("""
                insert into nite_connect.absence_justification_attachment_retention_holds(
                  event_id,hold_id,attachment_id,event_type,actor_id,occurred_at,basis,reason)
                values (?,?,?,?,?,?,?,?)
                """, UUID.randomUUID(), holdId, attachmentId, type, actor, Timestamp.from(at), basis, reason);
    }
}
