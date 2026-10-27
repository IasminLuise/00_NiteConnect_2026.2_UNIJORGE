import Link from "next/link";

type FoundationLabNavigationProps = {
  current: "colors" | "typography" | "spacing";
};

export function FoundationLabNavigation({ current }: FoundationLabNavigationProps) {
  return (
    <nav className="foundation-lab-navigation" aria-label="Fundações do design system">
      <Link
        className="font-sans text-type-label"
        aria-current={current === "colors" ? "page" : undefined}
        href="/dev/design-system/colors"
      >
        Cores
      </Link>
      <Link
        className="font-sans text-type-label"
        aria-current={current === "typography" ? "page" : undefined}
        href="/dev/design-system/typography"
      >
        Tipografia
      </Link>
      <Link
        className="font-sans text-type-label"
        aria-current={current === "spacing" ? "page" : undefined}
        href="/dev/design-system/spacing"
      >
        Espaçamento e densidade
      </Link>
    </nav>
  );
}
