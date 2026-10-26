"""Local compatibility provenance; synthetic preparation is not engine acceptance."""
import hashlib
import json
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile
from lxml import etree as ET

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prepare_local import prepare

W = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}


class LocalPreparationTests(unittest.TestCase):
    def test_records_distinct_original_and_compatible_input_without_fake_outputs(self):
        with TemporaryDirectory() as temp:
            folder = Path(temp)/'.private'/'campaign'
            plan = prepare(folder)
            original = folder/plan['original']['path']; compatible = folder/plan['compatible']['path']
            self.assertNotEqual(original.read_bytes(), compatible.read_bytes())
            for key in ('original', 'compatible'):
                self.assertEqual(plan[key]['sha256'], hashlib.sha256((folder/plan[key]['path']).read_bytes()).hexdigest())
            self.assertEqual(3, plan['compatibility']['converted_fields'])
            self.assertEqual({'no_edit', 'edit_text', 'edit_table'}, {c['name'] for c in plan['cases']})
            for case in plan['cases']:
                self.assertEqual('PLANNED', case['status'])
                self.assertIsNone(case['engine_docx'])
                self.assertEqual(compatible.read_bytes(), (folder/case['source']['path']).read_bytes())
            self.assertFalse(plan['gate_modified'])
            self.assertFalse(plan['native_f9_fixed'])
            self.assertEqual(plan, json.loads((folder/'plan-private.json').read_text()))

    def test_freezes_original_simple_fields_and_only_converts_compatible_copy(self):
        with TemporaryDirectory() as temp:
            folder = Path(temp)/'.private'/'campaign'; plan = prepare(folder)
            for key, simple in [('original', 3), ('compatible', 0)]:
                count = 0
                with ZipFile(folder/plan[key]['path']) as z:
                    for name in z.namelist():
                        if name.startswith('word/header') and name.endswith('.xml'):
                            count += len(ET.fromstring(z.read(name)).findall('.//w:fldSimple', W))
                self.assertEqual(simple, count)

    def test_edits_are_unique_and_do_not_touch_field_instructions(self):
        with TemporaryDirectory() as temp:
            folder = Path(temp)/'.private'/'campaign'; plan = prepare(folder)
            with ZipFile(folder/plan['compatible']['path']) as z:
                root = ET.fromstring(z.read('word/document.xml'))
            text = ''.join(root.xpath('//w:t/text()', namespaces=W))
            for case in plan['cases']:
                if case['expected_edit']:
                    self.assertEqual(1, text.count(case['expected_edit']['before']))
                    self.assertNotIn('SAVEDATE', case['expected_edit']['before'])

    def test_refuses_overwrite_without_changing_existing_evidence(self):
        with TemporaryDirectory() as temp:
            folder = Path(temp)/'.private'/'campaign'; prepare(folder)
            snapshot = (folder/'plan-private.json').read_bytes()
            with self.assertRaises(FileExistsError): prepare(folder)
            self.assertEqual(snapshot, (folder/'plan-private.json').read_bytes())

    def test_rejects_public_output_before_creating_files(self):
        with TemporaryDirectory() as temp:
            folder = Path(temp)/'public-campaign'
            with self.assertRaises(ValueError): prepare(folder)
            self.assertFalse(folder.exists())


if __name__ == '__main__': unittest.main()
