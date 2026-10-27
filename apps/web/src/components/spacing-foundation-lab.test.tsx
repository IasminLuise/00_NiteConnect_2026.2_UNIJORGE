import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { FoundationLabNavigation } from "./foundation-lab-navigation";
import { SpacingFoundationLab } from "./spacing-foundation-lab";

describe("Laboratório da fundação espacial", () => {
  it("expõe a nova seção na navegação interna do design system", () => {
    const html = renderToStaticMarkup(<FoundationLabNavigation current="spacing" />);

    expect(html).toContain('href="/dev/design-system/colors"');
    expect(html).toContain('href="/dev/design-system/typography"');
    expect(html).toContain('aria-current="page" href="/dev/design-system/spacing"');
    expect(html).toContain('href="/dev/design-system/geometry"');
  });

  it("demonstra escala, responsividade e densidade sem trocar a tipografia", () => {
    const html = renderToStaticMarkup(<SpacingFoundationLab />);

    expect(html).toContain("spacing-24");
    expect(html).toContain("max-width: 80rem");
    expect(html).toContain('data-density="comfortable"');
    expect(html).toContain('aria-pressed="true"');
    expect(html).toContain("Filtros");
    expect(html).toContain("text-type-table-cell");
    expect(html).toContain("Revisar a documentação de acompanhamento");
    expect(html).toContain('aria-label="Projetos demonstrativos; tabela com rolagem horizontal"');
  });
});
