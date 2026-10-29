import Link from "next/link";

/** Mensagem pública de implantação; não representa autenticação ou dashboard operacional. */
export function FoundationNotice() {
  return (
    <section aria-labelledby="nite-title" className="w-full max-w-xl space-y-5">
      <p className="text-xs font-semibold uppercase tracking-[0.16em] opacity-65">
        Núcleo de Inovação e Tecnologia
      </p>
      <h1 id="nite-title" className="text-4xl font-semibold tracking-tight sm:text-5xl">
        NITE Connect
      </h1>
      <p className="max-w-prose text-base leading-relaxed opacity-80">
        Ambiente interno de gestão do NITE. A interface operacional está em evolução.
      </p>
      <Link
        href="/login"
        className="inline-flex min-h-11 items-center rounded-control bg-nite-brand-primary px-4 py-2 font-sans text-type-label font-medium text-nite-action-text focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus"
      >
        Acessar login institucional
      </Link>
      <p className="text-sm opacity-65">
        Nenhuma informação de integrantes, projetos ou avaliações é exibida nesta página.
      </p>
    </section>
  );
}
