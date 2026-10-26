package br.edu.unijorge.experiments;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditorActionTest {
    @Test void onlyofficeUsesConfiguredOriginAndPreservesDiscoveryParameters() {
        var url=LabController.actionUrl("http://untrusted-host/hosting/wopi/word/edit?type=docx&<ui=UI_LLCC&>",
                "http://localhost:19981","http://wopi-host:8080/wopi/files/test");
        assertTrue(url.startsWith("http://localhost:19981/hosting/wopi/word/edit?type=docx&"));
        assertTrue(url.contains("WOPISrc=http%3A%2F%2Fwopi-host%3A8080"));
        assertFalse(url.contains("untrusted-host"));
    }
    @Test void engineCannotEscapeItsActionPrefixOrChangeToArbitraryScheme() {
        assertThrows(DocumentStore.Failure.class,()->LabController.actionUrl("http://host/admin", "http://localhost:19981","source"));
        assertThrows(DocumentStore.Failure.class,()->LabController.actionUrl("file:///hosting/wopi/edit", "http://localhost:19981","source"));
        assertThrows(DocumentStore.Failure.class,()->LabController.actionUrl("http://host/hosting/wopi/../admin", "http://localhost:19981","source"));
    }
}
