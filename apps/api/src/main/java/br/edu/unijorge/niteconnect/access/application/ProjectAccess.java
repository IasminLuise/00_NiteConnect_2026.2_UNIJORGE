package br.edu.unijorge.niteconnect.access.application;

import br.edu.unijorge.niteconnect.projects.application.ProjectDirectory;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import org.springframework.stereotype.Service;
import java.util.UUID;

/** Leadership is contextual; the member's current administrative state remains authoritative. */
@Service
public class ProjectAccess {
    private final AccessService access;
    private final ProjectDirectory projects;
    public ProjectAccess(AccessService access, ProjectDirectory projects) { this.access = access; this.projects = projects; }
    public void requireRead(UUID actor, UUID project) {
        access.requireActive(actor);
        if (!access.isAdministrator(actor) && !projects.participates(project, actor)) throw ApiException.notFound();
        projects.require(project);
    }
    public void requireManage(UUID actor, UUID project) {
        requireRead(actor, project);
        if (!access.isAdministrator(actor) && !actor.equals(projects.leader(project))) throw ApiException.forbidden();
    }
}
