package br.edu.unijorge.niteconnect.diaries.api;

import br.edu.unijorge.niteconnect.diaries.application.DiaryService;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class DiaryController {
    private final DiaryService diaries;
    private final IdentityDirectory identities;

    public DiaryController(DiaryService diaries, IdentityDirectory identities) {
        this.diaries = diaries;
        this.identities = identities;
    }

    @PostMapping(value = "/api/v1/diaries/{memberId}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    DiaryService.View importInitial(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId,
                                    @RequestParam String title, @RequestParam String reason,
                                    @RequestParam(required = false) Instant sourceCreatedAt,
                                    @RequestParam(required = false) Instant sourceModifiedAt,
                                    @RequestPart MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 8 * 1024 * 1024) {
            throw ApiException.invalid("DOCX obrigatório, máximo 8 MiB");
        }
        try {
            return diaries.importInitial(identities.member(jwt), memberId, title, file.getBytes(), reason,
                    sourceCreatedAt, sourceModifiedAt);
        } catch (IOException error) {
            throw ApiException.invalid("Arquivo DOCX indisponível");
        }
    }

    @GetMapping("/api/v1/diaries/me")
    DiaryService.View mine(@AuthenticationPrincipal Jwt jwt) {
        UUID member = identities.member(jwt);
        return diaries.read(member, member);
    }

    @GetMapping("/api/v1/diaries/{memberId}")
    DiaryService.View read(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId) {
        return diaries.read(identities.member(jwt), memberId);
    }

    @GetMapping("/api/v1/diaries/me/activities")
    DiaryService.ActivityPanel activities(@AuthenticationPrincipal Jwt jwt, @RequestParam LocalDate from,
                                          @RequestParam LocalDate to, @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return diaries.activities(identities.member(jwt), from, to, page, size);
    }
}
