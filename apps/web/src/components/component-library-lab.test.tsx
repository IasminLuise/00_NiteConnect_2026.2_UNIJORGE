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
    expect(html).toContain("Exibir erro demonstrativo");
    expect(html).toContain("aria-pressed=\"false\"");
    expect(html).toContain("Validar formulário");
    expect(html).toContain("Validar composição");
    expect(html).toContain("Estado misto");
    expect(html).toContain('aria-live="polite"');
  });
});
