package br.edu.unijorge.niteconnect.academic.integration;

import java.util.UUID;
/** Contract only. No document emission/storage is activated by declaring this request. */
public record AcademicDocumentRequest(int contractVersion,UUID homologationId,UUID calculationId,UUID memberId,UUID termId,String documentPurpose) {}
