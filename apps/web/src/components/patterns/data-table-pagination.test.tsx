import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { DataTable, type DataTableColumn } from "./data-table";
import { Pagination } from "./pagination";

type DemoRow = { id: string; name: string; status: string };

const columns: DataTableColumn<DemoRow>[] = [
  { id: "name", header: "Registro", cell: (row) => row.name, rowHeader: true },
  { id: "status", header: "Situação", cell: (row) => row.status, align: "end" },
];

describe("DataTable", () => {
  it("renderiza tabela semântica com caption, cabeçalhos, linhas e densidade sem trocar a tipografia", () => {
    const rows: DemoRow[] = [
      { id: "record-1", name: "Relatório de extensão com texto longo", status: "Em revisão" },
    ];
    const html = renderToStaticMarkup(
      <DataTable
        caption="Registros consultados"
        columns={columns}
        rows={rows}
        getRowId={(row) => row.id}
        density="compact"
      />,
    );

    expect(html).toContain("<table");
    expect(html).toContain("<caption class=\"sr-only\">Registros consultados</caption>");
    expect(html).toContain('<th scope="col"');
    expect(html).toContain('<th scope="row"');
    expect(html).toContain("Relatório de extensão com texto longo");
    expect(html).toContain('data-density="compact"');
    expect(html).toContain("px-3 py-2");
    expect(html).toContain("text-type-body-compact");
    expect(html).toContain("overflow-x-auto");

    const comfortableHtml = renderToStaticMarkup(
      <DataTable
        caption="Registros consultados"
        columns={columns}
        rows={rows}
        getRowId={(row) => row.id}
      />,
    );
    expect(comfortableHtml).toContain('data-density="comfortable"');
    expect(comfortableHtml).toContain("px-4 py-3");
    expect(comfortableHtml).toContain("text-type-body-compact");
  });

  it("permite inserir um estado vazio composto sem codificar regras de consulta", () => {
    const html = renderToStaticMarkup(
      <DataTable
        caption="Registros consultados"
        columns={columns}
        rows={[]}
        getRowId={(row) => row.id}
        emptyContent={<p>Nenhum resultado</p>}
      />,
    );

    expect(html).toContain('colSpan="2"');
    expect(html).toContain("Nenhum resultado");
  });
});

describe("Pagination", () => {
  it("desabilita voltar no início e anuncia a posição atual", () => {
    const html = renderToStaticMarkup(
      <Pagination currentPage={1} totalPages={4} onPageChange={() => undefined} />,
    );

    expect(html).toContain('aria-label="Paginação"');
    expect(html).toContain("Página 1 de 4");
    expect(html).toContain('class="sr-only">Página 1 de 4</span>');
    expect(html).toContain('class="sm:hidden" aria-hidden="true">1/4</span>');
    expect(html).toContain('aria-label="Página anterior"');
    expect(html).toContain('aria-label="Próxima página"');
    expect(html).toContain('aria-label="Página anterior" data-slot="button" data-variant="secondary" disabled=""');
    expect(html).not.toContain('aria-label="Próxima página" data-slot="button" data-variant="secondary" disabled=""');
  });

  it("desabilita ambas as direções quando não há páginas", () => {
    const html = renderToStaticMarkup(
      <Pagination currentPage={1} totalPages={0} onPageChange={() => undefined} />,
    );

    expect(html).toContain("Nenhuma página");
    expect(html).toContain('aria-label="Página anterior" data-slot="button" data-variant="secondary" disabled=""');
    expect(html).toContain('aria-label="Próxima página" data-slot="button" data-variant="secondary" disabled=""');
  });

  it("mantém a posição exibida dentro do novo total após redução de páginas", () => {
    const html = renderToStaticMarkup(
      <Pagination currentPage={8} totalPages={2} onPageChange={() => undefined} />,
    );

    expect(html).toContain("Página 2 de 2");
    expect(html).toContain('aria-label="Próxima página" data-slot="button" data-variant="secondary" disabled=""');
  });
});
