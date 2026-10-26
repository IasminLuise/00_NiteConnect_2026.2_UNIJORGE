import tempfile
import unittest
import zipfile
from pathlib import Path

from b05_qualification.semantic import inspect_docx, compare_docs, _destination
from tests.support import write_docx


class Semantics(unittest.TestCase):
    def setUp(self):
        self.d = tempfile.TemporaryDirectory()
        self.root = Path(self.d.name)
        self.src = inspect_docx(write_docx(self.root / "src.docx", page_break=True))

    def tearDown(self):
        self.d.cleanup()

    def candidate(self, **kw):
        return inspect_docx(write_docx(self.root / "other.docx", **kw))

    def test_source_fully_resolved(self):
        self.assertEqual(self.src['opc']['broken_relations'], 0)
        self.assertEqual(self.src['opc']['missing_xml_references'], 0)
        self.assertEqual(self.src['opc']['missing_content_types'], 0)

    def test_detects_features_from_xml(self):
        self.assertTrue({'tables', 'footnotes', 'page_breaks', 'fields', 'styles', 'images'} <= set(self.src['features']))

    def test_same_explicit_format(self):
        other = self.candidate(page_break=True)
        actual = compare_docs(self.src, other)
        self.assertEqual(actual['explicit_format_mismatch_chars_on_matched_text'], 0)
        self.assertTrue(actual['style_equivalence_proven'])

    def test_explicit_format_change_same_text(self):
        other = self.candidate(page_break=True, font='Arial')
        actual = compare_docs(self.src, other)
        self.assertGreater(actual['explicit_format_mismatch_chars_on_matched_text'], 0)
        self.assertFalse(actual['effective_style_equivalence_proven'])

    def test_paragraph_replacement_counts_matched_text_only(self):
        other = self.candidate(page_break=True, text='TEXTO ALTERADO')
        result = compare_docs(self.src, other)
        self.assertGreaterEqual(result['matching_text_characters_compared'], 0)
        self.assertEqual(result['explicit_format_mismatch_chars_on_matched_text'], 0)

    def test_unresolved_internal_relationship(self):
        other = self.candidate(page_break=True, missing_target=True)
        self.assertEqual(compare_docs(self.src, other)['candidate_new_broken_relationships'], 1)

    def test_missing_r_id(self):
        other = self.candidate(page_break=True, bad_ref=True)
        self.assertEqual(compare_docs(self.src, other)['candidate_new_broken_relationships'], 1)

    def test_missing_content_type(self):
        other = self.candidate(page_break=True, missing_content_type=True)
        self.assertEqual(compare_docs(self.src, other)['candidate_new_missing_content_types'], 1)

    def test_note_text_changed_not_equivalent(self):
        other = self.candidate(page_break=True, bad_stored_note=True)
        actual = compare_docs(self.src, other)
        self.assertFalse(actual['notes_referenced_text_signatures_equal'])
        self.assertFalse(actual['notes_semantic_equivalence_proven'])

    def test_note_text_same_does_not_certify_semantics(self):
        other = self.candidate(page_break=True)
        self.assertFalse(compare_docs(self.src, other)['notes_semantic_equivalence_proven'])

    def test_field_instruction_changed(self):
        other = self.candidate(page_break=True, field='PAGENUM')
        self.assertFalse(compare_docs(self.src, other)['field_instructions_equal_in_order'])

    def test_style_id_renamed_not_normalized(self):
        other = self.candidate(page_break=True, style='ChangedStyle')
        actual = compare_docs(self.src, other)
        self.assertEqual(actual['styles_ids_removed'], 1)
        self.assertEqual(actual['styles_ids_added'], 1)
        self.assertFalse(actual['style_equivalence_proven'])

    def test_zip_traversal_rejected(self):
        bad = write_docx(self.root / 'evil.docx', untrusted_part=True)
        with self.assertRaises(ValueError):
            inspect_docx(bad)

    def test_duplicate_parts_rejected(self):
        file = write_docx(self.root / 'dup.docx')
        import warnings
        with warnings.catch_warnings():
            warnings.simplefilter('ignore')
            with zipfile.ZipFile(file, 'a') as z:
                z.writestr('word/document.xml', 'duplicate')
        with self.assertRaises(ValueError):
            inspect_docx(file)

    def test_missing_docx_required_part(self):
        file = self.root / 'missing.docx'
        with zipfile.ZipFile(file, 'w') as z:
            z.writestr('word/document.xml', 'a')
        with self.assertRaises(ValueError):
            inspect_docx(file)

    def test_relpath_normalization(self):
        self.assertEqual(_destination('word/document.xml', 'media/pic.png'), 'word/media/pic.png')
        self.assertIsNone(_destination('word/document.xml', '../../outside.xml'))

    def test_external_links_are_not_accessed(self):
        from b05_qualification.semantic import _opc
        pkg = {'[Content_Types].xml': ('<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
                                         '<Default Extension="rels" ContentType="relationships"/></Types>').encode(),
               '_rels/.rels': ('<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
                              '<Relationship Id="rId1" TargetMode="External" Target="https://example.invalid"/></Relationships>').encode()}
        out = _opc(pkg)
        self.assertEqual(out['external_relations_declared'], 1)
        self.assertEqual(out['broken_relations'], 0)


if __name__ == '__main__':
    unittest.main()
