"""Entirely fictional, deterministic document and PDF fixtures for test code."""
from __future__ import annotations

import json
import zipfile
from pathlib import Path

from b05_decision.evidence import sha256

NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"


def docx(path: Path, marker: str = "MARCADOR_ORIGINAL", *, rich: bool = False,
         page_clear: bool = False, footer_page: str = "2", core_time: str = "t0",
         extra: dict | None = None) -> None:
    escaped_marker = marker
    runprops = '<w:rPr><w:rFonts w:ascii="Arial"/><w:b/><w:color w:val="fcfcfc"/><w:sz w:val="20"/></w:rPr>' if rich else ''
    page = '<w:br w:type="page" w:clear="all"/>' if page_clear else '<w:br w:type="page"/>'
    body = (f'<w:document xmlns:w="{NS}"><w:body><w:p><w:r>{runprops}<w:t>{escaped_marker}</w:t>'
            f'</w:r></w:p><w:p><w:r>{page}</w:r></w:p></w:body></w:document>')
    footer = (f'<w:ftr xmlns:w="{NS}"><w:p><w:r><w:fldChar w:fldCharType="begin"/></w:r>'
              '<w:r><w:instrText xml:space="preserve"> PAGE </w:instrText></w:r>'
              f'<w:r><w:fldChar w:fldCharType="separate"/></w:r><w:r><w:t>{footer_page}</w:t></w:r>'
              '<w:r><w:fldChar w:fldCharType="end"/></w:r></w:p></w:ftr>')
    parts = {"[Content_Types].xml": '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"/>',
             "word/document.xml": body, "word/footer1.xml": footer,
             "docProps/core.xml": f'<core timestamp="{core_time}"/>',
             "word/styles.xml": f'<w:styles xmlns:w="{NS}"><w:style w:styleId="Normal"/></w:styles>'}
    if extra:
        parts.update(extra)
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for k, v in parts.items():
            zf.writestr(k, v)


def pdf(path: Path, content: str = "Base", extra: bool = False) -> None:
    import fitz
    d = fitz.open()
    p = d.new_page(width=612, height=792)
    p.insert_text((70, 80), content)
    if extra:
        p.insert_text((70, 110), "Delta")
    d.new_page(width=612, height=792)
    d.save(path)
    d.close()


def case_files(folder: Path, mode: str, *, add_word: bool = True) -> dict:
    before, after = "MARCADOR_ORIGINAL", "MARCADOR_EDITADO"
    basename = folder / mode
    basename.mkdir(parents=True)
    src, sdk, callback = [basename / (x + ".docx") for x in ("src", "sdk", "callback")]
    docx(src)
    docx(sdk, before if mode == "no_edit" else after,
         rich=mode == "rich_paste", page_clear=True, core_time="t1")
    if mode != "no_edit":
        docx(callback, after, rich=mode == "rich_paste", page_clear=True,
             footer_page="1", core_time="t2")
    pdf0, pdf1, pdf2, pdfrepeat = [basename / (x + ".pdf") for x in ("source", "sdk", "callback", "repeat")]
    pdf(pdf0)
    pdf(pdf1, extra=True)
    if mode != "no_edit":
        pdf(pdf2, extra=True)
    if mode == "no_edit":
        pdf(pdfrepeat)
    log = basename / "operator.json"
    event = {"no_edit": "closed_without_changes", "typed": "typed_text",
             "plain_paste": "paste_plain", "rich_paste": "paste_rich"}[mode]
    log.write_text(json.dumps({"mode": mode, "session": mode, "editor_build": "10.0-test",
                               "strength": "observed", "events": [{"event": event}]}), encoding="utf-8")
    def record(path):
        return {"path": str(path.relative_to(folder)), "sha256": sha256(path)}
    result = {"id": mode, "mode": mode,
              "callback_status": "not_created" if mode == "no_edit" else "persisted",
              "expected_edit": {"before": before, "after": after} if mode != "no_edit" else None,
              "source_docx": record(src), "sdk_docx": record(sdk),
              "source_pdf": record(pdf0), "sdk_pdf": record(pdf1),
              "operator_log": record(log),
              "probes": [{"label": "probe-safe", "page": 1, "bbox_pt": [50, 60, 150, 130]}]}
    if mode != "no_edit":
        result["callback_docx"] = record(callback)
        result["callback_pdf"] = record(pdf2)
    else:
        result["source_pdf_repeat"] = record(pdfrepeat)
    if add_word:
        word = basename / "word.json"
        field = {"name": "PAGE", "instruction_sha256": "a" * 64, "result_sha256": "b" * 64}
        unit = {"source_unmodified": True, "layout_changed_during_pdf": False,
                "page_count_before": 2, "page_count_after": 2,
                "fields_before": [field], "fields_after": [field]}
        word.write_text(json.dumps({"source": unit, "sdk": unit}), encoding="utf-8")
        result["word_inventory"] = record(word)
    return result


def make_campaign(folder: Path, *, with_pdfs: bool = True, with_word: bool = True) -> Path:
    folder.mkdir(parents=True, exist_ok=True)
    modes = ("no_edit", "typed", "plain_paste", "rich_paste")
    cases = [case_files(folder, mode, add_word=with_word) for mode in modes]
    if not with_pdfs:
        for trial in cases:
            for key in ("source_pdf", "sdk_pdf", "callback_pdf", "source_pdf_repeat"):
                trial.pop(key, None)
    data = {"schema_version": 1, "campaign_id": "fictional-01", "corpus_kind": "synthetic",
            "environment": {"onlyoffice_build": "10-test", "word_build": "16-test",
                            "export_method": "SaveAs2", "dpi": 144},
            "cases": cases}
    manifest = folder / "manifest.json"
    manifest.write_text(json.dumps(data), encoding="utf-8")
    return manifest
