from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from b05_decision.analysis import analyze, markdown, write
from b05_decision.pdf import glyph_probes, raster_compare
from b05_decision.evidence import sha256
from tests.support import docx, make_campaign, pdf


class AnalysisTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.path = Path(self.tmp.name)
        self.manifest = make_campaign(self.path)

    def mutate(self, fn):
        obj = json.loads(self.manifest.read_text())
        fn(obj)
        self.manifest.write_text(json.dumps(obj))

    def test_all_four_modes_one_execution_two_answers(self):
        result = analyze(self.manifest)
        self.assertEqual(len(result["cases"]), 4)
        self.assertEqual(result["m2p4"]["scope"], "M2P4_ROUNDTRIP_FACTUAL_DIAGNOSIS")
        self.assertEqual(result["m2p5"]["scope"], "M2P5_ENGINE_ARCHITECTURE_DECISION")
        self.assertFalse(result["canPromote"])
        self.assertFalse(result["officialApproved"])
        self.assertFalse(result["m2p5"]["definitive_engine_approved"])
        self.assertEqual(result["blocked_gates"], ["G1", "G2", "M3"])

    def test_no_edit_detects_nonzero_pixels(self):
        result = analyze(self.manifest)
        no_edit = result["m2p4"]["baseline_no_edit"][0]
        self.assertGreater(no_edit["source_to_sdk_raster"]["total_different_pixels"], 0)
        self.assertEqual(no_edit["source_repeat_raster"]["total_different_pixels"], 0)

    def test_callback_sdk_stored_page_result_differs(self):
        result = analyze(self.manifest)
        for case in result["cases"][1:]:
            comparison = next(x for x in case["pairs"] if x["pair"] == "SDK_TO_CALLBACK")
            self.assertEqual(comparison["docx"]["field_analysis"]["stored_result_changes"], 1)
            self.assertIn("docProps/core.xml", comparison["docx"]["opc_modified"])

    def test_m2p5_does_not_approve_clean_synthetic(self):
        for trial in json.loads(self.manifest.read_text())["cases"]:
            source = self.path / trial["source_docx"]["path"]
            sdk = self.path / trial["sdk_docx"]["path"]
            if trial["mode"] == "no_edit":
                sdk.write_bytes(source.read_bytes())
                self.mutate(lambda m, trial=trial: next(c for c in m["cases"] if c["id"] == trial["id"])["sdk_docx"].update(sha256=sha256(sdk)))
        result = analyze(self.manifest)
        self.assertNotEqual(result["m2p5"]["recommendation"], "GO")
        self.assertFalse(result["m2p5"]["backend_final_integration_authorized"])

    def test_unexpected_visible_text_fails_decision(self):
        sdk = self.path / "typed" / "sdk.docx"
        docx(sdk, marker="unexpected")
        self.mutate(lambda m: m["cases"][1]["sdk_docx"].update(sha256=sha256(sdk)))
        report = analyze(self.manifest)
        self.assertIn("VISIBLE_BODY_TEXT", report["m2p5"]["failures"])
        self.assertEqual(report["m2p5"]["recommendation"], "NO_GO_FOR_DEFINITIVE_ADOPTION")

    def test_missing_pdfs_conservative(self):
        manifest = make_campaign(self.path / "second", with_pdfs=False)
        report = analyze(manifest)
        self.assertIn("VISUAL_FIDELITY", report["m2p5"]["unproven"])
        self.assertFalse(report["canPromote"])

    def test_missing_word_conservative(self):
        manifest = make_campaign(self.path / "second", with_word=False)
        report = analyze(manifest)
        self.assertIn("WORD_PRE_POST_EXPORT", report["m2p5"]["unproven"])

    def test_word_probe_reports_modification_fails(self):
        word = self.path / "typed" / "word.json"
        obj = json.loads(word.read_text())
        obj["source"]["layout_changed_during_pdf"] = True
        word.write_text(json.dumps(obj))
        self.mutate(lambda m: m["cases"][1]["word_inventory"].update(sha256=sha256(word)))
        report = analyze(self.manifest)
        self.assertIn("WORD_PRE_POST_EXPORT", report["m2p5"]["failures"])

    def test_reports_do_not_leak_path_hash_or_doc_text(self):
        report = analyze(self.manifest)
        out = self.path / "out"
        j, m = write(report, out)
        dump = j.read_text() + m.read_text()
        self.assertNotIn(str(self.path), dump)
        self.assertNotIn("MARCADOR_ORIGINAL", dump)
        self.assertNotIn("MARCADOR_EDITADO", dump)
        self.assertNotIn("a" * 64, dump)
        self.assertIn("HOLD_QUARANTINE", dump)
        self.assertIn("M2P5", markdown(report))

    def test_report_cannot_overwrite(self):
        report = analyze(self.manifest)
        write(report, self.path / "out")
        with self.assertRaises(FileExistsError):
            write(report, self.path / "out")

    def test_probe_outside_page_rejected(self):
        a = self.path / "a.pdf"
        b = self.path / "b.pdf"
        pdf(a)
        pdf(b)
        with self.assertRaisesRegex(ValueError, "fora da página"):
            glyph_probes(a, b, [{"label": "x", "page": 1, "bbox_pt": [0, 0, 900, 900]}])

    def test_glyph_probe_identical(self):
        a = self.path / "a.pdf"
        b = self.path / "b.pdf"
        pdf(a)
        pdf(b)
        p = glyph_probes(a, b, [{"label": "safe", "page": 1, "bbox_pt": [50, 60, 150, 130]}])[0]
        self.assertTrue(p["comparable"])
        self.assertEqual(p["max_dx_pt"], 0)
        self.assertFalse(p["font_or_color_changed"])

    def test_identical_pdf(self):
        a = self.path / "a.pdf"
        b = self.path / "b.pdf"
        pdf(a)
        pdf(b)
        self.assertEqual(raster_compare(a, b)["total_different_pixels"], 0)

    def test_pdf_difference(self):
        a = self.path / "a.pdf"
        b = self.path / "b.pdf"
        pdf(a)
        pdf(b, extra=True)
        out = raster_compare(a, b)
        self.assertTrue(out["comparable"])
        self.assertGreater(out["total_different_pixels"], 0)

    def test_pdf_page_mismatch(self):
        import fitz
        a = self.path / "a.pdf"
        b = self.path / "b.pdf"
        pdf(a)
        d = fitz.open()
        d.new_page()
        d.save(b)
        d.close()
        out = raster_compare(a, b)
        self.assertFalse(out["comparable"])
        self.assertEqual(out["per_page"][1]["issue"], "MISSING_PAGE")


if __name__ == "__main__":
    unittest.main()
