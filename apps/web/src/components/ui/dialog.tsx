"use client";

import type { ReactNode, RefObject } from "react";
import { DialogFrame } from "./dialog-frame";

type DialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: ReactNode;
  description?: ReactNode;
  children?: ReactNode;
  footer?: ReactNode;
  initialFocusRef?: RefObject<HTMLElement | null>;
  closeOnBackdrop?: boolean;
  className?: string;
};

function Dialog({
  closeOnBackdrop = true,
  ...props
}: DialogProps) {
  return <DialogFrame {...props} closeOnBackdrop={closeOnBackdrop} />;
}

export { Dialog };
export type { DialogProps };
