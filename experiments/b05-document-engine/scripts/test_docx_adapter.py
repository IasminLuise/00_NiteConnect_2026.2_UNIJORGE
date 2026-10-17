"""Adapter contract: expected field structure is independent of the adapter."""
import hashlib
import io
import unittest
import zipfile
import xml.etree.ElementTree as ET

from compatibility_cases import field_package
from fidelity import W
import docx_adapter as adapter


def replace_part(data, name, content):
    output = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(data)) as source, zipfile.ZipFile(output, 'w') as target:
        for entry in source.infolist():
            target.writestr(entry, content if entry.filename == name else source.read(entry))
    return output.getvalue()


def policy(data, fields=None):
    return {'original_sha256': hashlib.sha256(data).hexdigest(), 'template_version': 'synthetic-v1',
            'fields': fields if fields is not None else [{'part': 'word/header1.xml', 'instruction': 'SAVEDATE \\* MERGEFORMAT'}]}


class AdapterTest(unittest.TestCase):
    def test_simple_becomes_balanced_complex_preserving_cache_and_parts(self):
        data = field_package()
        output, record = adapter.normalize_copy(data, policy(data))
        with zipfile.ZipFile(io.BytesIO(data)) as original, zipfile.ZipFile(io.BytesIO(output)) as after:
            self.assertEqual(original.namelist(), after.namelist())
            for name in original.namelist():
                if name != 'word/header1.xml':
                    self.assertEqual(original.read(name), after.read(name))
            root = ET.fromstring(after.read('word/header1.xml'))
        self.assertEqual([], list(root.iter(W + 'fldSimple')))
        self.assertEqual(['begin', 'separate', 'end'], [n.get(W + 'fldCharType') for n in root.iter(W + 'fldChar')])
        self.assertEqual(['SAVEDATE \\* MERGEFORMAT'], [n.text for n in root.iter(W + 'instrText')])
        self.assertEqual(['23/09/2026 23:56:00'], [n.text for n in root.iter(W + 't')])
        self.assertEqual(['word/header1.xml'], [p['part'] for p in record['changes']])
        self.assertTrue(adapter.verify_idempotence(output, record))
        self.assertEqual(output, adapter.normalize_copy(data, policy(data))[0])

    def test_unapproved_hash_is_denied(self):
        with self.assertRaises(ValueError):
            adapter.normalize_copy(field_package(), {'original_sha256': '0' * 64, 'template_version': 'x', 'fields': []})

    def test_complex_and_other_fields_remain_byte_identical(self):
        for data in (field_package(True), field_package(table=True), field_package(field_xml='<w:fldSimple w:instr="REF bookmark"><w:r><w:t>value</w:t></w:r></w:fldSimple>')):
            with self.subTest():
                self.assertEqual(data, adapter.normalize_copy(data, policy(data, []))[0])

    def test_switch_whitespace_and_run_properties_are_preserved(self):
        field = '<w:fldSimple w:instr=" SAVEDATE  \\@ &quot;dd MM yyyy&quot; \\* MERGEFORMAT " w:dirty="true"><w:r w:rsidR="004D097E"><w:rPr><w:b/><w:lang w:val="pt-BR"/></w:rPr><w:t xml:space="preserve"> cache </w:t></w:r></w:fldSimple>'
        data = field_package(field_xml=field)
        output, _ = adapter.normalize_copy(data, policy(data, [{'part': 'word/header1.xml', 'instruction': ' SAVEDATE  \\@ "dd MM yyyy" \\* MERGEFORMAT '}]))
        with zipfile.ZipFile(io.BytesIO(output)) as z:
            root = ET.fromstring(z.read('word/header1.xml'))
        self.assertEqual(' cache ', next(root.iter(W + 't')).text)
        self.assertEqual(' SAVEDATE  \\@ "dd MM yyyy" \\* MERGEFORMAT ', next(root.iter(W + 'instrText')).text)
        self.assertEqual('true', next(root.iter(W + 'fldChar')).get(W + 'dirty'))
        self.assertEqual(1, len(list(root.iter(W + 'b'))))

    def test_unknown_nested_locked_or_mixed_structure_is_denied(self):
        cases = [
            '<w:fldSimple w:instr="SAVEDATE \\* MERGEFORMAT" w:unknown="x"><w:r><w:t>x</w:t></w:r></w:fldSimple>',
            '<w:fldSimple w:instr="SAVEDATE \\* MERGEFORMAT" w:fldLock="true"><w:r><w:t>x</w:t></w:r></w:fldSimple>',
            '<w:fldSimple w:instr="SAVEDATE \\* MERGEFORMAT"><w:fldSimple w:instr="DATE"/></w:fldSimple>',
            '<w:fldSimple w:instr="SAVEDATE \\* MERGEFORMAT">mixed<w:r><w:t>x</w:t></w:r></w:fldSimple>',
            '<w:fldSimple w:instr="SAVEDATE \\* MERGEFORMAT"><w:r><w:drawing/></w:r></w:fldSimple>',
        ]
        for field in cases:
            data = field_package(field_xml=field)
            with self.subTest(field=field), self.assertRaises(ValueError):
                adapter.normalize_copy(data, policy(data))

    def test_multiple_instances_are_explicitly_authorized(self):
        field = '<w:fldSimple w:instr="SAVEDATE \\* MERGEFORMAT"><w:r><w:t>x</w:t></w:r></w:fldSimple>'
        data = field_package(field_xml=field * 2)
        with self.assertRaises(ValueError):
            adapter.normalize_copy(data, policy(data))
        output, record = adapter.normalize_copy(data, policy(data, policy(data)['fields'] * 2))
        self.assertEqual(2, record['transformed_fields'])
        self.assertTrue(adapter.verify_idempotence(output, record))

    def test_malformed_package_external_image_dtd_and_macro_are_denied(self):
        data = field_package()
        cases = [b'not a zip', data[:-22],
                 replace_part(data, 'word/header1.xml', b'<!DOCTYPE x [<!ENTITY x "abc">]><x/>'),
                 replace_part(data, 'word/header1.xml', b'<bad>'),
                 replace_part(data, 'word/_rels/document.xml.rels', b'<Relationships><Relationship Id="x" Type="image" TargetMode="External" Target="https://example.test/x"/></Relationships>'),
                 replace_part(data, '[Content_Types].xml', b'<Types><Override PartName="/word/document.xml" ContentType="macroEnabled"/></Types>')]
        for candidate in cases:
            with self.subTest(), self.assertRaises(ValueError):
                adapter.normalize_copy(candidate, policy(candidate))

    def test_missing_relationship_target_is_denied(self):
        data = replace_part(field_package(), 'word/_rels/document.xml.rels', b'<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="x" Type="header" Target="missing.xml"/></Relationships>')
        with self.assertRaises(ValueError):
            adapter.normalize_copy(data, policy(data))

    def test_unknown_relationship_mode_and_mce_container_are_denied(self):
        data = field_package()
        cases = [replace_part(data, 'word/_rels/document.xml.rels', b'<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="x" Type="header" TargetMode="Unknown" Target="header1.xml"/></Relationships>'),
                 field_package(field_xml='<mc:AlternateContent xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"><mc:Fallback><w:p><w:fldSimple w:instr="SAVEDATE \\* MERGEFORMAT"><w:r><w:t>x</w:t></w:r></w:fldSimple></w:p></mc:Fallback></mc:AlternateContent>')]
        for candidate in cases:
            with self.subTest(), self.assertRaises(ValueError):
                adapter.normalize_copy(candidate, policy(candidate))

    def test_dangling_reference_and_non_absolute_content_type_part_are_denied(self):
        data = field_package()
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            document = z.read('word/document.xml').replace(b'<w:body>', b'<w:body><w:p r:id="missing"/>')
            types = z.read('[Content_Types].xml').replace(b'PartName="/word/header1.xml"', b'PartName="xword/header1.xml"')
        for candidate in (replace_part(data,'word/document.xml',document), replace_part(data,'[Content_Types].xml',types)):
            with self.subTest(), self.assertRaises(ValueError):
                adapter.normalize_copy(candidate, policy(candidate))

    def test_oversize_and_duplicate_package_is_denied(self):
        data = field_package()
        buf = io.BytesIO(data)
        with zipfile.ZipFile(buf, 'a') as z:
            z.writestr('word/header1.xml', '<x/>')
        for candidate in (buf.getvalue(), b'x' * (8 * 1024 * 1024 + 1)):
            with self.subTest(), self.assertRaises(ValueError):
                adapter.normalize_copy(candidate, policy(candidate))

    def test_field_nested_in_complex_field_is_denied(self):
        field = '<w:r><w:fldChar w:fldCharType="begin"/></w:r><w:r><w:instrText>IF</w:instrText></w:r><w:fldSimple w:instr="SAVEDATE \\* MERGEFORMAT"><w:r><w:t>x</w:t></w:r></w:fldSimple><w:r><w:fldChar w:fldCharType="end"/></w:r>'
        data = field_package(field_xml=field)
        with self.assertRaises(ValueError):
            adapter.normalize_copy(data, policy(data))

    def test_footer_instances_preserve_unrelated_header(self):
        data = field_package()
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            header = z.read('word/header1.xml')
        # A footer needs its own declared OPC content type and relationship.
        output = io.BytesIO()
        with zipfile.ZipFile(io.BytesIO(data)) as source, zipfile.ZipFile(output,'w') as target:
            for entry in source.infolist():
                content = source.read(entry)
                if entry.filename == '[Content_Types].xml':
                    content = content.replace(b'</Types>', b'<Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/></Types>')
                target.writestr(entry, content)
            target.writestr('word/footer1.xml', header.replace(b'w:hdr', b'w:ftr'))
        data = output.getvalue()
        fields = policy(data)['fields'] + [{'part':'word/footer1.xml','instruction':'SAVEDATE \\* MERGEFORMAT'}]
        out, record = adapter.normalize_copy(data, policy(data,fields))
        self.assertEqual(2,record['transformed_fields'])
        self.assertTrue(adapter.verify_idempotence(out,record))


if __name__ == '__main__':
    unittest.main()
