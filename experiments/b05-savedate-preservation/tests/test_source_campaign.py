from pathlib import Path
import hashlib
import json
import sys
import tempfile
import unittest
from zipfile import ZipFile
from lxml import etree

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from prepare_runtime_campaign import build_campaign,CASES
from prepare_source import prepare as prepare_source
from build_source_image import build


class SourceCampaignTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name)/'.private';self.root.mkdir()
    def tearDown(self):self.temp.cleanup()
    def test_native_matrix_has_both_representations_and_four_run_languages(self):
        folder=self.root/'campaign';plan=build_campaign(folder)
        self.assertEqual(8,len(plan['native_source_build']['body_fields']))
        for name,expected_simple in [('original',5),('compatible',2)]:
            with ZipFile(folder/(name+'.docx')) as z:
                roots=[etree.fromstring(z.read(n)) for n in z.namelist() if n.startswith('word/') and n.endswith('.xml')]
                simple=sum(len(r.xpath('//*[local-name()="fldSimple" and starts-with(@*[local-name()="instr"],"SAVEDATE")]')) for r in roots)
                self.assertEqual(expected_simple,simple)
                body=etree.fromstring(z.read('word/document.xml'))
                for label,*_ in CASES:self.assertEqual(1,''.join(body.itertext()).count(label+':'))
                self.assertTrue({'pt-BR','en-US','de-DE','fr-FR'}.issubset(set(body.xpath('//*[local-name()="lang"]/@*[local-name()="val"]'))))
            self.assertEqual(plan[name]['sha256'],hashlib.sha256((folder/(name+'.docx')).read_bytes()).hexdigest())
    def test_campaign_refuses_repeat_and_preserves_frozen_original(self):
        folder=self.root/'campaign';build_campaign(folder);before=(folder/'original.docx').read_bytes()
        with self.assertRaises(FileExistsError):build_campaign(folder)
        self.assertEqual(before,(folder/'original.docx').read_bytes())
    def test_source_archive_tamper_is_rejected_before_mutation(self):
        source=self.root/'sdk';source.mkdir();sentinel=source/'sentinel';sentinel.write_text('preserve')
        bad=self.root/'bad.tar';bad.write_bytes(b'not the pinned vendor archive')
        with self.assertRaisesRegex(ValueError,'Unsupported source archive'):prepare_source(source,bad)
        self.assertEqual('preserve',sentinel.read_text());self.assertEqual(['sentinel'],[p.name for p in source.iterdir()])
    def test_image_build_refuses_public_output_before_any_docker_operation(self):
        source=self.root/'sdk';source.mkdir()
        with self.assertRaisesRegex(ValueError,'Private source and build'):build(source,Path(self.temp.name)/'public','nite/onlyoffice-savedate:test')


if __name__=='__main__':unittest.main()
