package br.edu.unijorge.niteconnect.members.application;

import br.edu.unijorge.niteconnect.members.domain.Member;
import br.edu.unijorge.niteconnect.members.domain.MemberStatus;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.UUID;

/** Explicit member-module contract. Other modules do not query member tables directly. */
@Repository
public class MemberDirectory {
    private final JdbcTemplate jdbc;
    private static final RowMapper<Member> MAPPER = (rs, row) -> new Member(rs.getObject("id", UUID.class),
            rs.getString("name"), rs.getString("institutional_email"), MemberStatus.valueOf(rs.getString("status")),
            rs.getLong("version"), rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());

    public MemberDirectory(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Member require(UUID id) {
        return jdbc.query("select * from nite_connect.members where id=?", MAPPER, id).stream()
                .findFirst().orElseThrow(ApiException::notFound);
    }
    public PageResult<Member> list(int page, int size, MemberStatus status) {
        PageResult.validate(page, size);
        String where = status == null ? "" : " where status=?";
        Object[] countArgs = status == null ? new Object[0] : new Object[]{status.name()};
        Object[] args = status == null ? new Object[]{size, (long) page * size} : new Object[]{status.name(), size, (long) page * size};
        return new PageResult<>(jdbc.query("select * from nite_connect.members" + where + " order by name,id limit ? offset ?", MAPPER, args),
                page, size, jdbc.queryForObject("select count(*) from nite_connect.members" + where, Long.class, countArgs));
    }
    public Member create(String name, String email, MemberStatus status) {
        UUID id = UUID.randomUUID();
        validate(name, email);
        jdbc.update("insert into nite_connect.members(id,name,institutional_email,status) values (?,?,?,?)",
                id, name.trim(), normalizeEmail(email), status.name());
        return require(id);
    }
    public Member update(UUID id, String name, String email, long expectedVersion) {
        validate(name, email);
        changed(jdbc.update("""
                update nite_connect.members set name=?,institutional_email=?,version=version+1,updated_at=current_timestamp
                where id=? and version=?
                """, name.trim(), normalizeEmail(email), id, expectedVersion));
        return require(id);
    }
    public Member status(UUID id, MemberStatus status, long expectedVersion) {
        changed(jdbc.update("update nite_connect.members set status=?,version=version+1,updated_at=current_timestamp where id=? and version=?",
                status.name(), id, expectedVersion));
        return require(id);
    }
    public void touch(UUID id, long expectedVersion) {
        changed(jdbc.update("update nite_connect.members set version=version+1,updated_at=current_timestamp where id=? and version=?", id, expectedVersion));
    }
    public void checkVersion(Member member, long expectedVersion) {
        if (member.version() != expectedVersion) throw ApiException.conflict("Cadastro alterado; consulte a versão atual");
    }
    private void changed(int count) { if (count != 1) throw ApiException.conflict("Cadastro alterado; consulte a versão atual"); }
    public static String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    public static void validate(String name, String email) {
        if (name == null || name.isBlank() || name.length() > 160 || email == null || email.length() > 254
                || !normalizeEmail(email).matches("[^@\\s]+@unijorge\\.com\\.br")) {
            throw ApiException.invalid("Nome e e-mail institucional válidos são obrigatórios");
        }
    }
}
