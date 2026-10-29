import type { ComponentPropsWithoutRef, ReactNode } from "react";
import { cx } from "../ui/cx";

type DataTableDensity = "comfortable" | "compact";
type DataTableAlignment = "start" | "center" | "end";

type DataTableColumn<Row> = {
  id: string;
  header: ReactNode;
  cell: (row: Row) => ReactNode;
  align?: DataTableAlignment;
  rowHeader?: boolean;
  className?: string;
};

type DataTableProps<Row> = Omit<ComponentPropsWithoutRef<"div">, "children"> & {
  caption: ReactNode;
  columns: readonly DataTableColumn<Row>[];
  rows: readonly Row[];
  getRowId: (row: Row) => string | number;
  density?: DataTableDensity;
  emptyContent?: ReactNode;
};

const cellPadding: Record<DataTableDensity, string> = {
  comfortable: "px-4 py-3",
  compact: "px-3 py-2",
};

const alignmentClasses: Record<DataTableAlignment, string> = {
  start: "text-start",
  center: "text-center",
  end: "text-end",
};

function DataTable<Row>({
  caption,
  columns,
  rows,
  getRowId,
  density = "comfortable",
  emptyContent,
  className,
  ...props
}: DataTableProps<Row>) {
  return (
    <div
      {...props}
      data-slot="data-table-scroll-region"
      data-density={density}
      className={cx(
        "max-w-full min-w-0 overflow-x-auto overscroll-x-contain rounded-panel border border-nite-border-soft bg-nite-surface",
        className,
      )}
    >
      <table className="w-full min-w-[48rem] border-collapse font-sans text-type-body-compact text-nite-text-primary">
        <caption className="sr-only">{caption}</caption>
        <thead className="bg-nite-surface-subtle text-nite-text-secondary">
          <tr>
            {columns.map((column) => (
              <th
                key={column.id}
                scope="col"
                data-column-id={column.id}
                className={cx(
                  "border-b border-nite-border-soft px-4 py-3 font-sans text-type-label font-semibold",
                  alignmentClasses[column.align ?? "start"],
                  column.className,
                )}
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-nite-border-soft">
          {rows.map((row) => (
            <tr key={getRowId(row)} className="align-top">
              {columns.map((column) => {
                const className = cx(
                  "break-words",
                  cellPadding[density],
                  alignmentClasses[column.align ?? "start"],
                  column.className,
                );
                const cellContent = column.cell(row);

                return column.rowHeader ? (
                  <th
                    key={column.id}
                    scope="row"
                    data-column-id={column.id}
                    className={cx(className, "font-medium")}
                  >
                    {cellContent}
                  </th>
                ) : (
                  <td key={column.id} data-column-id={column.id} className={className}>
                    {cellContent}
                  </td>
                );
              })}
            </tr>
          ))}
          {rows.length === 0 && emptyContent !== undefined ? (
            <tr>
              <td
                colSpan={columns.length}
                className={cx(
                  "break-words text-center text-nite-text-secondary",
                  cellPadding[density],
                )}
              >
                {emptyContent}
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </div>
  );
}

export { DataTable };
export type {
  DataTableAlignment,
  DataTableColumn,
  DataTableDensity,
  DataTableProps,
};
