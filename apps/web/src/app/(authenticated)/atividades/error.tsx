"use client";

import { Button, InlineFeedback } from "../../../components/ui";

export default function ActivitiesError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return <main className="mx-auto w-full max-w-3xl space-y-5 px-4 py-12 sm:px-7 sm:py-16">
    <InlineFeedback variant="error" title="Não foi possível abrir esta página" description="Tente novamente. Se o problema continuar, volte à lista de atividades." announcement="assertive" />
    <Button type="button" variant="secondary" onClick={reset}>Tentar novamente</Button>
  </main>;
}
