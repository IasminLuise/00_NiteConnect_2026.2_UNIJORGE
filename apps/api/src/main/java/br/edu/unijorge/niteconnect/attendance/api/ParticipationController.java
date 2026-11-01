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
import java.time.LocalDate;
import java.util.UUID;

@RestController
public class ParticipationController {
    private final ParticipationService service; private final ParticipationQueries queries; private final IdentityDirectory identities;
    public ParticipationController(ParticipationService service,ParticipationQueries queries,IdentityDirectory identities) { this.service=service;this.queries=queries;this.identities=identities; }
    @PostMapping("/api/v1/me/attendance/check-in") @ResponseStatus(HttpStatus.CREATED)
    AttendanceView checkIn(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody CheckIn c) { return service.checkIn(identities.member(jwt),c); }
    @PostMapping("/api/v1/me/attendance/check-out") AttendanceView checkOut(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody CheckOut c) { return service.checkOut(identities.member(jwt),c); }
    @PostMapping("/api/v1/me/attendance/{id}/return") AttendanceView returnTo(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Return c) { return service.returnTo(identities.member(jwt),id,c); }
    @PostMapping("/api/v1/me/work-logs") @ResponseStatus(HttpStatus.CREATED)
    Period workLog(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody WorkLog c) { return service.workLog(identities.member(jwt),c); }
    @GetMapping("/api/v1/me/attendance") PageResult<AttendanceView> attendance(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return queries.attendance(identities.member(jwt),from,to,page,size); }
    @GetMapping("/api/v1/me/participation/expected-events") PageResult<ExpectedEvent> expectedEvents(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return queries.expectedEvents(identities.member(jwt),from,to,page,size);
    }
    @GetMapping("/api/v1/me/participation/timeline") PageResult<TimelineItem> timeline(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return queries.timeline(identities.member(jwt),from,to,page,size);
    }
    @GetMapping("/api/v1/me/participation/calendar-exceptions") PageResult<ParticipationGovernanceContracts.PersonalCalendarVersion> calendarExceptions(
            @AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return queries.personalCalendarHistory(identities.member(jwt),from,to,page,size);
    }
    @GetMapping("/api/v1/me/work-logs") PageResult<Period> logs(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return queries.periods(identities.member(jwt),false,null,PeriodKind.WORK_LOG,from,to,page,size); }
    @GetMapping("/api/v1/attendance/records") PageResult<Period> records(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID memberId,@RequestParam(required=false) PeriodKind kind,
            @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return queries.periods(identities.member(jwt),true,memberId,kind,from,to,page,size);
    }
    @GetMapping("/api/v1/me/participation-summary") ParticipationQueries.Summary summary(@AuthenticationPrincipal Jwt jwt,@RequestParam LocalDate from,@RequestParam LocalDate to) {
        return queries.summary(identities.member(jwt),null,false,from,to);
    }
    @GetMapping("/api/v1/me/participation-weekly-summary") ParticipationQueries.WeeklySummary weeklySummary(
            @AuthenticationPrincipal Jwt jwt,@RequestParam LocalDate from,@RequestParam LocalDate to) {
        return queries.weeklySummary(identities.member(jwt),from,to);
    }
    @GetMapping("/api/v1/attendance/summary") ParticipationQueries.Summary adminSummary(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID memberId,@RequestParam LocalDate from,@RequestParam LocalDate to) {
        return queries.summary(identities.member(jwt),memberId,true,from,to);
    }
    @GetMapping("/api/v1/me/participation-history") PageResult<History> history(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return queries.history(identities.member(jwt),false,null,from,to,page,size); }
    @GetMapping("/api/v1/attendance/history") PageResult<History> adminHistory(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID memberId,
            @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return queries.history(identities.member(jwt),true,memberId,from,to,page,size);
    }
}
