import io
import unittest
import zipfile

import compatibility as c
import compatibility_cases as cases

W = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'


def package(field=None, text='A  B', extra=None):
    parts = {'word/document.xml': f'<w:document xmlns:w="{W}"><w:body><w:p><w:r><w:t>{text}</w:t></w:r></w:p></w:body></w:document>'}
    if field is not None:
        parts['word/header1.xml'] = f'<w:hdr xmlns:w="{W}"><w:p>{field}</w:p></w:hdr>'
    parts.update(extra or {})
    out = io.BytesIO()
    with zipfile.ZipFile(out, 'w') as z:
        for name, value in parts.items():
            z.writestr(name, value)
    return out.getvalue()


SIMPLE = '<w:fldSimple w:instr="SAVEDATE  \\* MERGEFORMAT"><w:r><w:t>cached</w:t></w:r></w:fldSimple>'
COMPLEX = '<w:r><w:fldChar w:fldCharType="begin"/></w:r><w:r><w:instrText> SAVE</w:instrText></w:r><w:r><w:instrText>DATE \\* MERGEFORMAT </w:instrText></w:r><w:r><w:fldChar w:fldCharType="separate"/></w:r><w:r><w:t>cached</w:t></w:r><w:r><w:fldChar w:fldCharType="end"/></w:r>'


class CompatibilityTests(unittest.TestCase):
    def test_minimal_field_cases_preserve_semantics_in_both_representations(self):
        a, b = c.inventory(cases.field_package(False)), c.inventory(cases.field_package(True))
        self.assertEqual('SAVEDATE', a['fields'][0]['type'])
        self.assertEqual('unchanged', c.compare(a, b)['fields']['classification'])

    def test_synthetic_table_inherits_twelve_point_normal(self):
        data = cases.field_package(False, table=True)
        a = c.inventory(data)
        self.assertEqual([], a['fields'])
        self.assertIn('word/styles.xml', a['properties'])
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            self.assertIn(b'w:val="24"', z.read('word/styles.xml'))

    def test_reduced_settings_remove_unresolved_note_parts_only(self):
        source = f'<w:settings xmlns:w="{W}"><w:footnotePr><w:footnote w:id="-1"/></w:footnotePr><w:endnotePr><w:endnote w:id="0"/></w:endnotePr><w:compat/></w:settings>'.encode()
        result = cases.reduced_settings(source)
        self.assertNotIn(b'footnotePr', result)
        self.assertNotIn(b'endnotePr', result)
        self.assertIn(b'<w:compat/>', result)

    def test_simple_and_complex_fields_are_semantically_equivalent(self):
        before, after = c.inventory(package(SIMPLE)), c.inventory(package(COMPLEX))
        self.assertEqual('simple', before['fields'][0]['representation'])
        self.assertEqual('complex', after['fields'][0]['representation'])
        self.assertEqual('cached', after['fields'][0]['cached'])
        self.assertEqual('unchanged', c.compare(before, after)['fields']['classification'])

    def test_equivalent_unlocked_flags_do_not_create_a_regression(self):
        a = SIMPLE.replace('w:fldSimple w:instr', 'w:fldSimple w:fldLock="false" w:instr')
        b = COMPLEX.replace('w:fldCharType="begin"', 'w:fldCharType="begin" w:fldLock="0"')
        self.assertEqual('unchanged', c.compare(c.inventory(package(a)), c.inventory(package(b)))['fields']['classification'])

    def test_extra_part_is_unverified_until_story_references_are_checked(self):
        a = c.inventory(package(SIMPLE))
        b = c.inventory(package(SIMPLE, extra={'word/header2.xml': f'<w:hdr xmlns:w="{W}"><w:p>{SIMPLE}</w:p></w:hdr>'}))
        self.assertEqual('unverified', c.compare(a,b)['fields']['classification'])

    def test_field_type_loss_is_a_semantic_regression(self):
        result = c.compare(c.inventory(package(SIMPLE)), c.inventory(package(SIMPLE.replace('SAVEDATE', 'REF'))))
        self.assertEqual('semantic-regression', result['fields']['classification'])

    def test_picture_whitespace_is_not_normalized_away(self):
        a = c.normalize_instruction(' SAVEDATE   \\@ "dd  MM" ')
        b = c.normalize_instruction('SAVEDATE \\@ "dd MM"')
        self.assertNotEqual(a, b)

    def test_cache_change_requires_explicit_validation(self):
        result = c.compare(c.inventory(package(SIMPLE)), c.inventory(package(SIMPLE.replace('cached', 'new date'))))
        self.assertEqual('unverified', result['fields']['classification'])

    def test_expected_body_edit_does_not_hide_other_content_loss(self):
        a, b = c.inventory(package(text='A  B')), c.inventory(package(text='A B B1'))
        self.assertEqual('semantic-regression', c.compare(a, b)['body']['classification'])
        self.assertEqual('expected-edit', c.compare(a, b, expected_body=b['body'])['body']['classification'])
        self.assertEqual('unchanged', c.compare(a, b, expected_body=b['body'])['stories']['classification'])

    def test_settings_styles_footer_and_relations_are_inventoried(self):
        extra = {'word/settings.xml': f'<w:settings xmlns:w="{W}"><w:compat><w:doNotUseHTMLParagraphAutoSpacing/></w:compat></w:settings>',
                 'word/styles.xml': f'<w:styles xmlns:w="{W}"><w:style w:styleId="N"><w:pPr><w:keepNext/></w:pPr></w:style></w:styles>',
                 'word/footer1.xml': f'<w:ftr xmlns:w="{W}"><w:p><w:r><w:t>footer</w:t></w:r></w:p></w:ftr>',
                 'word/_rels/document.xml.rels': '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="hyperlink" Target="https://example.invalid/" TargetMode="External"/></Relationships>'}
        a = c.inventory(package(extra=extra))
        self.assertIn('word/settings.xml', a['properties'])
        self.assertIn('word/styles.xml', a['properties'])
        self.assertEqual(['footer'], a['stories']['word/footer1.xml'])
        self.assertEqual('https://example.invalid/', a['relationships'][0]['target'])
        self.assertEqual('unverified', c.compare(c.inventory(package()), a)['properties']['classification'])

    def test_utf16_dtd_and_duplicate_parts_are_rejected(self):
        xml = ('<?xml version="1.0" encoding="UTF-16"?><!DOCTYPE x [<!ENTITY e "expanded">]><x>&e;</x>').encode('utf-16')
        with self.assertRaises(ValueError):
            c.inventory(package(extra={'word/settings.xml': xml}))
        out = io.BytesIO()
        with zipfile.ZipFile(out, 'w') as z:
            z.writestr('word/document.xml', '<a/>')
            z.writestr('word/document.xml', '<b/>')
        with self.assertRaises(ValueError):
            c.inventory(out.getvalue())

    def test_nested_field_cache_is_scoped(self):
        nested = COMPLEX.replace('<w:t>cached</w:t>', '<w:t>outer</w:t>')
        nested = nested.replace('<w:r><w:t>outer</w:t></w:r>', '<w:r><w:t>outer</w:t></w:r>' + SIMPLE.replace('cached', 'inner'))
        values = c.inventory(package(nested))['fields']
        self.assertEqual(['outerinner', 'inner'], [f['cached'] for f in values])


if __name__ == '__main__':
    unittest.main()
