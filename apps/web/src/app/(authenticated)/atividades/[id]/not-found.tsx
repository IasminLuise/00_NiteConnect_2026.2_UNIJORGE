import Link from "next/link";

export default function ActivityNotFound() {
  return <main className="mx-auto w-full max-w-3xl space-y-5 px-4 py-12 sm:px-7 sm:py-16">
    <section className="border-y border-nite-border-soft py-8">
      <h1 className="font-display text-type-section-title text-nite-text-primary">Atividade não encontrada</h1>
      <p className="mt-2 text-type-body-compact text-nite-text-secondary">O registro não existe ou não está disponível para sua identidade.</p>
      <Link href="/atividades" className="mt-5 inline-flex min-h-11 items-center rounded-control border border-nite-border-soft px-3 text-type-body-compact font-medium text-nite-text-primary hover:bg-nite-surface-subtle focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus">Voltar para minhas atividades</Link>
    </section>
  </main>;
}
