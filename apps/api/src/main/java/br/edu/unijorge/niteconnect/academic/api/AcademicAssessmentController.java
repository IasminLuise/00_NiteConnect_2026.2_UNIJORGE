package br.edu.unijorge.niteconnect.academic.api;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import br.edu.unijorge.niteconnect.academic.application.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.api.AcademicContracts.*;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.shared.api.PageResult;

@RestController
public class AcademicAssessmentController {
    private final PillarAssessmentService assessments;private final AcademicCalculationService calculations;private final IdentityDirectory identities;
    public AcademicAssessmentController(PillarAssessmentService assessments,AcademicCalculationService calculations,IdentityDirectory identities) { this.assessments=assessments;this.calculations=calculations;this.identities=identities; }
    @PostMapping("/api/v1/academic/enrollments/{id}/assessments") @ResponseStatus(HttpStatus.CREATED) Assessment assess(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody AssessPillar c) { return assessments.assess(identities.member(jwt),id,c); }
    @PostMapping("/api/v1/academic/enrollments/{id}/calculations") @ResponseStatus(HttpStatus.CREATED) Calculation calculate(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Command c) { return calculations.calculate(identities.member(jwt),id,c); }
    @GetMapping("/api/v1/academic/enrollments/{id}/assessments") PageResult<Assessment> assessments(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return assessments.list(identities.member(jwt),id,false,page,size); }
    @GetMapping("/api/v1/me/academic/enrollments/{id}/assessments") PageResult<Assessment> ownAssessments(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return assessments.list(identities.member(jwt),id,true,page,size); }
    @GetMapping("/api/v1/academic/enrollments/{id}/calculations") PageResult<Calculation> calculations(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return calculations.list(identities.member(jwt),id,false,page,size); }
    @GetMapping("/api/v1/me/academic/enrollments/{id}/calculations") PageResult<Calculation> ownCalculations(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return calculations.list(identities.member(jwt),id,true,page,size); }
}
