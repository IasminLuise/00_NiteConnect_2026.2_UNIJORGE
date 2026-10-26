import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "NITE Connect", template: "%s | NITE Connect" },
  description: "Ambiente interno de gestão do Núcleo de Inovação e Tecnologia.",
  robots: { index: false, follow: false },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="pt-BR"><body>{children}</body></html>;
}
