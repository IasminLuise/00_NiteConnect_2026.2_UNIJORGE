import { isValidElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  requireSession: vi.fn(),
  getLeadershipProjects: vi.fn(),
  getDashboard: vi.fn(),
  getDashboardHistory: vi.fn(),
  getParticipationWeeklySummary: vi.fn(),
}));

vi.mock("../../../lib/auth/server-session", () => ({ requireAuthenticatedNiteSession: mocks.requireSession }));
vi.mock("../../../lib/projects/data", () => ({ getLeadershipProjects: mocks.getLeadershipProjects }));
vi.mock("../../../lib/dashboard/data", () => ({
  getDashboard: mocks.getDashboard,
  getDashboardHistory: mocks.getDashboardHistory,
  getParticipationWeeklySummary: mocks.getParticipationWeeklySummary,
}));

import DashboardPage from "./page";

describe("rota do painel autenticado", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-10-31T15:00:00Z"));
    mocks.requireSession.mockResolvedValue({
      member: { id: "member-1", name: "Pessoa", institutionalEmail: "person@example.invalid", status: "ATIVO" },
      roles: ["INTEGRANTE"],
      bearer: "server-only-bearer",
    });
    mocks.getLeadershipProjects.mockResolvedValue({ data: [], issue: null });
    mocks.getDashboard.mockResolvedValue({ data: null, issue: "unavailable" });
    mocks.getDashboardHistory.mockResolvedValue({ data: null, issue: "unavailable" });
    mocks.getParticipationWeeklySummary.mockResolvedValue({ data: null, issue: "unavailable" });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  it("usa o intervalo válido e limita a busca recebida pela URL", async () => {
    const page = await DashboardPage({ searchParams: Promise.resolve({
      scope: "personal",
      from: "2026-10-25",
      to: "2026-10-31",
      q: `${"x".repeat(120)}extra`,
    }) });

    expect(mocks.getDashboard).toHaveBeenCalledWith("server-only-bearer", "personal", "2026-10-25", "2026-10-31", undefined);
    expect(isValidElement(page)).toBe(true);
    if (!isValidElement<Record<string, unknown>>(page)) throw new Error("A rota não retornou DashboardView.");
    expect(page.props.from).toBe("2026-10-25");
    expect(page.props.to).toBe("2026-10-31");
    expect(page.props.search).toBe("x".repeat(120));
  });

  it("substitui datas impossíveis, invertidas ou acima de 365 dias pelo período padrão", async () => {
    for (const range of [
      { from: "2026-02-30", to: "2026-03-02" },
      { from: "2026-10-31", to: "2026-10-25" },
      { from: "2025-01-01", to: "2026-10-31" },
    ]) {
      mocks.getDashboard.mockClear();
      const page = await DashboardPage({ searchParams: Promise.resolve({ scope: "personal", ...range }) });

      expect(mocks.getDashboard).toHaveBeenCalledWith("server-only-bearer", "personal", "2026-10-25", "2026-10-31", undefined);
      if (!isValidElement<Record<string, unknown>>(page)) throw new Error("A rota não retornou DashboardView.");
      expect(page.props.from).toBe("2026-10-25");
      expect(page.props.to).toBe("2026-10-31");
    }
  });

  it("nega contextos institucionais e projetos fora da liderança atual vindos por URL", async () => {
    const unauthorizedInstitution = await DashboardPage({ searchParams: Promise.resolve({ scope: "institutional" }) });
    expect(mocks.getDashboard).not.toHaveBeenCalled();
    if (!isValidElement<Record<string, unknown>>(unauthorizedInstitution)) throw new Error("A rota não retornou DashboardView.");
    expect(unauthorizedInstitution.props.contextValue).toBe("personal");
    expect(unauthorizedInstitution.props.result).toEqual({ data: null, issue: "access" });

    const unauthorizedProject = await DashboardPage({ searchParams: Promise.resolve({ scope: "project:expired-project" }) });
    expect(mocks.getDashboard).not.toHaveBeenCalled();
    if (!isValidElement<Record<string, unknown>>(unauthorizedProject)) throw new Error("A rota não retornou DashboardView.");
    expect(unauthorizedProject.props.contextValue).toBe("personal");
    expect(unauthorizedProject.props.result).toEqual({ data: null, issue: "access" });
  });
});
