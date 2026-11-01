package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.attendance.application.JustificationAttachmentService;
import br.edu.unijorge.niteconnect.attendance.application.JustificationAttachmentStore;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentRepository;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentOperationLocks;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JustificationStorageReadinessTest {
    @Test
    void configuredPrivateStoreStillWaitsForExplicitInstitutionalApproval() {
        UUID actor = UUID.randomUUID();
        UUID justificationId = UUID.randomUUID();
        AccessService access = mock(AccessService.class);
        JustificationAttachmentRepository repository = mock(JustificationAttachmentRepository.class);
        JustificationAttachmentStore store = mock(JustificationAttachmentStore.class);
        AdministrativeTransactions transactions = mock(AdministrativeTransactions.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        JustificationAttachmentOperationLocks operationLocks = mock(JustificationAttachmentOperationLocks.class);
        when(store.enabled()).thenReturn(true);
        when(repository.justificationExists(justificationId)).thenReturn(true);
        when(repository.justificationOwned(justificationId, actor)).thenReturn(true);
        when(repository.attachments(justificationId)).thenReturn(List.of());
        when(repository.activeRetentionPolicy()).thenReturn(Optional.of(new JustificationAttachmentRepository.RetentionPolicy(
                UUID.randomUUID(), 1, 24, 30, "MAX_ANALYSIS_AND_SEMESTER_END", actor,
                Instant.parse("2026-01-01T00:00:00Z"), "Política aprovada", null)));

        JustificationAttachmentService service = new JustificationAttachmentService(
                access, repository, store, transactions, transactionManager, operationLocks, Clock.systemUTC(), "");

        assertThat(service.attachments(actor, justificationId, false).uploadAvailability())
                .isEqualTo("ARMAZENAMENTO_AGUARDANDO_APROVACAO");
    }
}
