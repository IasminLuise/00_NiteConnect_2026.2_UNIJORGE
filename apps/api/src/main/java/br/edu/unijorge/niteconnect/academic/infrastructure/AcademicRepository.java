package br.edu.unijorge.niteconnect.academic.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicStates.*;
import br.edu.unijorge.niteconnect.shared.api.*;

@Repository
public class AcademicRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public AcademicRepository(JdbcTemplate jdbc,ObjectMapper json) { this.jdbc=jdbc;this.json=json; }
    public String encode(Object value) { return json.writeValueAsString(value); }
    private <T> T decode(String value,Class<T> type) { return json.readValue(value,type); }
    private static UUID id(ResultSet r,String n) throws SQLException { return r.getObject(n,UUID.class); }
    private static Instant time(ResultSet r,String n) throws SQLException { return r.getTimestamp(n).toInstant(); }
    public static Timestamp ts(Instant value) { return Timestamp.from(value); }
    private <T> T one(String sql,RowMapper<T> mapper,Object... args) {
        return jdbc.query(sql,mapper,args).stream().findFirst().orElseThrow(ApiException::notFound);
    }
    public void changed(int count) { if(count!=1) throw ApiException.conflict("Registro alterado; consulte a versão atual"); }
    private final RowMapper<Term> termMapper=(r,n)->new Term(id(r,"id"),new TermData(r.getString("title"),time(r,"starts_at"),time(r,"ends_at"),r.getString("zone")),r.getLong("version"));
    private final RowMapper<Policy> policyMapper=(r,n)->new Policy(id(r,"id"),id(r,"term_id"),r.getInt("revision"),r.getString("status"),decode(r.getString("rules"),PolicyRules.class),r.getString("authority_reference"),r.getLong("version"));
    public Term term(UUID id) { return one("select * from nite_connect.academic_terms where id=?",termMapper,id); }
    public Policy policy(UUID id) { return one("select * from nite_connect.academic_policies where id=?",policyMapper,id); }
    public Term createTerm(TermData t) {
        UUID id=UUID.randomUUID();jdbc.update("insert into nite_connect.academic_terms(id,title,starts_at,ends_at,zone) values (?,?,?,?,?)",id,t.title().strip(),ts(t.startsAt()),ts(t.endsAt()),t.zone());return term(id);
    }
    public Policy createPolicy(UUID term,PolicyRules rules) {
        UUID id=UUID.randomUUID();
        jdbc.update("insert into nite_connect.academic_policies(id,term_id,revision,status,rules) select ?,?,coalesce(max(revision),0)+1,'DRAFT',?::jsonb from nite_connect.academic_policies where term_id=?",id,term,encode(rules),term);
        return policy(id);
    }
    public Policy confirmPolicy(Policy p,String reference) {
        changed(jdbc.update("update nite_connect.academic_policies set status='CONFIRMED',authority_reference=?,version=version+1 where id=? and version=? and status='DRAFT'",reference,p.id(),p.version()));return policy(p.id());
    }
    public PageResult<Term> terms(int page,int size) { return page("*","from nite_connect.academic_terms",List.of(),"starts_at desc,id",termMapper,page,size); }
    public PageResult<Policy> policies(UUID term,boolean confirmedOnly,int page,int size) { return page("*","from nite_connect.academic_policies where term_id=?"+(confirmedOnly?" and status='CONFIRMED'":""),List.of(term),"revision desc,id",policyMapper,page,size); }
    public <T> PageResult<T> page(String columns,String from,List<?> args,String order,RowMapper<T> mapper,int page,int size) {
        PageResult.validate(page,size);long total=jdbc.queryForObject("select count(*) "+from,Long.class,args.toArray());
        var values=new ArrayList<Object>(args);values.add(size);values.add((long)page*size);
        return new PageResult<>(jdbc.query("select "+columns+" "+from+" order by "+order+" limit ? offset ?",mapper,values.toArray()),page,size,total);
    }
    public void event(UUID actor,UUID enrollment,String type,UUID entity,String action,Object before,Object after,String reason) {
        jdbc.update("insert into nite_connect.academic_events(id,enrollment_id,entity_type,entity_id,actor_id,action,before_value,after_value,reason) values (?,?,?,?,?,?,?::jsonb,?::jsonb,?)",
                UUID.randomUUID(),enrollment,type,entity,actor,action,before==null?null:encode(before),after==null?null:encode(after),reason);
    }
    private final RowMapper<Enrollment> enrollmentMapper=(r,n)->new Enrollment(id(r,"id"),id(r,"term_id"),id(r,"member_id"),id(r,"policy_id"),EnrollmentStatus.valueOf(r.getString("status")),List.of(decode(r.getString("disciplines"),Discipline[].class)),r.getLong("version"),time(r,"created_at"),r.getString("feedback"));
    public Enrollment enrollment(UUID id) { return one("select * from nite_connect.academic_enrollments where id=?",enrollmentMapper,id); }
    public Enrollment createEnrollment(UUID member,Policy p,List<Discipline> ds) {
        UUID id=UUID.randomUUID();jdbc.update("insert into nite_connect.academic_enrollments(id,term_id,member_id,policy_id,status,disciplines) values (?,?,?,?,'SOLICITADA',?::jsonb)",id,p.termId(),member,p.id(),encode(ds));return enrollment(id);
    }
    public Enrollment updateEnrollment(Enrollment e,EnrollmentStatus state,List<Discipline> ds) {
        return updateEnrollment(e,state,ds,e.feedback());
    }
    public Enrollment updateEnrollment(Enrollment e,EnrollmentStatus state,List<Discipline> ds,String feedback) {
        changed(jdbc.update("update nite_connect.academic_enrollments set status=?,disciplines=?::jsonb,feedback=?,version=version+1 where id=? and version=?",state.name(),encode(ds),feedback,e.id(),e.version()));return enrollment(e.id());
    }
    public PageResult<Enrollment> enrollments(UUID member,int page,int size) {
        return page("*","from nite_connect.academic_enrollments"+(member==null?"":" where member_id=?"),member==null?List.of():List.of(member),"created_at desc,id",enrollmentMapper,page,size);
    }
    private final RowMapper<Plan> planMapper=(r,n)->new Plan(id(r,"id"),id(r,"enrollment_id"),r.getInt("revision"),PlanStatus.valueOf(r.getString("status")),r.getString("content"),r.getLong("version"),r.getString("feedback"));
    public Plan plan(UUID id) { return one("select * from nite_connect.academic_plans where id=?",planMapper,id); }
    public Optional<Plan> latestPlan(UUID enrollment) { return jdbc.query("select * from nite_connect.academic_plans where enrollment_id=? order by revision desc limit 1",planMapper,enrollment).stream().findFirst(); }
    public Plan createPlan(UUID enrollment,String content) {
        UUID id=UUID.randomUUID();jdbc.update("insert into nite_connect.academic_plans(id,enrollment_id,revision,status,content) select ?,?,coalesce(max(revision),0)+1,'RASCUNHO',? from nite_connect.academic_plans where enrollment_id=?",id,enrollment,content,enrollment);return plan(id);
    }
    public Plan planState(Plan p,PlanStatus state) { return planState(p,state,p.feedback()); }
    public Plan planState(Plan p,PlanStatus state,String feedback) { changed(jdbc.update("update nite_connect.academic_plans set status=?,feedback=?,version=version+1 where id=? and version=?",state.name(),feedback,p.id(),p.version()));return plan(p.id()); }
    public PageResult<Plan> plans(UUID enrollment,int page,int size) { return page("*","from nite_connect.academic_plans where enrollment_id=?",List.of(enrollment),"revision desc,id",planMapper,page,size); }
    private final RowMapper<Evidence> evidenceMapper=(r,n)->new Evidence(id(r,"id"),id(r,"enrollment_id"),decode(r.getString("snapshot"),SourceSnapshot.class),id(r,"captured_by"),time(r,"captured_at"));
    public Evidence evidence(UUID id) { return one("select * from nite_connect.academic_evidence where id=?",evidenceMapper,id); }
    public Evidence capture(UUID enrollment,UUID actor,SourceSnapshot snapshot) {
        UUID id=UUID.randomUUID();jdbc.update("insert into nite_connect.academic_evidence(id,enrollment_id,snapshot,captured_by) values (?,?,?::jsonb,?)",id,enrollment,encode(snapshot),actor);return evidence(id);
    }
    public PageResult<Evidence> evidence(UUID enrollment,int page,int size) { return page("*","from nite_connect.academic_evidence where enrollment_id=?",List.of(enrollment),"captured_at desc,id",evidenceMapper,page,size); }
    private final RowMapper<Assessment> assessmentMapper=(r,n)->new Assessment(id(r,"id"),id(r,"enrollment_id"),Pillar.valueOf(r.getString("pillar")),r.getBigDecimal("score"),List.of(decode(r.getString("evidence_ids"),UUID[].class)),r.getString("reason"),r.getInt("revision"),id(r,"assessed_by"),time(r,"created_at"));
    private static final String ASSESSMENT_COLUMNS="a.*,(select jsonb_agg(x.evidence_id order by x.position) from nite_connect.academic_assessment_evidence x where x.assessment_id=a.id) as evidence_ids";
    public Assessment assess(UUID actor,Enrollment e,Pillar pillar,java.math.BigDecimal score,List<UUID> evidence,String reason) {
        UUID id=UUID.randomUUID();jdbc.update("insert into nite_connect.academic_assessments(id,enrollment_id,pillar,score,reason,revision,assessed_by) select ?,?,?,?,?,coalesce(max(revision),0)+1,? from nite_connect.academic_assessments where enrollment_id=? and pillar=?",
                id,e.id(),pillar.name(),score,reason,actor,e.id(),pillar.name());
        for(int i=0;i<evidence.size();i++) jdbc.update("insert into nite_connect.academic_assessment_evidence(assessment_id,enrollment_id,evidence_id,position) values (?,?,?,?)",id,e.id(),evidence.get(i),i);
        return one("select "+ASSESSMENT_COLUMNS+" from nite_connect.academic_assessments a where a.id=?",assessmentMapper,id);
    }
    public List<Assessment> latestAssessments(UUID enrollment) { return jdbc.query("select distinct on (a.pillar) "+ASSESSMENT_COLUMNS+" from nite_connect.academic_assessments a where a.enrollment_id=? order by a.pillar,a.revision desc",assessmentMapper,enrollment); }
    public PageResult<Assessment> assessments(UUID enrollment,boolean publishedOnly,int page,int size) {
        String published=" and exists(select 1 from nite_connect.academic_homologations h join nite_connect.academic_calculations c on c.id=h.calculation_id cross join lateral jsonb_array_elements(c.data->'assessments') item where h.enrollment_id=a.enrollment_id and item.value->>'id'=a.id::text)";
        return page(ASSESSMENT_COLUMNS,"from nite_connect.academic_assessments a where a.enrollment_id=?"+(publishedOnly?published:""),List.of(enrollment),"a.created_at desc,a.id",assessmentMapper,page,size);
    }
    private final RowMapper<Calculation> calculationMapper=(r,n)->new Calculation(id(r,"id"),id(r,"enrollment_id"),id(r,"policy_id"),r.getLong("enrollment_version"),decode(r.getString("data"),CalculationData.class),time(r,"created_at"));
    public Calculation calculation(UUID id) { return one("select * from nite_connect.academic_calculations where id=?",calculationMapper,id); }
    public Calculation calculate(Enrollment e,CalculationData data) {
        UUID id=UUID.randomUUID();jdbc.update("insert into nite_connect.academic_calculations(id,enrollment_id,policy_id,enrollment_version,data) values (?,?,?,?,?::jsonb)",id,e.id(),e.policyId(),e.version(),encode(data));return calculation(id);
    }
    public PageResult<Calculation> calculations(UUID enrollment,boolean publishedOnly,int page,int size) { return page("c.*","from nite_connect.academic_calculations c where c.enrollment_id=?"+(publishedOnly?" and exists(select 1 from nite_connect.academic_homologations h where h.calculation_id=c.id)":""),List.of(enrollment),"c.created_at desc,c.id",calculationMapper,page,size); }
    public Optional<Calculation> latestCalculation(UUID enrollment) { return jdbc.query("select * from nite_connect.academic_calculations where enrollment_id=? order by created_at desc,id limit 1",calculationMapper,enrollment).stream().findFirst(); }
    private final RowMapper<Homologation> homologationMapper=(r,n)->new Homologation(id(r,"id"),id(r,"enrollment_id"),id(r,"calculation_id"),id(r,"supersedes_id"),id(r,"decided_by"),r.getString("reason"),time(r,"created_at"));
    public Homologation homologation(UUID id) { return one("select * from nite_connect.academic_homologations where id=?",homologationMapper,id); }
    public Optional<Homologation> currentHomologation(UUID enrollment) {
        return jdbc.query("select h.* from nite_connect.academic_homologations h where h.enrollment_id=? and not exists(select 1 from nite_connect.academic_homologations s where s.supersedes_id=h.id) order by h.created_at desc,h.id limit 1",homologationMapper,enrollment).stream().findFirst();
    }
    public Homologation homologate(UUID actor,Enrollment e,UUID calculation,UUID supersedes,String reason) {
        UUID id=UUID.randomUUID();jdbc.update("insert into nite_connect.academic_homologations(id,enrollment_id,calculation_id,supersedes_id,decided_by,reason) values (?,?,?,?,?,?)",id,e.id(),calculation,supersedes,actor,reason);return homologation(id);
    }
    private final RowMapper<Review> reviewMapper=(r,n)->new Review(id(r,"id"),id(r,"enrollment_id"),id(r,"calculation_id"),id(r,"homologation_id"),id(r,"requested_by"),r.getString("reason"),r.getString("status"),id(r,"decided_by"),r.getString("decision_reason"),r.getLong("version"),time(r,"created_at"));
    public Review review(UUID id) { return one("select * from nite_connect.academic_reviews where id=?",reviewMapper,id); }
    public boolean pendingReview(UUID enrollment) { return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from nite_connect.academic_reviews where enrollment_id=? and status='REQUESTED')",Boolean.class,enrollment)); }
    public Review requestReview(UUID actor,Enrollment e,Calculation c,UUID homologation,String reason) {
        UUID id=UUID.randomUUID();jdbc.update("insert into nite_connect.academic_reviews(id,enrollment_id,calculation_id,homologation_id,requested_by,reason,status) values (?,?,?,?,?,?,'REQUESTED')",id,e.id(),c.id(),homologation,actor,reason);return review(id);
    }
    public Review decideReview(Review r,UUID actor,boolean accepted,String reason) {
        changed(jdbc.update("update nite_connect.academic_reviews set status=?,decided_by=?,decision_reason=?,version=version+1 where id=? and version=? and status='REQUESTED'",accepted?"ACCEPTED":"REJECTED",actor,reason,r.id(),r.version()));return review(r.id());
    }
    public PageResult<Review> reviews(UUID enrollment,int page,int size) { return page("*","from nite_connect.academic_reviews where enrollment_id=?",List.of(enrollment),"created_at desc,id",reviewMapper,page,size); }
    private final RowMapper<Forwarding> forwardingMapper=(r,n)->new Forwarding(id(r,"id"),id(r,"homologation_id"),id(r,"forwarded_by"),r.getString("procedure_reference"),r.getString("receipt_reference"),time(r,"created_at"));
    public Forwarding forward(UUID actor,Homologation h,String procedure,String receipt) {
        UUID id=UUID.randomUUID();jdbc.update("insert into nite_connect.academic_forwardings(id,homologation_id,forwarded_by,procedure_reference,receipt_reference) values (?,?,?,?,?)",id,h.id(),actor,procedure,receipt);return one("select * from nite_connect.academic_forwardings where id=?",forwardingMapper,id);
    }
    public PageResult<Forwarding> forwardings(UUID homologation,int page,int size) { return page("*","from nite_connect.academic_forwardings where homologation_id=?",List.of(homologation),"created_at desc,id",forwardingMapper,page,size); }
    private final RowMapper<AcademicEvent> eventMapper=(r,n)->new AcademicEvent(id(r,"id"),id(r,"enrollment_id"),r.getString("entity_type"),id(r,"entity_id"),id(r,"actor_id"),r.getString("action"),r.getString("before_value"),r.getString("after_value"),r.getString("reason"),time(r,"occurred_at"));
    public PageResult<AcademicEvent> history(UUID enrollment,int page,int size) { return page("*","from nite_connect.academic_events where enrollment_id=?",List.of(enrollment),"occurred_at desc,id",eventMapper,page,size); }
}
