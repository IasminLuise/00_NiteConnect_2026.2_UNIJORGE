package br.edu.unijorge.niteconnect.academic.api;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import br.edu.unijorge.niteconnect.academic.application.AcademicTermService;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.api.AcademicContracts.*;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.shared.api.PageResult;

@RestController
public class AcademicTermController {
    private final AcademicTermService service;private final IdentityDirectory identities;
    public AcademicTermController(AcademicTermService service,IdentityDirectory identities) { this.service=service;this.identities=identities; }
    @PostMapping("/api/v1/academic/terms") @ResponseStatus(HttpStatus.CREATED)
    Term create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody CreateTerm c) { return service.create(identities.member(jwt),c); }
    @GetMapping("/api/v1/academic/terms") PageResult<Term> terms(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(identities.member(jwt),page,size); }
    @PostMapping("/api/v1/academic/terms/{id}/policies") @ResponseStatus(HttpStatus.CREATED)
    Policy policy(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody CreatePolicy c) { return service.createPolicy(identities.member(jwt),id,c); }
    @GetMapping("/api/v1/academic/terms/{id}/policies") PageResult<Policy> policies(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.policies(identities.member(jwt),id,page,size); }
    @PostMapping("/api/v1/academic/policies/{id}/confirmation") Policy confirm(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody ConfirmPolicy c) { return service.confirmPolicy(identities.member(jwt),id,c); }
}
