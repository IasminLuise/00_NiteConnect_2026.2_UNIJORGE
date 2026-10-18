package br.edu.unijorge.experiments;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

class DocsApiServiceTest {
    @TempDir Path root;
    HttpServer server;URI endpoint;final AtomicReference<byte[]> bytes=new AtomicReference<>();
    DocsJwt jwt;Path fixtures;
    @BeforeEach void setup()throws Exception {
        jwt=new DocsJwt("synthetic-test-secret-at-least-32-bytes".getBytes(),Clock.systemUTC());
        fixtures=root.resolve("fixtures");Files.createDirectories(fixtures);Files.write(fixtures.resolve("F01.docx"),DocumentStoreTest.docx("one"));
        Files.write(fixtures.resolve("F02.docx"),CandidateGateTest.withHeader("SAVEDATE","image"));
        bytes.set(DocumentStoreTest.docx("XYZone"));
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/cache/files/",exchange->{byte[] data=bytes.get();exchange.sendResponseHeaders(200,data.length);try(var out=exchange.getResponseBody()){out.write(data);}});
        server.start();endpoint=URI.create("http://127.0.0.1:"+server.getAddress().getPort());
    }
    @AfterEach void stop(){server.stop(0);}
    DocsApiService service()throws Exception {return new DocsApiService(root.resolve("docs"),fixtures,jwt,new DocsFetch(endpoint,Set.of(endpoint.toString())),"http://localhost:18087","http://wopi-host:8080","http://localhost:19982");}
    JsonNode body(JsonNode work,int status){return DocsJwt.JSON.valueToTree(Map.of("key",work.path("key").asString(),"status",status,"url",endpoint+"/cache/files/"+work.path("key").asString()+"_1/output.docx","filetype","docx"));}
    String signed(JsonNode body){return "Bearer "+jwt.sign(Map.of("payload",body,"operation","callback","exp",Instant.now().plusSeconds(120).getEpochSecond()));}
    UUID id(JsonNode work){return UUID.fromString(work.path("id").asString());}
    String cap(JsonNode work){return work.path("cap").asString();}
    @Test void signedCallbackPersistsExactBytesAndReplayIsIdempotentAcrossRestart()throws Exception {
        var service=service();var work=service.create("F01.docx","structural",true);var initial=service.state(id(work),cap(work));var body=body(work,2);
        assertEquals("editing-local",initial.path("state").asString());
        assertEquals(0,service.callback(id(work),cap(work),body,signed(body)));
        var saved=service.state(id(work),cap(work));assertEquals("committed",saved.path("state").asString());assertEquals(1,saved.path("commits").asInt());
        assertArrayEquals(bytes.get(),service.version(id(work),cap(work),saved.path("headVersion").asString()));
        assertNotEquals(initial.path("headVersion"),saved.path("headVersion"));
        assertEquals(0,service.callback(id(work),cap(work),body,signed(body)));assertEquals(1,service.state(id(work),cap(work)).path("commits").asInt());
        var restarted=service();assertEquals(0,restarted.callback(id(work),cap(work),body,signed(body)));
        assertArrayEquals(bytes.get(),restarted.version(id(work),cap(work),saved.path("headVersion").asString()));
    }
    @Test void strictRefRejectionKeepsOldVersionAndDurableRecovery()throws Exception {
        var service=service();var work=service.create("F02.docx","strict",true);var initial=service.state(id(work),cap(work));
        bytes.set(prefix(CandidateGateTest.withHeader("REF","image")));var body=body(work,2);
        assertThrows(DocumentStore.Failure.class,()->service.callback(id(work),cap(work),body,signed(body)));
        var state=service.state(id(work),cap(work));assertEquals("rejected",state.path("state").asString());assertEquals(initial.path("headVersion"),state.path("headVersion"));assertEquals(0,state.path("commits").asInt());
        assertArrayEquals(bytes.get(),service().recovery(id(work),cap(work)));
        var closed=body(work,4);service.callback(id(work),cap(work),closed,signed(closed));
        assertEquals("rejected",service.state(id(work),cap(work)).path("state").asString());
    }
    @Test void strictIndependentKnownScriptCanCommitWithoutLearningEngineOutput()throws Exception {
        var service=service();var work=service.create("F01.docx","strict",true);var message=body(work,2);
        assertEquals(0,service.callback(id(work),cap(work),message,signed(message)));
        var state=service.state(id(work),cap(work));assertEquals(1,state.path("commits").asInt());
        assertArrayEquals(bytes.get(),service.version(id(work),cap(work),state.path("headVersion").asString()));
    }
    @Test void downloadRedirectIsNeverFollowedAndCannotPromoteVersion()throws Exception {
        var service=service();var work=service.create("F01.docx","structural",true);var initial=service.state(id(work),cap(work));
        var reached=new java.util.concurrent.atomic.AtomicBoolean();
        server.createContext("/redirect-target",exchange->{reached.set(true);exchange.sendResponseHeaders(500,-1);exchange.close();});
        server.createContext("/cache/files/"+work.path("key").asString()+"_1/output.docx",exchange->{exchange.getResponseHeaders().set("Location",endpoint+"/redirect-target");exchange.sendResponseHeaders(302,-1);exchange.close();});
        var message=body(work,2);assertThrows(DocumentStore.Failure.class,()->service.callback(id(work),cap(work),message,signed(message)));
        assertFalse(reached.get());var state=service.state(id(work),cap(work));
        assertEquals("recovery-required",state.path("state").asString());assertEquals(initial.path("headVersion"),state.path("headVersion"));assertEquals(0,state.path("commits").asInt());
    }
    static byte[] prefix(byte[] data)throws Exception {
        var out=new java.io.ByteArrayOutputStream();
        try(var input=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(data));var output=new java.util.zip.ZipOutputStream(out)) {
            java.util.zip.ZipEntry entry;while((entry=input.getNextEntry())!=null){byte[] content=input.readAllBytes();
                if(entry.getName().equals("word/document.xml"))content=new String(content,java.nio.charset.StandardCharsets.UTF_8).replace("<w:t>one</w:t>","<w:t>XYZone</w:t>").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                output.putNextEntry(new java.util.zip.ZipEntry(entry.getName()));output.write(content);output.closeEntry();}
        }
        return out.toByteArray();
    }
    @Test void invalidJwtUnsignedChangesWrongKeyAndReadonlyCannotCommit()throws Exception {
        var service=service();var work=service.create("F01.docx","structural",true);var body=body(work,2);
        assertThrows(DocumentStore.Failure.class,()->service.callback(id(work),cap(work),body,"Bearer broken"));
        var altered=DocsJwt.JSON.valueToTree(Map.of("key",work.path("key").asString(),"status",6));
        assertThrows(DocumentStore.Failure.class,()->service.callback(id(work),cap(work),altered,signed(body)));
        var wrong=DocsJwt.JSON.valueToTree(Map.of("key","wrong","status",2));
        assertThrows(DocumentStore.Failure.class,()->service.callback(id(work),cap(work),wrong,signed(wrong)));
        var ro=service.create("F01.docx","structural",false);var roBody=body(ro,2);
        assertThrows(DocumentStore.Failure.class,()->service.callback(id(ro),cap(ro),roBody,signed(roBody)));
        assertEquals(0,service.state(id(work),cap(work)).path("commits").asInt());
    }
    @Test void statusOneFourAndErrorsNeverCreateVersionsAndStaleKeyCannotOverwrite()throws Exception {
        var service=service();var work=service.create("F01.docx","structural",true);
        for(int status:List.of(1,3,7)){var body=body(work,status);service.callback(id(work),cap(work),body,signed(body));assertEquals(0,service.state(id(work),cap(work)).path("commits").asInt());}
        var four=body(work,4);service.callback(id(work),cap(work),four,signed(four));
        var reopened=service.reopen(id(work),cap(work));assertNotEquals(work.path("key"),reopened.path("key"));
        var stale=body(work,2);assertThrows(DocumentStore.Failure.class,()->service.callback(id(work),cap(work),stale,signed(stale)));
    }
    @Test void installedFlatBodyTokenIsBoundToEveryUnsignedParameter()throws Exception {
        var service=service();var work=service.create("F01.docx","structural",true);var body=body(work,2);
        String token=jwt.sign(Map.of("key",work.path("key").asString(),"status",2,"url",body.path("url").asString(),"filetype","docx","operation","callback","exp",Instant.now().plusSeconds(120).getEpochSecond()));
        var request=DocsJwt.JSON.createObjectNode();for(String name:List.of("key","status","url","filetype"))request.set(name,body.get(name));request.put("token",token);
        var tampered=request.deepCopy();tampered.put("status",6);
        assertThrows(DocumentStore.Failure.class,()->service.callback(id(work),cap(work),tampered,null));
        assertEquals(0,service.callback(id(work),cap(work),request,null));
        assertEquals(1,service.state(id(work),cap(work)).path("commits").asInt());
    }
    @Test void metadataFailureAfterValidCommitIsRecoveryRequiredAndRetryReconcilesAcrossRestart()throws Exception {
        var once=new java.util.concurrent.atomic.AtomicBoolean();
        var service=new DocsApiService(root.resolve("docs"),fixtures,jwt,new DocsFetch(endpoint,Set.of(endpoint.toString())),"http://localhost:18087","http://wopi-host:8080","http://localhost:19982",(target,data)->{
            var state=DocsJwt.JSON.readTree(data);
            if(state.path("state").asString().equals("committed")&&once.compareAndSet(false,true))throw new DocumentStore.Failure(503,"Injected metadata write failure");
            try{Files.write(target,data);}catch(java.io.IOException error){throw new DocumentStore.Failure(503,"Injected metadata IO");}
        });
        var work=service.create("F01.docx","structural",true);var body=body(work,2);
        assertThrows(DocumentStore.Failure.class,()->service.callback(id(work),cap(work),body,signed(body)));
        assertEquals("recovery-required",service.state(id(work),cap(work)).path("state").asString());
        var restarted=service();assertEquals(0,restarted.callback(id(work),cap(work),body,signed(body)));
        var saved=restarted.state(id(work),cap(work));assertEquals(1,saved.path("commits").asInt());
        assertArrayEquals(bytes.get(),restarted.version(id(work),cap(work),saved.path("headVersion").asString()));
    }
}
