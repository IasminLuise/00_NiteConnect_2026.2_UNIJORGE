import { Button } from "../ui";
import { cx } from "../ui/cx";

type PaginationProps = {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
};

function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  className,
}: PaginationProps) {
  const pageCount = Number.isFinite(totalPages) ? Math.max(0, Math.floor(totalPages)) : 0;
  const requestedPage = Number.isFinite(currentPage) ? Math.floor(currentPage) : 1;
  const page = pageCount === 0 ? 0 : Math.min(Math.max(requestedPage, 1), pageCount);
  const hasPages = pageCount > 0;

  return (
    <nav
      data-slot="pagination"
      aria-label="Paginação"
      className={cx("flex min-w-0 flex-wrap items-center justify-between gap-3", className)}
    >
      <Button
        type="button"
        variant="secondary"
        aria-label="Página anterior"
        disabled={!hasPages || page <= 1}
        onClick={() => onPageChange(page - 1)}
      >
        Anterior
      </Button>
      <span className="min-w-0 text-center text-type-body-compact text-nite-text-secondary" aria-live="polite" aria-atomic="true">
        {hasPages ? (
          <>
            <span className="sr-only">Página {page} de {pageCount}</span>
            <span className="sm:hidden" aria-hidden="true">{page}/{pageCount}</span>
            <span className="hidden sm:inline" aria-hidden="true">Página {page} de {pageCount}</span>
          </>
        ) : (
          <>
            <span className="sr-only">Nenhuma página</span>
            <span className="sm:hidden" aria-hidden="true">0/0</span>
            <span className="hidden sm:inline" aria-hidden="true">Nenhuma página</span>
          </>
        )}
      </span>
      <Button
        type="button"
        variant="secondary"
        aria-label="Próxima página"
        disabled={!hasPages || page >= pageCount}
        onClick={() => onPageChange(page + 1)}
      >
        Próxima
      </Button>
    </nav>
  );
}

export { Pagination };
export type { PaginationProps };
