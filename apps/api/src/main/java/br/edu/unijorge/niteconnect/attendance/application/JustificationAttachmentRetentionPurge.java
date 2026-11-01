package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentPurgeContracts.Command;
import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentPurgeContracts.Mode;
import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentPurgeContracts.RunDetail;
import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentPurgeContracts.RunSummary;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentPurgeRepository;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentPurgeRepository.*;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentPurgeRepository.PurgeCandidate;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentRepository;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentRepository.Attachment;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentRepository.RetentionPolicy;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentOperationLocks;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Audited, repeat-safe retention sweep. It does not alter participation or justification records. */
@Service
public class JustificationAttachmentRetentionPurge {
    private static final int BATCH_SIZE = 200;
    private static final String SYSTEM_REASON = "Expurgo técnico agendado";

    private final AccessService access;
    private final JustificationAttachmentRepository attachments;
    private final JustificationAttachmentPurgeRepository purge;
    private final JustificationAttachmentStore store;
    private final AdministrativeTransactions lock;
    private final TransactionTemplate transactions;
    private final JustificationAttachmentOperationLocks operationLocks;
    private final Clock clock;
    @Value("${nite.justification.storage.approved:false}")
    private boolean storageApproved;
    @Value("${nite.justification.retention.purge.execution-enabled:false}")
    private boolean executionEnabled;

    public JustificationAttachmentRetentionPurge(AccessService access, JustificationAttachmentRepository attachments,
            JustificationAttachmentPurgeRepository purge, JustificationAttachmentStore store,
            AdministrativeTransactions lock, PlatformTransactionManager transactionManager,
            JustificationAttachmentOperationLocks operationLocks, Clock clock) {
        this.access = access;
        this.attachments = attachments;
        this.purge = purge;
        this.store = store;
        this.lock = lock;
        this.transactions = new TransactionTemplate(transactionManager);
        this.operationLocks = operationLocks;
        this.clock = clock;
    }

    public RunDetail run(UUID actor, Command command) {
        access.requireAdministrator(actor);
        if (command.mode() == Mode.EXECUTE && !Boolean.TRUE.equals(command.confirmed())) {
            throw ApiException.invalid("Confirmação explícita obrigatória para executar o expurgo");
        }
        return withPurgeLock(actor, command.mode().name(),
                () -> runLocked(actor, command.mode().name(), command.mode() == Mode.DRY_RUN, command.reason().strip()));
    }

    public RunDetail runScheduled(boolean dryRun) {
        return withPurgeLock(null, "SCHEDULED", () -> runLocked(null, "SCHEDULED", dryRun, SYSTEM_REASON));
    }

    public PageResult<RunSummary> history(UUID actor, int page, int size) {
        access.requireAdministrator(actor);
        return purge.history(page, size);
    }

    public RunDetail detail(UUID actor, UUID runId) {
        access.requireAdministrator(actor);
        return purge.detail(runId);
    }

