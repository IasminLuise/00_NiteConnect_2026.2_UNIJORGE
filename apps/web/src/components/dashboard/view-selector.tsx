"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useState, type FocusEvent as ReactFocusEvent, type KeyboardEvent as ReactKeyboardEvent } from "react";
import type { LeadershipProject } from "../../lib/projects/contracts";
import { resolveWorkspaceContext, workspaceContextHref } from "../shell/workspace-context";

export function ViewSelector({ id, projects, canAdminister }: {
  id: string;
  projects: LeadershipProject[];
  canAdminister: boolean;
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
    <div className="relative grid w-full max-w-64 gap-1" onBlur={handleBlur}>
      <label htmlFor={id} className="text-type-metadata font-medium text-nite-text-muted">Visão</label>
      <button
        type="button"
        id={id}
        role="combobox"
        aria-label="Visão"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={`${id}-listbox`}
        aria-activedescendant={open ? `${id}-option-${activeOptionIndex}` : undefined}
        className="flex min-h-9 w-full min-w-0 items-center justify-between gap-2 rounded-control border border-transparent bg-nite-surface-subtle px-3 text-left text-type-body-compact font-medium text-nite-text-primary transition-colors hover:bg-nite-surface-focus motion-reduce:transition-none focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus"
        onClick={() => (open ? setOpen(false) : openAt(selectedIndex >= 0 ? selectedIndex : 0))}
        onKeyDown={handleKeyDown}
      >
        <span className="min-w-0 truncate">{selectedIndex < 0 ? "Contexto indisponível" : options[selectedIndex]?.label}</span>
        <svg aria-hidden="true" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" className={`size-4 shrink-0 transition-transform motion-reduce:transition-none ${open ? "rotate-180" : ""}`}>
          <path d="m5 7.5 5 5 5-5" />
        </svg>
      </button>
      <ul
        id={`${id}-listbox`}
        role="listbox"
        aria-label="Visão"
        hidden={!open}
        className="absolute inset-x-0 top-full z-50 mt-1 max-h-64 overflow-y-auto rounded-panel border border-nite-border-soft bg-nite-background p-1 shadow-nite-floating"
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
              className={`flex min-h-9 cursor-pointer items-center justify-between gap-2 rounded-control px-3 py-1.5 text-type-body-compact focus-visible:outline-2 focus-visible:outline-nite-focus ${active ? "bg-nite-surface-subtle" : ""} ${selected ? "font-semibold text-nite-brand-primary" : "text-nite-text-primary"}`}
              onMouseDown={(event) => event.preventDefault()}
              onMouseEnter={() => setActiveIndex(index)}
              onClick={() => selectOption(index)}
            >
              <span className="min-w-0 truncate">{option.label}</span>
              {selected ? <svg aria-hidden="true" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" className="size-4 shrink-0 text-nite-brand-primary"><path d="m4.5 10.5 3.5 3.5 7.5-8" /></svg> : null}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
