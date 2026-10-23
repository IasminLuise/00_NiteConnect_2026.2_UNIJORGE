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
    public PageResult<Policy> policies(UUID term,int page,int size) { return page("*","from nite_connect.academic_policies where term_id=?",List.of(term),"revision desc,id",policyMapper,page,size); }
    public <T> PageResult<T> page(String columns,String from,List<?> args,String order,RowMapper<T> mapper,int page,int size) {
        PageResult.validate(page,size);long total=jdbc.queryForObject("select count(*) "+from,Long.class,args.toArray());
        var values=new ArrayList<Object>(args);values.add(size);values.add((long)page*size);
        return new PageResult<>(jdbc.query("select "+columns+" "+from+" order by "+order+" limit ? offset ?",mapper,values.toArray()),page,size,total);
    }
    public void event(UUID actor,UUID enrollment,String type,UUID entity,String action,Object before,Object after,String reason) {
        jdbc.update("insert into nite_connect.academic_events(id,enrollment_id,entity_type,entity_id,actor_id,action,before_value,after_value,reason) values (?,?,?,?,?,?,?::jsonb,?::jsonb,?)",
                UUID.randomUUID(),enrollment,type,entity,actor,action,before==null?null:encode(before),after==null?null:encode(after),reason);
    }
    private final RowMapper<Enrollment> enrollmentMapper=(r,n)->new Enrollment(id(r,"id"),id(r,"term_id"),id(r,"member_id"),id(r,"policy_id"),EnrollmentStatus.valueOf(r.getString("status")),List.of(decode(r.getString("disciplines"),Discipline[].class)),r.getLong("version"),time(r,"created_at"));
    public Enrollment enrollment(UUID id) { return one("select * from nite_connect.academic_enrollments where id=?",enrollmentMapper,id); }
    public Enrollment createEnrollment(UUID member,Policy p,List<Discipline> ds) {
        UUID id=UUID.randomUUID();jdbc.update("insert into nite_connect.academic_enrollments(id,term_id,member_id,policy_id,status,disciplines) values (?,?,?,?,'SOLICITADA',?::jsonb)",id,p.termId(),member,p.id(),encode(ds));return enrollment(id);
    }
    public Enrollment updateEnrollment(Enrollment e,EnrollmentStatus state,List<Discipline> ds) {
        changed(jdbc.update("update nite_connect.academic_enrollments set status=?,disciplines=?::jsonb,version=version+1 where id=? and version=?",state.name(),encode(ds),e.id(),e.version()));return enrollment(e.id());
    }
    public PageResult<Enrollment> enrollments(UUID member,int page,int size) {
        return page("*","from nite_connect.academic_enrollments"+(member==null?"":" where member_id=?"),member==null?List.of():List.of(member),"created_at desc,id",enrollmentMapper,page,size);
    }
    private final RowMapper<Plan> planMapper=(r,n)->new Plan(id(r,"id"),id(r,"enrollment_id"),r.getInt("revision"),PlanStatus.valueOf(r.getString("status")),r.getString("content"),r.getLong("version"));
    public Plan plan(UUID id) { return one("select * from nite_connect.academic_plans where id=?",planMapper,id); }
    public Optional<Plan> latestPlan(UUID enrollment) { return jdbc.query("select * from nite_connect.academic_plans where enrollment_id=? order by revision desc limit 1",planMapper,enrollment).stream().findFirst(); }
    public Plan createPlan(UUID enrollment,String content) {
        UUID id=UUID.randomUUID();jdbc.update("insert into nite_connect.academic_plans(id,enrollment_id,revision,status,content) select ?,?,coalesce(max(revision),0)+1,'RASCUNHO',? from nite_connect.academic_plans where enrollment_id=?",id,enrollment,content,enrollment);return plan(id);
    }
    public Plan planState(Plan p,PlanStatus state) { changed(jdbc.update("update nite_connect.academic_plans set status=?,version=version+1 where id=? and version=?",state.name(),p.id(),p.version()));return plan(p.id()); }
    public PageResult<Plan> plans(UUID enrollment,int page,int size) { return page("*","from nite_connect.academic_plans where enrollment_id=?",List.of(enrollment),"revision desc,id",planMapper,page,size); }
    private final RowMapper<Evidence> evidenceMapper=(r,n)->new Evidence(id(r,"id"),id(r,"enrollment_id"),decode(r.getString("snapshot"),SourceSnapshot.class),id(r,"captured_by"),time(r,"captured_at"));
    public Evidence evidence(UUID id) { return one("select * from nite_connect.academic_evidence where id=?",evidenceMapper,id); }
    public Evidence capture(UUID enrollment,UUID actor,SourceSnapshot snapshot) {
        UUID id=UUID.randomUUID();jdbc.update("insert into nite_connect.academic_evidence(id,enrollment_id,snapshot,captured_by) values (?,?,?::jsonb,?)",id,enrollment,encode(snapshot),actor);return evidence(id);
    }
    public PageResult<Evidence> evidence(UUID enrollment,int page,int size) { return page("*","from nite_connect.academic_evidence where enrollment_id=?",List.of(enrollment),"captured_at desc,id",evidenceMapper,page,size); }
}
