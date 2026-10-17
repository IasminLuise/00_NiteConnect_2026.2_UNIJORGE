import io
import tempfile
import unittest
from unittest.mock import patch
import zipfile
from pathlib import Path

import fidelity


def package(text='A B', field='SAVEDATE \\* MERGEFORMAT'):
    output = io.BytesIO()
    with zipfile.ZipFile(output, 'w') as archive:
        archive.writestr('word/document.xml', '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body><w:tbl><w:tr><w:tc><w:p><w:r><w:t>' + text + '</w:t></w:r></w:p></w:tc></w:tr></w:tbl></w:body></w:document>')
        archive.writestr('word/header1.xml', '<w:hdr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:p><w:fldSimple w:instr="' + field + '"><w:r><w:t>17/10/2026</w:t></w:r></w:fldSimple></w:p></w:hdr>')
    return output.getvalue()


class FidelityTests(unittest.TestCase):
    def test_spaces_and_cell_boundaries_are_not_discarded(self):
        self.assertNotEqual(fidelity.inventory(package('A B'))['body'], fidelity.inventory(package('AB'))['body'])

    def test_fields_in_simple_and_complex_forms_are_observed(self):
        original = fidelity.inventory(package())
        self.assertIn('SAVEDATE', original['fields'][0]['instruction'])
        self.assertEqual('word/header1.xml', original['fields'][0]['part'])

    def test_complex_instruction_fragments_are_joined_within_field_boundaries(self):
        source = package()
        with zipfile.ZipFile(io.BytesIO(source)) as z:
            parts = {n: z.read(n) for n in z.namelist()}
        parts['word/header1.xml'] = b'<w:hdr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:p><w:r><w:fldChar w:fldCharType="begin"/></w:r><w:r><w:instrText>SAVE</w:instrText></w:r><w:r><w:instrText>DATE</w:instrText></w:r><w:r><w:fldChar w:fldCharType="separate"/></w:r><w:r><w:t>cached</w:t></w:r><w:r><w:fldChar w:fldCharType="end"/></w:r></w:p></w:hdr>'
        output = io.BytesIO()
        with zipfile.ZipFile(output, 'w') as z:
            for n, b in parts.items():
                z.writestr(n, b)
        self.assertEqual(['SAVEDATE'], [f['instruction'] for f in fidelity.inventory(output.getvalue())['fields']])

    def test_run_fragmentation_does_not_change_paragraph_text(self):
        source = package()
        with zipfile.ZipFile(io.BytesIO(source)) as z:
            parts = {n: z.read(n) for n in z.namelist()}
        parts['word/document.xml'] = parts['word/document.xml'].replace(b'<w:t>A B</w:t>', b'<w:t>A </w:t></w:r><w:r><w:t>B</w:t>')
        output = io.BytesIO()
        with zipfile.ZipFile(output, 'w') as z:
            for n, b in parts.items():
                z.writestr(n, b)
        self.assertEqual(fidelity.inventory(source)['body'], fidelity.inventory(output.getvalue())['body'])

    def test_external_entity_is_rejected(self):
        output = io.BytesIO()
        with zipfile.ZipFile(output, 'w') as z:
            z.writestr('word/document.xml', '<!DOCTYPE x [<!ENTITY e SYSTEM "file:///private">]><x/>')
        with self.assertRaises(ValueError):
            fidelity.inventory(output.getvalue())

    def test_stale_version_is_not_accepted_as_saved(self):
        with self.assertRaises(ValueError):
            fidelity.validate_capture('same', 'same', package())

    def test_private_output_cannot_escape_runtime(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            with self.assertRaises(ValueError):
                fidelity.private_path(root / '.runtime', root / 'public')

    def test_public_summary_excludes_document_text_paths_and_links(self):
        result = fidelity.public_summary({'engine': 'test', 'source_path': 'private-name.docx', 'body': ['PRIVATE'], 'links': ['https://private.invalid'], 'cycles': 3, 'status': 'NO-GO'})
        self.assertEqual({'engine': 'test', 'cycles': 3, 'status': 'NO-GO'}, result)

    def test_capture_rejects_existing_checkpoint_without_touching_it(self):
        with tempfile.TemporaryDirectory() as folder:
            out=Path(folder)/'checkpoint';out.mkdir();(out/'engine.pdf').write_bytes(b'%PDF-old')
            with patch.object(fidelity, 'private_path', return_value=out):
                with self.assertRaises(FileExistsError):
                    fidelity.capture('http://localhost:1','id',out)
            self.assertEqual(b'%PDF-old',(out/'engine.pdf').read_bytes())

    def test_capture_uses_explicit_immutable_version_without_reading_current(self):
        class Response(io.BytesIO):
            headers={}
        calls=[]
        def request(host,path,post=False):
            calls.append(path)
            return Response(package())
        with tempfile.TemporaryDirectory() as folder:
            out=Path(folder)/'checkpoint'
            with patch.object(fidelity,'private_path',return_value=out),patch.object(fidelity,'request',side_effect=request):
                result=fidelity.capture('http://localhost:1','id',out,previous='old',pdf=False,version='observed')
            self.assertEqual('observed',result['version'])
            self.assertEqual(['/lab/documents/id/versions/observed'],calls)

    def test_conversion_refuses_to_reuse_existing_pdf_before_contacting_engine(self):
        with tempfile.TemporaryDirectory() as folder:
            out=Path(folder);(out/'engine.pdf').write_bytes(b'%PDF-old')
            with patch.object(fidelity,'private_path',return_value=out):
                with self.assertRaises(FileExistsError):
                    fidelity.convert_onlyoffice('http://localhost:1','id','version',out)
            self.assertEqual(b'%PDF-old',(out/'engine.pdf').read_bytes())


if __name__ == '__main__':
    unittest.main()
