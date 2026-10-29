import type { ComponentPropsWithoutRef, ReactNode } from "react";
import { Badge, Button } from "../ui";
import { cx } from "../ui/cx";

type FilterToolbarProps = Omit<ComponentPropsWithoutRef<"form">, "children"> & {
  children: ReactNode;
  actions?: ReactNode;
  hasActiveFilters?: boolean;
  onClear?: () => void;
  clearLabel?: string;
  activeLabel?: string;
};

function FilterToolbar({
  children,
  actions,
  hasActiveFilters = false,
  onClear,
  clearLabel = "Limpar filtros",
  activeLabel = "Filtros aplicados",
  className,
  ...props
}: FilterToolbarProps) {
  return (
    <form
      {...props}
      data-slot="filter-toolbar"
      data-active={hasActiveFilters ? "true" : "false"}
      className={cx("grid min-w-0 gap-4", className)}
    >
      <div className="flex min-w-0 flex-wrap items-end gap-4">
        <div className="grid min-w-0 flex-1 basis-full grid-cols-1 gap-4 sm:basis-auto sm:grid-cols-[repeat(auto-fit,minmax(min(100%,14rem),1fr))]">
          {children}
        </div>
        <div className="flex min-w-0 flex-wrap items-center gap-3 sm:ml-auto">
        {hasActiveFilters ? (
          <div className="flex min-w-0 flex-wrap items-center gap-3">
            <Badge variant="info">{activeLabel}</Badge>
            {onClear ? (
              <Button type="button" variant="quiet" onClick={onClear}>
                {clearLabel}
              </Button>
            ) : null}
          </div>
        ) : null}
        {actions}
        </div>
      </div>
    </form>
  );
}

export { FilterToolbar };
export type { FilterToolbarProps };
