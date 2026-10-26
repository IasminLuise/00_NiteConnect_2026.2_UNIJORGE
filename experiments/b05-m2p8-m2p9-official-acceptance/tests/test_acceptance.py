"""Unit self-tests with deliberately artificial files. NEVER motor acceptance tests."""
from __future__ import annotations
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile

from b05_acceptance.core import safe_file, checked_file, orphan_rels, table_cell_texts, assess, _case_receipt, _review_receipt, analyzers, _compare_docx
from prepare import prepare

W='http://schemas.openxmlformats.org/wordprocessingml/2006/main'
CT='http://schemas.openxmlformats.org/package/2006/content-types'
PKG='http://schemas.openxmlformats.org/package/2006/relationships'


def fake_docx(path: Path, message='CONTEUDO DE TESTE', orphan=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(path,'w') as zip:
        zip.writestr('[Content_Types].xml', f'<Types xmlns="{CT}"><Default Extension="xml" ContentType="application/xml"/><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/></Types>')
        zip.writestr('word/document.xml', f'<w:document xmlns:w="{W}"><w:body><w:p><w:r><w:t>{message}</w:t></w:r></w:p><w:sectPr/></w:body></w:document>')
        if orphan: zip.writestr('word/_rels/'+orphan+'.rels', f'<Relationships xmlns="{PKG}"/>')
    return path


def entry(root, path):
    return {'path':str(path.relative_to(root).as_posix()), 'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}


class OfficialAcceptanceTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        self.root=Path(self.temp.name)
        self.av3=fake_docx(self.root/'origin-av3.docx')
        self.week=fake_docx(self.root/'origin-weekly.docx','REGISTRO FICTICIO')

    def tearDown(self):
        self.temp.cleanup()

    def campaign(self):
        mf=prepare(self.av3,self.week,self.root/'camp-01')
        return mf, json.loads(mf.read_text(encoding='utf-8'))

    def test_prepare_preserves_real_bytes_without_editing_original(self):
        initial=[self.av3.read_bytes(),self.week.read_bytes()]
        mf, raw=self.campaign()
        for d in raw['documents']:
            self.assertEqual(len(d['scenarios']),3)
            src=mf.parent/d['official_source']['path']
            self.assertTrue(src.exists())
            self.assertEqual({x['name'] for x in d['scenarios']},{'no_edit','edit_text','edit_table'})
            for c in d['scenarios']:
                self.assertEqual((mf.parent/c['session_source_docx']['path']).read_bytes(),src.read_bytes())
        self.assertEqual([self.av3.read_bytes(),self.week.read_bytes()],initial)

    def test_prepare_cannot_overwrite_campaign(self):
        self.campaign()
        with self.assertRaises(ValueError): prepare(self.av3,self.week,self.root/'camp-01')

    def test_prepare_rejects_non_docx(self):
        bad=self.root/'bad.docx';bad.write_text('not zip')
        with self.assertRaises(ValueError): prepare(bad,self.week,self.root/'another')

    def test_path_traversal_blocked(self):
        mf,_=self.campaign()
        for p in ('../../origin-av3.docx','/tmp/file.docx','C:\\temp\\file.docx'):
            with self.assertRaises(ValueError): safe_file(mf.parent,p)

    def test_hash_tamper_fails(self):
        mf,raw=self.campaign(); e=raw['documents'][0]['official_source'];e['sha256']='0'*64
        with self.assertRaises(ValueError): checked_file(mf.parent,e,'.docx',{})

    def test_missing_artifacts_hold_not_approved(self):
        mf,_=self.campaign()
        result=assess(mf)
        self.assertEqual(result['recommendation'],'HOLD_MISSING_OFFICIAL_EVIDENCE')
        self.assertFalse(result['real_round_trip_completed'])
        self.assertEqual(len(result['cases']),6)
        self.assertFalse(result['canPromote'])
        self.assertFalse(result['officialApproved'])
        self.assertFalse(result['backend_final_integration_authorized'])

    def test_reject_generic_synthetic_manifest(self):
        mf,raw=self.campaign()
        raw['corpus_kind']='synthetic'
        mf.write_text(json.dumps(raw))
        with self.assertRaises(ValueError): assess(mf)

    def test_reject_missing_required_document(self):
        mf,raw=self.campaign();raw['documents']=raw['documents'][:1]
        mf.write_text(json.dumps(raw))
        with self.assertRaises(ValueError): assess(mf)

    def test_reject_missing_scenario(self):
        mf,raw=self.campaign();raw['documents'][0]['scenarios']=raw['documents'][0]['scenarios'][:2]
        mf.write_text(json.dumps(raw))
        with self.assertRaises(ValueError): assess(mf)

    def test_orphan_identity_detection(self):
        doc=fake_docx(self.root/'orphan.docx',orphan='comments.xml')
        self.assertEqual(orphan_rels(doc),['word/_rels/comments.xml.rels'])
        second=fake_docx(self.root/'other.docx',orphan='people.xml')
        self.assertNotEqual(set(orphan_rels(doc)),set(orphan_rels(second)))
        self.assertEqual(len(orphan_rels(doc)),len(orphan_rels(second)))

    def test_table_cell_scan_does_not_accept_regular_paragraph_as_cell(self):
        self.assertEqual(table_cell_texts(self.av3),[])

    def test_technical_review_cannot_be_editorial_review(self):
        mf,_=self.campaign(); r=mf.parent/'review.json'
        r.write_text(json.dumps({'kind':'editorial_review','document':'AV3','scenario':'no_edit','candidate_sha256':'q','reviewer_role':'editorial','full_pages_inspected':True,'decision':'ACCEPT','reason':'review'}))
        with self.assertRaises(ValueError):
            _review_receipt(mf.parent,entry(mf.parent,r),{},'AV3','no_edit','q','technical')

    def test_no_orphan_source(self):
        self.assertEqual(orphan_rels(self.av3),[])

    def test_normal_permission_override_is_invalid(self):
        mf,_=self.campaign(); r=mf.parent/'receipt.json'; origin=hashlib.sha256(self.av3.read_bytes()).hexdigest()
        r.write_text(json.dumps({'kind':'acquisition','document':'AV3','scenario':'edit_text','source_sha256':origin,'acquired_sha256':{},'normal_permissions':True,'permission_override':True,'test_authorized':True,'editor_version':'10','capture_method':'test','operator_recorded_at':'now','callback_status':2}))
        ledger={}; answer=_case_receipt(mf.parent, entry(mf.parent,r),ledger,'AV3','edit_text',origin,{})
        self.assertFalse(answer['valid_normal_permissions'])

    def test_acquisition_cannot_claim_different_sha(self):
        mf,_=self.campaign(); r=mf.parent/'receipt.json'; origin=hashlib.sha256(self.av3.read_bytes()).hexdigest()
        doc=mf.parent/'fake.docx'; fake_docx(doc,'different')
        r.write_text(json.dumps({'kind':'acquisition','document':'AV3','scenario':'edit_text','source_sha256':origin,'acquired_sha256':{'engine_docx':'0'*64},'normal_permissions':True,'permission_override':False,'test_authorized':True,'editor_version':'10','capture_method':'test','operator_recorded_at':'now','callback_status':2}))
        answer=_case_receipt(mf.parent,entry(mf.parent,r),{},'AV3','edit_text',origin,{'engine_docx':doc})
        self.assertFalse(answer['valid_normal_permissions'])
        self.assertFalse(answer['captured_hashes_verified'])

    def test_review_missing_returns_missing(self):
        self.assertEqual(_review_receipt(self.root,None,{},'AV3','edit_text','hash'),{'status':'MISSING'})

    def test_incomplete_review_does_not_approve(self):
        mf,_=self.campaign(); r=mf.parent/'review.json'
        r.write_text(json.dumps({'kind':'technical_review','document':'AV3','scenario':'no_edit','candidate_sha256':'q','reviewer_role':'technical','full_pages_inspected':False,'decision':'ACCEPT','reason':'some notes'}))
        x=_review_receipt(mf.parent,entry(mf.parent,r),{},'AV3','no_edit','q','technical')
        self.assertEqual(x['status'],'INCOMPLETE')

    def test_untrusted_document_not_silently_reconstructed(self):
        mf,_=self.campaign()
        self.assertEqual((mf.parent/'sources/AV3.docx').read_bytes(),self.av3.read_bytes())
        self.assertNotIn('SINTETICO',mf.read_text(encoding='utf-8'))


    def test_identical_docx_comparison_uses_existing_m2p6_contract(self):
        first = fake_docx(self.root / 'same-a.docx')
        second = fake_docx(self.root / 'same-b.docx')
        inspect, compare, _ = analyzers()
        actual = _compare_docx(first, second, None, inspect, compare)
        self.assertTrue(actual['text_as_expected'])
        self.assertEqual(actual['explicit_format_mismatch_chars'], 0)
        self.assertFalse(actual['structural_equivalence_not_proven'])

if __name__=='__main__': unittest.main()
