"""Synthetic controls for a narrow, opt-in SAVEDATE representation adapter."""
from io import BytesIO
from pathlib import Path
import sys
import unittest
from zipfile import ZipFile
from lxml import etree as ET

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from savedate import convert_savedate

W = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
NS = {'w': W}
INSTRUCTION = ' SAVEDATE  \\@ "dd/MM/yyyy HH:mm:ss" \\* MERGEFORMAT '


def package(header, **other):
    files = {'[Content_Types].xml': b'<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"/>',
             '_rels/.rels': b'<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>',
             'word/document.xml': f'<w:document xmlns:w="{W}"><w:body><w:p><w:r><w:t>Preservar corpo</w:t></w:r></w:p></w:body></w:document>'.encode(),
             'word/header1.xml': f'<w:hdr xmlns:w="{W}"><w:p>{header}</w:p></w:hdr>'.encode(), **other}
    out = BytesIO()
    with ZipFile(out, 'w') as z:
        for name, value in files.items(): z.writestr(name, value)
    return out.getvalue()


def field(instruction=INSTRUCTION, attributes=''):
    el = ET.Element(f'{{{W}}}fldSimple', {f'{{{W}}}instr': instruction}, nsmap={'w': W})
    if attributes:
        for name, value in attributes.items(): el.set(f'{{{W}}}{name}', value)
    r = ET.SubElement(el, f'{{{W}}}r')
    props = ET.SubElement(r, f'{{{W}}}rPr')
    ET.SubElement(props, f'{{{W}}}b')
    ET.SubElement(r, f'{{{W}}}t').text = '02/01/2020 03:04:05'
    return ET.tostring(el, encoding='unicode')


def parts(data):
    with ZipFile(BytesIO(data)) as z: return {n: z.read(n) for n in z.namelist()}


class SaveDateTests(unittest.TestCase):
    def test_preserves_exact_instruction_cached_result_formatting_and_other_parts(self):
        original = package(field(), **{'word/media/synthetic.bin': b'image-test'})
        fixed, changes = convert_savedate(original)
        before, after = parts(original), parts(fixed)
        self.assertEqual(['word/header1.xml'], [c['part'] for c in changes])
        self.assertEqual(before.keys(), after.keys())
        for name in before:
            if name != 'word/header1.xml': self.assertEqual(before[name], after[name])
        root = ET.fromstring(after['word/header1.xml'])
        self.assertEqual([], root.findall('.//w:fldSimple', NS))
        self.assertEqual([INSTRUCTION], root.xpath('//w:instrText/text()', namespaces=NS))
        self.assertEqual(['begin', 'separate', 'end'], root.xpath('//w:fldChar/@w:fldCharType', namespaces=NS))
        old_result = ET.fromstring(before['word/header1.xml']).find('.//w:fldSimple/w:r', NS)
        new_result = root.find('.//w:r[w:t]', NS)
        self.assertEqual(ET.tostring(old_result, method='c14n'), ET.tostring(new_result, method='c14n'))

    def test_keeps_lock_dirty_flags_without_forcing_updates(self):
        fixed, _ = convert_savedate(package(field(attributes={'fldLock': 'true', 'dirty': 'false'})))
        begin = ET.fromstring(parts(fixed)['word/header1.xml']).find('.//w:fldChar', NS)
        self.assertEqual('true', begin.get(f'{{{W}}}fldLock'))
        self.assertEqual('false', begin.get(f'{{{W}}}dirty'))

    def test_does_not_rewrite_ref_date_page_numpages_or_lookalike_names(self):
        original = package(''.join(field(x) for x in ['REF bookmark', 'DATE', 'PAGE', 'NUMPAGES', 'SAVEDATEX', 'REF SAVEDATE']))
        self.assertEqual((original, []), convert_savedate(original))

    def test_preserves_lowercase_instruction_and_is_idempotent(self):
        original = package(field('  savedate \\* MERGEFORMAT  '))
        fixed, changes = convert_savedate(original)
        self.assertEqual(1, len(changes))
        self.assertEqual((fixed, []), convert_savedate(fixed))
        self.assertIn(b'  savedate \\* MERGEFORMAT  ', parts(fixed)['word/header1.xml'])

    def test_handles_multiple_stories_and_fields_independently(self):
        footer = f'<w:ftr xmlns:w="{W}"><w:p>{field()}</w:p></w:ftr>'.encode()
        fixed, changes = convert_savedate(package(field()+field('PAGE')+field(), **{'word/footer1.xml': footer}))
        self.assertEqual(3, len(changes))
        self.assertEqual(1, len(ET.fromstring(parts(fixed)['word/header1.xml']).findall('.//w:fldSimple', NS)))

    def test_refuses_nested_field_instead_of_flattening_it(self):
        outer = ET.fromstring(field())
        outer.append(ET.fromstring(field('PAGE')))
        with self.assertRaises(ValueError): convert_savedate(package(ET.tostring(outer, encoding='unicode')))

    def test_refuses_unknown_attributes_instead_of_dropping_them(self):
        with self.assertRaises(ValueError): convert_savedate(package(field(attributes={'unsupported': 'value'})))

    def test_refuses_simple_savedate_inside_delimited_complex_field(self):
        begin = '<w:r><w:fldChar w:fldCharType="begin"/></w:r><w:r><w:instrText>IF 1 = 1</w:instrText></w:r><w:r><w:fldChar w:fldCharType="separate"/></w:r>'
        end = '<w:r><w:fldChar w:fldCharType="end"/></w:r>'
        with self.assertRaises(ValueError): convert_savedate(package(begin+field()+end))

    def test_refuses_signed_archives_and_unsafe_packages(self):
        for name in ['_xmlsignatures/sig1.xml', '../outside.xml', 'word/vbaProject.bin']:
            with self.subTest(name=name), self.assertRaises(ValueError):
                convert_savedate(package(field(), **{name: b'<data/>'}))
        malicious = b'<!DOCTYPE w:hdr [<!ENTITY secret SYSTEM "file:///never-read">]><w:hdr xmlns:w="'+W.encode()+b'">&secret;</w:hdr>'
        with self.assertRaises(ValueError): convert_savedate(package(field(), **{'word/header1.xml': malicious}))

    def test_refuses_implicit_external_target_without_target_mode(self):
        rels = b'<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="hyperlink" Target="https://example.invalid/never-fetch"/></Relationships>'
        with self.assertRaises(ValueError):
            convert_savedate(package(field(), **{'word/_rels/header1.xml.rels': rels}))


if __name__ == '__main__': unittest.main()
