package br.edu.unijorge.niteconnect.academic.integration;

import java.time.Instant;
import java.util.UUID;
/** Future notification payload contains references, never grades or private evidence. No external delivery yet. */
public record AcademicNotificationEvent(int contractVersion,UUID eventId,UUID enrollmentId,UUID memberId,String kind,Instant occurredAt) {}
