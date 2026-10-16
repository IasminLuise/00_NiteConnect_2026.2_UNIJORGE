package br.edu.unijorge.niteconnect.members.domain;

import java.time.Instant;
import java.util.UUID;

public record Member(UUID id, String name, String institutionalEmail, MemberStatus status,
                     long version, Instant createdAt, Instant updatedAt) {}
