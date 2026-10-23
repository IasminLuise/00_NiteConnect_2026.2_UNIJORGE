package br.edu.unijorge.niteconnect.academic.application;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.unijorge.niteconnect.academic.api.AcademicContracts.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.infrastructure.AcademicRepository;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import br.edu.unijorge.niteconnect.shared.api.*;
import static br.edu.unijorge.niteconnect.academic.domain.AcademicRules.*;

@Service
public class AcademicTermService {
    private final AcademicRepository repo;private final AcademicAuthorizationPolicy auth;private final AdministrativeTransactions tx;
    public AcademicTermService(AcademicRepository repo,AcademicAuthorizationPolicy auth,AdministrativeTransactions tx) { this.repo=repo;this.auth=auth;this.tx=tx; }
    @Transactional public Term create(UUID actor,CreateTerm c) {
        tx.lock();auth.requireCoordinator(actor);reason(c.reason());var data=new TermData(c.title(),c.startsAt(),c.endsAt(),c.zone());term(data);
        Term t=repo.createTerm(data);repo.event(actor,null,"TERM",t.id(),"CREATED",null,t,c.reason());return t;
    }
    @Transactional public Policy createPolicy(UUID actor,UUID termId,CreatePolicy c) {
        tx.lock();auth.requireCoordinator(actor);reason(c.reason());version(repo.term(termId).version(),c.expectedVersion());
        if(c.rules()==null) throw ApiException.invalid("Configuração obrigatória");
        Policy p=repo.createPolicy(termId,c.rules());repo.event(actor,null,"POLICY",p.id(),"CREATED",null,p,c.reason());return p;
    }
    @Transactional public Policy confirmPolicy(UUID actor,UUID policyId,ConfirmPolicy c) {
        tx.lock();auth.requireCoordinator(actor);reason(c.reason());confirmed(c.confirmed());text(c.authorityReference(),1000,"Referência institucional");
        Policy p=repo.policy(policyId);version(p.version(),c.expectedVersion());policy(p.rules());
        Policy result=repo.confirmPolicy(p,c.authorityReference());repo.event(actor,null,"POLICY",p.id(),"CONFIRMED",p,result,c.reason());return result;
    }
    @Transactional(readOnly=true) public PageResult<Term> list(UUID actor,int page,int size) { auth.requireActive(actor);return repo.terms(page,size); }
    @Transactional(readOnly=true) public PageResult<Policy> policies(UUID actor,UUID term,int page,int size) { auth.requireAdministrator(actor);repo.term(term);return repo.policies(term,page,size); }
}