    private RunDetail runLocked(UUID actor, String mode, boolean dryRun, String reason) {
        Optional<RetentionPolicy> policy = attachments.activeRetentionPolicy();
        Instant startedAt = clock.instant();
        UUID runId = transactions.execute(status -> {
            lock.lock();
            purge.recoverInterruptedRuns(startedAt);
            return purge.createRun(mode, policy.map(RetentionPolicy::id).orElse(null), actor, startedAt, reason);
        });
        if (runId == null) throw new IllegalStateException("Não foi possível registrar a execução de expurgo");

        String blockedCode = blockedCode(dryRun, policy.isPresent());
        if (blockedCode != null) {
            finish(runId, actor, "BLOQUEADA", blockedCode, reason, 0, 0, 0, 0);
            return purge.detail(runId);
        }

        int candidates = 0;
        int skipped = 0;
        int deleted = 0;
        int failed = 0;
        UUID cursor = null;
        try {
            while (true) {
                List<PurgeCandidate> batch = purge.eligibleBatch(LocalDate.now(clock), clock.getZone().getId(), cursor, BATCH_SIZE);
                if (batch.isEmpty()) break;
                cursor = batch.getLast().attachment().id();
                for (PurgeCandidate candidate : batch) {
                    Attachment candidateAttachment = candidate.attachment();
                    candidates++;
                    if (dryRun) {
                        recordItem(runId, candidateAttachment.id(), "ELEGIVEL", "RETENCAO_VENCIDA",
                                candidate.retentionExpiresOn(), candidate.deletionDeadlineOn(), clock.instant());
                        continue;
                    }
                    Optional<PurgeCandidate> claimed = transactions.execute(status -> {
                        lock.lock();
                        return purge.claim(candidateAttachment.id(), runId, LocalDate.now(clock), clock.getZone().getId(), clock.instant());
                    });
                    if (claimed.isEmpty()) {
                        skipped++;
                        recordItem(runId, candidateAttachment.id(), "IGNORADO", "ALTERADO_OU_RETIDO",
                                candidate.retentionExpiresOn(), candidate.deletionDeadlineOn(), clock.instant());
                        continue;
                    }
                    PurgeCandidate claimedCandidate = claimed.orElseThrow();
                    Attachment deleting = claimedCandidate.attachment();
                    try {
                        store.delete(deleting.objectKey());
                    } catch (RuntimeException storageFailure) {
                        failed++;
                        transactions.executeWithoutResult(status -> {
                            lock.lock();
                            purge.restoreAfterFailure(deleting, runId, clock.instant());
                            purge.recordItem(runId, deleting.id(), "FALHA", "ARMAZENAMENTO_INDISPONIVEL",
                                    claimedCandidate.retentionExpiresOn(), claimedCandidate.deletionDeadlineOn(), clock.instant());
                        });
                        continue;
                    }
                    try {
                        transactions.executeWithoutResult(status -> {
                            lock.lock();
                            purge.finishDelete(deleting, runId, clock.instant());
                            purge.recordItem(runId, deleting.id(), "EXCLUIDO", "DESCARTE_CONCLUIDO",
                                    claimedCandidate.retentionExpiresOn(), claimedCandidate.deletionDeadlineOn(), clock.instant());
                        });
                        deleted++;
                    } catch (RuntimeException finalizationFailure) {
                        failed++;
                        transactions.executeWithoutResult(status -> purge.recordItem(runId, deleting.id(), "FALHA",
                                "FINALIZACAO_PENDENTE_REPROCESSAVEL", claimedCandidate.retentionExpiresOn(),
                                claimedCandidate.deletionDeadlineOn(), clock.instant()));
                    }
                }
                if (batch.size() < BATCH_SIZE) break;
            }
        } catch (RuntimeException failure) {
            finish(runId, actor, "FALHA", "EXECUCAO_INTERROMPIDA", reason, candidates, skipped, deleted, failed);
            return purge.detail(runId);
        }
        String finalStatus = failed == 0 ? "CONCLUIDA" : "CONCLUIDA_COM_FALHAS";
        finish(runId, actor, dryRun ? "SIMULACAO" : finalStatus,
                failed == 0 ? null : "ITENS_REPROCESSAVEIS", reason, candidates, skipped, deleted, failed);
        return purge.detail(runId);
    }

    private String blockedCode(boolean dryRun, boolean hasValidatedPolicy) {
        if (!hasValidatedPolicy) return "POLITICA_NAO_VALIDADA";
        if (dryRun) return null;
        if (!executionEnabled) return "EXPURGO_DESABILITADO";
        if (!storageApproved || !store.enabled()) return "ARMAZENAMENTO_NAO_APROVADO";
        return null;
    }

    private void finish(UUID id, UUID actor, String status, String code, String reason,
                        int candidates, int skipped, int deleted, int failed) {
        transactions.executeWithoutResult(tx -> {
            lock.lock();
            purge.finishRun(id, status, code, clock.instant(), reason, candidates, skipped, deleted, failed, actor);
        });
    }

    private void recordItem(UUID runId, UUID attachmentId, String outcome, String code,
                            LocalDate retentionExpiresOn, LocalDate deletionDeadlineOn, Instant at) {
        transactions.executeWithoutResult(tx -> purge.recordItem(runId, attachmentId, outcome, code,
                retentionExpiresOn, deletionDeadlineOn, at));
    }

    private RunDetail withPurgeLock(UUID actor, String mode, java.util.function.Supplier<RunDetail> operation) {
        return operationLocks.tryWithKey(JustificationAttachmentOperationLocks.RETENTION_PURGE_KEY, operation)
                .orElseGet(() -> runBlocked(actor, mode,
                        "Expurgo já está em execução; solicitação registrada como bloqueada", "EXPURGO_EM_EXECUCAO"));
    }

    private RunDetail runBlocked(UUID actor, String mode, String reason, String code) {
        Instant started = clock.instant();
        UUID id = transactions.execute(status -> {
            lock.lock();
            return purge.createRun(mode, null, actor, started, reason);
        });
        if (id == null) throw new IllegalStateException("Não foi possível registrar a execução bloqueada");
        finish(id, actor, "BLOQUEADA", code, reason, 0, 0, 0, 0);
        return purge.detail(id);
    }

}
