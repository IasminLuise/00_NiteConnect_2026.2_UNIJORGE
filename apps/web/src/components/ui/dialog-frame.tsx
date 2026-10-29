"use client";

import {
  useEffect,
  useId,
  useRef,
  type KeyboardEvent as ReactKeyboardEvent,
  type MouseEvent,
  type ReactNode,
  type RefObject,
  type SyntheticEvent,
} from "react";
import { Button } from "./button";
import { cx } from "./cx";

type DialogFrameProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: ReactNode;
  description?: ReactNode;
  children?: ReactNode;
  footer?: ReactNode;
  initialFocusRef?: RefObject<HTMLElement | null>;
  showCloseButton?: boolean;
  closeOnBackdrop?: boolean;
  allowEscapeClose?: boolean;
  className?: string;
};

function DialogFrame({
  open,
  onOpenChange,
  title,
  description,
  children,
  footer,
  initialFocusRef,
  showCloseButton = true,
  closeOnBackdrop = true,
  allowEscapeClose = true,
  className,
}: DialogFrameProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const headingRef = useRef<HTMLHeadingElement>(null);
  const titleId = useId();
  const descriptionId = useId();

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    if (open && !dialog.open) {
      dialog.showModal();
      (initialFocusRef?.current ?? headingRef.current)?.focus();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [initialFocusRef, open]);

  function handleCancel(event: SyntheticEvent<HTMLDialogElement>) {
    event.preventDefault();
    if (allowEscapeClose) onOpenChange(false);
  }

  function handleBackdropClick(
    event: MouseEvent<HTMLDialogElement>,
  ) {
    if (closeOnBackdrop && event.target === event.currentTarget) {
      onOpenChange(false);
    }
  }

  function handleKeyDown(event: ReactKeyboardEvent<HTMLDialogElement>) {
    if (event.key !== "Tab") return;

    const dialog = event.currentTarget;
    const focusable = Array.from(
      dialog.querySelectorAll<HTMLElement>(
        "a[href], button:not([disabled]), input:not([disabled]):not([type='hidden']), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex='-1'])",
      ),
    ).filter(
      (element) =>
        element.getClientRects().length > 0 &&
        getComputedStyle(element).visibility !== "hidden",
    );
    const first = focusable[0];
    const last = focusable.at(-1);
    const active = document.activeElement;

    if (!first || !last) {
      event.preventDefault();
      headingRef.current?.focus();
      return;
    }

    if (
      event.shiftKey &&
      (active === first || active === headingRef.current || !dialog.contains(active))
    ) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && (active === last || !dialog.contains(active))) {
      event.preventDefault();
      first.focus();
    }
  }

  return (
    <dialog
      ref={dialogRef}
      data-slot="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      onCancel={handleCancel}
      onKeyDown={handleKeyDown}
      onClose={() => {
        if (open) onOpenChange(false);
      }}
      onClick={handleBackdropClick}
      className={cx(
        "m-auto max-h-[calc(100dvh-1rem)] w-[calc(100%-1rem)] max-w-2xl overflow-hidden rounded-dialog border border-nite-border-strong bg-nite-background p-0 text-nite-text-primary shadow-nite-modal backdrop:bg-black/60 sm:max-h-[calc(100dvh-4rem)]",
        className,
      )}
    >
      <div className="flex max-h-[calc(100dvh-1rem)] min-h-0 flex-col sm:max-h-[calc(100dvh-4rem)]">
        <header className="flex shrink-0 items-start justify-between gap-4 border-b border-nite-border-soft px-4 py-4 sm:px-6">
          <div className="min-w-0">
            <h2
              ref={headingRef}
              id={titleId}
              tabIndex={-1}
              className="font-display text-type-component-title focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus"
            >
              {title}
            </h2>
            {description ? (
              <div
                id={descriptionId}
                className="mt-1 break-words text-type-body-compact text-nite-text-secondary"
              >
                {description}
              </div>
            ) : null}
          </div>
          {showCloseButton ? (
            <Button
              type="button"
              variant="quiet"
              aria-label="Fechar diálogo"
              onClick={() => onOpenChange(false)}
            >
              Fechar
            </Button>
          ) : null}
        </header>
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 py-4 sm:px-6 sm:py-5">
          {children}
        </div>
        {footer ? (
          <footer className="flex shrink-0 flex-wrap items-center justify-end gap-3 border-t border-nite-border-soft px-4 py-4 sm:px-6">
            {footer}
          </footer>
        ) : null}
      </div>
    </dialog>
  );
}

export { DialogFrame };
export type { DialogFrameProps };
