from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from b05_decision.evidence import reverify, validate_manifest
from tests.support import make_campaign


class ValidationTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.path = Path(self.tmp.name)
        self.manifest = make_campaign(self.path)

    def change(self, method):
        data = json.loads(self.manifest.read_text())
        method(data)
        self.manifest.write_text(json.dumps(data))

    def test_valid_manifest(self):
        manifest, cases, files = validate_manifest(self.manifest)
        self.assertEqual(len(cases), 4)
        self.assertGreater(len(files), 25)
        reverify(files)

    def test_wrong_sha(self):
        self.change(lambda x: x["cases"][0]["sdk_docx"].update(sha256="a" * 64))
        with self.assertRaisesRegex(ValueError, "SHA-256 divergente"):
            validate_manifest(self.manifest)

    def test_path_escape(self):
        self.change(lambda x: x["cases"][0]["sdk_docx"].update(path="../../outside.docx"))
        with self.assertRaisesRegex(ValueError, "fora da campanha"):
            validate_manifest(self.manifest)

    def test_reused_evidence(self):
        self.change(lambda x: x["cases"][1].update(source_docx=x["cases"][0]["source_docx"]))
        with self.assertRaisesRegex(ValueError, "Reuso"):
            validate_manifest(self.manifest)

    def test_missing_mode(self):
        self.change(lambda x: x["cases"].pop())
        with self.assertRaises(ValueError):
            validate_manifest(self.manifest)

    def test_incorrect_callback_status(self):
        self.change(lambda x: x["cases"][1].update(callback_status="not_created"))
        with self.assertRaisesRegex(ValueError, "editado"):
            validate_manifest(self.manifest)

    def test_persisted_requires_candidate(self):
        self.change(lambda x: x["cases"][1].pop("callback_docx"))
        with self.assertRaisesRegex(ValueError, "Persistência"):
            validate_manifest(self.manifest)

    def test_word_has_sha_guard(self):
        self.change(lambda x: x["cases"][0]["word_inventory"].update(sha256="a" * 64))
        with self.assertRaises(ValueError):
            validate_manifest(self.manifest)

    def test_edited_requires_exact_expected(self):
        self.change(lambda x: x["cases"][1].update(expected_edit=None))
        with self.assertRaisesRegex(ValueError, "Edição exige"):
            validate_manifest(self.manifest)

    def test_mode_log_mismatch(self):
        f = self.path / "typed" / "operator.json"
        obj = json.loads(f.read_text())
        obj["mode"] = "rich_paste"
        f.write_text(json.dumps(obj))
        from b05_decision.evidence import sha256
        self.change(lambda x: x["cases"][1]["operator_log"].update(sha256=sha256(f)))
        with self.assertRaisesRegex(ValueError, "inconsistente"):
            validate_manifest(self.manifest)

    def test_input_mutated_after_validation(self):
        _, _, files = validate_manifest(self.manifest)
        files[0][0].write_bytes(b"tamper")
        with self.assertRaisesRegex(ValueError, "alterada durante"):
            reverify(files)

    def test_pdf_pair_required(self):
        self.change(lambda x: x["cases"][0].pop("sdk_pdf"))
        with self.assertRaisesRegex(ValueError, "formar um par"):
            validate_manifest(self.manifest)

    def test_dpi_reject_bool(self):
        self.change(lambda x: x["environment"].update(dpi=True))
        with self.assertRaisesRegex(ValueError, "dpi"):
            validate_manifest(self.manifest)


if __name__ == "__main__":
    unittest.main()
