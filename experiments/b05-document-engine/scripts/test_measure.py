import subprocess
import tempfile
import unittest
from pathlib import Path
import measure

class ComparisonTests(unittest.TestCase):
    def test_success_exit_without_new_output_never_reuses_old_pdf(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);(root/'libreoffice').mkdir();inputs=root/'inputs';inputs.mkdir()
            old=root/'libreoffice'/'compare-F01.pdf';old.write_bytes(b'%PDF-old-result')
            def no_output(command, **kwargs):
                return subprocess.CompletedProcess(command,0,'','')
            result=measure.convert_libreoffice(b'synthetic-source','F01.docx','version-b',root,inputs,runner=no_output)
            self.assertEqual('failed',result['status'])
            self.assertEqual(b'%PDF-old-result',old.read_bytes())

if __name__=='__main__':
    unittest.main()
