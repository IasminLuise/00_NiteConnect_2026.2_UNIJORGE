package br.edu.unijorge.niteconnect.attendance.api;

import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentPurgeContracts.Command;
import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentPurgeContracts.RunDetail;
import br.edu.unijorge.niteconnect.attendance.api.JustificationAttachmentPurgeContracts.RunSummary;
import br.edu.unijorge.niteconnect.attendance.application.JustificationAttachmentRetentionPurge;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
public class JustificationAttachmentPurgeController {
    private final JustificationAttachmentRetentionPurge purge;
    private final IdentityDirectory identities;

    public JustificationAttachmentPurgeController(JustificationAttachmentRetentionPurge purge, IdentityDirectory identities) {
        this.purge = purge;
        this.identities = identities;
    }

    @PostMapping("/api/v1/attendance/justification-attachment-retention-purges")
    RunDetail run(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Command request) {
        return purge.run(identities.member(jwt), request);
    }

    @GetMapping("/api/v1/attendance/justification-attachment-retention-purges")
    PageResult<RunSummary> history(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return purge.history(identities.member(jwt), page, size);
    }

    @GetMapping("/api/v1/attendance/justification-attachment-retention-purges/{runId}")
    RunDetail detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID runId) {
        return purge.detail(identities.member(jwt), runId);
    }
}
