package br.edu.unijorge.niteconnect.attendance.api;

import br.edu.unijorge.niteconnect.attendance.application.AgendaService;
import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.*;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.*;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agenda/events")
public class AgendaController {
    private final AgendaService agenda; private final IdentityDirectory identities;
    public AgendaController(AgendaService agenda,IdentityDirectory identities) { this.agenda=agenda;this.identities=identities; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    Event create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody EventCreate c) { return agenda.create(identities.member(jwt),c); }
    @PatchMapping("/{id}") Event update(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody EventUpdate c) { return agenda.update(identities.member(jwt),id,c); }
    @GetMapping("/{id}") Event read(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { return agenda.read(identities.member(jwt),id); }
    @GetMapping PageResult<Event> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return agenda.list(identities.member(jwt),from,to,page,size); }
    @PostMapping("/{id}/session") ResponseEntity<SessionPayload> session(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Command c) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(agenda.session(identities.member(jwt),id,c));
    }
    @PostMapping("/{id}/close") Event close(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Command c) { return agenda.close(identities.member(jwt),id,c,false); }
    @PostMapping("/{id}/cancel") Event cancel(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Command c) { return agenda.close(identities.member(jwt),id,c,true); }
    @GetMapping("/{id}/participants") PageResult<ExpectedParticipant> participants(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return agenda.participants(identities.member(jwt),id,page,size); }
}
