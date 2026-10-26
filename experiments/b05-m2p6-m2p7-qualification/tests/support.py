"""Fully fictitious DOCX/manifest builders; no real documents or credentials."""
from __future__ import annotations

import hashlib
import json
import zipfile
from pathlib import Path

W_URI = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
R_URI = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
PKG_URI = "http://schemas.openxmlformats.org/package/2006/relationships"
CT_URI = "http://schemas.openxmlformats.org/package/2006/content-types"


def write_docx(path: Path, *, text="TESTE FICTICIO", font="Cambria", style="Normal", note=True,
               page_break=False, bad_ref=False, missing_target=False, extra_media=False,
               missing_content_type=False, field="PAGE", table=True, bad_stored_note=False,
               untrusted_part=False) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    breaks = '<w:br w:type="page" />' if page_break else ""
    tabs = '<w:tbl><w:tr><w:tc><w:p><w:r><w:t>celula</w:t></w:r></w:p></w:tc></w:tr></w:tbl>' if table else ""
    reference = '<w:footnoteReference w:id="1" />' if note else ""
    rel_ref = '<w:hyperlink r:id="rId99"><w:r><w:t>link</w:t></w:r></w:hyperlink>' if bad_ref else ""
    document = (f'<w:document xmlns:w="{W_URI}" xmlns:r="{R_URI}"><w:body>'
                f'<w:p><w:pPr><w:pStyle w:val="{style}" /></w:pPr><w:r><w:rPr>'
                f'<w:rFonts w:ascii="{font}"/></w:rPr><w:t>{text}</w:t>{breaks}{reference}</w:r>'
                f'<w:r><w:fldChar w:fldCharType="begin"/></w:r>'
                f'<w:r><w:instrText>{field}</w:instrText></w:r>'
                f'<w:r><w:fldChar w:fldCharType="end"/></w:r></w:p>'
                f'{tabs}{rel_ref}<w:sectPr/></w:body></w:document>')
    core = (f'<Types xmlns="{CT_URI}"><Default Extension="xml" ContentType="application/xml"/>'
            f'<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
            f'{"" if missing_content_type else "<Default Extension=\"png\" ContentType=\"image/png\"/>"}'
            '</Types>')
    rels = (f'<Relationships xmlns="{PKG_URI}">'
            f'<Relationship Id="rId1" Type="image" Target="media/{"missing.png" if missing_target else "sample.png"}" />'
            '</Relationships>')
    styles = (f'<w:styles xmlns:w="{W_URI}"><w:style w:styleId="{style}">'
              f'<w:name w:val="{style}" /></w:style></w:styles>')
    foot = (f'<w:footnotes xmlns:w="{W_URI}">'
            '<w:footnote w:id="1"><w:p><w:r><w:t>'
            f'{"OUTRO" if bad_stored_note else "NOTA FICTICIA"}'
            '</w:t></w:r></w:p></w:footnote></w:footnotes>')
    files = {"[Content_Types].xml": core, "word/document.xml": document,
             "word/styles.xml": styles, "word/_rels/document.xml.rels": rels,
             "word/media/sample.png": b"fictitious-png", "word/footnotes.xml": foot}
    if extra_media:
        files["word/media/extra.png"] = b"extra"
    if untrusted_part:
        files["../escape.xml"] = "evil"
    with zipfile.ZipFile(path, "w") as z:
        for n, blob in files.items():
            z.writestr(n, blob)
    return path


def entry(path: Path, root: Path) -> dict:
    return {"path": path.relative_to(root).as_posix(),
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}


