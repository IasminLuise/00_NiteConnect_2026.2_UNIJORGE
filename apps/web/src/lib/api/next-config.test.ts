import { describe, expect, it } from "vitest";
import nextConfig from "../../../next.config";

describe("limite de Server Actions do Next.js", () => {
  it("permite upload de evidência de 10 MiB com margem multipart limitada", () => {
    expect(nextConfig.experimental?.serverActions?.bodySizeLimit).toBe("11mb");
  });
});
