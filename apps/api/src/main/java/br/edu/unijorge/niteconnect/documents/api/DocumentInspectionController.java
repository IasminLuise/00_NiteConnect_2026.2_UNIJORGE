package br.edu.unijorge.niteconnect.documents.api;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import br.edu.unijorge.niteconnect.documents.application.DocumentInspectionService;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.shared.api.ApiException;

@RestController
public class DocumentInspectionController {
    private final DocumentInspectionService service;
    private final IdentityDirectory identities;
    private final ObjectMapper json;
    public DocumentInspectionController(DocumentInspectionService service,IdentityDirectory identities,ObjectMapper json){
        this.service=service;this.identities=identities;this.json=json;
    }
    @PostMapping("/api/v1/documents/{document}/inspection-sessions")
    DocumentInspectionService.Session open(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID document,
            @Valid @RequestBody DocumentInspectionService.Open request){
        return service.open(identities.member(jwt),document,request);
    }
    @PostMapping("/api/v1/documents/{document}/inspection-sessions/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void close(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID document,@PathVariable UUID id){
        service.close(identities.member(jwt),document,id);
    }
    @PostMapping(value="/api/v1/documents/{document}/inspection-samples",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    br.edu.unijorge.niteconnect.documents.domain.DocumentRecords.VersionView sample(
            @AuthenticationPrincipal Jwt jwt,@PathVariable UUID document,@RequestParam UUID basedOnVersionId,
            @RequestParam long expectedVersion,@RequestParam String reason,
            @RequestPart org.springframework.web.multipart.MultipartFile file){
        if(file.isEmpty()||file.getSize()>8*1024*1024)throw ApiException.invalid("DOCX obrigatório, máximo 8 MiB");
        try(var input=file.getInputStream()){
            byte[] bytes=input.readNBytes(8*1024*1024+1);
            if(bytes.length>8*1024*1024)throw ApiException.invalid("DOCX excede o limite");
            return service.sample(identities.member(jwt),document,basedOnVersionId,expectedVersion,bytes,reason);
        }catch(IOException error){throw ApiException.invalid("Arquivo indisponível");}
    }
    @GetMapping("/api/v1/document-provider/inspection-sources/{id}")
    ResponseEntity<byte[]> source(@PathVariable UUID id,@RequestParam String token){
        byte[] bytes=service.source(id,token);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contentLength(bytes.length).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(bytes);
    }
    @PostMapping(value="/api/v1/document-provider/inspection-cycles/{id}/callback",consumes=MediaType.APPLICATION_JSON_VALUE)
    Map<String,Integer> callback(@PathVariable UUID id,@RequestHeader(value="Authorization",required=false) String authorization,
            HttpServletRequest request){
        if(authorization==null||!authorization.startsWith("Bearer ")||authorization.length()>65543)throw ApiException.forbidden();
        try{
            byte[] bytes=request.getInputStream().readNBytes(65537);
            if(bytes.length>65536)throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"Callback excede o limite");
            Map<String,Object> body=json.readValue(bytes,new TypeReference<Map<String,Object>>(){});
            if(body==null)throw ApiException.invalid("Callback inválido");
            return service.callback(id,authorization.substring(7),body);
        }catch(IOException|tools.jackson.core.JacksonException error){throw ApiException.invalid("Callback inválido");}
    }
}
