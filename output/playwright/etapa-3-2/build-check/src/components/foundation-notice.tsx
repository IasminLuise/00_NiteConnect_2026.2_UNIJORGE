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
        Ambiente interno de gestão do NITE. A aplicação web está em preparação.
        O acesso institucional e os módulos operacionais serão integrados nas próximas etapas.
      </p>
      <p className="text-sm opacity-65">
        Nenhuma informação de integrantes, projetos ou avaliações é exibida nesta página.
      </p>
    </section>
  );
}
