package br.edu.unijorge.experiments;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/wopi/files/{id}")
public final class WopiController {
    private final DocumentStore store;
    public WopiController(DocumentStore store){this.store=store;}
    @GetMapping public Map<String,Object> info(@PathVariable UUID id,@RequestParam(name="access_token",required=false)String token) {
        var info=store.info(id,token);
        return Map.ofEntries(Map.entry("BaseFileName",info.name()),Map.entry("OwnerId","synthetic-lab"),
            Map.entry("UserId",store.user(id,token)),Map.entry("UserFriendlyName","Pessoa sintética"),
            Map.entry("Size",info.size()),Map.entry("Version",info.version()),Map.entry("LastModifiedTime",info.modified()),
            Map.entry("UserCanWrite",info.writable()),Map.entry("ReadOnly",!info.writable()),
            Map.entry("SupportsUpdate",true),Map.entry("SupportsLocks",true),Map.entry("SupportsGetLock",true),
            Map.entry("UserCanNotWriteRelative",true),Map.entry("PostMessageOrigin",System.getenv().getOrDefault("LAB_ORIGIN","http://localhost:18085")));
    }
    @GetMapping("/contents") public ResponseEntity<byte[]> read(@PathVariable UUID id,@RequestParam(name="access_token",required=false)String token) {
        byte[] bytes=store.read(id,token);return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(bytes);
    }
    @PostMapping("/contents") public ResponseEntity<Map<String,String>> save(@PathVariable UUID id,@RequestParam(name="access_token",required=false)String token,HttpServletRequest request)throws IOException {
        store.info(id,token);
        if(!"PUT".equals(request.getHeader("X-WOPI-Override")))throw new DocumentStore.Failure(501,"Operação não implementada");
        byte[] bytes=request.getInputStream().readNBytes(DocxValidator.MAX_BYTES+1);
        if(bytes.length>DocxValidator.MAX_BYTES)throw new DocumentStore.Failure(413,"Arquivo excede limite técnico");
        String timestamp=request.getHeader("X-COOL-WOPI-Timestamp");
        if(timestamp==null)timestamp=request.getHeader("X-LOOL-WOPI-Timestamp");
        var saved=store.save(id,token,bytes,timestamp,request.getHeader("X-WOPI-Lock"));
        return ResponseEntity.ok().header("X-WOPI-ItemVersion",saved.version()).body(Map.of("LastModifiedTime",saved.modified()));
    }
    @PostMapping public ResponseEntity<Void> lock(@PathVariable UUID id,@RequestParam(name="access_token",required=false)String token,HttpServletRequest request) {
        store.info(id,token);String operation=request.getHeader("X-WOPI-Override");
        if("GET_LOCK".equals(operation))return ResponseEntity.ok().header("X-WOPI-Lock",store.lockValue(id)).build();
        if(!Set.of("LOCK","UNLOCK","REFRESH_LOCK").contains(operation==null?"":operation))throw new DocumentStore.Failure(501,"Operação não implementada");
        store.lock(id,token,operation,request.getHeader("X-WOPI-Lock"),request.getHeader("X-WOPI-OldLock"));return ResponseEntity.ok().build();
    }
    @RestControllerAdvice public static class Errors {
        @ExceptionHandler(DocumentStore.Failure.class) ResponseEntity<Map<String,Object>> failure(DocumentStore.Failure f) {
            var response=ResponseEntity.status(f.status()).cacheControl(CacheControl.noStore());
            if(f.status()==409)response.header("X-WOPI-Lock",f.lock());
            Map<String,Object> body=f.status()==409&&f.getMessage().equals("Versão de origem alterada")?Map.of("COOLStatusCode",1010):Map.of("error",f.getMessage());
            return response.body(body);
        }
        @ExceptionHandler(IOException.class) ResponseEntity<Map<String,String>> io(){return ResponseEntity.status(503).body(Map.of("error","Operação indisponível"));}
    }
}
