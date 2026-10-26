import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Diário contínuo | NITE Connect",
  description: "Registro semanal de atividades do integrante do NITE Connect.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="pt-BR"><body>{children}</body></html>;
}
