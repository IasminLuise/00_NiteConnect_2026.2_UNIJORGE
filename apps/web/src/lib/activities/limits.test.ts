import { describe, expect, it } from "vitest";
import { MAX_ACTIVITY_EVIDENCE_BYTES, isActivityEvidenceFileTooLarge } from "./limits";

describe("limite de upload de evidência", () => {
  it("aceita exatamente 10 MiB e rejeita arquivos maiores", () => {
    expect(MAX_ACTIVITY_EVIDENCE_BYTES).toBe(10 * 1024 * 1024);
    expect(isActivityEvidenceFileTooLarge({ size: MAX_ACTIVITY_EVIDENCE_BYTES })).toBe(false);
    expect(isActivityEvidenceFileTooLarge({ size: MAX_ACTIVITY_EVIDENCE_BYTES + 1 })).toBe(true);
  });
});
