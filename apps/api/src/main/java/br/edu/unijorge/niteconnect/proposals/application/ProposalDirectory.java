package br.edu.unijorge.niteconnect.proposals.application;

import br.edu.unijorge.niteconnect.proposals.domain.Proposal;
import br.edu.unijorge.niteconnect.proposals.domain.ProposalStatus;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.UUID;

@Repository
public class ProposalDirectory {
    private final JdbcTemplate jdbc;
    private static final RowMapper<Proposal> MAPPER = (rs, row) -> new Proposal(rs.getObject("id", UUID.class),
            rs.getString("title"), rs.getString("description"), rs.getObject("author_id", UUID.class),
            ProposalStatus.valueOf(rs.getString("status")), rs.getObject("decided_by", UUID.class),
            rs.getTimestamp("decided_at") == null ? null : rs.getTimestamp("decided_at").toInstant(),
            rs.getString("decision_reason"), rs.getLong("version"), rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());
    public ProposalDirectory(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Proposal require(UUID id) {
        return jdbc.query("select * from nite_connect.project_proposals where id=?", MAPPER, id).stream()
                .findFirst().orElseThrow(ApiException::notFound);
    }
    public PageResult<Proposal> list(UUID author, ProposalStatus status, int page, int size) {
        PageResult.validate(page, size);
        String where = " where 1=1"; var args = new ArrayList<Object>();
        if (author != null) { where += " and author_id=?"; args.add(author); }
        if (status != null) { where += " and status=?"; args.add(status.name()); }
        long total = jdbc.queryForObject("select count(*) from nite_connect.project_proposals" + where, Long.class, args.toArray());
        args.add(size); args.add((long) page * size);
        return new PageResult<>(jdbc.query("select * from nite_connect.project_proposals" + where + " order by created_at desc,id limit ? offset ?",
                MAPPER, args.toArray()), page, size, total);
    }
    public Proposal create(UUID actor, String title, String description) {
        validate(title, description); UUID id = UUID.randomUUID();
        jdbc.update("insert into nite_connect.project_proposals(id,title,description,author_id) values (?,?,?,?)", id, title.trim(), description.trim(), actor);
        return require(id);
    }
    public Proposal update(UUID id, String title, String description, long version) {
        validate(title, description);
        changed(jdbc.update("update nite_connect.project_proposals set title=?,description=?,version=version+1,updated_at=clock_timestamp() where id=? and version=?",
                title.trim(), description.trim(), id, version));
        return require(id);
    }
    public Proposal decide(UUID actor, UUID id, ProposalStatus next, long version, String reason) {
        changed(jdbc.update("""
                update nite_connect.project_proposals set status=?,decided_by=?,decided_at=clock_timestamp(),decision_reason=?,
                version=version+1,updated_at=clock_timestamp() where id=? and version=? and status='PENDENTE'
                """, next.name(), actor, reason, id, version));
        return require(id);
    }
    public void converted(UUID id, long version) {
        changed(jdbc.update("update nite_connect.project_proposals set status='CONVERTIDA',version=version+1,updated_at=clock_timestamp() where id=? and version=? and status='APROVADA'", id, version));
    }
    public void checkVersion(Proposal proposal, long version) {
        if (proposal.version() != version) throw ApiException.conflict("Proposta alterada; consulte a versão atual");
    }
    private void changed(int count) { if (count != 1) throw ApiException.conflict("Proposta alterada; consulte a versão atual"); }
    private void validate(String title, String description) {
        if (title == null || title.isBlank() || title.length() > 200 || description == null || description.isBlank() || description.length() > 10000)
            throw ApiException.invalid("Título ou descrição inválidos");
    }
}
