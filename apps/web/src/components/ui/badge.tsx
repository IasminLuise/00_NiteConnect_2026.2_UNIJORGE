import type { ComponentPropsWithRef, ReactNode } from "react";
import { cx } from "./cx";

type BadgeVariant =
  | "neutral"
  | "institutional"
  | "info"
  | "success"
  | "warning"
  | "error";

type BadgeProps = Omit<
  ComponentPropsWithRef<"span">,
  "onClick" | "role" | "tabIndex"
> & {
  children: ReactNode;
  variant?: BadgeVariant;
};

const badgeVariantClasses: Record<BadgeVariant, string> = {
  neutral:
    "border-nite-border-soft bg-nite-surface-subtle text-nite-text-secondary",
  institutional:
    "border-nite-brand-primary/25 bg-nite-brand-soft text-nite-brand-accent",
  info: "border-status-progress-border bg-status-progress-soft text-status-progress",
  success: "border-status-done-border bg-status-done-soft text-status-done",
  warning: "border-status-warning-border bg-status-warning-soft text-status-warning",
  error: "border-status-error-border bg-status-error-soft text-status-error",
};

function Badge({
  children,
  className,
  variant = "neutral",
  ...props
}: BadgeProps) {
  return (
    <span
      {...props}
      data-slot="badge"
      data-variant={variant}
      className={cx(
        "inline-flex min-h-6 max-w-full items-center rounded-pill border px-2.5 py-1 font-sans text-type-metadata font-medium leading-normal break-words",
        badgeVariantClasses[variant],
        className,
      )}
    >
      {children}
    </span>
  );
}

export { Badge };
export type { BadgeProps, BadgeVariant };
