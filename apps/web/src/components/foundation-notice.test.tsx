import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { FoundationNotice } from "./foundation-notice";

describe("Fundação do frontend", () => {
  it("identifica o produto e informa que a integração ainda não está disponível", () => {
    const html = renderToStaticMarkup(<FoundationNotice />);
    expect(html).toContain("NITE Connect");
    expect(html).toContain("está em preparação");
    expect(html).not.toContain("Entrar com Microsoft");
  });

  it("exibe um título semanticamente identificável", () => {
    const html = renderToStaticMarkup(<FoundationNotice />);
    expect(html).toContain('id="nite-title"');
    expect(html).toContain('aria-labelledby="nite-title"');
  });
});
