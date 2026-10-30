package br.edu.unijorge.niteconnect.projecthealth.api;

import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.projecthealth.application.ProjectHealthService;
import br.edu.unijorge.niteconnect.projecthealth.api.ProjectHealthContracts.*;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
public class ProjectHealthController {
    private final ProjectHealthService service;
    private final IdentityDirectory identities;

    public ProjectHealthController(ProjectHealthService service, IdentityDirectory identities) {
        this.service = service;
        this.identities = identities;
    }

    @GetMapping("/api/v1/projects/{projectId}/health")
    Health health(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return service.health(identities.member(jwt), projectId);
    }

    @PostMapping("/api/v1/projects/{projectId}/health-policy")
    @ResponseStatus(HttpStatus.CREATED)
    PolicyVersion changePolicy(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
                               @Valid @RequestBody PolicyChange change) {
        return service.changePolicy(identities.member(jwt), projectId, change);
    }

    @GetMapping("/api/v1/projects/{projectId}/health-policy/history")
    PageResult<PolicyVersion> policyHistory(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
                                            @RequestParam(defaultValue="0") int page,
                                            @RequestParam(defaultValue="20") int size) {
        return service.policyHistory(identities.member(jwt), projectId, page, size);
    }

    @PostMapping("/api/v1/projects/{projectId}/health-evaluations")
    @ResponseStatus(HttpStatus.CREATED)
    Evaluation evaluate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return service.evaluate(identities.member(jwt), projectId);
    }

    @GetMapping("/api/v1/projects/{projectId}/health-history")
    PageResult<Evaluation> history(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
                                   @RequestParam(defaultValue="0") int page,
                                   @RequestParam(defaultValue="20") int size) {
        return service.history(identities.member(jwt), projectId, page, size);
    }
}
