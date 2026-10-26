from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from b05_decision.ooxml import compare, inspect_docx
from tests.support import docx


class OOXMLTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.path = Path(self.tmp.name)
        self.a, self.b = self.path / "a.docx", self.path / "b.docx"

    def read(self):
        return inspect_docx(self.a), inspect_docx(self.b)

    def test_identical_docx(self):
        docx(self.a)
        docx(self.b)
        c = compare(*self.read(), expected_edit=None)
        self.assertTrue(c["expected_visible_text_only"])
        self.assertEqual(c["opc_modified"], [])

    def test_expected_only_edit(self):
        docx(self.a)
        docx(self.b, marker="MARCADOR_EDITADO")
        self.assertTrue(compare(*self.read(), expected_edit={"before": "MARCADOR_ORIGINAL", "after": "MARCADOR_EDITADO"})["expected_visible_text_only"])

    def test_unexpected_text(self):
        docx(self.a)
        docx(self.b, marker="OTHER")
        self.assertFalse(compare(*self.read(), expected_edit=None)["expected_visible_text_only"])

    def test_rich_style_detected_as_part_change(self):
        docx(self.a)
        docx(self.b, rich=True)
        result = compare(*self.read(), expected_edit=None)
        self.assertIn("word/document.xml", result["opc_modified"])
        self.assertFalse(result["opc_differences_have_equivalence_proven"])

    def test_break_change(self):
        docx(self.a)
        docx(self.b, page_clear=True)
        self.assertTrue(compare(*self.read(), expected_edit=None)["break_attributes_changed"])

    def test_field_result_change_even_if_instruction_same(self):
        docx(self.a, footer_page="2")
        docx(self.b, footer_page="1")
        fields = compare(*self.read(), expected_edit=None)["field_analysis"]
        self.assertTrue(fields["field_types_same"])
        self.assertEqual(fields["instruction_changes"], 0)
        self.assertEqual(fields["stored_result_changes"], 1)

    def test_core_metadata_change(self):
        docx(self.a)
        docx(self.b, core_time="new")
        self.assertEqual(compare(*self.read(), expected_edit=None)["opc_modified"], ["docProps/core.xml"])

    def test_media_difference(self):
        docx(self.a, extra={"word/media/x.png": "A"})
        docx(self.b, extra={"word/media/x.png": "B"})
        self.assertFalse(compare(*self.read(), expected_edit=None)["media_unchanged"])

    def test_added_part(self):
        docx(self.a)
        docx(self.b, extra={"docProps/custom.xml": "<root/>"})
        self.assertEqual(compare(*self.read(), expected_edit=None)["opc_added"], ["docProps/custom.xml"])

    def test_bad_zip(self):
        self.a.write_text("not a zip")
        with self.assertRaises(ValueError):
            inspect_docx(self.a)


if __name__ == "__main__":
    unittest.main()
