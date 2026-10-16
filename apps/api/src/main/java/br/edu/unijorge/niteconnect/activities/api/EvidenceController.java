package br.edu.unijorge.niteconnect.activities.api;

import br.edu.unijorge.niteconnect.activities.application.*;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
public class EvidenceController {
    private final EvidenceService service; private final EvidenceUploads uploads; private final IdentityDirectory identities;
    public EvidenceController(EvidenceService service,EvidenceUploads uploads,IdentityDirectory identities) { this.service=service;this.uploads=uploads;this.identities=identities; }
    @PostMapping("/api/v1/activities/{id}/evidence") @ResponseStatus(HttpStatus.CREATED)
    EvidenceService.View create(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody EvidenceContracts.Create c) { return service.create(identities.member(jwt),id,c); }
    @GetMapping("/api/v1/activities/{id}/evidence")
    PageResult<EvidenceService.View> list(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(identities.member(jwt),id,page,size); }
    @GetMapping("/api/v1/activities/{id}/evidence/{evidenceId}")
    EvidenceService.View read(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@PathVariable UUID evidenceId) { return service.read(identities.member(jwt),id,evidenceId); }
    @PostMapping("/api/v1/activities/{id}/evidence/{evidenceId}/withdraw")
    EvidenceService.View withdraw(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@PathVariable UUID evidenceId,@Valid @RequestBody ActivityContracts.Command c) { return service.withdraw(identities.member(jwt),id,evidenceId,c); }
    @PostMapping(value="/api/v1/activities/{id}/evidence/uploads",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    EvidenceService.View upload(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestPart MultipartFile file,@Valid @RequestPart("metadata") EvidenceContracts.Upload c) {
        try(var input=file.getInputStream()) { return uploads.upload(identities.member(jwt),id,c,file.getOriginalFilename(),file.getContentType(),input); }
        catch(IOException e) { throw new br.edu.unijorge.niteconnect.shared.api.ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Falha ao receber anexo"); }
    }
    @GetMapping("/api/v1/activities/{id}/evidence/{evidenceId}/download")
    ResponseEntity<byte[]> download(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@PathVariable UUID evidenceId) { return download(identities.member(jwt),id,evidenceId,false); }
    @GetMapping("/api/v1/me/activities/evidence")
    PageResult<EvidenceService.View> personalList(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.personalList(identities.member(jwt),page,size); }
    @GetMapping("/api/v1/me/activities/evidence/{evidenceId}")
    EvidenceService.View personal(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID evidenceId) { return service.personal(identities.member(jwt),evidenceId); }
    @GetMapping("/api/v1/me/activities/evidence/{evidenceId}/download")
    ResponseEntity<byte[]> personalDownload(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID evidenceId) { return download(identities.member(jwt),null,evidenceId,true); }
    private ResponseEntity<byte[]> download(UUID actor,UUID activity,UUID evidence,boolean personal) {
        var info=service.download(actor,activity,evidence,personal);
        byte[] bytes=uploads.bytes(actor,activity,evidence,personal);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(info.contentType())).contentLength(bytes.length)
                .header(HttpHeaders.CACHE_CONTROL,"no-store").header("X-Content-Type-Options","nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(info.filename(),StandardCharsets.UTF_8).build().toString()).body(bytes);
    }
}
