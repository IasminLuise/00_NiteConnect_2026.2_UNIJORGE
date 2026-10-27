"use client";

import { useCallback, type ComponentPropsWithRef, type RefCallback } from "react";
import { cx } from "./cx";

type CheckboxProps = Omit<ComponentPropsWithRef<"input">, "type"> & {
  indeterminate?: boolean;
};

function Checkbox({
  className,
  indeterminate = false,
  ref,
  ...props
}: CheckboxProps) {
  const setRef = useCallback<RefCallback<HTMLInputElement>>(
    (node) => {
      if (node) node.indeterminate = indeterminate;

      if (typeof ref === "function") return ref(node);
      if (ref) ref.current = node;
    },
    [indeterminate, ref],
  );

  return (
    <input
      {...props}
      ref={setRef}
      type="checkbox"
      data-slot="checkbox"
      className={cx(
        "size-5 shrink-0 cursor-pointer accent-nite-brand-primary focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus aria-invalid:focus-visible:outline-status-error disabled:cursor-not-allowed disabled:opacity-60",
        className,
      )}
    />
  );
}

export { Checkbox };
export type { CheckboxProps };
