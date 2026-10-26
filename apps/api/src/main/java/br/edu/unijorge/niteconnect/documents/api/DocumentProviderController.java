package br.edu.unijorge.niteconnect.documents.api;
import java.util.*;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;
import br.edu.unijorge.niteconnect.documents.application.DocumentEditorService;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
@RestController
public class DocumentProviderController {
    private final DocumentEditorService service;private final ObjectMapper json;
    public DocumentProviderController(DocumentEditorService service,ObjectMapper json){this.service=service;this.json=json;}
    @GetMapping("/api/v1/document-provider/sources/{id}") ResponseEntity<byte[]> source(@PathVariable UUID id,@RequestParam String token){byte[] bytes=service.source(id,token);return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")).contentLength(bytes.length).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(bytes);}
    @PostMapping(value="/api/v1/document-provider/cycles/{id}/callback",consumes=MediaType.APPLICATION_JSON_VALUE)
    Map<String,Integer> callback(@PathVariable UUID id,@RequestHeader(value="Authorization",required=false) String authorization,HttpServletRequest request){
        if(authorization==null||!authorization.startsWith("Bearer ")||authorization.length()>65543)throw ApiException.forbidden();
        try{byte[] bytes=request.getInputStream().readNBytes(65537);if(bytes.length>65536)throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"Callback excede o limite");
            Map<String,Object> body=json.readValue(bytes,new TypeReference<Map<String,Object>>(){});if(body==null)throw ApiException.invalid("Callback inválido");return service.callback(id,authorization.substring(7),body);
        }catch(IOException | tools.jackson.core.JacksonException e){throw ApiException.invalid("Callback inválido");}
    }
}
