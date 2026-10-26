import json
import tempfile
import unittest
from pathlib import Path

from b05_m2p3.runner import analyze_campaign, write_reports, as_markdown
from tests.support import make_campaign

try:
    import fitz
except ImportError:
    fitz = None


@unittest.skipUnless(fitz is not None, 'Requires PyMuPDF')
class CampaignTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.base = Path(self.tmp.name)
        self.manifest = make_campaign(self.base)

    def edit_manifest(self, func):
        manifest = json.loads(self.manifest.read_text())
        func(manifest)
        self.manifest.write_text(json.dumps(manifest))

    def test_complete_campaign_remains_quarantined(self):
        report = analyze_campaign(self.manifest)
        self.assertEqual('HOLD_QUARANTINE', report['status'])
        self.assertFalse(report['canPromote'])
        self.assertFalse(report['officialApproved'])
        self.assertEqual(4, len(report['trials']))
        self.assertEqual('OBSERVATIONAL_ASSOCIATION_ONLY', report['conclusion_kind'])
        self.assertIn('rich_paste', report['causal_note'])
        self.assertEqual(['G1', 'G2', 'M3'], report['blocked_gates'])

    def test_reports_redact_document_text(self):
        report = analyze_campaign(self.manifest)
        out = self.base / 'out'
        write_reports(report, out)
        contents = (out / 'qualification.json').read_text()
        self.assertNotIn('Antigo', contents)
        self.assertNotIn('Novo', contents)
        self.assertNotIn(str(self.base), contents)
        self.assertIn('HOLD_QUARANTINE', as_markdown(report))

    def test_no_overwrite(self):
        report = analyze_campaign(self.manifest)
        out = self.base / 'out'
        write_reports(report, out)
        with self.assertRaises(FileExistsError):
            write_reports(report, out)

    def test_wrong_sha_denied(self):
        self.edit_manifest(lambda d: d['trials'][0]['evidence'].update({'source_docx_sha256': '0' * 64}))
        with self.assertRaisesRegex(ValueError, 'SHA-256'):
            analyze_campaign(self.manifest)

    def test_missing_trial_denied(self):
        self.edit_manifest(lambda d: d['trials'].pop())
        with self.assertRaisesRegex(ValueError, 'quatro'):
            analyze_campaign(self.manifest)

    def test_duplicate_source_path_denied(self):
        def change(d):
            d['trials'][1]['source_docx'] = d['trials'][0]['source_docx']
            d['trials'][1]['evidence']['source_docx_sha256'] = d['trials'][0]['evidence']['source_docx_sha256']
        self.edit_manifest(change)
        with self.assertRaisesRegex(ValueError, 'reutilizado'):
            analyze_campaign(self.manifest)

    def test_control_with_declared_edit_denied(self):
        self.edit_manifest(lambda d: d['trials'][0].update({'expected_edit': {'before': 'a', 'after': 'b'}}))
        with self.assertRaisesRegex(ValueError, 'no_edit'):
            analyze_campaign(self.manifest)

    def test_operator_mode_mismatch_denied(self):
        log = self.base / 'session1' / 'operator.json'
        data = json.loads(log.read_text())
        data['confirmed_mode'] = 'rich_paste'
        log.write_text(json.dumps(data))
        with self.assertRaisesRegex(ValueError, 'não corresponde'):
            analyze_campaign(self.manifest)

    def test_missing_operator_event_denied(self):
        log = self.base / 'session1' / 'operator.json'
        data = json.loads(log.read_text())
        data['events'] = [{'at': '2026-10-23T10:00:00Z', 'event': 'opened'}]
        log.write_text(json.dumps(data))
        with self.assertRaisesRegex(ValueError, 'não comprova'):
            analyze_campaign(self.manifest)

    def test_expected_edit_missing_denied(self):
        self.edit_manifest(lambda d: d['trials'][2].update({'expected_edit': None}))
        with self.assertRaisesRegex(ValueError, 'expected_edit'):
            analyze_campaign(self.manifest)


if __name__ == '__main__':
    unittest.main()
