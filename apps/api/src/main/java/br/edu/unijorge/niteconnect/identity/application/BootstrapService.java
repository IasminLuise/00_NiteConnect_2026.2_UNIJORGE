package br.edu.unijorge.niteconnect.identity.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.access.domain.GlobalRole;
import br.edu.unijorge.niteconnect.audit.application.AuditTrail;
import br.edu.unijorge.niteconnect.members.application.MemberAdministration;
import br.edu.unijorge.niteconnect.members.application.MemberDirectory;
import br.edu.unijorge.niteconnect.members.domain.Member;
import br.edu.unijorge.niteconnect.members.domain.MemberStatus;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BootstrapService {
    private final IdentityDirectory identities;
    private final MemberDirectory members;
    private final MemberAdministration administration;
    private final AccessService access;
    private final AuditTrail audit;
    private final AdministrativeTransactions transactions;
    public BootstrapService(IdentityDirectory identities, MemberDirectory members, MemberAdministration administration,
                            AccessService access, AuditTrail audit, AdministrativeTransactions transactions) {
        this.identities=identities; this.members=members; this.administration=administration;
        this.access=access; this.audit=audit; this.transactions=transactions;
    }
    @Transactional
    public void provision(UUID tenant, UUID object, String name, String email, GlobalRole role, String reason) {
        transactions.lock();
        if (!identities.isEmpty()) throw ApiException.conflict("Provisionamento inicial já realizado");
        Member member = members.create(name, email, MemberStatus.ATIVO);
        identities.link(tenant, object, member.id());
        access.initialGlobalAdministrator(member.id(), role, reason);
        administration.recordStatus(member.id(), member.id(), null, MemberStatus.ATIVO, reason);
        audit.record(member.id(), member.id(), "ADMIN_BOOTSTRAPPED", null, MemberStatus.ATIVO.name(), reason);
    }
}
