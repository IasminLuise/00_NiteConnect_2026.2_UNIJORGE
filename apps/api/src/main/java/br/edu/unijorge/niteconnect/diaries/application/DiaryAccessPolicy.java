package br.edu.unijorge.niteconnect.diaries.application;

import br.edu.unijorge.niteconnect.access.domain.GlobalRole;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DiaryAccessPolicy {
    public void requireRead(UUID actor, UUID author, List<GlobalRole> roles) {
        if (!actor.equals(author) && roles.stream().noneMatch(DiaryAccessPolicy::isDiaryAuditor)) {
            throw ApiException.forbidden();
        }
    }

    public void requireEdit(UUID actor, UUID author) {
        if (!actor.equals(author)) throw ApiException.forbidden();
    }

    public void requireImport(UUID actor, List<GlobalRole> roles) {
        if (actor == null || roles.stream().noneMatch(DiaryAccessPolicy::isDiaryAuditor)) throw ApiException.forbidden();
    }

    private static boolean isDiaryAuditor(GlobalRole role) {
        return role == GlobalRole.COORDENADOR || role == GlobalRole.GESTOR_GERAL;
    }
}
