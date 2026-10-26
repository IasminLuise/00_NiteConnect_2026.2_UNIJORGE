"""Full raster comparison and targeted PDF glyph probes (read-only)."""
from __future__ import annotations


def _fitz():
    try:
        import fitz
    except ImportError as exc:
        raise RuntimeError("PyMuPDF necessário para PDFs: pip install pymupdf") from exc
    return fitz


def raster_compare(origin, candidate, *, dpi: int = 144) -> dict:
    fitz = _fitz()
    if type(dpi) is not int or not 72 <= dpi <= 300:
        raise ValueError("dpi inválido")
    pages = []
    with fitz.open(origin) as aa, fitz.open(candidate) as bb:
        n1, n2 = len(aa), len(bb)
        for index in range(max(n1, n2)):
            if index >= n1 or index >= n2:
                pages.append({"page": index + 1, "comparable": False, "issue": "MISSING_PAGE"})
                continue
            a, b = aa[index], bb[index]
            if tuple(a.rect) != tuple(b.rect):
                pages.append({"page": index + 1, "comparable": False, "issue": "PAGE_SIZE_CHANGED"})
                continue
            matrix = fitz.Matrix(dpi / 72, dpi / 72)
            x = a.get_pixmap(matrix=matrix, colorspace=fitz.csRGB, alpha=False)
            y = b.get_pixmap(matrix=matrix, colorspace=fitz.csRGB, alpha=False)
            if (x.width, x.height, x.n) != (y.width, y.height, y.n):
                pages.append({"page": index + 1, "comparable": False, "issue": "PIXMAP_SHAPE_CHANGED"})
                continue
            try:
                import numpy as np
                one = np.frombuffer(x.samples, dtype=np.uint8).reshape(x.height, x.width, x.n)
                two = np.frombuffer(y.samples, dtype=np.uint8).reshape(y.height, y.width, y.n)
                mask = np.any(one != two, axis=2)
                count = int(mask.sum())
                yy, xx = np.where(mask)
                bbox = [int(xx.min()), int(yy.min()), int(xx.max()), int(yy.max())] if count else None
            except ImportError:
                one, two = memoryview(x.samples), memoryview(y.samples)
                count, bbox = 0, None
                for row in range(x.height):
                    for col in range(x.width):
                        offset = (row * x.width + col) * x.n
                        if one[offset:offset + x.n] != two[offset:offset + x.n]:
                            count += 1
                            if bbox is None:
                                bbox = [col, row, col, row]
                            else:
                                bbox = [min(col, bbox[0]), min(row, bbox[1]),
                                        max(col, bbox[2]), max(row, bbox[3])]
            pages.append({"page": index + 1, "comparable": True,
                          "different_pixels": count, "bbox_px": bbox,
                          "dimensions_px": [x.width, x.height]})
    return {"dpi": dpi, "pages": [n1, n2], "per_page": pages,
            "comparable": bool(pages) and all(p["comparable"] for p in pages),
            "total_different_pixels": sum(p.get("different_pixels", 0) for p in pages)}


def _spans(page, rect):
    fitz = _fitz()
    output = []
    text = page.get_text("dict", clip=fitz.Rect(rect))
    for block in text["blocks"]:
        if block.get("type") != 0:
            continue
        for line in block["lines"]:
            for span in line["spans"]:
                output.append({"text": span.get("text", ""), "origin": span["origin"],
                               "font": span["font"], "size": span["size"],
                               "color": f"{span['color']:06x}"})
    return output


def glyph_probes(origin, candidate, probes: list[dict]) -> list[dict]:
    """Measure glyph origins in fixed regions; never elide raster differences."""
    fitz = _fitz()
    result = []
    with fitz.open(origin) as aa, fitz.open(candidate) as bb:
        for item in probes:
            if not isinstance(item, dict) or set(item) != {"label", "page", "bbox_pt"}:
                raise ValueError("Probe requer label/page/bbox_pt")
            label, index, bbox = item["label"], item["page"], item["bbox_pt"]
            if not isinstance(label, str) or not label or type(index) is not int or not isinstance(bbox, list) or len(bbox) != 4 or not all(isinstance(v, (int, float)) and not isinstance(v, bool) for v in bbox):
                raise ValueError("Probe inválido")
            if index < 1 or index > len(aa) or index > len(bb):
                result.append({"label": label, "page": index, "comparable": False,
                               "reason": "PAGE_MISSING"})
                continue
            ra = aa[index - 1].rect
            rb = bb[index - 1].rect
            if not (0 <= bbox[0] < bbox[2] <= min(ra.width, rb.width)
                    and 0 <= bbox[1] < bbox[3] <= min(ra.height, rb.height)):
                raise ValueError("Probe fora da página")
            a, b = _spans(aa[index - 1], bbox), _spans(bb[index - 1], bbox)
            if len(a) != len(b) or [s["text"] for s in a] != [s["text"] for s in b]:
                result.append({"label": label, "page": index, "comparable": False,
                               "reason": "SPAN_ALIGNMENT_UNKNOWN", "spans": [len(a), len(b)]})
                continue
            result.append({"label": label, "page": index, "comparable": True,
                           "span_count": len(a),
                           "max_dx_pt": round(max((abs(x["origin"][0] - y["origin"][0])
                                               for x, y in zip(a, b)), default=0), 6),
                           "max_dy_pt": round(max((abs(x["origin"][1] - y["origin"][1])
                                               for x, y in zip(a, b)), default=0), 6),
                           "font_or_color_changed": any((x["font"], x["size"], x["color"])
                                                        != (y["font"], y["size"], y["color"])
                                                        for x, y in zip(a, b)),
                           "fonts_before": sorted({x["font"] for x in a}),
                           "fonts_after": sorted({x["font"] for x in b}),
                           "colors_before": sorted({x["color"] for x in a}),
                           "colors_after": sorted({x["color"] for x in b})})
    return result
