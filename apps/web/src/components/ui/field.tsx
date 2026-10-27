import { cloneElement, type ReactElement, type ReactNode } from "react";
import { cx } from "./cx";

type FieldControlProps = {
  id?: string;
  required?: boolean;
  disabled?: boolean;
  "aria-describedby"?: string;
  "aria-invalid"?: boolean | "true" | "false" | "grammar" | "spelling";
};

type FieldProps = {
  id: string;
  label: ReactNode;
  description?: ReactNode;
  error?: ReactNode;
  required?: boolean;
  optional?: boolean;
  layout?: "stacked" | "checkbox";
  children: ReactElement<FieldControlProps>;
  className?: string;
};

function Field({
  id,
  label,
  description,
  error,
  required,
  optional = false,
  layout = "stacked",
  children,
  className,
}: FieldProps) {
  const hasError = Boolean(error);
  const controlRequired = required ?? children.props.required ?? false;
  const controlDisabled = children.props.disabled ?? false;
  const descriptionId = `${id}-description`;
  const errorId = `${id}-error`;
  const describedBy = Array.from(
    new Set(
      [
        children.props["aria-describedby"],
        description ? descriptionId : undefined,
        hasError ? errorId : undefined,
      ].filter((value): value is string => Boolean(value)),
    ),
  ).join(" ");
  const control = cloneElement(children, {
    id,
    required: controlRequired,
    "aria-describedby": describedBy || undefined,
    "aria-invalid": hasError
      ? true
      : (children.props["aria-invalid"] ?? undefined),
  });

  return (
    <div className={cx("grid min-w-0 gap-2", className)}>
      {layout === "checkbox" ? (
        <div className="flex min-h-11 min-w-0 items-center gap-3">
          {control}
          <label
            className={cx(
              "flex min-h-11 min-w-0 items-center font-sans text-type-label text-nite-text-primary",
              controlDisabled ? "cursor-not-allowed" : "cursor-pointer",
            )}
            htmlFor={id}
          >
            {label}
            {controlRequired ? (
              <span className="ms-1 text-nite-text-secondary"> (obrigatório)</span>
            ) : optional ? (
              <span className="ms-1 text-nite-text-secondary"> (opcional)</span>
            ) : null}
          </label>
        </div>
      ) : (
        <>
          <label
            className="font-sans text-type-label text-nite-text-primary"
            htmlFor={id}
          >
            {label}
            {controlRequired ? (
              <span className="ms-1 text-nite-text-secondary"> (obrigatório)</span>
            ) : optional ? (
              <span className="ms-1 text-nite-text-secondary"> (opcional)</span>
            ) : null}
          </label>
          {control}
        </>
      )}
      {description ? (
        <p
          className="font-sans text-type-metadata text-nite-text-secondary"
          id={descriptionId}
        >
          {description}
        </p>
      ) : null}
      {hasError ? (
        <p
          className="font-sans text-type-body-compact text-status-error"
          id={errorId}
        >
          {error}
        </p>
      ) : null}
    </div>
  );
}

export { Field };
export type { FieldProps };
