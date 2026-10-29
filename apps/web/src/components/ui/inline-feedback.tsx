import type { ReactNode } from "react";
import { cx } from "./cx";

type InlineFeedbackVariant = "info" | "success" | "warning" | "error";
type FeedbackAnnouncement = "none" | "polite" | "assertive";

type InlineFeedbackProps = {
  variant: InlineFeedbackVariant;
  description: ReactNode;
  title?: ReactNode;
  action?: ReactNode;
  announcement?: FeedbackAnnouncement;
  className?: string;
};

const feedbackVariantClasses: Record<InlineFeedbackVariant, string> = {
  info: "border-status-progress-border bg-status-progress-soft",
  success: "border-status-done-border bg-status-done-soft",
  warning: "border-status-warning-border bg-status-warning-soft",
  error: "border-status-error-border bg-status-error-soft",
};

const feedbackIconClasses: Record<InlineFeedbackVariant, string> = {
  info: "text-status-progress",
  success: "text-status-done",
  warning: "text-status-warning",
  error: "text-status-error",
};

const feedbackIcons: Record<InlineFeedbackVariant, string> = {
  info: "i",
  success: "✓",
  warning: "!",
  error: "×",
};

function InlineFeedback({
  variant,
  title,
  description,
  action,
  announcement = "none",
  className,
}: InlineFeedbackProps) {
  const role =
    announcement === "assertive"
      ? "alert"
      : announcement === "polite"
        ? "status"
        : undefined;

  return (
    <div
      data-slot="inline-feedback"
      data-variant={variant}
      role={role}
      aria-live={announcement === "none" ? undefined : announcement}
      aria-atomic={announcement === "none" ? undefined : true}
      className={cx(
        "flex min-w-0 items-start gap-3 rounded-panel border p-4",
        feedbackVariantClasses[variant],
        className,
      )}
    >
      <span
        aria-hidden="true"
        className={cx(
          "mt-0.5 inline-flex size-5 shrink-0 items-center justify-center rounded-full border border-current font-sans text-type-metadata font-semibold",
          feedbackIconClasses[variant],
        )}
      >
        {feedbackIcons[variant]}
      </span>
      <div className="min-w-0 flex-1">
        {title ? (
          <p
            className={cx(
              "font-sans text-type-label font-semibold",
              feedbackIconClasses[variant],
            )}
          >
            {title}
          </p>
        ) : null}
        <div className={cx(title ? "mt-1" : undefined, "break-words font-sans text-type-body-compact text-nite-text-primary")}>
          {description}
        </div>
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  );
}

export { InlineFeedback };
export type {
  FeedbackAnnouncement,
  InlineFeedbackProps,
  InlineFeedbackVariant,
};
