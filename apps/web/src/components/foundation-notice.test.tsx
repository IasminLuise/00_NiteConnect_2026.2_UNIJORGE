import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { FoundationNotice } from "./foundation-notice";

describe("Fundação do frontend", () => {
  it("identifica o produto e direciona ao login institucional", () => {
    const html = renderToStaticMarkup(<FoundationNotice />);
    expect(html).toContain("NITE Connect");
    expect(html).toContain("Acessar login institucional");
    expect(html).toContain('href="/login"');
    expect(html).not.toContain("serão integrados nas próximas etapas");
  });

  it("exibe um título semanticamente identificável", () => {
    const html = renderToStaticMarkup(<FoundationNotice />);
    expect(html).toContain('id="nite-title"');
    expect(html).toContain('aria-labelledby="nite-title"');
  });
});
