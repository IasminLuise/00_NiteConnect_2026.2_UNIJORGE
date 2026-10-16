package br.edu.unijorge.niteconnect.attendance.api;

import br.edu.unijorge.niteconnect.attendance.application.*;
import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.*;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.*;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
public class ParticipationReviewController {
    private final ParticipationReviewService service; private final ParticipationQueries queries; private final IdentityDirectory identities;
    public ParticipationReviewController(ParticipationReviewService service,ParticipationQueries queries,IdentityDirectory identities) { this.service=service;this.queries=queries;this.identities=identities; }
    @PostMapping("/api/v1/me/absence-justifications") @ResponseStatus(HttpStatus.CREATED)
    Justification justify(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Justify c) { return service.justify(identities.member(jwt),c); }
    @PostMapping("/api/v1/me/absence-justifications/{id}/complements") Justification complement(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Complement c) { return service.complement(identities.member(jwt),id,c); }
    @PostMapping("/api/v1/attendance/justifications/{id}/decisions") Justification decide(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Decide c) { return service.decide(identities.member(jwt),id,c); }
    @PostMapping("/api/v1/attendance/records/{id}/reviews") Period review(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Review c) { return service.review(identities.member(jwt),id,c); }
    @PostMapping("/api/v1/attendance/records/{id}/corrections") Period correct(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Correct c) { return service.correct(identities.member(jwt),id,c); }
    @PostMapping("/api/v1/me/attendance/corrections") @ResponseStatus(HttpStatus.CREATED)
    Correction request(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody RequestCorrection c) { return service.request(identities.member(jwt),c); }
    @PostMapping("/api/v1/attendance/corrections/{id}/decisions") Correction correctionDecision(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody DecideCorrection c) { return service.decideCorrection(identities.member(jwt),id,c); }
    @GetMapping("/api/v1/me/absence-justifications") PageResult<Justification> ownJustifications(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return queries.justifications(identities.member(jwt),false,null,page,size); }
    @GetMapping("/api/v1/attendance/justifications") PageResult<Justification> justifications(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID memberId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return queries.justifications(identities.member(jwt),true,memberId,page,size); }
    @GetMapping("/api/v1/me/attendance/corrections") PageResult<Correction> ownCorrections(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return queries.corrections(identities.member(jwt),false,null,page,size); }
    @GetMapping("/api/v1/attendance/corrections") PageResult<Correction> corrections(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID memberId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return queries.corrections(identities.member(jwt),true,memberId,page,size); }
}
