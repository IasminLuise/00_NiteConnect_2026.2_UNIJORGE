import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { ComponentLibraryLab } from "./component-library-lab";
import { FoundationLabNavigation } from "./foundation-lab-navigation";

describe("laboratório de componentes", () => {
  it("adiciona Componentes sem remover as quatro fundações anteriores", () => {
    const html = renderToStaticMarkup(
      <FoundationLabNavigation current="components" />,
    );

    expect(html).toContain('href="/dev/design-system/colors"');
    expect(html).toContain('href="/dev/design-system/typography"');
    expect(html).toContain('href="/dev/design-system/spacing"');
    expect(html).toContain('href="/dev/design-system/geometry"');
    expect(html).toContain('aria-current="page" href="/dev/design-system/components"');
  });

  it("demonstra os componentes reais, estados alternáveis e validação nativa", () => {
    const html = renderToStaticMarkup(<ComponentLibraryLab />);

    expect(html).toContain("data-slot=\"button\"");
    expect(html).toContain("data-slot=\"text-field\"");
    expect(html).toContain("data-slot=\"textarea\"");
    expect(html).toContain("data-slot=\"select\"");
    expect(html).toContain("data-slot=\"checkbox\"");
    expect(html).toContain("data-slot=\"badge\"");
    expect(html).toContain("data-slot=\"inline-feedback\"");
    expect(html).toContain("data-slot=\"surface\"");
    expect(html).toContain("data-slot=\"dialog\"");
    expect(html).toContain("Exibir erro demonstrativo");
    expect(html).toContain("aria-pressed=\"false\"");
    expect(html).toContain("Validar formulário");
    expect(html).toContain("Validar composição");
    expect(html).toContain("Estado misto");
    expect(html).toContain("Abrir diálogo de edição");
    expect(html).toContain("Abrir confirmação sensível");
    expect(html).toContain("Simular atualização");
    expect(html).toContain('aria-live="polite"');
    expect(html).toContain("Tabelas, filtros e paginação");
    expect(html).toContain('data-slot="filter-toolbar"');
    expect(html).toContain("Simular falha");
    expect(html).toContain("Simular coleção vazia");
    expect(html).toContain('data-slot="data-table-scroll-region"');
    expect(html).toContain('data-slot="pagination"');
    expect(html).toContain("Densidade confortável");
    expect(html).toContain("Densidade compacta");
  });
});
