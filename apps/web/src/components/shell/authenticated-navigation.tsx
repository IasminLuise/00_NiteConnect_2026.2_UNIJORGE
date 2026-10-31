import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef, type ReactNode } from "react";
import type { LeadershipProject } from "../../lib/projects/contracts";

export type AuthenticatedNavigationIcon =
  | "home"
  | "activity"
  | "clock"
  | "projects"
  | "calendar"
  | "team"
  | "report"
  | "attendance"
  | "settings"
  | "academic"
  | "review";

export type AuthenticatedNavigationItem = {
  label: string;
  icon: AuthenticatedNavigationIcon;
  href?: string;
  status?: "development";
};

export type AuthenticatedNavigationGroup = {
  label: string;
  items: AuthenticatedNavigationItem[];
  defaultOpen: boolean;
  note?: string;
  collapsible?: boolean;
};

type NavigationOptions = {
  projects: readonly LeadershipProject[];
  canAdminister: boolean;
  canCoordinate: boolean;
};

function planned(label: string, icon: AuthenticatedNavigationIcon): AuthenticatedNavigationItem {
  return { label, icon, status: "development" };
}

export function buildAuthenticatedNavigationGroups(options: NavigationOptions): AuthenticatedNavigationGroup[] {
  const groups: AuthenticatedNavigationGroup[] = [
    {
      label: "Início",
      defaultOpen: true,
      collapsible: false,
      items: [{ label: "Visão geral", icon: "home", href: "/dashboard" }],
    },
    {
      label: "Meu espaço",
      defaultOpen: true,
      items: [
        planned("Minhas atividades", "activity"),
        planned("Minha participação", "clock"),
        planned("Meus projetos", "projects"),
        planned("Agenda", "calendar"),
      ],
    },
  ];

  if (options.projects.length > 0) {
    groups.push({
      label: "Projetos que lidero",
      defaultOpen: false,
      note: "Selecione um projeto no seletor de espaço de trabalho para atualizar o painel.",
      items: [
        planned("Atividades do projeto", "activity"),
        planned("Equipe e responsabilidades", "team"),
        planned("Relatórios do projeto", "report"),
      ],
    });
  }

  if (options.canAdminister) {
    groups.push(
      {
        label: "Acompanhamento do NITE",
        defaultOpen: false,
        items: [
          planned("Projetos", "projects"),
          planned("Atividades e entregas", "activity"),
          planned("Frequência e revisões", "attendance"),
          planned("Relatórios", "report"),
        ],
      },
      {
        label: "Administração",
        defaultOpen: false,
        items: [
          planned("Integrantes", "team"),
          planned("Acessos e convites", "review"),
          planned("Regras e parâmetros", "settings"),
        ],
      },
    );
  }

  if (options.canCoordinate) {
    groups.push({
      label: "Acompanhamento acadêmico",
      defaultOpen: false,
      items: [planned("Avaliações", "academic"), planned("Revisões e homologações", "review")],
    });
  }

  return groups;
}

const iconPaths: Record<AuthenticatedNavigationIcon, ReactNode> = {
  home: <><path d="m3 10 9-7 9 7" /><path d="M5 9v11h14V9M9 20v-7h6v7" /></>,
  activity: <><path d="M3 12h4l3-8 4 16 3-8h4" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  projects: <><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M3 9h18M8 4v5" /></>,
  calendar: <><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M16 3v4M8 3v4M3 10h18" /></>,
  team: <><path d="M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="10" cy="7" r="4" /><path d="M20 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" /></>,
  report: <><path d="M4 19V5M4 19h17" /><path d="m7 15 4-4 3 2 5-6" /></>,
  attendance: <><rect x="4" y="5" width="16" height="16" rx="2" /><path d="M8 3v4m8-4v4M4 10h16M8 14h2m4 0h2m-8 3h2m4 0h2" /></>,
  settings: <><circle cx="12" cy="12" r="3" /><path d="m19.4 15 .1.1 1.4 1.1-1.4 2.4-1.7-.6a8.1 8.1 0 0 1-1.8 1l-.3 1.8h-2.8l-.3-1.8a8.1 8.1 0 0 1-1.8-1l-1.7.6-1.4-2.4 1.4-1.1a7.7 7.7 0 0 1 0-2.1l-1.4-1.1 1.4-2.4 1.7.6a8.1 8.1 0 0 1 1.8-1l.3-1.8h2.8l.3 1.8a8.1 8.1 0 0 1 1.8 1l1.7-.6 1.4 2.4-1.4 1.1a7.7 7.7 0 0 1 0 2.1Z" /></>,
  academic: <><path d="m2 9 10-5 10 5-10 5-10-5Z" /><path d="M6 11v5c3 3 9 3 12 0v-5M22 9v6" /></>,
  review: <><path d="M7 3h8l4 4v14H7z" /><path d="M15 3v5h5m-9 5 2 2 4-4" /></>,
};

