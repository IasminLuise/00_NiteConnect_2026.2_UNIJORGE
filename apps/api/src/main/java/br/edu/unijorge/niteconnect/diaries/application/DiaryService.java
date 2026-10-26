package br.edu.unijorge.niteconnect.diaries.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.activities.application.ActivityService;
import br.edu.unijorge.niteconnect.activities.application.EvidenceService;
import br.edu.unijorge.niteconnect.activities.infrastructure.ActivityRepository.Filter;
import br.edu.unijorge.niteconnect.documents.application.DocumentService;
import br.edu.unijorge.niteconnect.documents.domain.DocumentRecords.Doc;
import br.edu.unijorge.niteconnect.diaries.infrastructure.DiaryRepository;
import br.edu.unijorge.niteconnect.diaries.infrastructure.DiaryRepository.Diary;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiaryService {
    private static final ZoneId DIARY_PERIOD_ZONE = ZoneId.of("America/Bahia");
    private final DiaryRepository repository;
    private final DiaryAccessPolicy policy;
    private final AccessService access;
    private final DocumentService documents;
    private final ActivityService activities;
    private final EvidenceService evidence;
    private final AdministrativeTransactions transactions;

    public DiaryService(DiaryRepository repository, DiaryAccessPolicy policy, AccessService access,
                        DocumentService documents, ActivityService activities, EvidenceService evidence,
                        AdministrativeTransactions transactions) {
        this.repository = repository;
        this.policy = policy;
        this.access = access;
        this.documents = documents;
        this.activities = activities;
        this.evidence = evidence;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public View read(UUID actor, UUID memberId) {
        access.requireActive(actor);
        policy.requireRead(actor, memberId, access.globalRoles(actor));
        return view(repository.require(memberId));
    }

    @Transactional
    public View importInitial(UUID actor, UUID memberId, String title, byte[] bytes, String reason,
                              Instant sourceCreatedAt, Instant sourceModifiedAt) {
        access.requireActive(actor);
        policy.requireImport(actor, access.globalRoles(actor));
        access.requireActive(memberId);
        transactions.lock();
        if (repository.exists(memberId)) throw ApiException.conflict("O integrante já possui um Diário contínuo");
        Doc document = documents.importPersonal(actor, memberId, title, bytes, reason);
        boolean sourceMetadataProvided = sourceCreatedAt != null || sourceModifiedAt != null;
        repository.create(memberId, document.id(), actor, sourceCreatedAt, sourceModifiedAt,
                sourceMetadataProvided ? "SHAREPOINT" : null, reason);
        return view(repository.require(memberId));
    }

    @Transactional(readOnly = true)
    public ActivityPanel activities(UUID actor, LocalDate from, LocalDate to, int page, int size) {
        access.requireActive(actor);
        validateWeek(from, to);
        PageResult<ActivityService.View> activityPage = activities.list(actor, true,
                new Filter(null, null, null, from, to, null), page, size);
        Instant start = from.atStartOfDay(DIARY_PERIOD_ZONE).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(DIARY_PERIOD_ZONE).toInstant();
        PageResult<EvidenceService.View> evidencePage = evidence.personalBetween(actor, start, end, page, size);
        return new ActivityPanel(from, to, activityPage, evidencePage);
    }

    private static void validateWeek(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.equals(LocalDate.MAX) || from.isAfter(to)
                || ChronoUnit.DAYS.between(from, to) > 6) {
            throw ApiException.invalid("O painel aceita um intervalo de até sete dias");
        }
    }

    private View view(Diary diary) {
        return new View(diary.memberId(), diary.documentId(), diary.title(), diary.createdAt(), diary.updatedAt(),
                diary.currentVersionId(), diary.currentRevision(), diary.documentVersion(), diary.importedBy(), diary.importedAt(),
                diary.sourceCreatedAt(), diary.sourceModifiedAt(), diary.sourceMetadataOrigin());
    }

    public record View(UUID memberId, UUID documentId, String title, Instant createdAt, Instant updatedAt,
                       UUID currentVersionId, int currentRevision, long documentVersion, UUID importedBy, Instant importedAt,
                       Instant sourceCreatedAt, Instant sourceModifiedAt, String sourceMetadataOrigin) {}

    public record ActivityPanel(LocalDate from, LocalDate to, PageResult<ActivityService.View> activities,
                                PageResult<EvidenceService.View> evidence) {}
}
