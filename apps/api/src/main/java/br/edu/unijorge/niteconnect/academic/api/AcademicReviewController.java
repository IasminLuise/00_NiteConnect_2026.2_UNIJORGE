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
public class AcademicReviewController {
    private final AcademicReviewService reviews;private final AcademicHomologationService homologations;private final AcademicForwardingService forwarding;private final IdentityDirectory identities;
    public AcademicReviewController(AcademicReviewService reviews,AcademicHomologationService homologations,AcademicForwardingService forwarding,IdentityDirectory identities) { this.reviews=reviews;this.homologations=homologations;this.forwarding=forwarding;this.identities=identities; }
    @PostMapping("/api/v1/me/academic/enrollments/{id}/reviews") @ResponseStatus(HttpStatus.CREATED) Review ownRequest(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody RequestReview c) { return reviews.request(identities.member(jwt),id,c,true); }
    @PostMapping("/api/v1/academic/enrollments/{id}/reviews") @ResponseStatus(HttpStatus.CREATED) Review request(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody RequestReview c) { return reviews.request(identities.member(jwt),id,c,false); }
    @PostMapping("/api/v1/academic/reviews/{id}/decisions") Review decide(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody DecideReview c) { return reviews.decide(identities.member(jwt),id,c); }
    @PostMapping("/api/v1/academic/calculations/{id}/homologations") @ResponseStatus(HttpStatus.CREATED) Homologation homologate(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Command c) { return homologations.homologate(identities.member(jwt),id,c); }
    @PostMapping("/api/v1/academic/homologations/{id}/reopen") @ResponseStatus(HttpStatus.CREATED) Review reopen(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Command c) { return reviews.reopen(identities.member(jwt),id,c); }
    @GetMapping("/api/v1/academic/enrollments/{id}/homologation") Homologation current(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { return homologations.current(identities.member(jwt),id); }
    @PostMapping("/api/v1/academic/homologations/{id}/forwardings") @ResponseStatus(HttpStatus.CREATED) Forwarding forward(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody ForwardAssessment c) { return forwarding.forward(identities.member(jwt),id,c); }
    @GetMapping("/api/v1/academic/homologations/{id}/forwardings") PageResult<Forwarding> forwardings(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return forwarding.list(identities.member(jwt),id,page,size); }
    @GetMapping("/api/v1/me/academic/enrollments/{id}/reviews") PageResult<Review> ownReviews(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return reviews.list(identities.member(jwt),id,true,page,size); }
    @GetMapping("/api/v1/academic/enrollments/{id}/reviews") PageResult<Review> reviews(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return reviews.list(identities.member(jwt),id,false,page,size); }
    @GetMapping("/api/v1/academic/enrollments/{id}/history") PageResult<AcademicEvent> history(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return forwarding.history(identities.member(jwt),id,page,size); }
}
