package br.edu.unijorge.niteconnect.proposals.domain;

import java.time.Instant;
import java.util.UUID;

public record Proposal(UUID id, String title, String description, UUID authorId, ProposalStatus status,
                       UUID decidedBy, Instant decidedAt, String decisionReason, long version,
                       Instant createdAt, Instant updatedAt) {}
