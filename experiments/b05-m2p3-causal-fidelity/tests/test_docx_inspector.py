import tempfile
import unittest
import zipfile
from pathlib import Path

from b05_m2p3.docx_inspector import inspect_docx, compare_docx
from tests.support import make_document


class DocxTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.a = self.root / 'a.docx'
        self.b = self.root / 'b.docx'

    def inspect(self, *, source=None, candidate=None, expected=None):
        make_document(self.a, **(source or {}))
        make_document(self.b, **(candidate or {}))
        return compare_docx(inspect_docx(self.a), inspect_docx(self.b), expected)

    def codes(self, result):
        return [item['code'] for item in result['issues']]

    def test_identical(self):
        result = self.inspect()
        self.assertEqual([], result['issues'])
        self.assertTrue(result['expected_text_only_edit_confirmed'])

    def test_expected_edit_only(self):
        result = self.inspect(candidate={'text': 'Novo'}, expected={'before': 'Antigo', 'after': 'Novo'})
        self.assertTrue(result['expected_text_only_edit_confirmed'])
        self.assertNotIn('UNEXPECTED_VISIBLE_TEXT_CHANGE', self.codes(result))

    def test_unexpected_edit_blocked(self):
        result = self.inspect(candidate={'text': 'Outro'}, expected={'before': 'Antigo', 'after': 'Novo'})
        self.assertIn('UNEXPECTED_VISIBLE_TEXT_CHANGE', self.codes(result))

    def test_extra_text_under_no_edit(self):
        result = self.inspect(candidate={'text': 'Novo'})
        self.assertIn('UNEXPECTED_VISIBLE_TEXT_CHANGE', self.codes(result))

    def test_explicit_run_format(self):
        result = self.inspect(candidate={'font': 'Arial', 'bold': True, 'color': 'fcfcfc', 'size': '20', 'text': 'Novo'},
                              expected={'before': 'Antigo', 'after': 'Novo'})
        self.assertIn('RUN_FORMAT_CHANGED', self.codes(result))
        self.assertTrue(result['expected_text_only_edit_confirmed'])

    def test_page_break_clear_attribute(self):
        result = self.inspect(candidate={'br_clear': 'all'})
        self.assertIn('BREAK_ATTR_CHANGED', self.codes(result))
        self.assertTrue(result['expected_text_only_edit_confirmed'])

    def test_field_instruction_mutation(self):
        result = self.inspect(candidate={'field': 'PAGE \\* MERGEFORMAT'})
        self.assertIn('FIELD_INSTRUCTION_CHANGED', self.codes(result))
        self.assertEqual([1, 1], result['field_counts'])

    def test_field_difference_not_exposed_as_plain_content(self):
        result = self.inspect(candidate={'field': 'PAGE field-private-sentence'})
        self.assertNotIn('field-private-sentence', repr(result))
        self.assertIn('FIELD_INSTRUCTION_CHANGED', self.codes(result))
        self.assertNotIn('instruction_sha256', repr(result))

    def test_new_ooxml_part(self):
        result = self.inspect(candidate={'extra_parts': {'word/endnotes.xml': b'<w:endnotes xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"/>'}})
        self.assertIn('OOXML_PARTS_CHANGED', self.codes(result))

    def test_modified_media(self):
        result = self.inspect(candidate={'media': b'changed'})
        self.assertIn('MEDIA_CHANGED', self.codes(result))

    def test_duplicate_zip_members_denied(self):
        make_document(self.a)
        with zipfile.ZipFile(self.a, 'a') as zf:
            import warnings
            with warnings.catch_warnings():
                warnings.simplefilter('ignore', UserWarning)
                zf.writestr('word/document.xml', b'extra')
        with self.assertRaisesRegex(ValueError, 'duplicadas'):
            inspect_docx(self.a)

    def test_invalid_input_denied(self):
        self.a.write_bytes(b'not a docx')
        with self.assertRaises(ValueError):
            inspect_docx(self.a)

    def test_missing_body_denied(self):
        make_document(self.a)
        with zipfile.ZipFile(self.a, 'r') as zf:
            members = {p: zf.read(p) for p in zf.namelist()}
        members['word/document.xml'] = b'<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"/>'
        with zipfile.ZipFile(self.a, 'w') as zf:
            for name, data in members.items():
                zf.writestr(name, data)
        with self.assertRaisesRegex(ValueError, 'body'):
            inspect_docx(self.a)


if __name__ == '__main__':
    unittest.main()
