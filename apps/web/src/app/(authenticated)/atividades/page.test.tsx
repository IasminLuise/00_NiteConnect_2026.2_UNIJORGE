import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { previewActivities } from "../../../lib/activities/preview-fixtures";

const { getMyActivities, getAllMyProjects, getMyActivityCapabilities } = vi.hoisted(() => ({
  getMyActivities: vi.fn(),
  getAllMyProjects: vi.fn(),
  getMyActivityCapabilities: vi.fn(),
}));

vi.mock("../../../lib/auth/server-session", () => ({
  requireAuthenticatedNiteSession: vi.fn(async () => ({ bearer: "server-only", member: { id: "member-1" } })),
}));
vi.mock("../../../lib/activities/data", () => ({ getMyActivities, getAllMyProjects, getMyActivityCapabilities }));
vi.mock("../../../components/activities/activity-detail", () => ({ ActivityFeedback: () => null }));

import ActivitiesPage from "./page";

function pageResult(total: number, page = 0) {
  return { data: { items: [previewActivities[0]], page, size: 20, total }, issue: null };
}

describe("página Minhas atividades", () => {
  it("mantém busca visível, filtros avançados recolhidos e oculta paginação de página única", async () => {
    getMyActivities.mockResolvedValue(pageResult(1));
    getAllMyProjects.mockResolvedValue({ data: [], issue: null });
    getMyActivityCapabilities.mockResolvedValue({ data: { createIndependent: false }, issue: null });

    const page = await ActivitiesPage({ searchParams: Promise.resolve({}) });
    const markup = renderToStaticMarkup(page);

    expect(markup).toContain('type="search"');
    expect(markup).toContain("Filtros avançados");
    expect(markup).toContain("lg:grid-cols-[minmax(16rem,1fr)_minmax(14rem,auto)_auto]");
    expect(markup).toContain("group-open:bg-nite-surface-subtle");
    expect(markup).toContain("overflow-x-auto");
    expect(markup).not.toContain("Mais recentes primeiro");
    expect(markup).not.toContain('aria-label="Paginação"');
  });

  it("mantém a paginação quando a API retorna mais de uma página", async () => {
    getMyActivities.mockResolvedValue(pageResult(41));
    getAllMyProjects.mockResolvedValue({ data: [], issue: null });
    getMyActivityCapabilities.mockResolvedValue({ data: { createIndependent: false }, issue: null });

    const page = await ActivitiesPage({ searchParams: Promise.resolve({}) });
    const markup = renderToStaticMarkup(page);

    expect(markup).toContain('aria-label="Paginação"');
    expect(markup).toContain("Itens 1–20 de 41");
  });
});
