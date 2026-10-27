import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { FoundationLabNavigation } from "./foundation-lab-navigation";
import { TypographyFoundationLab } from "./typography-foundation-lab";

describe("Laboratório da fundação tipográfica", () => {
  it("mantém navegação acessível entre cores e tipografia", () => {
    const html = renderToStaticMarkup(<FoundationLabNavigation current="typography" />);

    expect(html).toContain('aria-label="Fundações do design system"');
    expect(html).toContain('href="/dev/design-system/colors"');
    expect(html).toContain('aria-current="page" href="/dev/design-system/typography"');
  });

  it("apresenta os papéis tipográficos reais e exemplos operacionais", () => {
    const html = renderToStaticMarkup(<TypographyFoundationLab />);

    expect(html).toContain("Sora");
    expect(html).toContain("Geist Mono");
    expect(html).toContain('data-type-token="page-title"');
    expect(html).toContain('data-type-token="label"');
    expect(html).toContain('data-type-token="table-cell-compact"');
    expect(html).toContain("Gestão do núcleo");
    expect(html).toContain("Nome do projeto");
    expect(html).toContain("Projetos atualizados");
    expect(html).toContain("tabular-nums");
    expect(html).toContain("Reflow em telas estreitas");
  });
});
