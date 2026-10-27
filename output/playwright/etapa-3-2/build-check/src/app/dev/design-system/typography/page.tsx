import { notFound } from "next/navigation";
import { FoundationLabNavigation } from "../../../../components/foundation-lab-navigation";
import { ThemeControl } from "../../../../components/theme-control";
import { TypographyFoundationLab } from "../../../../components/typography-foundation-lab";

export default function DesignSystemTypographyPage() {
  if (process.env.NODE_ENV === "production") notFound();

  return (
    <>
      <FoundationLabNavigation current="typography" />
      <main className="typography-lab-shell">
        <header className="typography-lab-header">
          <div>
            <p className="typography-lab-eyebrow">NITE CONNECT · FUNDAÇÃO TIPOGRÁFICA</p>
            <h1 className="font-display text-type-page-title">Design System Lab</h1>
            <p>Validação dos papéis tipográficos reais sobre a fundação cromática ativa.</p>
          </div>
          <div className="typography-lab-header-controls">
            <span className="text-type-metadata">Selecione Light, Dark ou System</span>
            <ThemeControl />
          </div>
        </header>
        <TypographyFoundationLab />
      </main>
    </>
  );
}