function Icon({ name }: { name: AuthenticatedNavigationIcon }) {
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" className="size-4 shrink-0">{iconPaths[name]}</svg>;
}

function NavigationLink({ item, currentPath, onNavigate }: {
  item: AuthenticatedNavigationItem;
  currentPath: string;
  onNavigate?: () => void;
}) {
  if (!item.href) {
    return <span aria-disabled="true" aria-label={`${item.label}, em desenvolvimento`} title="Em desenvolvimento" className="flex min-h-9 items-center gap-2.5 rounded-control px-3 py-1.5 text-type-body-compact text-nite-text-muted">
      <Icon name={item.icon} />
      <span className="min-w-0 flex-1 truncate">{item.label}</span>
      {item.status === "development" ? <span aria-hidden="true" className="size-1.5 shrink-0 rounded-full bg-nite-border-strong" /> : null}
    </span>;
  }
  const isCurrent = item.href === currentPath;
  return <Link href={item.href} onClick={onNavigate} aria-current={isCurrent ? "page" : undefined} className={`flex min-h-9 items-center gap-2.5 rounded-control px-3 py-1.5 text-type-body-compact transition-colors focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus ${isCurrent ? "bg-nite-brand-soft font-medium text-nite-brand-primary" : "text-nite-text-secondary hover:bg-nite-surface-subtle hover:text-nite-text-primary"}`}>
    <Icon name={item.icon} />
    <span className="min-w-0 truncate">{item.label}</span>
  </Link>;
}

function NavigationGroup({ group, currentPath, onNavigate }: {
  group: AuthenticatedNavigationGroup;
  currentPath: string;
  onNavigate?: () => void;
}) {
  const detailsRef = useRef<HTMLDetailsElement>(null);
  const ready = useRef(false);
  useEffect(() => {
    const details = detailsRef.current;
    if (!details) return;
    try {
      const saved = window.sessionStorage.getItem(`nite-navigation-group:${group.label}`);
      details.open = saved === null ? group.defaultOpen : saved === "open";
    } catch {
      details.open = group.defaultOpen;
    }
    ready.current = true;
  }, [group.label, group.defaultOpen]);
  const rows = <>
    {group.note ? <p className="px-3 pb-1 text-type-metadata leading-relaxed text-nite-text-muted">{group.note}</p> : null}
    <ul className="grid gap-0.5">
      {group.items.map((item) => <li key={item.label}><NavigationLink item={item} currentPath={currentPath} onNavigate={onNavigate} /></li>)}
    </ul>
  </>;
  if (group.collapsible === false) return <section aria-label={group.label} className="grid gap-1">
    <p className="px-3 pb-1 text-type-metadata font-medium text-nite-text-muted">{group.label}</p>{rows}
  </section>;
  return <details ref={detailsRef} open={group.defaultOpen} onToggle={(event) => {
    if (!ready.current) return;
    const next = event.currentTarget.open;
    try { window.sessionStorage.setItem(`nite-navigation-group:${group.label}`, next ? "open" : "closed"); } catch { /* A expansão continua funcional sem storage. */ }
  }} className="group/nav-group">
    <summary className="flex min-h-9 cursor-pointer list-none items-center justify-between gap-3 rounded-control px-3 py-1 text-type-metadata font-medium text-nite-text-muted hover:bg-nite-surface-subtle focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus [&::-webkit-details-marker]:hidden">
      <span>{group.label}</span>
      <svg aria-hidden="true" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" className="size-4 transition-transform group-open/nav-group:rotate-180"><path d="m5 7.5 5 5 5-5" /></svg>
    </summary>
    <div className="pt-1">{rows}</div>
  </details>;
}

export function AuthenticatedNavigation({ groups, onNavigate }: {
  groups: AuthenticatedNavigationGroup[];
  onNavigate?: () => void;
}) {
  const currentPath = usePathname();
  return <nav aria-label="Navegação principal" className="grid gap-1.5">
    {groups.map((group) => <NavigationGroup key={group.label} group={group} currentPath={currentPath} onNavigate={onNavigate} />)}
  </nav>;
}
