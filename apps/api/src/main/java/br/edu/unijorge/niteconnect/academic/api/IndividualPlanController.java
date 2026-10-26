package br.edu.unijorge.niteconnect.academic.api;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import br.edu.unijorge.niteconnect.academic.application.IndividualPlanService;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.api.AcademicContracts.*;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.shared.api.PageResult;

@RestController
public class IndividualPlanController {
    private final IndividualPlanService service;private final IdentityDirectory identities;
    public IndividualPlanController(IndividualPlanService service,IdentityDirectory identities) { this.service=service;this.identities=identities; }
    @PostMapping("/api/v1/me/academic/enrollments/{id}/plans") @ResponseStatus(HttpStatus.CREATED) Plan save(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody SavePlan c) { return service.save(identities.member(jwt),id,c); }
    @PostMapping("/api/v1/me/academic/plans/{id}/submission") Plan submit(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Command c) { return service.submit(identities.member(jwt),id,c); }
    @PostMapping("/api/v1/academic/plans/{id}/decisions") Plan decide(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody DecidePlan c) { return service.decide(identities.member(jwt),id,c); }
    @GetMapping("/api/v1/me/academic/enrollments/{id}/plans") PageResult<Plan> own(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(identities.member(jwt),id,true,page,size); }
    @GetMapping("/api/v1/academic/enrollments/{id}/plans") PageResult<Plan> list(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(identities.member(jwt),id,false,page,size); }
}
