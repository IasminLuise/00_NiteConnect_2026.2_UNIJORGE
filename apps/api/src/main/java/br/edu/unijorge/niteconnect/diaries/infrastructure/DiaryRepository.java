package br.edu.unijorge.niteconnect.diaries.infrastructure;

import br.edu.unijorge.niteconnect.shared.api.ApiException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DiaryRepository {
    private static final String SELECT = "select i.member_id,i.document_id,i.imported_by,i.imported_at,"
            + "i.source_created_at,i.source_modified_at,i.source_metadata_origin,d.title,d.created_at,d.version document_version,"
            + "v.id current_version_id,v.revision,v.created_at updated_at "
            + "from nite_connect.diaries i join nite_connect.documents d on d.id=i.document_id "
            + "join nite_connect.document_versions v on v.id=d.current_version_id";
    private final JdbcTemplate jdbc;

    public DiaryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean exists(UUID memberId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from nite_connect.diaries where member_id=?)", Boolean.class, memberId));
    }

    public void create(UUID memberId, UUID documentId, UUID importer, Instant sourceCreatedAt,
                       Instant sourceModifiedAt, String origin, String reason) {
        jdbc.update("insert into nite_connect.diaries(member_id,document_id,imported_by,import_reason,source_created_at,source_modified_at,source_metadata_origin) values (?,?,?,?,?,?,?)",
                memberId, documentId, importer, reason, timestamp(sourceCreatedAt), timestamp(sourceModifiedAt), origin);
    }

    public Diary require(UUID memberId) {
        return jdbc.query(SELECT + " where i.member_id=?", (row, number) -> new Diary(
                row.getObject("member_id", UUID.class), row.getObject("document_id", UUID.class),
                row.getObject("imported_by", UUID.class), row.getTimestamp("imported_at").toInstant(),
                instant(row.getTimestamp("source_created_at")), instant(row.getTimestamp("source_modified_at")),
                row.getString("source_metadata_origin"), row.getString("title"), row.getTimestamp("created_at").toInstant(),
                row.getTimestamp("updated_at").toInstant(), row.getObject("current_version_id", UUID.class), row.getInt("revision"),
                row.getLong("document_version")), memberId)
                .stream().findFirst().orElseThrow(ApiException::notFound);
    }

    private static Timestamp timestamp(Instant instant) { return instant == null ? null : Timestamp.from(instant); }
    private static Instant instant(Timestamp timestamp) { return timestamp == null ? null : timestamp.toInstant(); }

    public record Diary(UUID memberId, UUID documentId, UUID importedBy, Instant importedAt,
                        Instant sourceCreatedAt, Instant sourceModifiedAt, String sourceMetadataOrigin,
                        String title, Instant createdAt, Instant updatedAt, UUID currentVersionId, int currentRevision,
                        long documentVersion) {}
}
