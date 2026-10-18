package br.edu.unijorge.experiments;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.net.URI;
import tools.jackson.databind.JsonNode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocsApiHttpTest {
    @TempDir Path root;DocsApiService service;DocsJwt jwt;MockMvc mvc;JsonNode work;
    final String origin="http://localhost:18087",operator="synthetic-lab-operator";
    @BeforeEach void setup()throws Exception {
        Path fixtures=root.resolve("fixtures");Files.createDirectories(fixtures);Files.write(fixtures.resolve("F01.docx"),DocumentStoreTest.docx("one"));
        jwt=new DocsJwt("synthetic-test-secret-at-least-32-bytes".getBytes(),Clock.systemUTC());
        service=new DocsApiService(root.resolve("docs"),fixtures,jwt,new DocsFetch(URI.create("http://127.0.0.1:12345"),Set.of("http://localhost:19982")),origin,"http://wopi-host:8080","http://localhost:19982");
        work=service.create("F01.docx","structural",true);
        mvc=MockMvcBuilders.standaloneSetup(new DocsApiController(service,operator,origin,"http://localhost:19982","127.0.0.1")).setControllerAdvice(new WopiController.Errors()).build();
    }
    String id(){return work.path("id").asString();}String cap(){return work.path("cap").asString();}
    @Test void operatorOriginAndSessionCapabilityAreAllRequired()throws Exception {
        mvc.perform(post("/docs-api/sessions").header("Origin",origin).content("{}")) .andExpect(status().isUnauthorized());
        mvc.perform(post("/docs-api/sessions").header("X-Lab-Operator",operator).header("Origin","https://untrusted.test").content("{}")) .andExpect(status().isForbidden());
        mvc.perform(get("/docs-api/state/"+id()).param("cap","wrong")) .andExpect(status().isUnauthorized());
    }
    @Test void invalidAndWrongNetworkCallbacksReturnNumericFailureWithoutPromotion()throws Exception {
        String body="{\"key\":\""+work.path("key").asString()+"\",\"status\":2}";
        mvc.perform(post("/docs-api/callback/"+id()).param("cap",cap()).content(body))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("error").value(1));
        mvc.perform(post("/docs-api/callback/"+id()).param("cap",cap()).with(request->{request.setRemoteAddr("192.0.2.9");return request;}).content(body))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("error").value(1));
        Assertions.assertEquals(0,service.state(UUID.fromString(id()),cap()).path("commits").asInt());
    }
    @Test void signedStatusOneIsOnlyAcknowledgmentAndDownloadsArePurposeBound()throws Exception {
        var body=Map.of("key",work.path("key").asString(),"status",1);
        String token=jwt.sign(Map.of("operation","callback","payload",body,"exp",Instant.now().plusSeconds(60).getEpochSecond()));
        mvc.perform(post("/docs-api/callback/"+id()).param("cap",cap()).header("Authorization","Bearer "+token).content(DocsJwt.JSON.writeValueAsBytes(body)))
            .andExpect(status().isOk()).andExpect(jsonPath("error").value(0));
        mvc.perform(get("/docs-api/source/"+id()).param("cap",cap()).header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        String url=service.config(UUID.fromString(id()),cap()).path("document").path("url").asString();
        String download=jwt.sign(Map.of("operation","download","payload",Map.of("url",url),"exp",Instant.now().plusSeconds(60).getEpochSecond()));
        mvc.perform(get("/docs-api/source/"+id()).param("cap",cap()).header("Authorization","Bearer "+download))
            .andExpect(status().isOk()).andExpect(header().exists("X-Content-SHA256"));
        Assertions.assertEquals(0,service.state(UUID.fromString(id()),cap()).path("commits").asInt());
    }
}
