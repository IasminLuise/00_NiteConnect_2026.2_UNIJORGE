import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

from b05_qualification.decision import evaluate, write
from b05_qualification.evidence import digest
from tests.support import make_campaign


class DecisionTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.folder = Path(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def campaign(self, **kw):
        return make_campaign(self.folder / 'data', **kw)

    def test_conservative_synthetic_baseline(self):
        result = evaluate(self.campaign())
        self.assertEqual(result['m2p7']['recommendation'], 'HOLD_DECISION_NOT_ENOUGH_EVIDENCE')
        self.assertFalse(result['canPromote'])
        self.assertFalse(result['officialApproved'])
        self.assertFalse(result['backend_final_integration_authorized'])
        self.assertEqual(result['blocked_gates'], ['G1', 'G2', 'M3'])
        self.assertEqual(result['m2p6']['prior_m2p4_no_edit_raster'][0]
                         ['source_to_sdk_total_different_pixels'], 13)
        self.assertFalse(result['m2p6']['prior_m2p4_no_edit_raster'][0]
                         ['raster_recomputed_in_this_patch'])

    def test_ready_for_review_still_not_go(self):
        result = evaluate(self.campaign(document_origin='official_controlled', complete=True))
        self.assertEqual(result['m2p7']['recommendation'], 'READY_FOR_FORMAL_GATE_REVIEW_NOT_APPROVAL')
        self.assertFalse(result['m2p7']['definitive_engine_approved'])
        self.assertFalse(result['canPromote'])
        self.assertFalse(result['m2p7']['corpus_representativeness_independently_proven'])

    def test_new_dangling_reference_is_risk(self):
        result = evaluate(self.campaign(bad_ref=True))
        self.assertTrue(result['m2p6']['objective_broken_candidate_opc'])
        self.assertEqual(result['m2p7']['recommendation'], 'STOP_AND_REVIEW_DEMONSTRATED_OR_ATTESTED_RISKS')

    def test_invalid_callback_relation_is_risk(self):
        result = evaluate(self.campaign(malformed_callback=True))
        self.assertTrue(result['m2p6']['objective_broken_candidate_opc'])

    def test_prior_fail_is_not_auto_cleared(self):
        result = evaluate(self.campaign(previous_fail=True))
        self.assertIn('UNRESOLVED_PRIOR_M2P5_FAIL', result['m2p7']['risk_flags'])

    def test_changed_format_is_diagnostic_not_auto_go(self):
        result = evaluate(self.campaign(changed_font=True))
        findings = result['m2p6']['documents'][0]['pairs']['SOURCE_TO_SDK']
        self.assertGreater(findings['explicit_format_mismatch_chars_on_matched_text'], 0)
        self.assertFalse(findings['effective_style_equivalence_proven'])

    def test_corrupt_sha_rejected(self):
        manifest = self.campaign()
        obj = json.loads(manifest.read_text())
        obj['documents'][0]['sdk_docx']['sha256'] = 'a' * 64
        manifest.write_text(json.dumps(obj))
        with self.assertRaises(ValueError):
            evaluate(manifest)

    def test_escape_path_rejected(self):
        manifest = self.campaign()
        obj = json.loads(manifest.read_text())
        obj['documents'][0]['sdk_docx']['path'] = '../../outside.docx'
        manifest.write_text(json.dumps(obj))
        with self.assertRaises(ValueError):
            evaluate(manifest)

    def test_requires_explicit_all_eight_integration_checks(self):
        manifest = self.campaign()
        obj = json.loads(manifest.read_text())
        obj['integration_checks'].pop()
        manifest.write_text(json.dumps(obj))
        with self.assertRaises(ValueError):
            evaluate(manifest)

    def test_missing_previous_adjudication_rejected(self):
        manifest = self.campaign()
        obj = json.loads(manifest.read_text())
        obj['adjudications'].pop()
        manifest.write_text(json.dumps(obj))
        with self.assertRaises(ValueError):
            evaluate(manifest)

    def test_unverified_does_not_turn_pass(self):
        result = evaluate(self.campaign(document_origin='official_controlled', complete=False))
        self.assertEqual(len(result['m2p7']['missing_integration_checks']), 8)
        self.assertEqual(len(result['m2p7']['missing_review_roles']), 3)

    def test_declared_feature_not_automatically_observed(self):
        manifest = self.campaign()
        obj = json.loads(manifest.read_text())
        obj['required_features'].append('comments')
        obj['documents'][0]['declared_features'].append('comments')
        manifest.write_text(json.dumps(obj))
        result = evaluate(manifest)
        self.assertIn('comments', result['m2p7']['feature_gaps'])

    def test_no_reuse_same_physical_doc(self):
        manifest = self.campaign()
        obj = json.loads(manifest.read_text())
        obj['documents'][0]['sdk_docx'] = obj['documents'][0]['source_docx']
        manifest.write_text(json.dumps(obj))
        with self.assertRaises(ValueError):
            evaluate(manifest)

    def test_wrong_previous_product_flags_rejected(self):
        manifest = self.campaign()
        previous = manifest.parent / 'old.json'
        obj = json.loads(previous.read_text())
        obj['canPromote'] = True
        previous.write_text(json.dumps(obj))
        obj2 = json.loads(manifest.read_text())
        obj2['m2p5_report']['sha256'] = digest(previous)
        manifest.write_text(json.dumps(obj2))
        with self.assertRaises(ValueError):
            evaluate(manifest)

    def test_output_write_no_overwrite(self):
        manifest = self.campaign()
        result = evaluate(manifest)
        out = self.folder / 'output'
        file1, file2 = write(result, out)
        self.assertTrue(file1.exists())
        self.assertTrue(file2.exists())
        self.assertNotIn(str(manifest.resolve()), file1.read_text())
        self.assertNotIn('FICTITIOUS CLAIM RECEIPT', file1.read_text())
        with self.assertRaises(FileExistsError):
            write(result, out)

    def test_mutation_before_reverification_blocks_report(self):
        manifest = self.campaign()
        from b05_qualification import decision
        real = decision.reverify
        def mutate_then_reverify(files):
            Path(files[0][0]).write_text('tampered')
            return real(files)
        with patch.object(decision, 'reverify', mutate_then_reverify):
            with self.assertRaises(ValueError):
                evaluate(manifest)

    def test_review_rejection_requires_evidence(self):
        manifest = self.campaign()
        obj = json.loads(manifest.read_text())
        obj['reviews'][0]['decision'] = 'REJECT_ATTESTED'
        manifest.write_text(json.dumps(obj))
        with self.assertRaises(ValueError):
            evaluate(manifest)

    def test_evidenced_review_rejection_is_risk(self):
        manifest = self.campaign(document_origin='official_controlled', complete=True)
        obj = json.loads(manifest.read_text())
        obj['reviews'][0]['decision'] = 'REJECT_ATTESTED'
        manifest.write_text(json.dumps(obj))
        result = evaluate(manifest)
        self.assertIn('REVIEW_REJECTED', result['m2p7']['risk_flags'])

    def test_official_without_authorization_rejected(self):
        manifest = self.campaign(document_origin='official_controlled')
        obj = json.loads(manifest.read_text())
        del obj['documents'][0]['authorization_receipt']
        manifest.write_text(json.dumps(obj))
        with self.assertRaises(ValueError):
            evaluate(manifest)

    def test_json_only_contains_bounded_metadata(self):
        result = evaluate(self.campaign())
        as_json = json.dumps(result)
        self.assertNotIn('FICTITIOUS', as_json)
        self.assertNotIn('TESTE FICTICIO', as_json)
        self.assertNotIn('/tmp/', as_json)
        self.assertNotIn('"sha256"', as_json)


    def test_unexpected_content_change_prevents_readiness(self):
        manifest = self.campaign(document_origin='official_controlled', complete=True)
        from tests.support import write_docx, entry
        altered = write_docx(manifest.parent / 'docs' / 'sdk.docx', text='UNEXPECTED CONTENT')
        obj = json.loads(manifest.read_text())
        obj['documents'][0]['sdk_docx'] = entry(altered, manifest.parent)
        manifest.write_text(json.dumps(obj))
        result = evaluate(manifest)
        self.assertIn('UNEXPECTED_VISIBLE_TEXT_CHANGE_IN_SDK', result['m2p7']['risk_flags'])
        self.assertEqual(result['m2p7']['recommendation'], 'STOP_AND_REVIEW_DEMONSTRATED_OR_ATTESTED_RISKS')

    def test_expected_edit_does_not_count_as_unexpected(self):
        manifest = self.campaign()
        from tests.support import write_docx, entry
        sdk = write_docx(manifest.parent / 'docs' / 'sdk.docx', text='MARCADOR NOVO', page_break=True)
        obj = json.loads(manifest.read_text())
        obj['documents'][0]['sdk_docx'] = entry(sdk, manifest.parent)
        obj['documents'][0]['operation'] = 'edited'
        obj['documents'][0]['expected_edit'] = {'before': 'TESTE FICTICIO', 'after': 'MARCADOR NOVO'}
        cb = write_docx(manifest.parent / 'docs' / 'callback.docx', text='MARCADOR NOVO', page_break=True)
        obj['documents'][0]['callback_docx'] = entry(cb, manifest.parent)
        manifest.write_text(json.dumps(obj))
        result = evaluate(manifest)
        self.assertNotIn('UNEXPECTED_VISIBLE_TEXT_CHANGE_IN_SDK', result['m2p7']['risk_flags'])
        self.assertTrue(result['m2p6']['documents'][0]['pairs']['SOURCE_TO_SDK']['expected_visible_text_only'])

    def test_style_change_is_hold_not_proven_failure(self):
        result = evaluate(self.campaign(changed_font=True))
        self.assertIn('NEW_STRUCTURAL_DIFFERENCES_WITHOUT_SEMANTIC_REVIEW', result['m2p7']['hold_reasons'])
        self.assertEqual(result['m2p7']['recommendation'], 'HOLD_DECISION_NOT_ENOUGH_EVIDENCE')

    def add_empty_orphan(self, manifest, stages):
        raw = json.loads(manifest.read_text())
        for stage in stages:
            ref = raw['documents'][0][stage]
            docx = manifest.parent / ref['path']
            with zipfile.ZipFile(docx, 'a') as package:
                package.writestr('word/_rels/comments.xml.rels',
                    '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>')
            ref['sha256'] = digest(docx)
        manifest.write_text(json.dumps(raw))

    def test_new_empty_relationship_part_without_owner_is_risk(self):
        manifest = self.campaign()
        self.add_empty_orphan(manifest, ['sdk_docx'])
        result = evaluate(manifest)
        pair = result['m2p6']['documents'][0]['pairs']['SOURCE_TO_SDK']
        self.assertEqual(pair['opcs_before_after'][1]['missing_owners'], 1)
        self.assertGreater(pair['candidate_new_broken_relationships'], 0)
        self.assertTrue(result['m2p6']['objective_broken_candidate_opc'])
        self.assertEqual(result['m2p7']['recommendation'],
                         'STOP_AND_REVIEW_DEMONSTRATED_OR_ATTESTED_RISKS')
        self.assertFalse(result['canPromote'])

    def test_new_empty_callback_relationship_part_without_owner_is_risk(self):
        manifest = self.campaign()
        self.add_empty_orphan(manifest, ['callback_docx'])
        result = evaluate(manifest)
        self.assertIn('NEW_BROKEN_OPC_IN_CALLBACK', result['m2p7']['risk_flags'])
        self.assertEqual(result['m2p7']['recommendation'],
                         'STOP_AND_REVIEW_DEMONSTRATED_OR_ATTESTED_RISKS')

    def test_existing_orphan_in_all_stages_is_not_a_new_candidate_error(self):
        manifest = self.campaign()
        self.add_empty_orphan(manifest, ['source_docx', 'sdk_docx', 'callback_docx'])
        result = evaluate(manifest)
        self.assertFalse(result['m2p6']['objective_broken_candidate_opc'])
        self.assertEqual(result['m2p7']['recommendation'],
                         'HOLD_DECISION_NOT_ENOUGH_EVIDENCE')
        self.assertTrue(all(pair['opcs_before_after'][1]['missing_owners'] == 1
                            for pair in result['m2p6']['documents'][0]['pairs'].values()))

if __name__ == '__main__':
    unittest.main()
