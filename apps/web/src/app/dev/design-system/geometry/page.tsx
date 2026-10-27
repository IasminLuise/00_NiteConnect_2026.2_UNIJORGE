import { notFound } from "next/navigation";
import { FoundationLabNavigation } from "../../../../components/foundation-lab-navigation";
import { GeometryFoundationLab } from "../../../../components/geometry-foundation-lab";
import { ThemeControl } from "../../../../components/theme-control";

export default function DesignSystemGeometryPage() {
  if (process.env.NODE_ENV === "production") notFound();

  return (
    <>
      <FoundationLabNavigation current="geometry" />
      <main className="mx-auto w-full max-w-7xl space-y-8 px-4 py-8 sm:px-6 md:space-y-12 md:py-12 lg:px-8">
        <header className="geometry-lab-header flex flex-wrap items-end justify-between gap-6 border-b border-nite-border-soft pb-6">
          <div className="min-w-0 max-w-3xl">
            <p className="lab-eyebrow">NITE CONNECT · FUNDAÇÃO GEOMÉTRICA</p>
            <h1 className="font-display text-type-page-title">Geometria e elevação</h1>
            <p className="mt-3 max-w-prose text-type-body text-nite-text-secondary">
              Raios, contornos e camadas funcionais demonstrados sobre as fundações cromática, tipográfica e espacial existentes.
            </p>
          </div>
          <div className="lab-header-controls w-full sm:w-auto">
            <span className="lab-theme-state text-type-metadata">Selecione Light, Dark ou System</span>
            <ThemeControl />
          </div>
        </header>
        <GeometryFoundationLab />
      </main>
    </>
  );
}
