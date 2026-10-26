package br.edu.unijorge.experiments;

import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.nio.file.*;
import java.net.URI;
import java.time.Clock;
import java.util.Set;

@Configuration
@ConditionalOnProperty(name="b05.docs.enabled",havingValue="true")
class DocsApiConfig {
    @Bean DocsApiService docsApiService()throws Exception {
        var jwt=new DocsJwt(Files.readAllBytes(Path.of(required("B05_JWT_SECRET_FILE"))),Clock.systemUTC());
        var pinned=URI.create(required("B05_ENGINE_PIN"));String editor=required("B05_EDITOR_ORIGIN");
        var fetch=new DocsFetch(pinned,Set.of(editor,"http://onlyoffice","http://onlyoffice:80",pinned.toString()));
        return new DocsApiService(Path.of(required("B05_DOCS_DATA")),Path.of(required("LAB_FIXTURES")),jwt,fetch,required("LAB_ORIGIN"),"http://wopi-host:8080",editor);
    }
    static String required(String name){String value=System.getenv(name);if(value==null||value.isBlank())throw new IllegalStateException("Missing lab setting: "+name);return value;}
}
