import tempfile
import unittest
from pathlib import Path

from b05_m2p3.pdf_inspector import compare_pdfs
from tests.support import make_pdf

try:
    import fitz
except ImportError:
    fitz = None


@unittest.skipUnless(fitz is not None, 'Requires PyMuPDF')
class PdfTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.base = Path(self.tmp.name)
        self.a = self.base / 'a.pdf'
        self.b = self.base / 'b.pdf'

    def test_identical_pixel_count(self):
        make_pdf(self.a)
        make_pdf(self.b)
        report = compare_pdfs(self.a, self.b)
        self.assertEqual(0, report['total_different_pixels'])
        self.assertEqual([1, 1], report['pages'])

    def test_changed_text_is_not_ignored(self):
        make_pdf(self.a)
        make_pdf(self.b, text='Novo')
        report = compare_pdfs(self.a, self.b)
        self.assertGreater(report['total_different_pixels'], 0)
        self.assertEqual(report['total_different_pixels'], report['per_page'][0]['different_pixels_outside_edit_region'])

    def test_roi_does_not_remove_pixels(self):
        make_pdf(self.a)
        make_pdf(self.b, text='Novo')
        report = compare_pdfs(self.a, self.b, edit_region={'page': 1, 'bbox_pt': [0, 0, 80, 80]})
        self.assertGreater(report['total_different_pixels'], 0)
        self.assertLessEqual(report['per_page'][0]['different_pixels_outside_edit_region'],
                             report['total_different_pixels'])

    def test_size_difference_incomplete(self):
        make_pdf(self.a)
        make_pdf(self.b, size=(220, 200))
        report = compare_pdfs(self.a, self.b)
        self.assertFalse(report['raster_comparable'])
        self.assertEqual('PAGE_SIZE_CHANGED', report['per_page'][0]['error'])

    def test_bad_region_denied(self):
        make_pdf(self.a)
        make_pdf(self.b)
        with self.assertRaisesRegex(ValueError, 'fora dos limites'):
            compare_pdfs(self.a, self.b, edit_region={'page': 1, 'bbox_pt': [0, 0, 900, 900]})

    def test_invalid_dpi_denied(self):
        make_pdf(self.a)
        make_pdf(self.b)
        with self.assertRaisesRegex(ValueError, 'DPI'):
            compare_pdfs(self.a, self.b, dpi=400)


if __name__ == '__main__':
    unittest.main()
