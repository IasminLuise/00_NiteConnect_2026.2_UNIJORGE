"use client";

import { useRef, type ReactNode } from "react";
import { Button } from "./button";
import { DialogFrame } from "./dialog-frame";

type AlertDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: ReactNode;
  description: ReactNode;
  children?: ReactNode;
  cancelLabel?: string;
  confirmLabel?: string;
  pending?: boolean;
  onConfirm: () => void;
  className?: string;
};

function AlertDialog({
  open,
  onOpenChange,
  title,
  description,
  children,
  cancelLabel = "Cancelar",
  confirmLabel = "Confirmar",
  pending = false,
  onConfirm,
  className,
}: AlertDialogProps) {
  const cancelButtonRef = useRef<HTMLButtonElement>(null);

  function confirm() {
    if (!pending) onConfirm();
  }

  return (
    <DialogFrame
      open={open}
      onOpenChange={onOpenChange}
      title={title}
      description={description}
      initialFocusRef={cancelButtonRef}
      showCloseButton={false}
      closeOnBackdrop={false}
      allowEscapeClose={!pending}
      className={className}
      footer={
        <>
          <Button
            ref={cancelButtonRef}
            type="button"
            variant="secondary"
            disabled={pending}
            onClick={() => onOpenChange(false)}
          >
            {cancelLabel}
          </Button>
          <Button
            type="button"
            variant="destructive"
            loading={pending}
            disabled={pending}
            onClick={confirm}
          >
            {pending ? "Executando…" : confirmLabel}
          </Button>
        </>
      }
    >
      {children}
    </DialogFrame>
  );
}

export { AlertDialog };
export type { AlertDialogProps };
