package br.edu.unijorge.niteconnect.attendance.api;

import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.CheckIn;
import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.CheckOut;
import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.Return;
import br.edu.unijorge.niteconnect.attendance.application.AttendanceQueries;
import br.edu.unijorge.niteconnect.attendance.application.AttendanceService;
import br.edu.unijorge.niteconnect.attendance.domain.Attendance.View;
import br.edu.unijorge.niteconnect.attendance.domain.Agenda.ExpectedEvent;
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
public class AttendanceController {
    private final AttendanceService attendance;
    private final AttendanceQueries queries;
    private final IdentityDirectory identities;

    public AttendanceController(AttendanceService attendance, AttendanceQueries queries, IdentityDirectory identities) {
        this.attendance = attendance;
        this.queries = queries;
        this.identities = identities;
    }

    @PostMapping("/api/v1/me/attendance/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    View checkIn(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckIn command) {
        return attendance.checkIn(identities.member(jwt), command);
    }

    @PostMapping("/api/v1/me/attendance/check-out")
    View checkOut(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckOut command) {
        return attendance.checkOut(identities.member(jwt), command);
    }

    @PostMapping("/api/v1/me/attendance/{id}/return")
    View returnTo(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody Return command) {
        return attendance.returnTo(identities.member(jwt), id, command);
    }

    @GetMapping("/api/v1/me/attendance")
    PageResult<View> attendance(@AuthenticationPrincipal Jwt jwt,
                                          @RequestParam(required = false) LocalDate from,
                                          @RequestParam(required = false) LocalDate to,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return queries.attendance(identities.member(jwt), from, to, page, size);
    }

    @GetMapping("/api/v1/me/agenda/expected-events")
    PageResult<ExpectedEvent> expectedEvents(@AuthenticationPrincipal Jwt jwt,
                                              @RequestParam(required = false) LocalDate from,
                                              @RequestParam(required = false) LocalDate to,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return queries.expectedEvents(identities.member(jwt), from, to, page, size);
    }
}
