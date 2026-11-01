export default function ActivitiesLoading() {
  return <main className="mx-auto w-full max-w-6xl space-y-7 px-4 py-7 sm:px-7 sm:py-10 lg:px-9" aria-busy="true" aria-label="Carregando atividades">
    <div className="grid gap-3">
      <span className="h-8 w-64 animate-pulse rounded-subtle bg-nite-surface-elevated motion-reduce:animate-none" />
      <span className="h-4 w-80 max-w-full animate-pulse rounded-subtle bg-nite-surface-elevated motion-reduce:animate-none" />
    </div>
    <div className="h-11 animate-pulse rounded-control bg-nite-surface-elevated motion-reduce:animate-none" />
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      {Array.from({ length: 4 }, (_, index) => <span key={index} className="h-11 animate-pulse rounded-control bg-nite-surface-elevated motion-reduce:animate-none" />)}
    </div>
    <div className="divide-y divide-nite-border-soft border-y border-nite-border-soft">
      {Array.from({ length: 5 }, (_, index) => <div key={index} className="grid gap-3 py-5 sm:grid-cols-[minmax(0,1fr)_18rem]">
        <span className="h-5 w-3/4 max-w-lg animate-pulse rounded-subtle bg-nite-surface-elevated motion-reduce:animate-none" />
        <span className="h-5 w-full animate-pulse rounded-subtle bg-nite-surface-elevated motion-reduce:animate-none" />
      </div>)}
    </div>
    <p className="sr-only" role="status">Carregando atividades</p>
  </main>;
}
