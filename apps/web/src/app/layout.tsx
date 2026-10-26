import type { Metadata } from "next";
import "./globals.css";
import { ThemeBootstrapScript } from "../components/theme-bootstrap-script";
import { ThemeProvider } from "../components/theme-provider";
import { buildThemeBootstrapScript } from "../lib/theme";

export const metadata: Metadata = {
  title: { default: "NITE Connect", template: "%s | NITE Connect" },
  description: "Ambiente interno de gestão do Núcleo de Inovação e Tecnologia.",
  robots: { index: false, follow: false },
};

const themeBootstrapScript = buildThemeBootstrapScript();

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <head>
        <ThemeBootstrapScript html={themeBootstrapScript} />
      </head>
      <body>
        <ThemeProvider>{children}</ThemeProvider>
      </body>
    </html>
  );
}
