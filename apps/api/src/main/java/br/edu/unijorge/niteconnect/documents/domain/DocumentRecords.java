package br.edu.unijorge.niteconnect.documents.domain;
import java.time.Instant;
import java.util.UUID;
public final class DocumentRecords {
    private DocumentRecords() {}
    public record Template(UUID id,String title,boolean official,long version,Instant retiredAt) {}
    public record TemplateVersion(UUID id,UUID templateId,int revision,UUID objectId,long size,String sha256,UUID author,Instant createdAt) {}
    public record Doc(UUID id,String title,String context,UUID ownerId,UUID projectId,UUID eventId,String demandReference,
        UUID templateVersionId,boolean official,UUID currentVersionId,long version,Instant archivedAt) {}
    public record Version(UUID id,UUID documentId,int revision,UUID objectId,long size,String sha256,String status,UUID basedOn,
        String rejectionCode,UUID author,Instant createdAt) {}
    public record VersionView(UUID id,int revision,String status,UUID basedOn,String rejectionCode,UUID author,Instant createdAt) {}
    public record Grant(UUID id,UUID memberId,boolean canEdit,Instant createdAt,Instant revokedAt) {}
    public record Event(UUID id,UUID actor,String action,UUID reference,String reason,Instant occurredAt) {}
    public record Cycle(UUID id,UUID documentId,UUID sourceVersionId,UUID headVersionId,String key,UUID actor,boolean readOnly,String status,Instant expiresAt) {}
    public record SourceGrant(UUID id,UUID documentId,UUID versionId,UUID actor,UUID cycleId,Instant expiresAt) {}
    public record Pdf(UUID id,UUID versionId,UUID objectId,long size,String sha256) {}
}