def make_campaign(root: Path, *, document_origin="synthetic", changed_font=False,
                  bad_ref=False, malformed_callback=False, previous_fail=False,
                  complete=False, callback=True) -> Path:
    root.mkdir(parents=True, exist_ok=True)
    previous = {"schema_version": 1, "campaign_id": "PREVIOUS-SYNTHETIC-01",
                "status": "HOLD_QUARANTINE", "canPromote": False, "officialApproved": False,
                "blocked_gates": ["G1", "G2", "M3"],
                "m2p4": {"baseline_no_edit": [{"id": "FAKE-CTRL-01",
                         "source_to_sdk_raster": {"total_different_pixels": 13},
                         "source_repeat_raster": {"total_different_pixels": 0}}]},
                "m2p5": {"failures": ["FIELD_INSTRUCTIONS"] if previous_fail else [],
                          "unproven": ["VISUAL_FIDELITY", "NOTES_SEMANTICS"],
                          "recommendation": "HOLD_DECISION_NOT_ENOUGH_EVIDENCE",
                          "definitive_engine_approved": False,
                          "backend_final_integration_authorized": False,
                          "advisory_only": True}}
    (root / "old.json").write_text(json.dumps(previous), encoding="utf-8")
    source = write_docx(root / "docs" / "source.docx", page_break=True)
    sdk = write_docx(root / "docs" / "sdk.docx", page_break=True,
                     font="Arial" if changed_font else "Cambria", bad_ref=bad_ref)
    doc = {"id": "FIX-001", "origin": document_origin, "operation": "no_edit",
           "declared_features": ["tables", "footnotes", "page_breaks", "fields"],
           "source_docx": entry(source, root), "sdk_docx": entry(sdk, root)}
    if callback:
        cb = write_docx(root / "docs" / "callback.docx", page_break=True,
                        bad_ref=malformed_callback)
        doc["callback_docx"] = entry(cb, root)
    if document_origin == "official_controlled":
        (root / "authorization.txt").write_text("Authorized synthetic stand-in receipt", encoding="utf-8")
        doc["authorization_receipt"] = entry(root / "authorization.txt", root)
    checks = []
    from b05_qualification.evidence import CHECKS
    for code in sorted(CHECKS):
        fp = root / "receipts" / (code + ".txt")
        fp.parent.mkdir(exist_ok=True)
        fp.write_text("FICTITIOUS CLAIM RECEIPT", encoding="utf-8")
        checks.append({"id": code, "result": "PASS_ATTESTED" if complete else "UNVERIFIED",
                       "evidence": entry(fp, root) if complete else None})
    reviews = []
    for role in ("technical", "editorial", "institutional"):
        fp = root / "receipts" / (role + ".txt")
        fp.write_text("FICTITIOUS REVIEW RECEIPT", encoding="utf-8")
        reviews.append({"role": role, "decision": "ACCEPT_ATTESTED" if complete else "PENDING",
                        "evidence": entry(fp, root) if complete else None})
    adjudications = []
    for cid in previous["m2p5"]["unproven"]:
        fp = root / "receipts" / (cid + ".txt")
        fp.write_text("FICTITIOUS ADJUDICATION RECEIPT", encoding="utf-8")
        adjudications.append({"criterion": cid,
                              "status": "RESOLVED_ATTESTED" if complete else "PENDING",
                              "evidence": entry(fp, root) if complete else None})
    scope_file = root / "receipts" / "corpus-scope.txt"
    scope_file.write_text("FICTITIOUS CORPUS SCOPE RECEIPT", encoding="utf-8")
    docs = [doc]
    if complete:
        source_edit = write_docx(root / 'docs' / 'edit-source.docx', text='ALTERAR FICTICIO', page_break=True)
        sdk_edit = write_docx(root / 'docs' / 'edit-sdk.docx', text='ALTERADO FICTICIO', page_break=True)
        cb_edit = write_docx(root / 'docs' / 'edit-callback.docx', text='ALTERADO FICTICIO', page_break=True)
        changed = {"id": "FIX-002", "origin": document_origin, "operation": "edited",
                   "expected_edit": {"before": "ALTERAR FICTICIO", "after": "ALTERADO FICTICIO"},
                   "declared_features": ["tables", "fields"],
                   "source_docx": entry(source_edit, root), "sdk_docx": entry(sdk_edit, root),
                   "callback_docx": entry(cb_edit, root)}
        if document_origin == 'official_controlled':
            changed['authorization_receipt'] = entry(root / 'authorization.txt', root)
            # Distinct physical authorization receipt, even if its contents match.
            authorized2 = root / 'authorization2.txt'
            authorized2.write_text('Authorized second synthetic stand-in', encoding='utf-8')
            changed['authorization_receipt'] = entry(authorized2, root)
        review2 = root / 'receipts' / 'semantic-review-edited.txt'
        review2.write_text('FICTITIOUS SEMANTIC REVIEW', encoding='utf-8')
        changed['semantic_review_receipt'] = entry(review2, root)
        docs.append(changed)
    manifest = {"schema_version": 1, "campaign_id": "CAMPAIGN-SYNTHETIC-01",
                "corpus_kind": "official_controlled" if document_origin == "official_controlled" else "synthetic",
                "required_features": ["tables", "footnotes", "page_breaks", "fields"],
                "m2p5_report": entry(root / "old.json", root), "documents": docs,
                "integration_checks": checks, "reviews": reviews, "adjudications": adjudications,
                "corpus_scope_receipt": entry(scope_file, root) if complete else None}
    path = root / "manifest.json"
    path.write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    return path
