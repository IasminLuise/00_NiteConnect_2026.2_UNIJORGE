import type { ComponentPropsWithRef } from "react";
import { cx } from "./cx";
import { formControlClasses } from "./control-styles";

type TextareaProps = ComponentPropsWithRef<"textarea">;

function Textarea({ className, ...props }: TextareaProps) {
  return (
    <textarea
      {...props}
      data-slot="textarea"
      className={cx(
        formControlClasses,
        "min-h-24 resize-y align-top leading-relaxed",
        className,
      )}
    />
  );
}

export { Textarea };
export type { TextareaProps };
