package br.edu.unijorge.niteconnect.attendance.api;

import br.edu.unijorge.niteconnect.attendance.application.JustificationAttachmentService;
import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentContracts.*;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
public class JustificationAttachmentController {
    private final JustificationAttachmentService service;
    private final IdentityDirectory identities;

    public JustificationAttachmentController(JustificationAttachmentService service, IdentityDirectory identities) {
        this.service = service;
        this.identities = identities;
    }

    @GetMapping("/api/v1/me/absence-justifications/{id}/attachments")
    AttachmentCollection personalAttachments(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.attachments(identities.member(jwt), id, false);
    }

    @PostMapping(value="/api/v1/me/absence-justifications/{id}/attachments", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    AttachmentView upload(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                          @RequestPart("file") MultipartFile file,
                          @RequestParam(defaultValue="JUSTIFICATIVA") String context) {
        try {
            return service.upload(identities.member(jwt), id, Context.valueOf(context), file.getOriginalFilename(),
                    file.getContentType(), file.getSize(), file.getInputStream());
        } catch (IllegalArgumentException e) {
            throw new br.edu.unijorge.niteconnect.shared.api.ApiException(HttpStatus.BAD_REQUEST, "Contexto de comprovante inválido");
        } catch (IOException e) {
            throw new br.edu.unijorge.niteconnect.shared.api.ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Falha ao receber comprovante");
        }
    }

    @GetMapping("/api/v1/me/absence-justifications/{id}/attachments/{attachmentId}/download")
    ResponseEntity<byte[]> personalDownload(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                            @PathVariable UUID attachmentId) {
        return response(service.download(identities.member(jwt), id, attachmentId, false));
    }

    @GetMapping("/api/v1/attendance/justifications/{id}/attachments")
    AttachmentCollection administrativeAttachments(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.attachments(identities.member(jwt), id, true);
    }

    @GetMapping("/api/v1/attendance/justifications/{id}/attachments/{attachmentId}/download")
    ResponseEntity<byte[]> administrativeDownload(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                  @PathVariable UUID attachmentId) {
        return response(service.download(identities.member(jwt), id, attachmentId, true));
    }

    @GetMapping("/api/v1/attendance/justification-attachment-retention-policies")
    PageResult<RetentionPolicyView> retentionPolicies(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return service.retentionPolicies(identities.member(jwt), page, size);
    }

    @PostMapping("/api/v1/attendance/justification-attachment-retention-policies")
    @ResponseStatus(HttpStatus.CREATED)
    RetentionPolicyView proposeRetentionPolicy(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RetentionPolicyProposal request) {
        return service.proposeRetentionPolicy(identities.member(jwt), request);
    }

    @PostMapping("/api/v1/attendance/justification-attachment-retention-policies/{id}/revisions")
    @ResponseStatus(HttpStatus.CREATED)
    RetentionPolicyView reviseRetentionPolicy(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody RetentionPolicyRevision request) {
        return service.reviseRetentionPolicy(identities.member(jwt), id, request);
    }

    @PostMapping("/api/v1/attendance/justification-attachment-retention-policies/{id}/validations")
    RetentionPolicyView validateRetentionPolicy(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody RetentionPolicyValidation request) {
        return service.validateRetentionPolicy(identities.member(jwt), id, request);
    }

    @PostMapping("/api/v1/attendance/justifications/{id}/retention-classifications")
    ClassificationView classifyRetention(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody RetentionClassification request) {
        var classification = service.classifyRetention(identities.member(jwt), id, request);
        return new ClassificationView(classification.version(), classification.semesterEndOn(),
                classification.sourceReference(), classification.reason(), classification.createdBy(), classification.createdAt());
    }

    @PostMapping("/api/v1/attendance/justification-attachments/{attachmentId}/retention-holds")
    RetentionHoldView placeHold(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID attachmentId,
            @Valid @RequestBody RetentionHold request) {
        return service.placeHold(identities.member(jwt), attachmentId, request);
    }

    @GetMapping("/api/v1/attendance/justification-attachments/{attachmentId}/retention-holds")
    List<RetentionHoldEventView> retentionHoldHistory(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID attachmentId) {
        return service.retentionHoldHistory(identities.member(jwt), attachmentId);
    }

    @PostMapping("/api/v1/attendance/justification-attachments/{attachmentId}/retention-holds/{holdId}/releases")
    RetentionHoldView releaseHold(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID attachmentId,
            @PathVariable UUID holdId, @Valid @RequestBody RetentionHoldRelease request) {
        return service.releaseHold(identities.member(jwt), attachmentId, holdId, request);
    }

    @PostMapping("/api/v1/attendance/justification-attachments/{attachmentId}/deletions")
    AttachmentView delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID attachmentId,
            @Valid @RequestBody DeleteAttachment request) {
        return service.delete(identities.member(jwt), attachmentId, request);
    }

    private static ResponseEntity<byte[]> response(JustificationAttachmentService.Downloaded download) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.bytes().length).header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.filename(), StandardCharsets.UTF_8).build().toString()).body(download.bytes());
    }

    public record ClassificationView(int version, java.time.LocalDate semesterEndOn, String sourceReference,
                                     String reason, UUID createdBy, java.time.Instant createdAt) {}
}
