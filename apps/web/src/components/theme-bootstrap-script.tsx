"use client";

export function ThemeBootstrapScript({ html }: Readonly<{ html: string }>) {
  return (
    <script
      id="theme-bootstrap"
      type={typeof window === "undefined" ? "text/javascript" : "text/plain"}
      suppressHydrationWarning
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}
