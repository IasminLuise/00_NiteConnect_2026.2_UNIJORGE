package br.edu.unijorge.experiments;

import org.junit.jupiter.api.Test;
import java.net.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DocsSecurityTest {
    final byte[] secret="synthetic-test-secret-at-least-32-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    @Test void jwtIsSignedPurposeBoundAndRejectsTamperingAndExpiry() {
        var jwt=new DocsJwt(secret,Clock.systemUTC());
        String token=jwt.sign(Map.of("operation","callback","payload",Map.of("status",2,"key","test"),"exp",Instant.now().plusSeconds(60).getEpochSecond()));
        assertEquals(2,jwt.verify(token,"callback").path("payload").path("status").asInt());
        assertThrows(DocumentStore.Failure.class,()->jwt.verify(token,"download"));
        assertThrows(DocumentStore.Failure.class,()->jwt.verify(token.substring(0,token.lastIndexOf('.')+1)+"AAAA","callback"));
        assertThrows(DocumentStore.Failure.class,()->jwt.verify(jwt.sign(Map.of("operation","callback","exp",1)),"callback"));
        assertThrows(DocumentStore.Failure.class,()->jwt.verify("a.b.c","callback"));
    }
    @Test void candidateUrlOnlyReachesPinnedIpAndExactKeyPath() {
        var fetch=new DocsFetch(URI.create("http://127.0.0.1:12345"),Set.of("http://localhost:19982"));
        assertEquals("http://127.0.0.1:12345/cache/files/test_1/output.docx?x=y",fetch.resolve("http://localhost:19982/cache/files/test_1/output.docx?x=y","test").toString());
        assertEquals("http://127.0.0.1:12345/cache/files/data/test_1/output.docx/output.docx?md5=synthetic",fetch.resolve("http://localhost:19982/cache/files/data/test_1/output.docx/output.docx?md5=synthetic","test").toString());
        assertThrows(DocumentStore.Failure.class,()->fetch.resolve("http://localhost:19982/cache/files/data/other_1/output.docx","test"));
        for(String bad:List.of("http://evil.test/cache/files/test_1/output.docx","http://localhost:19982/cache/files/other_1/output.docx","http://localhost:19982/cache/files/test_1/../other.docx","http://localhost:19982/cache/files/test_1/%2e%2e/a","http://u:p@localhost:19982/cache/files/test_1/a"))
            assertThrows(DocumentStore.Failure.class,()->fetch.resolve(bad,"test"));
    }
}
