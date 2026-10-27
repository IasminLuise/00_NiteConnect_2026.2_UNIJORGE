import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { FoundationLabNavigation } from "./foundation-lab-navigation";
import { GeometryFoundationLab } from "./geometry-foundation-lab";

describe("Laboratório de geometria e elevação", () => {
  it("expõe a nova seção na navegação sem substituir as fundações anteriores", () => {
    const html = renderToStaticMarkup(<FoundationLabNavigation current="geometry" />);

    expect(html).toContain('href="/dev/design-system/colors"');
    expect(html).toContain('href="/dev/design-system/typography"');
    expect(html).toContain('href="/dev/design-system/spacing"');
    expect(html).toContain('aria-current="page" href="/dev/design-system/geometry"');
  });

  it("demonstra raios, estados e camadas com controles acessíveis", () => {
    const html = renderToStaticMarkup(<GeometryFoundationLab />);

    expect(html).toContain("rounded-subtle");
    expect(html).toContain("rounded-control");
    expect(html).toContain("rounded-panel");
    expect(html).toContain("rounded-dialog");
    expect(html).toContain("rounded-pill");
    expect(html).toContain('aria-pressed="false"');
    expect(html).toContain('aria-expanded="false"');
    expect(html).toContain('hidden=""');
    expect(html).toContain("--nite-surface-elevated");
    expect(html).toContain("geometry-demo-dialog");
    expect(html).toContain('aria-labelledby="geometry-dialog-title"');
  });
});
