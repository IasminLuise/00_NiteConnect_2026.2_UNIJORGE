import sys,unittest,tempfile
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
from docx import Document
from docx.shared import Pt
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'scripts'))
from ooxml_gate import inventory,compare,load,W
from make_synthetic import make

def change(src,dst,old,new):
    with ZipFile(src) as z,ZipFile(dst,'w') as out:
        for n in z.namelist():
            dat=z.read(n)
            if n=='word/document.xml':
                assert old in dat
                dat=dat.replace(old,new,1)
            out.writestr(n,dat)

class StructuralGateTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
        self.path=Path(self.tmp.name)/'original.docx';make(self.path)
    def test_identity_pass(self):
        p=inventory(self.path)
        self.assertEqual(compare(p,p)['status'],'PASS_STRUCTURAL')
    def test_finds_savedate(self):
        x=inventory(self.path)
        self.assertTrue(any('SAVEDATE' in f['instruction'] for f in x['fields']))
        self.assertTrue(any('PAGE' in f['instruction'] for f in x['fields']))
    def test_extended_fixture_covers_complex_savedate_numpages_logo_and_explicit_second_page(self):
        target=Path(self.tmp.name)/'extended.docx';make(target,extended=True)
        x=inventory(target)
        self.assertEqual(2,sum('SAVEDATE' in f['instruction'] for f in x['fields']))
        self.assertTrue(any(f['form']=='complex' and 'SAVEDATE' in f['instruction'] for f in x['fields']))
        self.assertTrue(any('NUMPAGES' in f['instruction'] for f in x['fields']))
        self.assertEqual(1,len(x['media']))
        self.assertIn(b'w:type="page"',load(target)['word/document.xml'])
    def test_spacing_regression_fails(self):
        p=Path(self.tmp.name)/'bad.docx'
        change(self.path,p,b'w:after="160"',b'w:after="0"')
        issues=compare(inventory(self.path),inventory(p))['issues']
        self.assertTrue(any('CHANGED_PARAGRAPH_PROPERTIES' in item for item in issues))
    def test_body_edit_requires_explicit_expected_prefix(self):
        p=Path(self.tmp.name)/'edit.docx'
        # Edit only first paragraph content, do not alter any property.
        change(self.path,p,'NITE — fixture sintética de compatibilidade'.encode(),
               'XYZNITE — fixture sintética de compatibilidade'.encode())
        self.assertEqual(compare(inventory(self.path),inventory(p))['status'],'FAIL_STRUCTURAL')
        self.assertEqual(compare(inventory(self.path),inventory(p),'XYZ')['status'],'PASS_STRUCTURAL')
    def test_table_properties_change_with_same_text(self):
        target=Path(self.tmp.name)/'changed-table.docx'
        with ZipFile(self.path) as z,ZipFile(target,'w') as out:
            for name in z.namelist():
                data=z.read(name)
                if name=='word/document.xml':
                    from xml.etree import ElementTree as ET
                    from ooxml_gate import W
                    root=ET.fromstring(data)
                    table=next(root.iter(W+'tbl'))
                    grid=table.find(W+'tblGrid')
                    ET.SubElement(grid,W+'gridCol', {W+'w':'100'})
                    data=ET.tostring(root,encoding='utf-8')
                out.writestr(name,data)
        issues=compare(inventory(self.path),inventory(target))['issues']
        self.assertIn('CHANGED_XML_STRUCTURE:word/document.xml',issues)
    def test_relationship_changed_is_rejected(self):
        target=Path(self.tmp.name)/'changed-rels.docx'
        with ZipFile(self.path) as z,ZipFile(target,'w') as out:
            for name in z.namelist():
                data=z.read(name)
                if name=='_rels/.rels':
                    from xml.etree import ElementTree as ET
                    root=ET.fromstring(data)
                    root[0].set('Id','unexpected')
                    data=ET.tostring(root,encoding='utf-8')
                out.writestr(name,data)
        self.assertIn('CHANGED_XML_STRUCTURE:_rels/.rels',compare(inventory(self.path),inventory(target))['issues'])
    def test_missing_media_rejected(self):
        p=Path(self.tmp.name)/'bad.docx'
        with ZipFile(self.path) as z,ZipFile(p,'w') as out:
            for name in z.namelist():
                if name!='word/styles.xml':out.writestr(name,z.read(name))
        self.assertIn('CHANGED_PROTECTED_PARTS',compare(inventory(self.path),inventory(p))['issues'])
    def test_bad_duplicate_zip_entry_rejected(self):
        import warnings
        with warnings.catch_warnings():
            warnings.simplefilter('ignore')
            with ZipFile(self.path,'a') as z:z.writestr('word/document.xml',z.read('word/document.xml'))
        with self.assertRaises(ValueError):load(self.path)
    def with_ids(self, source, target, value=None, spacing=False):
        from xml.etree import ElementTree as ET
        with ZipFile(source) as zin,ZipFile(target,'w') as out:
            for name in zin.namelist():
                data=zin.read(name)
                if name=='word/document.xml' or name.startswith(('word/header','word/footer')) and name.endswith('.xml'):
                    root=ET.fromstring(data)
                    for i,p in enumerate(root.iter(W+'p'),1):p.set('{http://schemas.microsoft.com/office/word/2010/wordml}paraId',value if value is not None and (value=='00000001' or i==1) else f'{i:08X}')
                    if spacing:
                        next(e for e in root.iter(W+'spacing') if e.get(W+'after')=='160').set(W+'after','0')
                    data=ET.tostring(root,encoding='utf-8')
                out.writestr(name,data)
    def test_valid_unreferenced_paragraph_ids_are_opt_in_only_and_spacing_stays_protected(self):
        target=Path(self.tmp.name)/'ids.docx';self.with_ids(self.path,target)
        self.assertEqual('FAIL_STRUCTURAL',compare(inventory(self.path),inventory(target))['status'])
        self.assertEqual('PASS_STRUCTURAL',compare(inventory(self.path),inventory(target),allow_paragraph_id_additions=True)['status'])
        changed=Path(self.tmp.name)/'ids-spacing.docx'
        with ZipFile(target) as zin,ZipFile(changed,'w') as out:
            for name in zin.namelist():
                data=zin.read(name)
                if name=='word/document.xml':data=data.replace(b'after="160"',b'after="0"',1)
                out.writestr(name,data)
        self.assertEqual('FAIL_STRUCTURAL',compare(inventory(self.path),inventory(changed),allow_paragraph_id_additions=True)['status'])
    def test_duplicate_out_of_range_or_changed_existing_paragraph_ids_are_rejected(self):
        for value in ('00000001','00000000','80000000','not-an-id'):
            target=Path(self.tmp.name)/(value+'.docx');self.with_ids(self.path,target,value)
            self.assertEqual('FAIL_STRUCTURAL',compare(inventory(self.path),inventory(target),allow_paragraph_id_additions=True)['status'])
        baseline=Path(self.tmp.name)/'existing.docx';self.with_ids(self.path,baseline)
        changed=Path(self.tmp.name)/'changed-existing.docx'
        self.with_ids(baseline,changed)
        change(changed,Path(self.tmp.name)/'renamed.docx',b'paraId="00000001"',b'paraId="0000007F"')
        self.assertEqual('FAIL_STRUCTURAL',compare(inventory(baseline),inventory(Path(self.tmp.name)/'renamed.docx'),allow_paragraph_id_additions=True)['status'])
    def test_referenced_paragraph_ids_cannot_be_added_as_ignorable_metadata(self):
        from xml.etree import ElementTree as ET
        target=Path(self.tmp.name)/'references.docx';self.with_ids(self.path,target)
        referenced=Path(self.tmp.name)/'referenced.docx'
        with ZipFile(target) as zin,ZipFile(referenced,'w') as out:
            for name in zin.namelist():
                data=zin.read(name)
                if name=='word/settings.xml':
                    root=ET.fromstring(data);ET.SubElement(root,'{urn:synthetic-reference}commentEx',{'paraId':'00000001'});data=ET.tostring(root,encoding='utf-8')
                out.writestr(name,data)
        self.assertEqual('FAIL_STRUCTURAL',compare(inventory(self.path),inventory(referenced),allow_paragraph_id_additions=True)['status'])
    def test_numbering_xml_namespace_reserialization_is_not_a_property_change(self):
        from xml.etree import ElementTree as ET
        target=Path(self.tmp.name)/'lexical.docx'
        with ZipFile(self.path) as zin,ZipFile(target,'w') as out:
            for name in zin.namelist():
                data=zin.read(name)
                if name=='word/numbering.xml':data=ET.tostring(ET.fromstring(data),encoding='utf-8')
                out.writestr(name,data)
        self.assertEqual('PASS_STRUCTURAL',compare(inventory(self.path),inventory(target))['status'])
    def test_plain_run_segmentation_and_empty_runs_need_explicit_policy(self):
        from xml.etree import ElementTree as ET
        target=Path(self.tmp.name)/'split.docx'
        with ZipFile(self.path) as zin,ZipFile(target,'w') as out:
            for name in zin.namelist():
                data=zin.read(name)
                if name=='word/document.xml':
                    root=ET.fromstring(data);p=next(root.iter(W+'p'));run=ET.Element(W+'r');ET.SubElement(run,W+'t').text='XYZ';p.insert(1,run);p.insert(2,ET.Element(W+'r'));data=ET.tostring(root,encoding='utf-8')
                out.writestr(name,data)
        self.assertEqual('FAIL_STRUCTURAL',compare(inventory(self.path),inventory(target),'XYZ')['status'])
        self.assertEqual('PASS_STRUCTURAL',compare(inventory(self.path),inventory(target),'XYZ',allow_plain_text_run_segmentation=True)['status'])
        styled=Path(self.tmp.name)/'split-styled.docx'
        with ZipFile(target) as zin,ZipFile(styled,'w') as out:
            for name in zin.namelist():
                data=zin.read(name)
                if name=='word/document.xml':
                    root=ET.fromstring(data);p=next(root.iter(W+'p'));r=next(p.iter(W+'r'));pr=ET.Element(W+'rPr');ET.SubElement(pr,W+'b',{W+'val':'false'});r.insert(0,pr);data=ET.tostring(root,encoding='utf-8')
                out.writestr(name,data)
        self.assertEqual('FAIL_STRUCTURAL',compare(inventory(self.path),inventory(styled),'XYZ',allow_plain_text_run_segmentation=True)['status'])
    def test_non_xml_part_content_stays_protected(self):
        for part in ('word/embeddings/object.bin','word/fonts/font.odttf'):
            paths=[]
            for number,payload in enumerate((b'original',b'changed')):
                target=Path(self.tmp.name)/f'binary-{number}.docx';paths.append(target)
                with ZipFile(self.path) as source,ZipFile(target,'w') as out:
                    for name in source.namelist():out.writestr(name,source.read(name))
                    out.writestr(part,payload)
            self.assertEqual('FAIL_STRUCTURAL',compare(inventory(paths[0]),inventory(paths[1]))['status'])
    def test_whitespace_in_note_text_is_significant(self):
        paths=[]
        for number,spaces in enumerate((' ','  ')):
            target=Path(self.tmp.name)/f'note-{number}.docx';paths.append(target)
            with ZipFile(self.path) as source,ZipFile(target,'w') as out:
                for name in source.namelist():out.writestr(name,source.read(name))
                out.writestr('word/footnotes.xml','<w:footnotes xmlns:w="'+W[1:-1]+'"><w:footnote w:id="1"><w:p><w:r><w:t xml:space="preserve">'+spaces+'</w:t></w:r></w:p></w:footnote></w:footnotes>')
        self.assertEqual('FAIL_STRUCTURAL',compare(inventory(paths[0]),inventory(paths[1]))['status'])
    def test_plain_run_policy_does_not_discard_mixed_content(self):
        from xml.etree import ElementTree as ET
        from ooxml_gate import normalize_plain_runs,tree_signature
        for xml in (
            '<w:r>unexpected</w:r>',
            '<w:r/>unexpected',
            '<w:r><w:t>XYZ</w:t>unexpected</w:r>',
            '<w:r>unexpected<w:t>XYZ</w:t></w:r>',
        ):
            root=ET.fromstring('<w:p xmlns:w="'+W[1:-1]+'">'+xml+'</w:p>')
            before=tree_signature(root);normalize_plain_runs(root)
            self.assertEqual(before,tree_signature(root))
if __name__=='__main__':unittest.main()
