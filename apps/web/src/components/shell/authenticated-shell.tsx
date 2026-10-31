"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useRef, useState, type FocusEvent as ReactFocusEvent, type KeyboardEvent as ReactKeyboardEvent, type ReactNode } from "react";
import { ThemeControl } from "../theme-control";
import { signOutOfNite } from "../../lib/auth/actions";
import type { NiteMember } from "../../lib/auth/identity";
import type { LeadershipProject } from "../../lib/projects/contracts";
import { AuthenticatedNavigation, buildAuthenticatedNavigationGroups } from "./authenticated-navigation";
import { resolveWorkspaceContext, workspaceContextHref } from "./workspace-context";

type AuthenticatedShellProps = {
  children: ReactNode;
  member: NiteMember;
  roles: string[];
  projects: LeadershipProject[];
  leadershipProjectsUnavailable: boolean;
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

function WorkspaceSelector({ id, projects, canAdminister, onContextChange }: {
  id: string;
  projects: LeadershipProject[];
  canAdminister: boolean;
  onContextChange?: () => void;
}) {
  const pathname = usePathname();
  const router = useRouter();
  const searchParams = useSearchParams();
  const requestedContext = searchParams.get("scope");
  const contextValue = resolveWorkspaceContext(requestedContext, projects, canAdminister);
  const value = requestedContext === null || requestedContext === contextValue ? contextValue : "";
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const options: { value: string; label: string }[] = [
    { value: "personal", label: "Meu espaço" },
    ...projects.map((project) => ({ value: `project:${project.projectId}`, label: project.title })),
    ...(canAdminister ? [{ value: "institutional", label: "Visão geral do NITE" }] : []),
  ];
  const selectedIndex = options.findIndex((option) => option.value === value);
  const activeOptionIndex = Math.min(activeIndex, options.length - 1);

  const changeContext = (nextValue: string) => {
    if (!nextValue) return;
    const allowedContext = resolveWorkspaceContext(nextValue, projects, canAdminister);
    if (allowedContext !== nextValue) return;
    onContextChange?.();
    router.push(workspaceContextHref(pathname, searchParams.toString(), allowedContext));
  };

  const openAt = (index: number) => {
    setActiveIndex(index);
    setOpen(true);
  };

  const selectOption = (index: number) => {
    const option = options[index];
    if (!option) return;
    changeContext(option.value);
    setOpen(false);
  };

  const handleKeyDown = (event: ReactKeyboardEvent<HTMLButtonElement>) => {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      if (!open) openAt(selectedIndex >= 0 ? selectedIndex : 0);
      else setActiveIndex((current) => (current + 1) % options.length);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      if (!open) openAt(selectedIndex >= 0 ? selectedIndex : options.length - 1);
      else setActiveIndex((current) => (current - 1 + options.length) % options.length);
    } else if (event.key === "Home") {
      event.preventDefault();
      openAt(0);
    } else if (event.key === "End") {
      event.preventDefault();
      openAt(options.length - 1);
    } else if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      if (open) selectOption(activeOptionIndex);
      else openAt(selectedIndex >= 0 ? selectedIndex : 0);
    } else if (event.key === "Escape" && open) {
      event.preventDefault();
      setOpen(false);
    } else if (event.key === "Tab") {
      setOpen(false);
    }
  };

  const handleBlur = (event: ReactFocusEvent<HTMLDivElement>) => {
    if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setOpen(false);
  };

  return (
    <div className="relative grid gap-1.5" onBlur={handleBlur}>
      <label htmlFor={id} className="text-type-metadata font-medium text-nite-text-muted">Espaço de trabalho</label>
      <button
        type="button"
        id={id}
        role="combobox"
        aria-label="Espaço de trabalho"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={`${id}-listbox`}
        aria-activedescendant={open ? `${id}-option-${activeOptionIndex}` : undefined}
        className="flex min-h-10 w-full min-w-0 items-center justify-between gap-2 rounded-control border border-nite-border-soft bg-nite-surface px-3 text-left text-type-body-compact font-medium text-nite-text-primary focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus"
        onClick={() => (open ? setOpen(false) : openAt(selectedIndex >= 0 ? selectedIndex : 0))}
        onKeyDown={handleKeyDown}
      >
        <span className="min-w-0 truncate">{selectedIndex < 0 ? "Contexto indisponível" : options[selectedIndex]?.label}</span>
        <svg aria-hidden="true" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" className={`size-4 shrink-0 transition-transform ${open ? "rotate-180" : ""}`}>
          <path d="m5 7.5 5 5 5-5" />
        </svg>
      </button>
      <ul
        id={`${id}-listbox`}
        role="listbox"
        aria-label="Espaço de trabalho"
        hidden={!open}
        className="absolute inset-x-0 top-full z-50 mt-1 max-h-64 overflow-y-auto rounded-control border border-nite-border-soft bg-nite-surface p-1 shadow-lg"
      >
        {options.map((option, index) => {
          const selected = option.value === value;
          const active = index === activeOptionIndex;
          return (
            <li
              key={option.value}
              id={`${id}-option-${index}`}
              role="option"
              aria-selected={selected}
              className={`flex min-h-10 cursor-pointer items-center justify-between gap-2 rounded-control px-3 py-2 text-type-body-compact focus-visible:outline-2 focus-visible:outline-nite-focus ${active ? "bg-nite-surface-subtle" : ""} ${selected ? "font-semibold text-nite-brand-primary" : "text-nite-text-primary"}`}
              onMouseDown={(event) => event.preventDefault()}
              onMouseEnter={() => setActiveIndex(index)}
              onClick={() => selectOption(index)}
            >
              <span className="min-w-0 truncate">{option.label}</span>
              {selected ? <span aria-hidden="true">✓</span> : null}
            </li>
          );
        })}
      </ul>
    </div>
  );
}

