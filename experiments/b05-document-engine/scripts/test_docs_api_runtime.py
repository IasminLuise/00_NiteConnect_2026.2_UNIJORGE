import json
import zipfile
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from adapter_cases import savedate_source
import prepare_docs_api


class DocsApiRuntimeTest(unittest.TestCase):
    def test_clean_checkout_generates_source_free_complex_fixture_and_refuses_reuse(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'fixtures/generated').mkdir(parents=True)
            (root / 'fixtures/generated/F01.docx').write_bytes(savedate_source())
            target = root / '.runtime/ci'
            with patch.object(prepare_docs_api, 'ROOT', root):
                prepare_docs_api.prepare(target)
                manifest = json.loads((target / 'private-manifest.json').read_text())
                self.assertEqual('source-free-ci-v1', manifest['complex_fixture_provenance'])
                with zipfile.ZipFile(target / 'fixtures/F02.docx') as package:
                    header = package.read('word/header1.xml')
                    self.assertNotIn(b'fldSimple', header)
                    self.assertIn(b'SAVEDATE', header)
                with self.assertRaises(FileExistsError):
                    prepare_docs_api.prepare(target)


if __name__ == '__main__':
    unittest.main()
