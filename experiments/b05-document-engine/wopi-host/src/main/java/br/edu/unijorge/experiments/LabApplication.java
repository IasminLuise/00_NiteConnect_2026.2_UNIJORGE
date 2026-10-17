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
        var store=new DocumentStore(Path.of(System.getenv().getOrDefault("LAB_DATA","target/lab-data")),Clock.systemUTC());
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