function WorkspaceSelectorFallback({ id }: {
  id: string;
}) {
  return (
    <div className="grid gap-1.5" aria-busy="true">
      <label htmlFor={id} className="text-type-metadata font-medium text-nite-text-muted">Espaço de trabalho</label>
      <button id={id} type="button" role="combobox" aria-label="Espaço de trabalho" aria-haspopup="listbox" aria-expanded="false" aria-controls={`${id}-listbox`} disabled className="flex min-h-10 w-full min-w-0 items-center justify-between gap-2 rounded-control border border-nite-border-soft bg-nite-surface px-3 text-left text-type-body-compact font-medium text-nite-text-primary">
        <span className="truncate">Meu espaço</span>
        <span aria-hidden="true">⌄</span>
      </button>
      <ul id={`${id}-listbox`} role="listbox" aria-label="Espaço de trabalho" hidden>
        <li role="option" aria-selected="true">Meu espaço</li>
      </ul>
    </div>
  );
}

export function AuthenticatedShell({ children, member, roles, projects, leadershipProjectsUnavailable }: AuthenticatedShellProps) {
  const [mobileNavigationOpen, setMobileNavigationOpen] = useState(false);
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
  const mobileNavigationRef = useRef<HTMLElement>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);
  const accountMenuRef = useRef<HTMLDetailsElement>(null);
  const accountSummaryRef = useRef<HTMLElement>(null);
  const canAdminister = roles.some((role) => role === "GESTOR_GERAL" || role === "COORDENADOR");
  const navigationGroups = buildAuthenticatedNavigationGroups({
    projects,
    canAdminister,
    canCoordinate: roles.includes("COORDENADOR"),
  });

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

  useEffect(() => {
    if (!accountMenuOpen) return;
    const details = accountMenuRef.current;
    const handlePointerDown = (event: PointerEvent) => {
      if (!(event.target instanceof Node) || details?.contains(event.target)) return;
      if (details) details.open = false;
      setAccountMenuOpen(false);
    };
    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, [accountMenuOpen]);

  const handleAccountMenuToggle = () => {
    const details = accountMenuRef.current;
    const open = details?.open ?? false;
    setAccountMenuOpen(open);
    if (open && details) {
      window.requestAnimationFrame(() => {
        details.querySelector<HTMLElement>(
          'select:not([disabled]), button:not([disabled]), a[href], input:not([disabled]), [tabindex]:not([tabindex="-1"])',
        )?.focus();
      });
    }
  };

  const handleAccountMenuKeyDown = (event: ReactKeyboardEvent<HTMLDetailsElement>) => {
    if (event.key !== "Escape" || !event.currentTarget.open) return;
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.open = false;
    setAccountMenuOpen(false);
    accountSummaryRef.current?.focus();
  };

  const handleAccountMenuBlur = (event: ReactFocusEvent<HTMLDetailsElement>) => {
    const nextFocus = event.relatedTarget;
    if (!(nextFocus instanceof Node) || event.currentTarget.contains(nextFocus)) return;
    event.currentTarget.open = false;
    setAccountMenuOpen(false);
  };

  const closeMobileNavigation = () => setMobileNavigationOpen(false);
  const openMobileNavigation = () => {
    const accountMenu = accountMenuRef.current;
    if (accountMenu) accountMenu.open = false;
    setAccountMenuOpen(false);
    setMobileNavigationOpen(true);
  };
  const renderSidebar = (mobile: boolean) => {
    const selectorId = mobile ? "workspace-context-mobile" : "workspace-context-desktop";
    return (
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
          <Suspense fallback={<WorkspaceSelectorFallback id={selectorId} />}>
            <WorkspaceSelector id={selectorId} projects={projects} canAdminister={canAdminister} onContextChange={mobile ? closeMobileNavigation : undefined} />
          </Suspense>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto px-3 py-3">
          <AuthenticatedNavigation groups={navigationGroups} onNavigate={closeMobileNavigation} />
        </div>
      </>
    );
  };

  return (
    <div className="min-h-svh bg-nite-background text-nite-text-primary">
      <a href="#authenticated-content" className="sr-only z-50 rounded-control bg-nite-surface px-3 py-2 text-nite-text-primary focus:not-sr-only focus:fixed focus:left-3 focus:top-3 focus:outline-3 focus:outline-nite-focus">Ir para o conteúdo principal</a>
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-[256px] flex-col border-r border-nite-border-soft bg-nite-surface lg:flex" aria-label="NITE Connect">
        {renderSidebar(false)}
      </aside>

      <div className="fixed inset-0 z-50 lg:hidden" hidden={!mobileNavigationOpen}>
        <button type="button" aria-label="Fechar navegação" className="absolute inset-0 bg-black/45" onClick={closeMobileNavigation} />
        <aside ref={mobileNavigationRef} id="authenticated-mobile-sidebar" role="dialog" aria-modal="true" className="relative flex h-full w-[min(19rem,calc(100vw-2.75rem))] flex-col border-r border-nite-border-soft bg-nite-background shadow-nite-modal" aria-label="Navegação principal">
          {renderSidebar(true)}
        </aside>
      </div>

      <div className="min-w-0 lg:pl-[256px]">
        <header className="sticky top-0 z-20 flex min-h-16 items-center gap-3 border-b border-nite-border-soft bg-nite-background/95 px-3 backdrop-blur sm:px-5 lg:px-7">
          <button type="button" aria-label="Abrir navegação" aria-expanded={mobileNavigationOpen} aria-controls="authenticated-mobile-sidebar" className="grid size-9 shrink-0 place-items-center rounded-control border border-nite-border-soft text-nite-text-secondary hover:bg-nite-surface-subtle focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus lg:hidden" onClick={openMobileNavigation}>
            <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" className="size-4"><path d="M4 6h16M4 12h16M4 18h16" /></svg>
          </button>
          <div className="min-w-0 flex-1" aria-hidden="true" />
          <details ref={accountMenuRef} onToggle={handleAccountMenuToggle} onKeyDown={handleAccountMenuKeyDown} onBlurCapture={handleAccountMenuBlur} className="relative ml-auto shrink-0">
            <summary ref={accountSummaryRef} aria-label="Abrir menu da conta" aria-controls="account-menu-panel" aria-expanded={accountMenuOpen} title="Conta" className="grid size-10 cursor-pointer list-none place-items-center rounded-full bg-nite-surface-elevated text-type-body-compact font-semibold text-nite-text-secondary hover:bg-nite-surface-subtle focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus [&::-webkit-details-marker]:hidden">
              {initials(member.name)}
            </summary>
            <div id="account-menu-panel" className="absolute right-0 top-[calc(100%+0.5rem)] z-50 max-h-[calc(100svh-5rem)] w-[min(18rem,calc(100vw-2rem))] overflow-y-auto rounded-panel border border-nite-border-soft bg-nite-background p-4 shadow-nite-floating">
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

        <main id="authenticated-content" className="mx-auto min-w-0 max-w-[1600px] px-4 py-5 sm:px-6 sm:py-6 lg:px-7 lg:py-7">
          {leadershipProjectsUnavailable ? <p className="mb-4 rounded-control border border-status-warning-border bg-status-warning-soft px-4 py-3 text-type-body-compact text-status-warning" role="status">Não foi possível consultar seus projetos sob liderança; os contextos disponíveis podem estar incompletos.</p> : null}
          {children}
        </main>
      </div>
    </div>
  );
}
