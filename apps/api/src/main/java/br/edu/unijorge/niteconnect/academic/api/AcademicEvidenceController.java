package br.edu.unijorge.niteconnect.academic.api;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import br.edu.unijorge.niteconnect.academic.application.AcademicEvidenceService;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.api.AcademicContracts.*;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.shared.api.PageResult;

@RestController
public class AcademicEvidenceController {
    private final AcademicEvidenceService service;private final IdentityDirectory identities;
    public AcademicEvidenceController(AcademicEvidenceService service,IdentityDirectory identities) { this.service=service;this.identities=identities; }
    @PostMapping("/api/v1/me/academic/enrollments/{id}/evidence") @ResponseStatus(HttpStatus.CREATED) Evidence ownCapture(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody CaptureEvidence c) { return service.capture(identities.member(jwt),id,c,true); }
    @PostMapping("/api/v1/academic/enrollments/{id}/evidence") @ResponseStatus(HttpStatus.CREATED) Evidence capture(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody CaptureEvidence c) { return service.capture(identities.member(jwt),id,c,false); }
    @PostMapping("/api/v1/academic/enrollments/{id}/presentations") @ResponseStatus(HttpStatus.CREATED) Evidence presentation(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Presentation c) { return service.presentation(identities.member(jwt),id,c); }
    @GetMapping("/api/v1/academic/evidence/{id}/comparison") AcademicEvidenceService.Comparison compare(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { return service.compare(identities.member(jwt),id); }
    @GetMapping("/api/v1/me/academic/enrollments/{id}/evidence") PageResult<Evidence> own(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(identities.member(jwt),id,true,page,size); }
    @GetMapping("/api/v1/academic/enrollments/{id}/evidence") PageResult<Evidence> list(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(identities.member(jwt),id,false,page,size); }
}
