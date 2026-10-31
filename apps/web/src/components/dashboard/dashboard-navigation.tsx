import Link from "next/link";
import { useEffect, useRef, type ReactNode } from "react";
import type { LeadershipProject } from "../../lib/dashboard/contracts";

export type DashboardNavigationIcon =
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

export type DashboardNavigationItem = {
  label: string;
  icon: DashboardNavigationIcon;
  href?: string;
  scope?: string;
  anchor?: string;
  status?: "development";
  unavailableBecause?: string;
};

export type DashboardNavigationGroup = {
  label: string;
  items: DashboardNavigationItem[];
  defaultOpen: boolean;
  note?: string;
  collapsible?: boolean;
};

type NavigationOptions = {
  contextValue: string;
  projects: LeadershipProject[];
  canAdminister: boolean;
  canCoordinate: boolean;
  from: string;
  to: string;
  search: string;
};

function destination(options: NavigationOptions, scope: string, anchor: string): string {
  const params = new URLSearchParams({ scope, from: options.from, to: options.to });
  if (scope === options.contextValue && options.search) params.set("q", options.search);
  return `/dashboard?${params.toString()}#${anchor}`;
}

function planned(label: string, icon: DashboardNavigationIcon): DashboardNavigationItem {
  return { label, icon, status: "development" };
}

