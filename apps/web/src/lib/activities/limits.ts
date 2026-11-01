export const MAX_ACTIVITY_EVIDENCE_BYTES = 10 * 1024 * 1024;

export function isActivityEvidenceFileTooLarge(file: Pick<Blob, "size">): boolean {
  return file.size > MAX_ACTIVITY_EVIDENCE_BYTES;
}
