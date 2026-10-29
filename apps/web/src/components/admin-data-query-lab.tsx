"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Badge, Button, Field, InlineFeedback, Select, Surface, TextField } from "./ui";
import {
  DataTable,
  EmptyState,
  ErrorState,
  FilterToolbar,
  LoadingState,
  Pagination,
  type DataTableColumn,
} from "./patterns";

type DemoRecord = {
  id: string;
  title: string;
  category: string;
  updatedAt: string;
  status: "active" | "review" | "done";
};

const titles = [
  "Plano de extensão universitária",
  "Relatório de atividades do núcleo",
  "Pesquisa de iniciação científica",
  "Oficina de tecnologias assistivas",
  "Revisão do inventário de equipamentos",
  "Documentação de acompanhamento e registro das ações realizadas ao longo do semestre",
  "Preparação do calendário de atividades",
  "Levantamento de referências bibliográficas",
  "Organização dos materiais de divulgação",
];
const categories = ["Extensão", "Gestão", "Pesquisa", "Formação"];
const statuses: DemoRecord["status"][] = ["active", "review", "done"];
const statusLabels: Record<DemoRecord["status"], string> = {
  active: "Em andamento",
  review: "Em revisão",
  done: "Concluído",
};
const statusVariants: Record<DemoRecord["status"], "info" | "warning" | "success"> = {
  active: "info",
  review: "warning",
  done: "success",
};
const records: DemoRecord[] = Array.from({ length: 18 }, (_, index) => ({
  id: `NC-${String(index + 1).padStart(3, "0")}`,
  title: titles[index % titles.length],
  category: categories[index % categories.length],
  updatedAt: `${String((index % 27) + 1).padStart(2, "0")} out 2026`,
  status: statuses[index % statuses.length],
}));

type QueryView = "content" | "loading" | "error";
type Density = "comfortable" | "compact";
const pageSize = 5;

