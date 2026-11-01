package br.edu.unijorge.niteconnect.attendance.infrastructure;

import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentPurgeContracts.Item;
import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentPurgeContracts.RunDetail;
import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentPurgeContracts.RunSummary;
import br.edu.unijorge.niteconnect.attendance.domain.JustificationAttachmentRules;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentRepository.Attachment;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JustificationAttachmentPurgeRepository {
    private static final UUID FIRST_UUID = new UUID(0L, 0L);
    public record PurgeCandidate(Attachment attachment, LocalDate retentionExpiresOn, LocalDate deletionDeadlineOn) {}
    private static final RowMapper<Attachment> ATTACHMENT = (rs, row) -> new Attachment(
            rs.getObject("id", UUID.class), rs.getObject("justification_id", UUID.class),
            rs.getObject("member_id", UUID.class), rs.getObject("retention_policy_id", UUID.class),
            rs.getObject("object_key", UUID.class),
            br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentContracts.Context.valueOf(rs.getString("context")),
            rs.getString("filename"), rs.getString("content_type"), rs.getObject("size_bytes", Long.class),
            rs.getString("sha256"), rs.getString("state"), rs.getObject("uploaded_by", UUID.class),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("deleted_at") == null ? null : rs.getTimestamp("deleted_at").toInstant(), rs.getLong("version"));
    private static final RowMapper<PurgeCandidate> CANDIDATE = (rs, row) -> new PurgeCandidate(
            ATTACHMENT.mapRow(rs, row), rs.getObject("retention_expires_on", LocalDate.class),
            rs.getObject("deletion_deadline_on", LocalDate.class));
    private static final RowMapper<RunSummary> SUMMARY = (rs, row) -> new RunSummary(
            rs.getObject("id", UUID.class), rs.getString("mode"), rs.getString("status"), rs.getString("result_code"),
            rs.getObject("retention_policy_id", UUID.class), rs.getObject("requested_by", UUID.class),
            rs.getTimestamp("requested_at").toInstant(),
            rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
            rs.getString("reason"), rs.getInt("candidate_count"), rs.getInt("skipped_count"),
            rs.getInt("deleted_count"), rs.getInt("failed_count"));
    private static final String SELECT_ELIGIBLE = """
            select a.*,
              retention_dates.retention_expires_on,retention_dates.deletion_deadline_on
            from nite_connect.absence_justification_attachments a
            join nite_connect.absence_justifications j on j.id=a.justification_id
            join lateral (
              select c.semester_end_on from nite_connect.absence_justification_retention_classifications c
              where c.justification_id=j.id order by c.version desc limit 1
            ) classification on true
            join nite_connect.justification_attachment_retention_policies p on p.id=a.retention_policy_id
            join lateral (
              select greatest((j.decided_at at time zone ?)::date,classification.semester_end_on) as base_on
            ) retention_anchor on true
            join lateral (
              select (retention_anchor.base_on + p.retention_months * interval '1 month')::date as retention_expires_on,
                (retention_anchor.base_on + p.retention_months * interval '1 month'
                    + greatest(p.deletion_grace_days, %d) * interval '1 day')::date as deletion_deadline_on
            ) retention_dates on true
            where a.state in ('DISPONIVEL','EXCLUSAO_PENDENTE')
              and j.status in ('ACEITA','RECUSADA') and j.decided_at is not null
              and exists(select 1 from nite_connect.justification_attachment_retention_events e
                         where e.policy_id=a.retention_policy_id and e.event_type='VALIDADA')
              and not exists(
                select 1 from nite_connect.absence_justification_attachment_retention_holds h
                where h.attachment_id=a.id and h.event_type='APLICADO'
                  and not exists(select 1 from nite_connect.absence_justification_attachment_retention_holds r
                    where r.hold_id=h.hold_id and r.event_type='LIBERADO')
              )
              and retention_dates.deletion_deadline_on <= ?
            """.formatted(JustificationAttachmentRules.MINIMUM_DELETION_GRACE_DAYS);

    private final JdbcTemplate jdbc;
    public JustificationAttachmentPurgeRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public UUID createRun(String mode, UUID policyId, UUID actor, Instant startedAt, String reason) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into nite_connect.justification_attachment_retention_purge_runs(
                  id,mode,status,retention_policy_id,requested_by,requested_at,reason)
                values (?,?,'INICIADA',?,?,?,?)
                """, id, mode, policyId, actor, Timestamp.from(startedAt), reason);
        appendRunEvent(id, "INICIADO", actor, startedAt, reason, null, 0, 0, 0, 0);
        return id;
    }

    public void finishRun(UUID id, String status, String resultCode, Instant finishedAt,
                          String reason, int candidates, int skipped, int deleted, int failed, UUID actor) {
        int changed = jdbc.update("""
                update nite_connect.justification_attachment_retention_purge_runs
                set status=?,result_code=?,completed_at=?,candidate_count=?,skipped_count=?,deleted_count=?,failed_count=?
                where id=? and status='INICIADA'
                """, status, resultCode, Timestamp.from(finishedAt), candidates, skipped, deleted, failed, id);
        if (changed != 1) throw ApiException.conflict("Execução de expurgo alterada; consulte novamente");
        String event = switch (status) {
            case "BLOQUEADA" -> "BLOQUEADO";
            case "SIMULACAO" -> "SIMULACAO_CONCLUIDA";
            case "CONCLUIDA" -> "EXECUCAO_CONCLUIDA";
            case "CONCLUIDA_COM_FALHAS" -> "EXECUCAO_COM_FALHAS";
            default -> "FALHA";
        };
        appendRunEvent(id, event, actor, finishedAt, reason, resultCode, candidates, skipped, deleted, failed);
    }

    public void appendRunEvent(UUID runId, String event, UUID actor, Instant at, String reason,
                               String resultCode, int candidates, int skipped, int deleted, int failed) {
        jdbc.update("""
                insert into nite_connect.justification_attachment_retention_purge_run_events(
                  id,run_id,event_type,actor_id,occurred_at,reason,result_code,candidate_count,skipped_count,deleted_count,failed_count)
                values (?,?,?,?,?,?,?,?,?,?,?)
                """, UUID.randomUUID(), runId, event, actor, Timestamp.from(at), reason, resultCode, candidates, skipped, deleted, failed);
    }

    public void recoverInterruptedRuns(Instant at) {
        List<RunSummary> interrupted = jdbc.query("""
                select * from nite_connect.justification_attachment_retention_purge_runs
                where status='INICIADA' order by requested_at,id for update
                """, SUMMARY);
        for (RunSummary run : interrupted) {
            int changed = jdbc.update("""
                    update nite_connect.justification_attachment_retention_purge_runs
                    set status='FALHA',result_code='EXECUCAO_INTERROMPIDA',completed_at=?
                    where id=? and status='INICIADA'
                    """, Timestamp.from(at), run.id());
            if (changed == 1) appendRunEvent(run.id(), "FALHA", null, at,
                    "Execução interrompida identificada por uma nova tentativa", "EXECUCAO_INTERROMPIDA",
                    run.candidateCount(), run.skippedCount(), run.deletedCount(), run.failedCount());
        }
    }

    public List<PurgeCandidate> eligibleBatch(LocalDate today, String zone, UUID afterId, int limit) {
        return jdbc.query(SELECT_ELIGIBLE + " and a.id > ? order by a.id limit ?", CANDIDATE,
                zone, today, afterId == null ? FIRST_UUID : afterId, limit);
    }

    public Optional<PurgeCandidate> eligible(UUID id, LocalDate today, String zone) {
        return jdbc.query(SELECT_ELIGIBLE + " and a.id=?", CANDIDATE, zone, today, id)
                .stream().findFirst();
    }

    public Optional<PurgeCandidate> claim(UUID id, UUID runId, LocalDate today, String zone, Instant at) {
        Optional<PurgeCandidate> current = eligible(id, today, zone);
        if (current.isEmpty()) return Optional.empty();
        PurgeCandidate candidate = current.orElseThrow();
        Attachment attachment = candidate.attachment();
        if (attachment.state().equals("DISPONIVEL")) {
            int changed = jdbc.update("""
                    update nite_connect.absence_justification_attachments
                    set state='EXCLUSAO_PENDENTE',version=version+1
                    where id=? and version=? and state='DISPONIVEL'
                    """, id, attachment.version());
            if (changed != 1) return Optional.empty();
        }
        appendAttachmentPurgeEvent(id, runId, "EXCLUSAO_INICIADA", at, "Expurgo de retenção");
        return eligible(id, today, zone);
    }

    public void restoreAfterFailure(Attachment attachment, UUID runId, Instant at) {
        jdbc.update("""
                update nite_connect.absence_justification_attachments
                set state='DISPONIVEL',version=version+1
                where id=? and state='EXCLUSAO_PENDENTE'
                """, attachment.id());
        appendAttachmentPurgeEvent(attachment.id(), runId, "EXCLUSAO_FALHOU", at, "Falha temporária de armazenamento");
    }

    public void finishDelete(Attachment attachment, UUID runId, Instant at) {
        int changed = jdbc.update("""
                update nite_connect.absence_justification_attachments
                set state='REMOVIDO',object_key=null,filename=null,content_type=null,size_bytes=null,sha256=null,
                    deleted_at=?,version=version+1
                where id=? and state='EXCLUSAO_PENDENTE'
                """, Timestamp.from(at), attachment.id());
        if (changed != 1) throw ApiException.conflict("Comprovante alterado durante o descarte");
        appendAttachmentPurgeEvent(attachment.id(), runId, "EXCLUIDO", at, "Expurgo de retenção concluído");
    }

    private void appendAttachmentPurgeEvent(UUID attachmentId, UUID runId, String event, Instant at, String reason) {
        jdbc.update("""
                insert into nite_connect.absence_justification_attachment_events(
                  id,attachment_id,event_type,actor_id,occurred_at,reason,retention_purge_run_id)
                values (?,?,?,null,?,?,?)
                """, UUID.randomUUID(), attachmentId, event, Timestamp.from(at), reason, runId);
    }

    private Attachment attachment(UUID id) {
        return jdbc.query("select * from nite_connect.absence_justification_attachments where id=?", ATTACHMENT, id)
                .stream().findFirst().orElseThrow(ApiException::notFound);
    }

    public void recordItem(UUID runId, UUID attachmentId, String outcome, String resultCode,
                           LocalDate retentionExpiresOn, LocalDate deletionDeadlineOn, Instant at) {
        jdbc.update("""
                insert into nite_connect.justification_attachment_retention_purge_items(
                  id,run_id,attachment_id,outcome,result_code,retention_expires_on,deletion_deadline_on,occurred_at)
                values (?,?,?,?,?,?,?,?)
                """, UUID.randomUUID(), runId, attachmentId, outcome, resultCode, retentionExpiresOn,
                deletionDeadlineOn, Timestamp.from(at));
    }

    public RunSummary summary(UUID id) {
        return jdbc.query("select * from nite_connect.justification_attachment_retention_purge_runs where id=?", SUMMARY, id)
                .stream().findFirst().orElseThrow(ApiException::notFound);
    }

    public RunDetail detail(UUID id) {
        RunSummary run = summary(id);
        List<Item> items = jdbc.query("""
                select attachment_id,outcome,result_code,retention_expires_on,deletion_deadline_on,occurred_at
                from nite_connect.justification_attachment_retention_purge_items where run_id=?
                order by occurred_at,attachment_id
                """, (rs, row) -> new Item(rs.getObject("attachment_id", UUID.class), rs.getString("outcome"),
                rs.getString("result_code"), rs.getObject("retention_expires_on", LocalDate.class),
                rs.getObject("deletion_deadline_on", LocalDate.class), rs.getTimestamp("occurred_at").toInstant()), id);
        return new RunDetail(run.id(), run.mode(), run.status(), run.resultCode(), run.retentionPolicyId(),
                run.requestedBy(), run.requestedAt(), run.completedAt(), run.reason(), run.candidateCount(), run.skippedCount(),
                run.deletedCount(), run.failedCount(), items);
    }

    public PageResult<RunSummary> history(int page, int size) {
        PageResult.validate(page, size);
        long total = jdbc.queryForObject("select count(*) from nite_connect.justification_attachment_retention_purge_runs", Long.class);
        List<RunSummary> runs = jdbc.query("""
                select * from nite_connect.justification_attachment_retention_purge_runs
                order by requested_at desc,id limit ? offset ?
                """, SUMMARY, size, (long) page * size);
        return new PageResult<>(runs, page, size, total);
    }
}
