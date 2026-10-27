import type { ComponentPropsWithRef, ReactNode } from "react";
import { cx } from "./cx";

type ButtonVariant = "primary" | "secondary" | "quiet" | "destructive";

type ButtonProps = Omit<ComponentPropsWithRef<"button">, "children"> & {
  children?: ReactNode;
  variant?: ButtonVariant;
  loading?: boolean;
  leadingIcon?: ReactNode;
  trailingIcon?: ReactNode;
};

const buttonVariantClasses: Record<ButtonVariant, string> = {
  primary:
    "border-nite-brand-primary bg-nite-brand-primary text-nite-action-text hover:bg-nite-brand-primary/90",
  secondary:
    "border-nite-border-strong bg-nite-surface text-nite-text-primary hover:bg-nite-surface-focus",
  quiet:
    "border-transparent bg-transparent text-nite-text-secondary hover:bg-nite-surface-subtle hover:text-nite-text-primary",
  destructive:
    "border-status-error-border bg-status-error-soft text-status-error hover:border-status-error",
};

function Button({
  children,
  className,
  variant = "primary",
  loading = false,
  disabled,
  leadingIcon,
  trailingIcon,
  ...props
}: ButtonProps) {
  return (
    <button
      {...props}
      data-slot="button"
      data-variant={variant}
      data-loading={loading ? "true" : undefined}
      aria-busy={loading || props["aria-busy"] || undefined}
      disabled={disabled || loading}
      className={cx(
        "inline-flex min-h-11 shrink-0 items-center justify-center gap-2 rounded-control border px-4 py-2 font-sans text-type-label font-medium transition-colors active:translate-y-px focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus disabled:cursor-not-allowed disabled:border-nite-border-soft disabled:bg-nite-surface-subtle disabled:text-nite-text-muted disabled:active:translate-y-0 motion-reduce:transition-none",
        buttonVariantClasses[variant],
        className,
      )}
    >
      {loading ? (
        <span
          className="size-4 animate-spin rounded-full border-2 border-current border-r-transparent motion-reduce:animate-none"
          aria-hidden="true"
        />
      ) : leadingIcon ? (
        <span className="inline-flex shrink-0" aria-hidden="true">
          {leadingIcon}
        </span>
      ) : null}
      {children}
      {!loading && trailingIcon ? (
        <span className="inline-flex shrink-0" aria-hidden="true">
          {trailingIcon}
        </span>
      ) : null}
    </button>
  );
}

export { Button };
export type { ButtonProps, ButtonVariant };
