"use client";

import { useFormStatus } from "react-dom";
import { signInWithMicrosoft } from "../../lib/auth/actions";
import { Button } from "../ui";

function MicrosoftMark() {
  return (
    <svg viewBox="0 0 23 23" className="size-5" aria-hidden="true">
      <path fill="#F25022" d="M0 0h10.4v10.4H0z" />
      <path fill="#7FBA00" d="M12.3 0h10.4v10.4H12.3z" />
      <path fill="#00A4EF" d="M0 12.3h10.4v10.4H0z" />
      <path fill="#FFB900" d="M12.3 12.3h10.4v10.4H12.3z" />
    </svg>
  );
}

function MicrosoftSignInButton() {
  const { pending } = useFormStatus();

  return (
    <Button
      type="submit"
      variant="primary"
      loading={pending}
      leadingIcon={<MicrosoftMark />}
      className="w-full"
    >
      {pending ? "Conectando à Microsoft…" : "Entrar com Microsoft"}
    </Button>
  );
}

function MicrosoftSignInForm() {
  return (
    <form action={signInWithMicrosoft}>
      <MicrosoftSignInButton />
    </form>
  );
}

export { MicrosoftSignInForm };
