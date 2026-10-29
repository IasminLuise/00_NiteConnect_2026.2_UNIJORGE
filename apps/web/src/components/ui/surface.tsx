import type { ComponentPropsWithRef } from "react";
import { cx } from "./cx";

type SurfaceVariant = "default" | "subtle" | "elevated";
type SurfacePadding = "comfortable" | "compact" | "none";

type SurfaceProps = ComponentPropsWithRef<"div"> & {
  variant?: SurfaceVariant;
  padding?: SurfacePadding;
};

const surfaceVariantClasses: Record<SurfaceVariant, string> = {
  default: "border-nite-border-soft bg-nite-surface",
  subtle: "border-nite-border-soft bg-nite-surface-subtle",
  elevated:
    "border-nite-border-strong bg-nite-surface-elevated shadow-nite-floating",
};

const surfacePaddingClasses: Record<SurfacePadding, string> = {
  comfortable: "p-4 sm:p-6",
  compact: "p-3 sm:p-4",
  none: "p-0",
};

function Surface({
  children,
  className,
  variant = "default",
  padding = "comfortable",
  ...props
}: SurfaceProps) {
  return (
    <div
      {...props}
      data-slot="surface"
      data-variant={variant}
      data-padding={padding}
      className={cx(
        "min-w-0 rounded-panel border",
        surfaceVariantClasses[variant],
        surfacePaddingClasses[padding],
        className,
      )}
    >
      {children}
    </div>
  );
}

export { Surface };
export type { SurfacePadding, SurfaceProps, SurfaceVariant };
