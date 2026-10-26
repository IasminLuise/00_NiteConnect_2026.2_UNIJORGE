package br.edu.unijorge.niteconnect.documents.infrastructure;
import java.util.*;
import java.time.Instant;
import java.sql.Timestamp;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import br.edu.unijorge.niteconnect.documents.domain.DocumentRecords.*;
import br.edu.unijorge.niteconnect.shared.api.*;
@Repository
public class DocumentRepository {
    private final JdbcTemplate jdbc;
    public DocumentRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public int update(String sql,Object... args){return jdbc.update(sql,args);}
    public long count(String sql,Object... args){return jdbc.queryForObject(sql,Long.class,args);}
    public static Timestamp ts(Instant i){return i==null?null:Timestamp.from(i);}
    private static final RowMapper<Template> TEMPLATE=(r,n)->new Template(r.getObject("id",UUID.class),r.getString("title"),r.getBoolean("official"),r.getLong("version"),r.getTimestamp("retired_at")==null?null:r.getTimestamp("retired_at").toInstant());
    private static final RowMapper<TemplateVersion> TV=(r,n)->new TemplateVersion(r.getObject("id",UUID.class),r.getObject("template_id",UUID.class),r.getInt("revision"),r.getObject("object_id",UUID.class),r.getLong("size"),r.getString("sha256"),r.getObject("created_by",UUID.class),r.getTimestamp("created_at").toInstant());
    private static final RowMapper<Doc> DOC=(r,n)->new Doc(r.getObject("id",UUID.class),r.getString("title"),r.getString("context"),r.getObject("owner_id",UUID.class),r.getObject("project_id",UUID.class),r.getObject("event_id",UUID.class),r.getString("demand_reference"),r.getObject("template_version_id",UUID.class),r.getBoolean("official"),r.getObject("current_version_id",UUID.class),r.getLong("version"),r.getTimestamp("archived_at")==null?null:r.getTimestamp("archived_at").toInstant());
    private static final RowMapper<Version> VERSION=(r,n)->new Version(r.getObject("id",UUID.class),r.getObject("document_id",UUID.class),r.getInt("revision"),r.getObject("object_id",UUID.class),r.getLong("size"),r.getString("sha256"),r.getString("status"),r.getObject("based_on",UUID.class),r.getString("rejection_code"),r.getObject("created_by",UUID.class),r.getTimestamp("created_at").toInstant());
    private static final RowMapper<Cycle> CYCLE=(r,n)->new Cycle(r.getObject("id",UUID.class),r.getObject("document_id",UUID.class),r.getObject("source_version_id",UUID.class),r.getObject("head_version_id",UUID.class),r.getString("editor_key"),r.getObject("actor_id",UUID.class),r.getBoolean("read_only"),r.getString("status"),r.getTimestamp("expires_at").toInstant());
    public Template template(UUID id){return one("select * from nite_connect.document_templates where id=?",TEMPLATE,id);}
    public TemplateVersion templateVersion(UUID id){return one("select * from nite_connect.document_template_versions where id=?",TV,id);}
    public Doc document(UUID id){return one("select * from nite_connect.documents where id=?",DOC,id);}
    public Version version(UUID id){return one("select * from nite_connect.document_versions where id=?",VERSION,id);}
    public Cycle cycle(UUID id){return one("select * from nite_connect.document_cycles where id=?",CYCLE,id);}
    public Optional<Cycle> activeCycle(UUID doc,boolean readOnly){return jdbc.query("select * from nite_connect.document_cycles where document_id=? and read_only=? and status='OPEN'",CYCLE,doc,readOnly).stream().findFirst();}
    public SourceGrant sourceGrant(UUID id){return one("select * from nite_connect.document_source_grants where id=?",(r,n)->new SourceGrant(r.getObject("id",UUID.class),r.getObject("document_id",UUID.class),r.getObject("version_id",UUID.class),r.getObject("actor_id",UUID.class),r.getObject("cycle_id",UUID.class),r.getTimestamp("expires_at").toInstant()),id);}
    public Optional<Pdf> pdf(UUID version){return jdbc.query("select * from nite_connect.document_pdfs where version_id=?",(r,n)->new Pdf(r.getObject("id",UUID.class),r.getObject("version_id",UUID.class),r.getObject("object_id",UUID.class),r.getLong("size"),r.getString("sha256")),version).stream().findFirst();}
    public boolean grant(UUID doc,UUID actor,boolean write){return count("select count(*) from nite_connect.document_grants where document_id=? and member_id=? and revoked_at is null"+(write?" and can_edit=true":""),doc,actor)>0;}
    public boolean diary(UUID document){return count("select count(*) from nite_connect.diaries where document_id=?",document)>0;}
    public PageResult<Template> templates(int page,int size){PageResult.validate(page,size);return new PageResult<>(jdbc.query("select * from nite_connect.document_templates order by created_at,id limit ? offset ?",TEMPLATE,size,(long)page*size),page,size,count("select count(*) from nite_connect.document_templates"));}
    public PageResult<TemplateVersion> templateVersions(UUID template,int page,int size){PageResult.validate(page,size);return new PageResult<>(jdbc.query("select * from nite_connect.document_template_versions where template_id=? order by revision desc limit ? offset ?",TV,template,size,(long)page*size),page,size,count("select count(*) from nite_connect.document_template_versions where template_id=?",template));}
    public PageResult<VersionView> versions(UUID doc,int page,int size){PageResult.validate(page,size);var v=jdbc.query("select * from nite_connect.document_versions where document_id=? order by revision desc limit ? offset ?",VERSION,doc,size,(long)page*size);return new PageResult<>(v.stream().map(DocumentRepository::view).toList(),page,size,count("select count(*) from nite_connect.document_versions where document_id=?",doc));}
    public PageResult<Event> events(UUID doc,int page,int size){PageResult.validate(page,size);return new PageResult<>(jdbc.query("select * from nite_connect.document_events where document_id=? order by occurred_at desc,id limit ? offset ?",(r,n)->new Event(r.getObject("id",UUID.class),r.getObject("actor_id",UUID.class),r.getString("action"),r.getObject("reference_id",UUID.class),r.getString("reason"),r.getTimestamp("occurred_at").toInstant()),doc,size,(long)page*size),page,size,count("select count(*) from nite_connect.document_events where document_id=?",doc));}
    public PageResult<Doc> list(UUID actor,boolean admin,List<UUID> projectLeads,List<UUID> projects,int page,int size){
        PageResult.validate(page,size);var args=new ArrayList<Object>();String where="";
        if(!admin){where=" where (d.owner_id=? or exists(select 1 from nite_connect.document_grants g where g.document_id=d.id and g.member_id=? and g.revoked_at is null)";args.add(actor);args.add(actor);
            if(!projectLeads.isEmpty()){where+=" or d.project_id in ("+placeholders(projectLeads.size())+")";args.addAll(projectLeads);}where+=")";
            where+=" and (d.context<>'PROJECT'";
            if(!projects.isEmpty()){where+=" or d.project_id in ("+placeholders(projects.size())+")";args.addAll(projects);}where+=")";
        }
        long total=count("select count(*) from nite_connect.documents d"+where,args.toArray());args.add(size);args.add((long)page*size);
        return new PageResult<>(jdbc.query("select d.* from nite_connect.documents d"+where+" order by d.created_at desc,d.id limit ? offset ?",DOC,args.toArray()),page,size,total);
    }
    private static String placeholders(int n){return String.join(",",Collections.nCopies(n,"?"));}
    public void event(UUID actor,UUID doc,String action,UUID reference,String reason){update("insert into nite_connect.document_events(id,document_id,actor_id,action,reference_id,reason) values (?,?,?,?,?,?)",UUID.randomUUID(),doc,actor,action,reference,reason);}
    public void revokeCycles(UUID doc){update("update nite_connect.document_cycles set status='REVOKED' where document_id=? and status='OPEN'",doc);}
    public int nextRevision(UUID doc){return (int)count("select coalesce(max(revision),0)+1 from nite_connect.document_versions where document_id=?",doc);}
    public static VersionView view(Version v){return new VersionView(v.id(),v.revision(),v.status(),v.basedOn(),v.rejectionCode(),v.author(),v.createdAt());}
    private <T>T one(String sql,RowMapper<T> mapper,Object... args){return jdbc.query(sql,mapper,args).stream().findFirst().orElseThrow(ApiException::notFound);}
}
