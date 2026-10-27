import type { ComponentPropsWithRef } from "react";
import { cx } from "./cx";
import { formControlClasses } from "./control-styles";

type NativeSelectProps = Omit<
  ComponentPropsWithRef<"select">,
  "defaultValue" | "multiple" | "size" | "value"
>;

type SelectProps = NativeSelectProps & {
  defaultValue?: string;
  placeholder?: string;
  value?: string;
};

function Select({
  children,
  className,
  defaultValue,
  placeholder,
  value,
  ...props
}: SelectProps) {
  const initialValue =
    placeholder && value === undefined && defaultValue === undefined
      ? ""
      : defaultValue;

  return (
    <select
      {...props}
      data-slot="select"
      value={value}
      defaultValue={initialValue}
      className={cx(formControlClasses, "cursor-pointer", className)}
    >
      {placeholder ? (
        <option value="" disabled>
          {placeholder}
        </option>
      ) : null}
      {children}
    </select>
  );
}

export { Select };
export type { SelectProps };
