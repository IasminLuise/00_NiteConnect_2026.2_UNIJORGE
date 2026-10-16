package br.edu.unijorge.niteconnect.proposals.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.audit.application.AuditTrail;
import br.edu.unijorge.niteconnect.projects.application.ProjectService;
import br.edu.unijorge.niteconnect.projects.domain.Project;
import br.edu.unijorge.niteconnect.proposals.domain.Proposal;
import br.edu.unijorge.niteconnect.proposals.domain.ProposalStatus;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class ProposalService {
    private final ProposalDirectory proposals;
    private final AccessService access;
    private final AuditTrail audit;
    private final ProjectService projects;
    private final AdministrativeTransactions transactions;
    public ProposalService(ProposalDirectory proposals, AccessService access, AuditTrail audit, ProjectService projects, AdministrativeTransactions transactions) {
        this.proposals = proposals; this.access = access; this.audit = audit; this.projects = projects; this.transactions = transactions;
    }
    private Proposal requireReadable(UUID actor, UUID id) {
        access.requireActive(actor); Proposal proposal = proposals.require(id);
        if (!actor.equals(proposal.authorId()) && !access.isAdministrator(actor)) throw ApiException.notFound();
        return proposal;
    }
    @Transactional(readOnly=true)
    public Proposal read(UUID actor, UUID id) { return requireReadable(actor, id); }
    @Transactional(readOnly=true)
    public PageResult<Proposal> list(UUID actor, ProposalStatus status, int page, int size) {
        access.requireActive(actor);
        return proposals.list(access.isAdministrator(actor) ? null : actor, status, page, size);
    }
    @Transactional
    public Proposal create(UUID actor, String title, String description, String reason) {
        transactions.lock(); access.requireActive(actor);
        Proposal result = proposals.create(actor, title, description);
        audit.recordEntity(actor, "PROPOSAL", result.id(), "PROPOSAL_CREATED", null, result.toString(), reason);
        return result;
    }
    @Transactional
    public Proposal update(UUID actor, UUID id, String title, String description, long version, String reason) {
        transactions.lock(); Proposal before = requireReadable(actor, id); proposals.checkVersion(before, version);
        if (before.status() != ProposalStatus.PENDENTE) throw ApiException.conflict("Somente propostas pendentes podem ser editadas");
        Proposal after = proposals.update(id, title, description, version);
        audit.recordEntity(actor, "PROPOSAL", id, "PROPOSAL_UPDATED", before.toString(), after.toString(), reason);
        return after;
    }
    @Transactional
    public Proposal decide(UUID actor, UUID id, ProposalStatus next, long version, String reason) {
        transactions.lock(); access.requireAdministrator(actor);
        Proposal before = proposals.require(id); proposals.checkVersion(before, version);
        if (!before.status().canDecideTo(next)) throw ApiException.conflict("Decisão de proposta não permitida");
        Proposal after = proposals.decide(actor, id, next, version, reason);
        audit.recordEntity(actor, "PROPOSAL", id, "PROPOSAL_DECIDED", before.status().name(), next.name(), reason);
        return after;
    }
    @Transactional
    public Project convert(UUID actor, UUID id, long version, String reason) {
        transactions.lock(); access.requireAdministrator(actor);
        Proposal before = proposals.require(id); proposals.checkVersion(before, version);
        if (!before.status().canConvert()) throw ApiException.conflict("Proposta não está aprovada para conversão");
        Project project = projects.fromProposal(actor, id, before.title(), before.description(), reason);
        proposals.converted(id, version);
        audit.recordEntity(actor, "PROPOSAL", id, "PROPOSAL_CONVERTED", before.status().name(), project.id().toString(), reason);
        return project;
    }
}
