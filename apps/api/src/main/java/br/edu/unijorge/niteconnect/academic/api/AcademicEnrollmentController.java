package br.edu.unijorge.niteconnect.academic.api;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import br.edu.unijorge.niteconnect.academic.application.AcademicEnrollmentService;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.api.AcademicContracts.*;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.shared.api.PageResult;

@RestController
public class AcademicEnrollmentController {
    private final AcademicEnrollmentService service;private final IdentityDirectory identities;
    public AcademicEnrollmentController(AcademicEnrollmentService service,IdentityDirectory identities) { this.service=service;this.identities=identities; }
    @PostMapping("/api/v1/me/academic/enrollments") @ResponseStatus(HttpStatus.CREATED) Enrollment request(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody RequestEnrollment c) { return service.request(identities.member(jwt),c); }
    @PutMapping("/api/v1/me/academic/enrollments/{id}") Enrollment revise(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody ReviseEnrollment c) { return service.revise(identities.member(jwt),id,c); }
    @GetMapping("/api/v1/me/academic/enrollments/{id}") Enrollment own(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { return service.read(identities.member(jwt),id,true); }
    @GetMapping("/api/v1/academic/enrollments/{id}") Enrollment read(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { return service.read(identities.member(jwt),id,false); }
    @GetMapping("/api/v1/me/academic/enrollments") PageResult<Enrollment> ownList(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(identities.member(jwt),true,page,size); }
    @GetMapping("/api/v1/academic/enrollments") PageResult<Enrollment> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(identities.member(jwt),false,page,size); }
    @PostMapping("/api/v1/academic/enrollments/{id}/decisions") Enrollment decide(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody DecideEnrollment c) { return service.decide(identities.member(jwt),id,c); }
}
