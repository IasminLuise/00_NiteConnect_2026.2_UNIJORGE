import { notFound } from "next/navigation";
import { ComponentLibraryLab } from "../../../../components/component-library-lab";
import { FoundationLabNavigation } from "../../../../components/foundation-lab-navigation";
import { ThemeControl } from "../../../../components/theme-control";

export default function DesignSystemComponentsPage() {
  if (process.env.NODE_ENV === "production") notFound();

  return (
    <>
      <FoundationLabNavigation current="components" />
      <main className="mx-auto w-full max-w-7xl space-y-8 px-4 py-8 sm:px-6 md:space-y-12 md:py-12 lg:px-8">
        <header className="flex flex-wrap items-end justify-between gap-6 border-b border-nite-border-soft pb-6">
          <div className="min-w-0 max-w-3xl">
            <p className="lab-eyebrow">NITE CONNECT · BIBLIOTECA DE COMPONENTES</p>
            <h1 className="font-display text-type-page-title">Componentes fundamentais</h1>
            <p className="mt-3 max-w-prose text-type-body text-nite-text-secondary">
              Button, TextField, Textarea, Field, Select, Checkbox e Badge demonstrados com as fundações visuais ativas. As interações e os dados desta página são apenas de validação.
            </p>
          </div>
          <div className="lab-header-controls w-full sm:w-auto">
            <span className="lab-theme-state text-type-metadata">Selecione Light, Dark ou System</span>
            <ThemeControl />
          </div>
        </header>
        <ComponentLibraryLab />
      </main>
    </>
  );
}
