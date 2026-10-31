export default function DashboardLoading() {
  return (
    <div className="grid gap-8" aria-label="Carregando painel" aria-busy="true">
      <div className="grid gap-3 border-b border-nite-border-soft pb-6">
        <div className="h-3 w-36 animate-pulse rounded bg-nite-surface-elevated motion-reduce:animate-none" />
        <div className="h-8 w-64 max-w-full animate-pulse rounded bg-nite-surface-elevated motion-reduce:animate-none" />
        <div className="h-4 w-full max-w-xl animate-pulse rounded bg-nite-surface-elevated motion-reduce:animate-none" />
      </div>
      <div className="grid gap-8 xl:grid-cols-[minmax(0,1.25fr)_minmax(20rem,0.75fr)]">
        <div className="grid min-h-36 gap-3 rounded-panel border border-nite-border-soft bg-nite-surface p-5">
          <div className="h-5 w-32 animate-pulse rounded bg-nite-surface-elevated motion-reduce:animate-none" />
          <div className="h-10 animate-pulse rounded bg-nite-surface-elevated motion-reduce:animate-none" />
          <div className="h-10 animate-pulse rounded bg-nite-surface-elevated motion-reduce:animate-none" />
        </div>
        <div className="grid gap-3">
          <div className="h-16 animate-pulse rounded-panel border border-nite-border-soft bg-nite-surface motion-reduce:animate-none" />
          <div className="h-16 animate-pulse rounded-panel border border-nite-border-soft bg-nite-surface motion-reduce:animate-none" />
          <div className="h-16 animate-pulse rounded-panel border border-nite-border-soft bg-nite-surface motion-reduce:animate-none" />
        </div>
      </div>
      <div className="h-64 animate-pulse rounded-panel border border-nite-border-soft bg-nite-surface motion-reduce:animate-none" />
    </div>
  );
}
