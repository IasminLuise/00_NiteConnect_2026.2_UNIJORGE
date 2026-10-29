import type { ReactNode } from "react";
import { Button, InlineFeedback } from "../ui";
import { cx } from "../ui/cx";

type LoadingStateProps = {
  label?: string;
  className?: string;
};

function LoadingState({
  label = "Carregando conteúdo",
  className,
}: LoadingStateProps) {
  return (
    <div
      data-slot="loading-state"
      role="status"
      aria-live="polite"
      aria-busy="true"
      className={cx(
        "flex min-h-24 min-w-0 items-center justify-center gap-3 rounded-panel border border-nite-border-soft bg-nite-surface p-4 text-type-body-compact text-nite-text-secondary",
        className,
      )}
    >
      <span
        aria-hidden="true"
        className="size-4 shrink-0 animate-spin rounded-full border-2 border-nite-border-strong border-t-nite-brand-primary motion-reduce:animate-none"
      />
      <span className="break-words">{label}</span>
    </div>
  );
}

type EmptyStateKind = "initial" | "filtered";

type EmptyStateProps = {
  kind: EmptyStateKind;
  title?: ReactNode;
  description?: ReactNode;
  visual?: ReactNode;
  action?: ReactNode;
  className?: string;
};

const emptyContent: Record<
  EmptyStateKind,
  { title: string; description: string }
> = {
  initial: {
    title: "Ainda não há registros",
    description: "Quando houver registros disponíveis, eles aparecerão aqui.",
  },
  filtered: {
    title: "Nenhum resultado encontrado",
    description: "Ajuste ou limpe os filtros para ampliar a consulta.",
  },
};

function EmptyState({
  kind,
  title,
  description,
  visual,
  action,
  className,
}: EmptyStateProps) {
  const content = emptyContent[kind];

  return (
    <div
      data-slot="empty-state"
      data-kind={kind}
      className={cx(
        "flex min-h-40 min-w-0 flex-col items-center justify-center gap-3 rounded-panel border border-dashed border-nite-border-soft bg-nite-surface-subtle p-6 text-center",
        className,
      )}
    >
      {visual ? <div className="shrink-0" aria-hidden="true">{visual}</div> : null}
      <div className="min-w-0 space-y-1">
        <p className="break-words font-sans text-type-component-title text-nite-text-primary">
          {title ?? content.title}
        </p>
        {description ? (
          <div className="mx-auto max-w-prose break-words text-type-body-compact text-nite-text-secondary">
            {description}
          </div>
        ) : (
          <p className="mx-auto max-w-prose break-words text-type-body-compact text-nite-text-secondary">
            {content.description}
          </p>
        )}
      </div>
      {action ? <div className="flex max-w-full flex-wrap justify-center gap-3">{action}</div> : null}
    </div>
  );
}

type ErrorStateProps = {
  title?: ReactNode;
  description: ReactNode;
  onRetry?: () => void;
  retryLabel?: string;
  className?: string;
};

function ErrorState({
  title = "Não foi possível carregar o conteúdo",
  description,
  onRetry,
  retryLabel = "Tentar novamente",
  className,
}: ErrorStateProps) {
  return (
    <div data-slot="error-state" className={cx("grid min-w-0 gap-3", className)}>
      <InlineFeedback
        variant="error"
        title={title}
        description={description}
        announcement="polite"
      />
      {onRetry ? (
        <div className="flex justify-end">
          <Button type="button" variant="secondary" onClick={onRetry}>
            {retryLabel}
          </Button>
        </div>
      ) : null}
    </div>
  );
}

export { EmptyState, ErrorState, LoadingState };
export type {
  EmptyStateKind,
  EmptyStateProps,
  ErrorStateProps,
  LoadingStateProps,
};
