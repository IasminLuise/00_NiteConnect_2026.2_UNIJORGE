"""Independent PDF raster comparison; reports every differing pixel at 144 dpi.

PyMuPDF is optional for installing the package, but without it the campaign is
incomplete. No tolerance or ignored region is used for approval.
"""
from __future__ import annotations


def compare_pdfs(origin, candidate, *, dpi=144, edit_region=None):
    try:
        import fitz
    except ImportError as exc:
        raise RuntimeError("PyMuPDF é necessário para comparação PDF (pip install pymupdf)") from exc
    if not 72 <= dpi <= 300:
        raise ValueError("DPI fora da faixa permitida 72..300")
    with fitz.open(origin) as first, fitz.open(candidate) as second:
        counts = [len(first), len(second)]
        results = []
        count = max(counts)
        for idx in range(count):
            if idx >= counts[0] or idx >= counts[1]:
                results.append({"page": idx + 1, "error": "PAGE_MISSING", "different_pixels": None})
                continue
            a, b = first[idx], second[idx]
            page_sizes = [[round(a.rect.width, 4), round(a.rect.height, 4)],
                          [round(b.rect.width, 4), round(b.rect.height, 4)]]
            if page_sizes[0] != page_sizes[1]:
                results.append({"page": idx + 1, "error": "PAGE_SIZE_CHANGED",
                                "page_sizes_pt": page_sizes, "different_pixels": None})
                continue
            zoom = dpi / 72
            pix_a = a.get_pixmap(matrix=fitz.Matrix(zoom, zoom), alpha=False, colorspace=fitz.csRGB)
            pix_b = b.get_pixmap(matrix=fitz.Matrix(zoom, zoom), alpha=False, colorspace=fitz.csRGB)
            if pix_a.width != pix_b.width or pix_a.height != pix_b.height:
                results.append({"page": idx + 1, "error": "RASTER_DIMENSIONS_CHANGED",
                                "different_pixels": None})
                continue
            width, height = pix_a.width, pix_a.height
            channels = pix_a.n
            av, bv = memoryview(pix_a.samples), memoryview(pix_b.samples)
            bbox = [width, height, -1, -1]
            different = outside = 0
            region = None
            if edit_region and edit_region.get("page") == idx + 1:
                x0, y0, x1, y1 = edit_region["bbox_pt"]
                if not (0 <= x0 < x1 <= a.rect.width and 0 <= y0 < y1 <= a.rect.height):
                    raise ValueError("Retângulo da edição fora dos limites da página")
                region = (x0 * zoom, y0 * zoom, x1 * zoom, y1 * zoom)
            for y in range(height):
                line = y * width * channels
                if av[line:line + width * channels] == bv[line:line + width * channels]:
                    continue
                for x in range(width):
                    offset = line + x * channels
                    if av[offset:offset + channels] == bv[offset:offset + channels]:
                        continue
                    different += 1
                    bbox[0] = min(bbox[0], x)
                    bbox[1] = min(bbox[1], y)
                    bbox[2] = max(bbox[2], x)
                    bbox[3] = max(bbox[3], y)
                    if region is None or not (region[0] <= x < region[2] and region[1] <= y < region[3]):
                        outside += 1
            results.append({"page": idx + 1, "size_pt": page_sizes[0],
                            "dimensions_px": [width, height], "different_pixels": different,
                            "different_pixels_outside_edit_region": outside,
                            "difference_bbox_px": bbox if different else None,
                            "edit_region_defined": region is not None})
        return {"pages": counts, "dpi": dpi, "per_page": results,
                "total_different_pixels": sum(p["different_pixels"] or 0 for p in results),
                "raster_comparable": all("error" not in p for p in results)}
