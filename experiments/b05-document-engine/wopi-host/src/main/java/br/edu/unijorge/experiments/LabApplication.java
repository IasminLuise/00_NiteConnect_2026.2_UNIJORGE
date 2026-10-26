package br.edu.unijorge.experiments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.Clock;
import java.util.UUID;

@SpringBootApplication
public class LabApplication {
    public static void main(String[] args){SpringApplication.run(LabApplication.class,args);}
    @Bean DocumentStore documents()throws IOException {
        CandidateGate gate=(id,after,number)->{};
        String policy=System.getenv("LAB_SAVE_SCRIPT");
        if(policy!=null) {
            var entries=new java.util.Properties();try(var input=Files.newInputStream(Path.of(policy))){entries.load(input);}
            var scripts=new java.util.HashMap<String,java.util.List<String>>();
            for(String key:entries.stringPropertyNames())scripts.put(key,java.util.Arrays.asList(entries.getProperty(key).split("\\|",-1)));
            gate=new ScriptedDocxGate(scripts);
        }
        var store=new DocumentStore(Path.of(System.getenv().getOrDefault("LAB_DATA","target/lab-data")),Clock.systemUTC(),gate);
        Path fixtures=Path.of(System.getenv().getOrDefault("LAB_FIXTURES","../fixtures/generated"));
        if(Files.isDirectory(fixtures))try(var paths=Files.list(fixtures)) {
            for(var path:paths.filter(p->p.getFileName().toString().matches("F0[1-9]\\.docx")).sorted().toList())
                store.seed(UUID.nameUUIDFromBytes(path.getFileName().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),path.getFileName().toString(),Files.readAllBytes(path));
        }
        return store;
    }
    @Bean OncePerRequestFilter privacyHeaders() {
        return new OncePerRequestFilter(){
            @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
                response.setHeader("Cache-Control","no-store");
                response.setHeader("Referrer-Policy",request.getRequestURI().equals("/")||request.getRequestURI().equals("/index.html")?"same-origin":"no-referrer");
                response.setHeader("X-Content-Type-Options","nosniff");
                chain.doFilter(request,response);
            }
        };
    }
}