export function buildDashboardNavigationGroups(options: NavigationOptions): DashboardNavigationGroup[] {
  const selectedProject = options.projects.find((project) => options.contextValue === `project:${project.projectId}`);
  const projectForNavigation = selectedProject ?? (options.projects.length === 1 ? options.projects[0] : undefined);
  const groups: DashboardNavigationGroup[] = [
    {
      label: "Início",
      defaultOpen: true,
      collapsible: false,
      items: [{ label: "Visão geral", icon: "home", scope: options.contextValue, anchor: "overview", href: destination(options, options.contextValue, "overview") }],
    },
    {
      label: "Meu espaço",
      defaultOpen: options.contextValue === "personal",
      items: [
        { label: "Minhas atividades", icon: "activity", scope: "personal", anchor: "records", href: destination(options, "personal", "records") },
        { label: "Minha participação", icon: "clock", scope: "personal", anchor: "participation", href: destination(options, "personal", "participation") },
        planned("Meus projetos", "projects"),
        planned("Agenda", "calendar"),
      ],
    },
  ];

  if (options.projects.length > 0) {
    const projectScope = projectForNavigation ? `project:${projectForNavigation.projectId}` : null;
    groups.push({
      label: "Projetos que lidero",
      defaultOpen: options.contextValue.startsWith("project:"),
      note: projectForNavigation ? undefined : "Escolha um projeto no espaço de trabalho acima.",
      items: [
        projectScope
          ? { label: "Acompanhamento", icon: "projects", scope: projectScope, anchor: "overview", href: destination(options, projectScope, "overview") }
          : { label: "Acompanhamento", icon: "projects", unavailableBecause: "Escolha um projeto no espaço de trabalho acima." },
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
        defaultOpen: options.contextValue === "institutional",
        items: [
          { label: "Projetos", icon: "projects", scope: "institutional", anchor: "records", href: destination(options, "institutional", "records") },
          planned("Atividades e entregas", "activity"),
          { label: "Frequência e revisões", icon: "attendance", scope: "institutional", anchor: "participation", href: destination(options, "institutional", "participation") },
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

const iconPaths: Record<DashboardNavigationIcon, ReactNode> = {
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

function Icon({ name }: { name: DashboardNavigationIcon }) {
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" className="size-4 shrink-0">{iconPaths[name]}</svg>;
}

function NavigationLink({ item, activeAnchor, currentScope, onNavigate }: {
  item: DashboardNavigationItem;
  activeAnchor: string;
  currentScope: string;
  onNavigate?: () => void;
}) {
  if (!item.href) {
    const reason = item.unavailableBecause ?? "Em desenvolvimento";
    return <span aria-disabled="true" aria-label={`${item.label}, ${reason}`} title={reason} className="flex min-h-9 items-center gap-2.5 rounded-control px-3 py-1.5 text-type-body-compact text-nite-text-muted">
      <Icon name={item.icon} />
      <span className="min-w-0 flex-1 truncate">{item.label}</span>
      {item.status === "development" ? <span aria-hidden="true" className="size-1.5 shrink-0 rounded-full bg-nite-border-strong" /> : null}
    </span>;
  }
  const isCurrent = item.scope === currentScope && item.anchor === activeAnchor;
  return <Link href={item.href} onClick={onNavigate} aria-current={isCurrent ? "page" : undefined} className={`flex min-h-9 items-center gap-2.5 rounded-control px-3 py-1.5 text-type-body-compact transition-colors focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus ${isCurrent ? "bg-nite-brand-soft font-medium text-nite-brand-primary" : "text-nite-text-secondary hover:bg-nite-surface-subtle hover:text-nite-text-primary"}`}>
    <Icon name={item.icon} />
    <span className="min-w-0 truncate">{item.label}</span>
  </Link>;
}

function NavigationGroup({ group, activeAnchor, currentScope, onNavigate }: {
  group: DashboardNavigationGroup;
  activeAnchor: string;
  currentScope: string;
  onNavigate?: () => void;
}) {
  const detailsRef = useRef<HTMLDetailsElement>(null);
  const ready = useRef(false);
  useEffect(() => {
    const details = detailsRef.current;
    if (!details) return;
    try {
      const saved = window.sessionStorage.getItem(`nite-dashboard-group:${group.label}`);
      details.open = saved === null ? group.defaultOpen : saved === "open";
    } catch {
      details.open = group.defaultOpen;
    }
    ready.current = true;
  }, [group.label, group.defaultOpen]);
  const rows = <>
    {group.note ? <p className="px-3 pb-1 text-type-metadata leading-relaxed text-nite-text-muted">{group.note}</p> : null}
    <ul className="grid gap-0.5">
      {group.items.map((item) => <li key={item.label}><NavigationLink item={item} activeAnchor={activeAnchor} currentScope={currentScope} onNavigate={onNavigate} /></li>)}
    </ul>
  </>;
  if (group.collapsible === false) return <section aria-label={group.label} className="grid gap-1">
    <p className="px-3 pb-1 text-type-metadata font-medium text-nite-text-muted">{group.label}</p>{rows}
  </section>;
  return <details ref={detailsRef} open={group.defaultOpen} onToggle={(event) => {
    if (!ready.current) return;
    const next = event.currentTarget.open;
    try { window.sessionStorage.setItem(`nite-dashboard-group:${group.label}`, next ? "open" : "closed"); } catch { /* A expansão continua funcional sem storage. */ }
  }} className="group/nav-group">
    <summary className="flex min-h-9 cursor-pointer list-none items-center justify-between gap-3 rounded-control px-3 py-1 text-type-metadata font-medium text-nite-text-muted hover:bg-nite-surface-subtle focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus [&::-webkit-details-marker]:hidden">
      <span>{group.label}</span>
      <svg aria-hidden="true" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" className="size-4 transition-transform group-open/nav-group:rotate-180"><path d="m5 7.5 5 5 5-5" /></svg>
    </summary>
    <div className="pt-1">{rows}</div>
  </details>;
}

export function DashboardNavigation({ groups, activeAnchor, currentScope, onNavigate }: {
  groups: DashboardNavigationGroup[];
  activeAnchor: string;
  currentScope: string;
  onNavigate?: () => void;
}) {
  return <nav aria-label="Navegação principal" className="grid gap-1.5">
    {groups.map((group) => <NavigationGroup key={group.label} group={group} activeAnchor={activeAnchor} currentScope={currentScope} onNavigate={onNavigate} />)}
  </nav>;
}
