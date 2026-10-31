"use client";

import Link from "next/link";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { ThemeControl } from "../theme-control";
import { signOutOfNite } from "../../lib/auth/actions";
import type { NiteMember } from "../../lib/auth/identity";
import type { LeadershipProject } from "../../lib/dashboard/contracts";
import { buildDashboardNavigationGroups, DashboardNavigation } from "./dashboard-navigation";

type DashboardShellProps = {
  children: ReactNode;
  member: NiteMember;
  roles: string[];
  contextLabel: string;
  contextValue: string;
  projects: LeadershipProject[];
  canAdminister: boolean;
  from: string;
  to: string;
  search: string;
};

function initials(name: string): string {
  const value = name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]?.toLocaleUpperCase("pt-BR") ?? "").join("");
  return value || "NC";
}

function accountRole(roles: string[]): string {
  if (roles.includes("COORDENADOR")) return "Coordenação";
  if (roles.includes("GESTOR_GERAL")) return "Gestão Geral";
  return "Integrante";
}

export function DashboardShell({ children, member, roles, contextLabel, contextValue, projects, canAdminister, from, to, search }: DashboardShellProps) {
  const [mobileNavigationOpen, setMobileNavigationOpen] = useState(false);
  const [activeAnchor, setActiveAnchor] = useState("overview");
  const mobileNavigationRef = useRef<HTMLElement>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    const syncAnchor = () => setActiveAnchor(window.location.hash.slice(1) || "overview");
    syncAnchor();
    window.addEventListener("hashchange", syncAnchor);
    return () => window.removeEventListener("hashchange", syncAnchor);
  }, []);

  useEffect(() => {
    if (!mobileNavigationOpen) {
      previousFocusRef.current?.focus();
      return;
    }
    previousFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    mobileNavigationRef.current?.querySelector<HTMLButtonElement>('button[aria-label="Fechar navegação"]')?.focus();
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setMobileNavigationOpen(false);
        return;
      }
      if (event.key !== "Tab") return;
      const focusable = Array.from(mobileNavigationRef.current?.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), summary, input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
      ) ?? []).filter((element) => element.getClientRects().length > 0);
      const first = focusable[0];
      const last = focusable.at(-1);
      if (!first || !last) return;
      if (!mobileNavigationRef.current?.contains(document.activeElement)) {
        event.preventDefault();
        (event.shiftKey ? last : first).focus();
      } else if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [mobileNavigationOpen]);

  const navigationGroups = buildDashboardNavigationGroups({
    contextValue,
    projects,
    canAdminister,
    canCoordinate: roles.includes("COORDENADOR"),
    from,
    to,
    search,
  });

  const closeMobileNavigation = () => setMobileNavigationOpen(false);
  const sidebar = (
    <>
      <div className="flex h-16 shrink-0 items-center border-b border-nite-border-soft px-5">
        <Link href="/dashboard" className="flex min-w-0 items-center gap-3 rounded-control focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus" onClick={closeMobileNavigation}>
          <span className="grid size-8 shrink-0 place-items-center rounded-control bg-nite-brand-primary font-display text-type-label font-semibold text-nite-action-text">N</span>
          <span className="truncate font-display text-type-label font-semibold leading-tight text-nite-text-primary">NITE Connect</span>
        </Link>
        <button type="button" className="ml-auto grid size-9 place-items-center rounded-control border border-nite-border-soft text-nite-text-muted hover:bg-nite-surface-subtle focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus lg:hidden" aria-label="Fechar navegação" onClick={closeMobileNavigation}>
          <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" className="size-4"><path d="m6 6 12 12M18 6 6 18" /></svg>
        </button>
      </div>

      <div className="shrink-0 border-b border-nite-border-soft px-4 py-3">
        <form action="/dashboard" method="get" className="grid gap-1.5">
          <input type="hidden" name="from" value={from} />
          <input type="hidden" name="to" value={to} />
          <label htmlFor="dashboard-context" className="text-type-metadata font-medium text-nite-text-muted">Espaço de trabalho</label>
          <select
            id="dashboard-context"
            name="scope"
            value={contextValue}
            className="min-h-10 w-full min-w-0 rounded-control border border-nite-border-soft bg-nite-surface px-3 text-type-body-compact font-medium text-nite-text-primary focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus"
            onChange={(event) => event.currentTarget.form?.requestSubmit()}
          >
            <option value="personal">Meu espaço</option>
            {projects.map((project) => <option key={project.projectId} value={`project:${project.projectId}`}>{project.title}</option>)}
            {canAdminister ? <option value="institutional">Visão geral do NITE</option> : null}
          </select>
        </form>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto px-3 py-3">
        <DashboardNavigation
          groups={navigationGroups}
          activeAnchor={activeAnchor}
          currentScope={contextValue}
          onNavigate={closeMobileNavigation}
        />
      </div>

    </>
  );

  return (
    <div className="min-h-svh bg-nite-background text-nite-text-primary">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-[256px] flex-col border-r border-nite-border-soft bg-nite-surface lg:flex" aria-label="NITE Connect">
        {sidebar}
      </aside>

      <div className="fixed inset-0 z-50 lg:hidden" hidden={!mobileNavigationOpen}>
        <button type="button" aria-label="Fechar navegação" className="absolute inset-0 bg-black/45" onClick={closeMobileNavigation} />
        <aside ref={mobileNavigationRef} id="dashboard-mobile-sidebar" role="dialog" aria-modal="true" className="relative flex h-full w-[min(19rem,calc(100vw-2.75rem))] flex-col border-r border-nite-border-soft bg-nite-surface shadow-nite-modal" aria-label="Navegação principal">
          {sidebar}
        </aside>
      </div>

      <div className="min-w-0 lg:pl-[256px]">
        <header className="sticky top-0 z-20 flex min-h-16 items-center gap-3 border-b border-nite-border-soft bg-nite-background/95 px-3 backdrop-blur sm:px-5 lg:px-7">
          <button type="button" aria-label="Abrir navegação" aria-expanded={mobileNavigationOpen} aria-controls="dashboard-mobile-sidebar" className="grid size-9 shrink-0 place-items-center rounded-control border border-nite-border-soft text-nite-text-secondary hover:bg-nite-surface-subtle focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus lg:hidden" onClick={() => setMobileNavigationOpen(true)}>
            <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" className="size-4"><path d="M4 6h16M4 12h16M4 18h16" /></svg>
          </button>

          <form action="/dashboard" method="get" role="search" className="relative min-w-0 w-full max-w-[min(36rem,55vw)] flex-1">
            <input type="hidden" name="scope" value={contextValue} />
            <input type="hidden" name="from" value={from} />
            <input type="hidden" name="to" value={to} />
            <label htmlFor="dashboard-search" className="sr-only">Buscar nos registros exibidos</label>
            <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-nite-text-muted"><circle cx="10.8" cy="10.8" r="6.8" /><path d="m16 16 4.5 4.5" /></svg>
            <input id="dashboard-search" name="q" type="search" defaultValue={search} placeholder="Buscar nos registros exibidos" className="min-h-10 w-full rounded-control border border-nite-border-soft bg-nite-surface py-2 pl-9 pr-3 text-type-body-compact text-nite-text-primary placeholder:text-nite-text-muted focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus" />
          </form>

          <span className="hidden max-w-52 truncate text-type-body-compact font-medium text-nite-text-secondary md:block" title={contextLabel}>{contextLabel}</span>
          <details className="relative ml-auto shrink-0">
            <summary aria-label="Abrir menu da conta" title="Conta" className="grid size-10 cursor-pointer list-none place-items-center rounded-full bg-nite-surface-elevated text-type-body-compact font-semibold text-nite-text-secondary hover:bg-nite-surface-subtle focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus [&::-webkit-details-marker]:hidden">
              {initials(member.name)}
            </summary>
            <div className="absolute right-0 top-[calc(100%+0.5rem)] z-50 w-[min(18rem,calc(100vw-2rem))] rounded-panel border border-nite-border-soft bg-nite-surface p-4 shadow-nite-floating">
              <p className="truncate text-type-body-compact font-semibold text-nite-text-primary">{member.name}</p>
              <p className="mt-1 break-all text-type-metadata text-nite-text-muted">{member.institutionalEmail}</p>
              <p className="mt-3 border-t border-nite-border-soft pt-3 text-type-metadata text-nite-text-secondary">{accountRole(roles)}</p>
              <div className="mt-3 flex items-center justify-between gap-3 border-t border-nite-border-soft pt-3">
                <span className="text-type-body-compact text-nite-text-secondary">Tema</span>
                <ThemeControl className="theme-control" />
              </div>
              <form action={signOutOfNite} className="mt-3 border-t border-nite-border-soft pt-3">
                <button type="submit" className="min-h-9 w-full rounded-control px-3 text-left text-type-body-compact text-nite-text-secondary hover:bg-nite-surface-subtle hover:text-nite-text-primary focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus">Sair da conta</button>
              </form>
            </div>
          </details>
        </header>

        <main id="dashboard-content" className="mx-auto min-w-0 max-w-[1600px] px-4 py-5 sm:px-6 sm:py-6 lg:px-7 lg:py-7">{children}</main>
      </div>
    </div>
  );
}
