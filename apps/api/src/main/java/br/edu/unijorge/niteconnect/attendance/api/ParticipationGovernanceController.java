package br.edu.unijorge.niteconnect.attendance.api;

import br.edu.unijorge.niteconnect.attendance.application.ParticipationGovernanceService;
import br.edu.unijorge.niteconnect.attendance.application.ParticipationReviewService;
import br.edu.unijorge.niteconnect.attendance.api.ParticipationGovernanceContracts.*;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

@RestController
public class ParticipationGovernanceController {
    private final ParticipationGovernanceService service;
    private final ParticipationReviewService reviews;
    private final IdentityDirectory identities;

    public ParticipationGovernanceController(ParticipationGovernanceService service,ParticipationReviewService reviews,IdentityDirectory identities) {
        this.service = service;
        this.reviews = reviews;
        this.identities = identities;
    }

    @PostMapping("/api/v1/attendance/calendar-exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    CalendarVersion calendar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CalendarChange change) {
        return service.changeCalendar(identities.member(jwt), change);
    }

    @GetMapping("/api/v1/attendance/calendar-exceptions")
    PageResult<CalendarVersion> calendarHistory(@AuthenticationPrincipal Jwt jwt,
                                                @RequestParam(required=false) LocalDate from,
                                                @RequestParam(required=false) LocalDate to,
                                                @RequestParam(defaultValue="0") int page,
                                                @RequestParam(defaultValue="20") int size) {
        return service.calendarHistory(identities.member(jwt), from, to, page, size);
    }

    @GetMapping("/api/v1/attendance/participation-load-policies")
    PageResult<LoadPolicyAdministrationView> loadPolicyHistory(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return service.loadPolicyHistory(identities.member(jwt), page, size);
    }

    @PostMapping("/api/v1/attendance/participation-load-policies")
    @ResponseStatus(HttpStatus.CREATED)
    LoadPolicyAdministrationView createLoadPolicyDraft(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateLoadPolicyDraft request) {
        return service.createLoadPolicyDraft(identities.member(jwt), request);
    }

    @PostMapping("/api/v1/attendance/participation-load-policies/{id}/revisions")
    @ResponseStatus(HttpStatus.CREATED)
    LoadPolicyAdministrationView reviseLoadPolicyDraft(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ReviseLoadPolicyDraft request) {
        return service.reviseLoadPolicyDraft(identities.member(jwt), id, request);
    }

    @PostMapping("/api/v1/attendance/participation-load-policies/{id}/approvals")
    LoadPolicyAdministrationView approveLoadPolicyDraft(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ApproveLoadPolicyDraft request) {
        return service.approveLoadPolicyDraft(identities.member(jwt), id, request);
    }

    @PostMapping("/api/v1/attendance/participation-load-policies/{id}/activations")
    LoadPolicyAdministrationView activateLoadPolicyDraft(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ActivateLoadPolicyDraft request) {
        return service.activateLoadPolicyDraft(identities.member(jwt), id, request);
    }

    @PostMapping("/api/v1/attendance/work-log-review-batches")
    @ResponseStatus(HttpStatus.CREATED)
    WeeklyReviewBatchResult reviewWeek(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody WeeklyReviewBatch batch) {
        return service.reviewWeek(identities.member(jwt), batch);
    }

    @GetMapping("/api/v1/me/work-logs/review-requests")
    PageResult<ReviewRequest> reviewRequests(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(defaultValue="0") int page,
                                             @RequestParam(defaultValue="20") int size) {
        return service.reviewRequests(identities.member(jwt), page, size);
    }

    @GetMapping("/api/v1/attendance/work-log-review-requests")
    PageResult<AdministrativeReviewRequest> reviewRequestsAdmin(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required=false) UUID memberId,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        return service.reviewRequestsAdmin(identities.member(jwt),memberId,page,size);
    }

    @PostMapping("/api/v1/me/work-logs/review-requests/{id}/responses")
    ReviewRequest respondToWorkLogCorrection(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,
            @Valid @RequestBody SubmitWorkLogCorrection response) {
        return reviews.respondToWorkLogCorrection(identities.member(jwt),id,response);
    }

    @PostMapping("/api/v1/attendance/work-log-review-requests/{id}/decisions")
    ReviewRequest decideWorkLogCorrection(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,
            @Valid @RequestBody DecideWorkLogCorrection decision) {
        return reviews.decideWorkLogCorrection(identities.member(jwt),id,decision);
    }

    @PostMapping("/api/v1/me/participation-replacements")
    @ResponseStatus(HttpStatus.CREATED)
    AdjustmentView requestReplacement(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReplacementRequest request) {
        return service.requestReplacement(identities.member(jwt), request);
    }

    @PostMapping("/api/v1/attendance/participation-replacements/{id}/decisions")
    AdjustmentView decideReplacement(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                     @Valid @RequestBody ReplacementDecision decision) {
        return service.decideReplacement(identities.member(jwt), id, decision);
    }

    @PostMapping("/api/v1/attendance/participation-dispensations")
    @ResponseStatus(HttpStatus.CREATED)
    AdjustmentView grantDispensation(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DispensationGrant grant) {
        return service.grantDispensation(identities.member(jwt), grant);
    }

    @PostMapping("/api/v1/attendance/participation-replacements/{id}/periods/{periodId}")
    @ResponseStatus(HttpStatus.CREATED)
    ReplacementAllocation allocateReplacement(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                              @PathVariable UUID periodId) {
        return service.allocateReplacement(identities.member(jwt), id, periodId);
    }

    @GetMapping("/api/v1/me/participation-adjustments")
    PageResult<AdjustmentView> ownAdjustments(@AuthenticationPrincipal Jwt jwt,
                                              @RequestParam(defaultValue="0") int page,
                                              @RequestParam(defaultValue="20") int size) {
        return service.adjustments(identities.member(jwt), false, null, page, size);
    }

    @GetMapping("/api/v1/me/participation-replacements")
    PageResult<ReplacementView> ownReplacements(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return service.replacements(identities.member(jwt),page,size);
    }

    @GetMapping("/api/v1/attendance/participation-adjustments")
    PageResult<AdjustmentView> adjustments(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(required=false) UUID memberId,
                                           @RequestParam(defaultValue="0") int page,
                                           @RequestParam(defaultValue="20") int size) {
        return service.adjustments(identities.member(jwt), true, memberId, page, size);
    }
}
