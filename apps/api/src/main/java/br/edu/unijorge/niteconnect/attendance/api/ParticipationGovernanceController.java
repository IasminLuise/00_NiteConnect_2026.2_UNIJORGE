package br.edu.unijorge.niteconnect.attendance.api;

import br.edu.unijorge.niteconnect.attendance.application.ParticipationGovernanceService;
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
    private final IdentityDirectory identities;

    public ParticipationGovernanceController(ParticipationGovernanceService service, IdentityDirectory identities) {
        this.service = service;
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

    @GetMapping("/api/v1/attendance/participation-adjustments")
    PageResult<AdjustmentView> adjustments(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(required=false) UUID memberId,
                                           @RequestParam(defaultValue="0") int page,
                                           @RequestParam(defaultValue="20") int size) {
        return service.adjustments(identities.member(jwt), true, memberId, page, size);
    }
}
