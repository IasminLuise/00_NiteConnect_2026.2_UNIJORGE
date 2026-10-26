package br.edu.unijorge.niteconnect;

import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.documents.application.*;
import br.edu.unijorge.niteconnect.documents.infrastructure.DocumentInspectionRepository;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentInspectionConfigurationTest {
    @ParameterizedTest
    @ValueSource(strings={"open","sample","close"})
    void rechecksAdministratorAfterWaitingForTheAdministrativeLock(String operation){
        var documents=mock(DocumentService.class);
        var access=mock(AccessService.class);
        var store=mock(DocumentObjectStore.class);when(store.enabled()).thenReturn(true);
        var provider=mock(DocumentEditorProvider.class);when(provider.enabled()).thenReturn(true);
        var revoked=new java.util.concurrent.atomic.AtomicBoolean();
        doAnswer(call->{revoked.set(true);return null;}).when(documents).lock();
        doAnswer(call->{if(revoked.get())throw ApiException.forbidden();return null;})
                .when(access).requireAdministrator(any());
        var service=new DocumentInspectionService(mock(DocumentInspectionRepository.class),documents,access,store,provider,
                new DocxIntegrity(),Clock.systemUTC(),true,true,"127.0.0.1","http://127.0.0.1:18083",
                "synthetic-capability-secret-at-least-32-bytes");
        UUID actor=UUID.randomUUID(),document=UUID.randomUUID(),version=UUID.randomUUID();
        assertThatThrownBy(()->{
            switch(operation){
                case "open"->service.open(actor,document,new DocumentInspectionService.Open(version,"Teste"));
                case "sample"->service.sample(actor,document,version,0,new byte[]{1},"Teste");
                case "close"->service.close(actor,document,version);
                default->throw new IllegalArgumentException(operation);
            }
        }).isInstanceOfSatisfying(ApiException.class,error->assertThat(error.status().value()).isEqualTo(403));
        verify(documents,never()).editorGuard(any(),any(),anyBoolean());
    }
    @ParameterizedTest
    @CsvSource({
        "false,true,127.0.0.1,http://127.0.0.1:18083",
        "true,false,127.0.0.1,http://127.0.0.1:18083",
        "true,true,0.0.0.0,http://127.0.0.1:18083",
        "true,true,127.0.0.1,https://example.com",
        "true,true,127.0.0.1,http://localhost:18083/path",
        "true,true,127.0.0.1,http://user@localhost:18083"
    })
    void refusesNonlocalOrDisabledConfigurationBeforeAnyDocumentRead(
            boolean enabled,boolean local,String bind,String base){
        var documents=mock(DocumentService.class);
        var store=mock(DocumentObjectStore.class);when(store.enabled()).thenReturn(true);
        var provider=mock(DocumentEditorProvider.class);when(provider.enabled()).thenReturn(true);
        var inspections=mock(DocumentInspectionRepository.class);
        var service=new DocumentInspectionService(inspections,documents,mock(AccessService.class),store,provider,
                new DocxIntegrity(),Clock.systemUTC(),enabled,local,bind,base,"synthetic-capability-secret-at-least-32-bytes");
        assertThatThrownBy(()->service.open(UUID.randomUUID(),UUID.randomUUID(),
                new DocumentInspectionService.Open(UUID.randomUUID(),"Teste local")))
                .isInstanceOfSatisfying(ApiException.class,error->assertThat(error.status().value()).isEqualTo(503));
        verifyNoInteractions(documents,inspections);
        verify(provider,never()).openInspection(anyString(),anyString(),any(),any(),any());
    }
}
