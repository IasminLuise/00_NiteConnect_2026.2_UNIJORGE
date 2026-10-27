import type { Metadata } from "next";
import { Geist, Geist_Mono, Sora } from "next/font/google";
import "./globals.css";
import { ThemeBootstrapScript } from "../components/theme-bootstrap-script";
import { ThemeProvider } from "../components/theme-provider";
import { buildThemeBootstrapScript } from "../lib/theme";

const sora = Sora({ subsets: ["latin"], display: "swap", variable: "--font-sora" });
const geist = Geist({ subsets: ["latin"], display: "swap", variable: "--font-geist" });
const geistMono = Geist_Mono({ subsets: ["latin"], display: "swap", variable: "--font-geist-mono" });

export const metadata: Metadata = {
  title: { default: "NITE Connect", template: "%s | NITE Connect" },
  description: "Ambiente interno de gestão do Núcleo de Inovação e Tecnologia.",
  robots: { index: false, follow: false },
};

const themeBootstrapScript = buildThemeBootstrapScript();

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR" className={`${sora.variable} ${geist.variable} ${geistMono.variable}`} suppressHydrationWarning>
      <head>
        <ThemeBootstrapScript html={themeBootstrapScript} />
      </head>
      <body>
        <ThemeProvider>{children}</ThemeProvider>
      </body>
    </html>
  );
}
