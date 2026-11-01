package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentContracts.*;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentRepository;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentRepository.Attachment;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentRepository.Classification;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentRepository.RetentionPolicy;
import br.edu.unijorge.niteconnect.attendance.domain.JustificationAttachmentRules;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentOperationLocks;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** B04 private attachment flow. It has an independent object key and metadata from B03 activity evidence. */
@Service
public class JustificationAttachmentService {
    private static final String ANCHOR_RULE = "MAX_ANALYSIS_AND_SEMESTER_END";
    private static final Logger log = LoggerFactory.getLogger(JustificationAttachmentService.class);
    private final AccessService access;
    private final JustificationAttachmentRepository repo;
    private final JustificationAttachmentStore store;
    private final AdministrativeTransactions lock;
    private final JustificationAttachmentOperationLocks operationLocks;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final Set<UUID> retentionValidators;
    @Value("${nite.justification.storage.approved:false}")
    private boolean storageApproved;

    public JustificationAttachmentService(AccessService access, JustificationAttachmentRepository repo,
            JustificationAttachmentStore store, AdministrativeTransactions lock, PlatformTransactionManager txManager,
            JustificationAttachmentOperationLocks operationLocks, Clock clock,
            @Value("${nite.justification.retention.authorized-validator-ids:}") String validatorIds) {
        this.access = access;
        this.repo = repo;
        this.store = store;
        this.lock = lock;
        this.operationLocks = operationLocks;
        this.transactions = new TransactionTemplate(txManager);
        this.clock = clock;
        try {
            this.retentionValidators = Arrays.stream(validatorIds.split(","))
                    .map(String::strip).filter(value -> !value.isEmpty()).map(UUID::fromString).collect(Collectors.toUnmodifiableSet());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JUSTIFICATION_RETENTION_AUTHORIZED_VALIDATOR_IDS contém um identificador inválido");
        }
    }

    public record Downloaded(String filename, String contentType, byte[] bytes) {}

    public AttachmentCollection attachments(UUID actor, UUID justificationId, boolean administrative) {
        access.requireActive(actor);
        authorizeJustification(actor, justificationId, administrative);
        String availability = uploadAvailability();
        List<AttachmentView> items = repo.attachments(justificationId).stream().map(this::view).toList();
        return new AttachmentCollection(availability, items);
    }