function AdminDataQueryLab() {
  const [searchDraft, setSearchDraft] = useState("");
  const [statusDraft, setStatusDraft] = useState("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [appliedStatus, setAppliedStatus] = useState("");
  const [view, setView] = useState<QueryView>("content");
  const [density, setDensity] = useState<Density>("comfortable");
  const [page, setPage] = useState(1);
  const [initialEmpty, setInitialEmpty] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  useEffect(() => {
    if (view !== "loading") return;
    const timeoutId = window.setTimeout(() => setView("content"), 500);
    return () => window.clearTimeout(timeoutId);
  }, [view]);

  const hasActiveFilters = Boolean(appliedSearch.trim() || appliedStatus);
  const filteredRecords = initialEmpty
    ? []
    : records.filter((record) => {
        const searchable = `${record.id} ${record.title} ${record.category}`.toLocaleLowerCase("pt-BR");
        return searchable.includes(appliedSearch.trim().toLocaleLowerCase("pt-BR")) &&
          (!appliedStatus || record.status === appliedStatus);
      });
  const totalPages = Math.ceil(filteredRecords.length / pageSize);
  const startIndex = Math.max(0, Math.min(page - 1, totalPages - 1)) * pageSize;
  const pageRecords = filteredRecords.slice(startIndex, startIndex + pageSize);
  const selectedRecord = records.find((record) => record.id === selectedId);

  const columns: DataTableColumn<DemoRecord>[] = [
    {
      id: "id",
      header: "ID",
      className: "w-28 whitespace-nowrap",
      cell: (record) => <code className="font-mono text-type-technical">{record.id}</code>,
    },
    {
      id: "title",
      header: "Registro",
      rowHeader: true,
      className: "min-w-64",
      cell: (record) => record.title,
    },
    { id: "category", header: "Categoria", className: "w-36", cell: (record) => record.category },
    {
      id: "updated-at",
      header: "Atualizado em",
      align: "end",
      className: "w-36 whitespace-nowrap",
      cell: (record) => record.updatedAt,
    },
    {
      id: "status",
      header: "Situação",
      className: "w-36",
      cell: (record) => <Badge variant={statusVariants[record.status]}>{statusLabels[record.status]}</Badge>,
    },
    {
      id: "actions",
      header: "Ações",
      align: "end",
      className: "w-36",
      cell: (record) => (
        <Button type="button" variant="quiet" onClick={() => setSelectedId(record.id)}>
          Ver registro
        </Button>
      ),
    },
  ];

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAppliedSearch(searchDraft);
    setAppliedStatus(statusDraft);
    setInitialEmpty(false);
    setPage(1);
    setSelectedId(null);
    setView("loading");
  }

  function clearFilters() {
    setSearchDraft("");
    setStatusDraft("");
    setAppliedSearch("");
    setAppliedStatus("");
    setInitialEmpty(false);
    setPage(1);
    setSelectedId(null);
    setView("content");
  }

  function showInitialEmpty() {
    clearFilters();
    setInitialEmpty(true);
  }

  return (
    <section className="space-y-4" aria-labelledby="query-patterns-heading">
      <header>
        <p className="lab-eyebrow">11 · CONSULTA ADMINISTRATIVA</p>
        <h2 id="query-patterns-heading" className="font-display text-type-section-title">
          Tabelas, filtros e paginação
        </h2>
        <p className="mt-2 max-w-prose text-type-body-compact text-nite-text-secondary">
          A composição sincroniza filtros, densidade e página atual; os registros são fictícios e locais.
        </p>
      </header>

      <Surface>
        <FilterToolbar
          aria-label="Filtros da lista demonstrativa"
          onSubmit={applyFilters}
          hasActiveFilters={hasActiveFilters}
          onClear={clearFilters}
          actions={<Button type="submit">Aplicar filtros</Button>}
        >
          <Field id="query-search" label="Pesquisar registros">
            <TextField
              type="search"
              value={searchDraft}
              onChange={(event) => setSearchDraft(event.target.value)}
              placeholder="ID, nome ou categoria"
            />
          </Field>
          <Field id="query-status" label="Situação">
            <Select value={statusDraft} onChange={(event) => setStatusDraft(event.target.value)}>
              <option value="">Todas as situações</option>
              <option value="active">Em andamento</option>
              <option value="review">Em revisão</option>
              <option value="done">Concluído</option>
            </Select>
          </Field>
        </FilterToolbar>
      </Surface>

      <div className="flex min-w-0 flex-wrap items-center justify-between gap-4">
        <div className="flex min-w-0 flex-wrap items-center gap-3" role="group" aria-label="Densidade da tabela">
          <span className="text-type-body-compact text-nite-text-secondary">Densidade</span>
          <Button
            type="button"
            variant={density === "comfortable" ? "secondary" : "quiet"}
            aria-pressed={density === "comfortable"}
            onClick={() => setDensity("comfortable")}
          >
            Densidade confortável
          </Button>
          <Button
            type="button"
            variant={density === "compact" ? "secondary" : "quiet"}
            aria-pressed={density === "compact"}
            onClick={() => setDensity("compact")}
          >
            Densidade compacta
          </Button>
        </div>
        <div className="flex min-w-0 flex-wrap items-center gap-3" role="group" aria-label="Controles de demonstração da consulta">
          <Button type="button" variant="quiet" onClick={() => setView("loading")}>
            Simular carregamento
          </Button>
          <Button type="button" variant="quiet" onClick={() => setView("error")}>
            Simular falha
          </Button>
          <Button type="button" variant="quiet" onClick={showInitialEmpty}>
            Simular coleção vazia
          </Button>
          {initialEmpty ? (
            <Button type="button" variant="quiet" onClick={clearFilters}>
              Restaurar registros demonstrativos
            </Button>
          ) : null}
        </div>
      </div>

      <section aria-labelledby="query-results-heading" className="min-w-0 space-y-3">
        <header className="flex min-w-0 flex-wrap items-end justify-between gap-3">
          <div>
            <h3 id="query-results-heading" className="font-sans text-type-component-title text-nite-text-primary">
              Registros demonstrativos
            </h3>
            {view === "content" ? (
              <p className="mt-1 text-type-metadata text-nite-text-secondary" role="status" aria-live="polite">
                {filteredRecords.length} {filteredRecords.length === 1 ? "registro encontrado" : "registros encontrados"}
              </p>
            ) : null}
          </div>
          {hasActiveFilters ? <Badge variant="info">Filtros aplicados</Badge> : null}
        </header>

        {view === "loading" ? (
          <LoadingState label="Atualizando registros demonstrativos…" />
        ) : view === "error" ? (
          <ErrorState
            description="A consulta local simulou uma falha. Tente novamente para recuperar os resultados demonstrativos."
            onRetry={() => setView("loading")}
          />
        ) : filteredRecords.length === 0 ? (
          <EmptyState kind={hasActiveFilters ? "filtered" : "initial"} />
        ) : (
          <DataTable
            caption="Registros consultados nesta demonstração"
            columns={columns}
            rows={pageRecords}
            getRowId={(record) => record.id}
            density={density}
          />
        )}

        {selectedRecord ? (
          <InlineFeedback
            variant="info"
            description={`Registro ${selectedRecord.id} selecionado apenas para demonstração.`}
            announcement="polite"
          />
        ) : null}

        {view === "content" ? (
          <Pagination
            currentPage={page}
            totalPages={totalPages}
            onPageChange={(nextPage) => {
              setPage(nextPage);
              setSelectedId(null);
            }}
          />
        ) : null}
      </section>
    </section>
  );
}

export { AdminDataQueryLab };
