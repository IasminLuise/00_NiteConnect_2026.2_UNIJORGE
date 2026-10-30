package br.edu.unijorge.niteconnect.projecthealth.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.access.application.ProjectAccess;
import br.edu.unijorge.niteconnect.activities.infrastructure.ActivityRepository;
import br.edu.unijorge.niteconnect.audit.application.AuditTrail;
import br.edu.unijorge.niteconnect.projecthealth.api.ProjectHealthContracts.*;
import br.edu.unijorge.niteconnect.projecthealth.domain.ProjectHealthRules;
import br.edu.unijorge.niteconnect.projecthealth.domain.ProjectHealthRules.Policy;
import br.edu.unijorge.niteconnect.projecthealth.domain.ProjectHealthRules.Signals;
import br.edu.unijorge.niteconnect.projecthealth.domain.ProjectHealthRules.Status;
import br.edu.unijorge.niteconnect.projecthealth.infrastructure.ProjectHealthRepository;
import br.edu.unijorge.niteconnect.projects.application.ProjectDirectory;
import br.edu.unijorge.niteconnect.projects.domain.Project;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectHealthService {
    private final ProjectHealthRepository repo;
    private final ProjectDirectory projects;
    private final ActivityRepository activities;
    private final ProjectAccess projectAccess;
    private final AccessService access;
    private final AdministrativeTransactions transactions;
    private final AuditTrail audit;
    private final Clock clock;

    public ProjectHealthService(ProjectHealthRepository repo, ProjectDirectory projects, ActivityRepository activities,
                                ProjectAccess projectAccess, AccessService access,
                                AdministrativeTransactions transactions, AuditTrail audit, Clock clock) {
        this.repo = repo;
        this.projects = projects;
        this.activities = activities;
        this.projectAccess = projectAccess;
        this.access = access;
        this.transactions = transactions;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Health health(UUID actor, UUID projectId) {
        projectAccess.requireRead(actor, projectId);
        return calculate(actor, projects.require(projectId));
    }

    @Transactional
    public PolicyVersion changePolicy(UUID actor, UUID projectId, PolicyChange change) {
        access.requireAdministrator(actor);
        projects.require(projectId);
        transactions.lock();
        int currentVersion = repo.currentVersion(projectId);
        if (currentVersion != change.expectedVersion()) {
            throw ApiException.conflict("Parâmetros alterados; consulte a versão atual");
        }
        try {
            new Policy(change.attentionOverdueDays(), change.criticalOverdueDays(),
                    change.attentionNoUpdateDays(), change.criticalNoUpdateDays());
        } catch (IllegalArgumentException ex) {
            throw ApiException.invalid(ex.getMessage());
        }
        Instant now = clock.instant();
        PolicyVersion created = repo.addPolicy(projectId, currentVersion + 1,
                change.attentionOverdueDays(), change.criticalOverdueDays(),
                change.attentionNoUpdateDays(), change.criticalNoUpdateDays(), actor, change.reason(), now);
        audit.recordEntity(actor, "PROJECT", projectId, "HEALTH_POLICY_CHANGED",
                "version=" + currentVersion, "version=" + created.version(), change.reason());
        return created;
    }

    @Transactional
    public Evaluation evaluate(UUID actor, UUID projectId) {
        projectAccess.requireManage(actor, projectId);
        Project project = projects.require(projectId);
        PolicyVersion policyVersion = repo.currentPolicy(projectId).orElseThrow(() ->
                ApiException.conflict("Configure os parâmetros de criticidade antes de registrar uma avaliação"));
        Health health = calculate(actor, project);
        String reason = String.join(" ", health.reasons());
        return repo.record(projectId, policyVersion, health.status(), actor, health.maxOverdueDays(),
                health.daysSinceRelevantUpdate(), health.overdueActivityIds(),
                health.latestUpdatedActivityId(), reason, clock.instant());
    }

    @Transactional(readOnly = true)
    public PageResult<PolicyVersion> policyHistory(UUID actor, UUID projectId, int page, int size) {
        access.requireAdministrator(actor);
        projects.require(projectId);
        return repo.policies(projectId, page, size);
    }

    @Transactional(readOnly = true)
    public PageResult<Evaluation> history(UUID actor, UUID projectId, int page, int size) {
        projectAccess.requireRead(actor, projectId);
        return repo.evaluations(projectId, page, size);
    }

    private Health calculate(UUID actor, Project project) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock);
        ActivityRepository.ProjectHealthSignals activitySignals = activities.projectHealth(project.id(), today, now);
        PolicyVersion current = repo.currentPolicy(project.id()).orElse(null);
        Status status;
        int maxOverdueDays = activitySignals.maxOverdueDays();
        Integer daysSinceUpdate = activitySignals.daysSinceRelevantUpdate();
        var sources = new ArrayList<>(activitySignals.overdueActivityIds());
        UUID latestActivity = activitySignals.latestUpdatedActivityId();
        if (latestActivity != null && !sources.contains(latestActivity)) sources.add(latestActivity);
        var reasons = new ArrayList<String>();
        if (project.status() != br.edu.unijorge.niteconnect.projects.domain.ProjectStatus.ATIVO) {
            status = Status.DADOS_INSUFICIENTES;
            reasons.add("O projeto não está ativo; os parâmetros configurados não se aplicam ao estado atual.");
        } else if (current == null) {
            status = Status.DADOS_INSUFICIENTES;
            reasons.add("Os parâmetros de criticidade deste projeto ainda não foram configurados.");
        } else {
            Policy policy = new Policy(current.attentionOverdueDays(), current.criticalOverdueDays(),
                    current.attentionNoUpdateDays(), current.criticalNoUpdateDays());
            boolean available = activitySignals.activityCount() > 0;
            status = ProjectHealthRules.classify(policy,
                    new Signals(maxOverdueDays, daysSinceUpdate, available));
            if (!available) {
                reasons.add("Não há atividades operacionais suficientes para classificar este projeto.");
            } else {
                appendReasons(reasons, policy, maxOverdueDays, daysSinceUpdate);
            }
        }
        return new Health(project.id(), project.status(), status, current == null ? 0 : current.version(),
                maxOverdueDays, daysSinceUpdate,
                current == null ? null : current.attentionOverdueDays(),
                current == null ? null : current.criticalOverdueDays(),
                current == null ? null : current.attentionNoUpdateDays(),
                current == null ? null : current.criticalNoUpdateDays(),
                List.copyOf(reasons), List.copyOf(sources), activitySignals.overdueActivityIds(), latestActivity, now);
    }

    private static void appendReasons(List<String> reasons, Policy policy, int overdueDays, Integer daysSinceUpdate) {
        if (overdueDays >= policy.criticalOverdueDays()) {
            reasons.add("O atraso máximo de " + overdueDays + " dias atingiu o limiar crítico configurado de "
                    + policy.criticalOverdueDays() + " dias.");
        } else if (overdueDays >= policy.attentionOverdueDays()) {
            reasons.add("O atraso máximo de " + overdueDays + " dias atingiu o limiar de atenção configurado de "
                    + policy.attentionOverdueDays() + " dias.");
        } else {
            reasons.add("O atraso máximo de " + overdueDays + " dias permanece abaixo do limiar de atenção configurado.");
        }
        if (daysSinceUpdate == null) {
            reasons.add("Não há evento B03 de atividade para determinar o tempo sem atualização relevante.");
        } else if (policy.criticalNoUpdateDays() != null && daysSinceUpdate >= policy.criticalNoUpdateDays()) {
            reasons.add("A última atualização relevante ocorreu há " + daysSinceUpdate + " dias e atingiu o limiar crítico configurado.");
        } else if (policy.attentionNoUpdateDays() != null && daysSinceUpdate >= policy.attentionNoUpdateDays()) {
            reasons.add("A última atualização relevante ocorreu há " + daysSinceUpdate + " dias e atingiu o limiar de atenção configurado.");
        } else if (policy.attentionNoUpdateDays() != null) {
            reasons.add("O intervalo sem atualização relevante permanece abaixo do limiar de atenção configurado.");
        }
    }
}