    public AttachmentView upload(UUID actor, UUID justificationId, Context context,
                                 String filename, String contentType, long sizeBytes, InputStream input) {
        access.requireActive(actor);
        if (context == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Informe o contexto do comprovante");
        if (sizeBytes > JustificationAttachmentRules.MAX_BYTES_PER_ATTACHMENT) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Comprovante excede 5.000.000 bytes");
        }
        if (sizeBytes <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Tamanho do comprovante inválido");
        authorizeJustification(actor, justificationId, false);
        ensureUploadAvailable();
        String safeName = br.edu.unijorge.niteconnect.attendance.infrastructure.LocalJustificationAttachmentStore.safeName(filename);
        UUID attachmentId = UUID.randomUUID();
        return operationLocks.withNamedLock(uploadLockName(attachmentId), () -> uploadLocked(
                actor, justificationId, context, filename, contentType, sizeBytes, input, safeName, attachmentId));
    }

    private AttachmentView uploadLocked(UUID actor, UUID justificationId, Context context,
            String filename, String contentType, long sizeBytes, InputStream input, String safeName, UUID attachmentId) {
        access.requireActive(actor);
        authorizeJustification(actor, justificationId, false);
        ensureUploadAvailable();
        recoverInterruptedUploads(actor);
        checkContext(justificationId, context);
        RetentionPolicy policy = repo.activeRetentionPolicy().orElseThrow(this::retentionUnavailable);
        Attachment pending = transactions.execute(status -> {
            lock.lock();
            access.requireActive(actor);
            authorizeJustification(actor, justificationId, false);
            ensureUploadAvailable();
            checkContext(justificationId, context);
            RetentionPolicy currentPolicy = repo.activeRetentionPolicy().orElseThrow(this::retentionUnavailable);
            if (!currentPolicy.id().equals(policy.id())) throw ApiException.conflict("A política de retenção foi alterada; tente novamente");
            requireAttachmentCapacity(justificationId, actor, sizeBytes);
            return repo.beginUpload(attachmentId, justificationId, actor, policy.id(), context, safeName,
                    sizeBytes, actor, clock.instant());
        });
        if (pending == null) throw new IllegalStateException("Não foi possível iniciar o armazenamento do comprovante");

        try {
            JustificationAttachmentStore.Stored stored = store.write(pending.objectKey(), filename, contentType, input);
            if (stored.size() != sizeBytes) throw new ApiException(HttpStatus.BAD_REQUEST, "Tamanho recebido diferente do informado");
            Attachment complete = transactions.execute(status -> {
                lock.lock();
                access.requireActive(actor);
                authorizeJustification(actor, justificationId, false);
                checkContext(justificationId, context);
                repo.makeAvailable(pending.id(), actor, stored.filename(), stored.contentType(), stored.size(), stored.sha256(), clock.instant());
                return repo.attachment(pending.id());
            });
            if (complete == null) throw new IllegalStateException("Não foi possível concluir o armazenamento do comprovante");
            return view(complete);
        } catch (RuntimeException failure) {
            try {
                store.delete(pending.objectKey());
                transactions.executeWithoutResult(status -> {
                    lock.lock();
                    repo.failUpload(pending.id(), actor, clock.instant());
                });
            } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    private void recoverInterruptedUploads(UUID actor) {
        for (Attachment pending : repo.pendingUploads(actor)) {
            try {
                operationLocks.tryWithNamedLock(uploadLockName(pending.id()), () -> {
                    Attachment current = repo.attachment(pending.id());
                    if (!current.state().equals("PENDENTE")) return Boolean.FALSE;
                    store.delete(current.objectKey());
                    transactions.executeWithoutResult(status -> {
                        lock.lock();
                        Attachment latest = repo.attachment(current.id());
                        if (latest.state().equals("PENDENTE")) {
                            repo.failUpload(current.id(), current.uploadedBy(), clock.instant());
                        }
                    });
                    return Boolean.TRUE;
                });
            } catch (RuntimeException failure) {
                log.warn("Não foi possível recuperar a reserva de upload pendente {}", pending.id(), failure);
            }
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedUploadsAtStartup() {
        if (store.enabled()) recoverInterruptedUploads(null);
    }

    private static String uploadLockName(UUID attachmentId) {
        return "nite-connect:justification-attachment-upload:" + attachmentId;
    }

    public Downloaded download(UUID actor, UUID justificationId, UUID attachmentId, boolean administrative) {
        access.requireActive(actor);
        authorizeJustification(actor, justificationId, administrative);
        Attachment attachment = repo.attachment(attachmentId);
        if (!attachment.justificationId().equals(justificationId)) throw ApiException.notFound();
        if (!attachment.memberId().equals(actor) && !administrative) throw ApiException.notFound();
        if (administrative) access.requireAdministrator(actor);
        if (!attachment.state().equals("DISPONIVEL")) throw ApiException.notFound();
        ensureStorageApproved();
        transactions.executeWithoutResult(status -> repo.attachmentEvent(attachment.id(), actor, "DOWNLOAD_SOLICITADO", clock.instant(), null));
        byte[] bytes = store.read(attachment.objectKey(), attachment.sizeBytes(), attachment.sha256());
        return new Downloaded(attachment.filename(), attachment.contentType(), bytes);
    }

    public PageResult<RetentionPolicyView> retentionPolicies(UUID actor, int page, int size) {
        access.requireAdministrator(actor);
        return repo.policyHistory(page, size);
    }

    public RetentionPolicyView proposeRetentionPolicy(UUID actor, RetentionPolicyProposal request) {
        access.requireAdministrator(actor);
        if (!JustificationAttachmentRules.hasApprovedDeletionGrace(request.deletionGraceDays())) {
            throw ApiException.invalid("A carência mínima para descarte é de 30 dias");
        }
        return transactions.execute(status -> {
            lock.lock();
            if (repo.hasPendingPolicyProposal()) throw ApiException.conflict("Conclua ou revise a proposta de retenção pendente");
            if (!repo.hasValidatedPolicy() && (request.retentionMonths() != 24 || request.deletionGraceDays() != 30)) {
                throw ApiException.invalid("A primeira proposta deve registrar 24 meses de retenção e a janela de 30 dias para descarte");
            }
            RetentionPolicy previous = repo.latestPolicy().orElse(null);
            Instant now = clock.instant();
            RetentionPolicy policy = new RetentionPolicy(UUID.randomUUID(), repo.nextPolicyVersion(), request.retentionMonths(),
                    request.deletionGraceDays(), ANCHOR_RULE, actor, now, request.reason().strip(), previous == null ? null : previous.id());
            repo.createPolicy(policy);
            repo.appendPolicyEvent(UUID.randomUUID(), policy.id(), "CRIADA", actor, now, request.reason().strip(), null, null, null);
            return repo.policyView(policy.id());
        });
    }

    public RetentionPolicyView reviseRetentionPolicy(UUID actor, UUID id, RetentionPolicyRevision request) {
        access.requireAdministrator(actor);
        if (!JustificationAttachmentRules.hasApprovedDeletionGrace(request.deletionGraceDays())) {
            throw ApiException.invalid("A carência mínima para descarte é de 30 dias");
        }
        return transactions.execute(status -> {
            lock.lock();
            RetentionPolicy previous = repo.policy(id);
            if (previous.version() != request.expectedVersion()) throw ApiException.conflict("Proposta alterada; consulte a versão atual");
            if (!repo.policyStatus(id).equals("CRIADA")) throw ApiException.conflict("Somente propostas ainda não validadas podem ser revisadas");
            Instant now = clock.instant();
            RetentionPolicy revision = new RetentionPolicy(UUID.randomUUID(), repo.nextPolicyVersion(), request.retentionMonths(),
                    request.deletionGraceDays(), ANCHOR_RULE, actor, now, request.reason().strip(), previous.id());
            repo.createPolicy(revision);
            repo.appendPolicyEvent(UUID.randomUUID(), revision.id(), "CRIADA", actor, now, request.reason().strip(), null, null, null);
            repo.appendPolicyEvent(UUID.randomUUID(), previous.id(), "SUBSTITUIDA", actor, now,
                    "Substituída pela versão " + revision.version() + ": " + request.reason().strip(), null, null, null);
            return repo.policyView(revision.id());
        });
    }

    public RetentionPolicyView validateRetentionPolicy(UUID actor, UUID id, RetentionPolicyValidation request) {
        access.requireActive(actor);
        if (!retentionValidators.contains(actor)) throw ApiException.forbidden();
        return transactions.execute(status -> {
            lock.lock();
            RetentionPolicy policy = repo.policy(id);
            if (policy.version() != request.expectedVersion()) throw ApiException.conflict("Proposta alterada; consulte a versão atual");
            if (!repo.policyStatus(id).equals("CRIADA")) throw ApiException.conflict("Somente uma proposta pendente pode ser validada");
            Instant now = clock.instant();
            RetentionPolicy previous = repo.activeRetentionPolicy().orElse(null);
            if (previous != null) repo.appendPolicyEvent(UUID.randomUUID(), previous.id(), "SUBSTITUIDA", actor, now,
                    "Substituída pela versão " + policy.version(), null, null, null);
            repo.appendPolicyEvent(UUID.randomUUID(), policy.id(), "VALIDADA", actor, now,
                    request.reason().strip(), request.sourceReference().strip(), request.backupHandlingReference().strip(),
                    request.sensitiveDataHandlingReference().strip());
            return repo.policyView(id);
        });
    }

    public Classification classifyRetention(UUID actor, UUID justificationId, RetentionClassification request) {
        access.requireAdministrator(actor);
        if (!repo.justificationStatus(justificationId).matches("ACEITA|RECUSADA") || repo.justificationDecidedAt(justificationId) == null) {
            throw ApiException.conflict("A retenção exige análise definitivamente encerrada");
        }
        return transactions.execute(status -> {
            lock.lock();
            if (repo.hasPendingDeletionForJustification(justificationId)) {
                throw ApiException.conflict("Há um descarte em andamento para esta justificativa");
            }
            int version = repo.nextClassificationVersion(justificationId);
            repo.classify(justificationId, version, request.semesterEndOn(), request.sourceReference().strip(),
                    request.reason().strip(), actor, clock.instant());
            return repo.classification(justificationId).orElseThrow();
        });
    }

    public RetentionHoldView placeHold(UUID actor, UUID attachmentId, RetentionHold request) {
        access.requireAdministrator(actor);
        return transactions.execute(status -> {
            lock.lock();
            Attachment attachment = repo.attachment(attachmentId);
            if (!attachment.state().equals("DISPONIVEL")) throw ApiException.conflict("Somente comprovantes disponíveis podem receber conservação excepcional");
            var hold = repo.createHold(attachmentId, request.basis().strip(), request.reason().strip(), actor, clock.instant());
            return new RetentionHoldView(hold.id(), hold.attachmentId(), hold.basis(), hold.reason(), hold.actorId(), hold.occurredAt());
        });
    }

    public List<RetentionHoldEventView> retentionHoldHistory(UUID actor, UUID attachmentId) {
        access.requireAdministrator(actor);
        repo.attachment(attachmentId);
        return repo.holdHistory(attachmentId);
    }

    public RetentionHoldView releaseHold(UUID actor, UUID attachmentId, UUID holdId, RetentionHoldRelease request) {
        access.requireAdministrator(actor);
        var hold = repo.hold(attachmentId, holdId);
        Instant now = clock.instant();
        transactions.executeWithoutResult(status -> {
            lock.lock();
            repo.releaseHold(holdId, attachmentId, actor, request.reason().strip(), now);
        });
        return new RetentionHoldView(hold.id(), hold.attachmentId(), hold.basis(), request.reason().strip(), actor, now);
    }

    public AttachmentView delete(UUID actor, UUID attachmentId, DeleteAttachment request) {
        access.requireAdministrator(actor);
        ensureStorageApproved();
        if (!Boolean.TRUE.equals(request.confirmed())) throw ApiException.invalid("Confirmação explícita obrigatória");
        return operationLocks.tryWithKey(JustificationAttachmentOperationLocks.RETENTION_PURGE_KEY,
                () -> deleteLocked(actor, attachmentId, request))
                .orElseThrow(() -> ApiException.conflict("Há um expurgo ou descarte em execução; tente novamente"));
    }

    private AttachmentView deleteLocked(UUID actor, UUID attachmentId, DeleteAttachment request) {
        Attachment attachment = repo.attachment(attachmentId);
        if (attachment.version() != request.expectedVersion()) throw ApiException.conflict("Comprovante alterado; consulte novamente");
        if (!attachment.state().equals("DISPONIVEL")) throw ApiException.conflict("Comprovante não está disponível para descarte");
        if (repo.hasActiveHold(attachmentId)) throw ApiException.conflict("Há conservação excepcional ativa para este comprovante");
        if (!repo.wasPolicyValidated(attachment.retentionPolicyId())) throw ApiException.conflict("Política de retenção não validada");
        Classification classification = repo.classification(attachment.justificationId())
                .orElseThrow(() -> ApiException.conflict("Sem classificação oficial do encerramento do semestre"));
        Instant decidedAt = repo.justificationDecidedAt(attachment.justificationId());
        if (decidedAt == null || !repo.justificationStatus(attachment.justificationId()).matches("ACEITA|RECUSADA")) {
            throw ApiException.conflict("A análise da justificativa ainda não foi encerrada definitivamente");
        }
        RetentionPolicy policy = repo.policy(attachment.retentionPolicyId());
        LocalDate base = decidedAt.atZone(clock.getZone()).toLocalDate().isAfter(classification.semesterEndOn())
                ? decidedAt.atZone(clock.getZone()).toLocalDate() : classification.semesterEndOn();
        LocalDate expiresOn = base.plusMonths(policy.retentionMonths());
        if (LocalDate.now(clock).isBefore(JustificationAttachmentRules.deletionDeadline(expiresOn, policy.deletionGraceDays()))) {
            throw ApiException.conflict("O prazo mínimo de retenção e carência de 30 dias ainda não venceu");
        }

        transactions.executeWithoutResult(status -> {
            lock.lock();
            Attachment current = repo.attachment(attachmentId);
            if (current.version() != request.expectedVersion() || !current.state().equals("DISPONIVEL")) {
                throw ApiException.conflict("Comprovante alterado; consulte novamente");
            }
            if (repo.hasActiveHold(attachmentId)) throw ApiException.conflict("Há conservação excepcional ativa para este comprovante");
            if (!repo.wasPolicyValidated(current.retentionPolicyId())) throw ApiException.conflict("Política de retenção não validada");
            Classification currentClassification = repo.classification(current.justificationId())
                    .orElseThrow(() -> ApiException.conflict("Sem classificação oficial do encerramento do semestre"));
            Instant currentDecision = repo.justificationDecidedAt(current.justificationId());
            if (currentDecision == null || !repo.justificationStatus(current.justificationId()).matches("ACEITA|RECUSADA")) {
                throw ApiException.conflict("A análise da justificativa ainda não foi encerrada definitivamente");
            }
            RetentionPolicy currentPolicy = repo.policy(current.retentionPolicyId());
            LocalDate currentAnalysisDate = currentDecision.atZone(clock.getZone()).toLocalDate();
            LocalDate currentBase = currentAnalysisDate.isAfter(currentClassification.semesterEndOn())
                    ? currentAnalysisDate : currentClassification.semesterEndOn();
            LocalDate currentExpiresOn = currentBase.plusMonths(currentPolicy.retentionMonths());
            if (LocalDate.now(clock).isBefore(JustificationAttachmentRules.deletionDeadline(
                    currentExpiresOn, currentPolicy.deletionGraceDays()))) {
                throw ApiException.conflict("O prazo mínimo de retenção e carência de 30 dias ainda não venceu");
            }
            repo.markDeletePending(current, actor, request.reason().strip(), clock.instant());
        });
        try {
            store.delete(attachment.objectKey());
        } catch (RuntimeException failure) {
            transactions.executeWithoutResult(status -> repo.restoreAfterDeleteFailure(attachment, actor, request.reason().strip(), clock.instant()));
            throw failure;
        }
        transactions.executeWithoutResult(status -> repo.finishDelete(attachment, actor, request.reason().strip(), clock.instant()));
        return view(repo.attachment(attachmentId));
    }

    private AttachmentView view(Attachment attachment) {
        LocalDate expiresOn = null;
        LocalDate deletionDeadline = null;
        if (attachment.state().equals("DISPONIVEL") || attachment.state().equals("EXCLUSAO_PENDENTE")) {
            Classification classification = repo.classification(attachment.justificationId()).orElse(null);
            Instant decidedAt = repo.justificationDecidedAt(attachment.justificationId());
            if (classification != null && decidedAt != null && repo.wasPolicyValidated(attachment.retentionPolicyId())) {
                RetentionPolicy policy = repo.policy(attachment.retentionPolicyId());
                LocalDate analysisDate = decidedAt.atZone(clock.getZone()).toLocalDate();
                LocalDate base = analysisDate.isAfter(classification.semesterEndOn()) ? analysisDate : classification.semesterEndOn();
                expiresOn = base.plusMonths(policy.retentionMonths());
                deletionDeadline = JustificationAttachmentRules.deletionDeadline(expiresOn, policy.deletionGraceDays());
            }
        }
        boolean downloadable = attachment.state().equals("DISPONIVEL") && store.enabled() && storageApproved;
        String state = attachment.state().equals("DISPONIVEL") && !store.enabled()
                ? "ARMAZENAMENTO_INDISPONIVEL"
                : attachment.state().equals("DISPONIVEL") && !storageApproved
                        ? "ARMAZENAMENTO_AGUARDANDO_APROVACAO" : attachment.state();
        return new AttachmentView(attachment.id(), attachment.context(), attachment.filename(), attachment.contentType(),
                attachment.sizeBytes(), state, attachment.createdAt(), attachment.version(), expiresOn, deletionDeadline,
                repo.hasActiveHold(attachment.id()), downloadable);
    }

    private String uploadAvailability() {
        if (!store.enabled()) return "ARMAZENAMENTO_PRIVADO_NAO_CONFIGURADO";
        if (!storageApproved) return "ARMAZENAMENTO_AGUARDANDO_APROVACAO";
        if (repo.activeRetentionPolicy().isEmpty()) return "RETENCAO_AGUARDANDO_VALIDACAO";
        return "DISPONIVEL";
    }
    private void ensureUploadAvailable() {
        String availability = uploadAvailability();
        if (!availability.equals("DISPONIVEL")) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, availability);
    }
    private void requireAttachmentCapacity(UUID justificationId, UUID memberId, long incomingBytes) {
        var justificationUsage = repo.attachmentUsageForJustification(justificationId);
        if (justificationUsage.count() >= JustificationAttachmentRules.MAX_ATTACHMENTS_PER_JUSTIFICATION
                || justificationUsage.sizeBytes() > JustificationAttachmentRules.MAX_BYTES_PER_JUSTIFICATION - incomingBytes) {
            throw ApiException.conflict("A justificativa atingiu o limite de 5 anexos ou 25.000.000 bytes");
        }
        var memberUsage = repo.attachmentUsageForMember(memberId);
        if (memberUsage.count() >= JustificationAttachmentRules.MAX_ATTACHMENTS_PER_MEMBER
                || memberUsage.sizeBytes() > JustificationAttachmentRules.MAX_BYTES_PER_MEMBER - incomingBytes) {
            throw ApiException.conflict("O integrante atingiu o limite de 10 anexos ou 50.000.000 bytes");
        }
    }
    private void ensureStorageApproved() {
        if (!store.enabled() || !storageApproved) throw storageUnavailable();
    }
    private ApiException storageUnavailable() { return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Armazenamento privado indisponível"); }
    private ApiException retentionUnavailable() { return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Política de retenção ainda não validada"); }
    private void authorizeJustification(UUID actor, UUID justificationId, boolean administrative) {
        if (!repo.justificationExists(justificationId)) throw ApiException.notFound();
        if (administrative) {
            access.requireAdministrator(actor);
        } else if (!repo.justificationOwned(justificationId, actor)) {
            throw ApiException.notFound();
        }
    }
    private void checkContext(UUID justificationId, Context context) {
        String status = repo.justificationStatus(justificationId);
        boolean allowed = context == Context.JUSTIFICATIVA ? status.equals("PENDENTE")
                : status.equals("COMPLEMENTACAO_SOLICITADA");
        if (!allowed) throw ApiException.conflict("O contexto do comprovante não está aberto para envio");
    }
}
