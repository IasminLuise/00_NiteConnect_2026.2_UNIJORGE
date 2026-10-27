import type { ComponentPropsWithRef } from "react";
import { cx } from "./cx";
import { formControlClasses } from "./control-styles";

type TextFieldProps = ComponentPropsWithRef<"input">;

function TextField({ className, type = "text", ...props }: TextFieldProps) {
  return (
    <input
      {...props}
      data-slot="text-field"
      type={type}
      className={cx(formControlClasses, className)}
    />
  );
}

export { TextField };
export type { TextFieldProps };
