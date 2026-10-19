from pathlib import Path
import sys,tempfile,unittest
from zipfile import ZipFile
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from notes_probe import inventory_notes
from ooxml_gate import compare,inventory,W
from make_synthetic import make
class NotesTest(unittest.TestCase):
 def test_separators_are_not_regular_notes_and_real_content_is_not_ignored(self):
  with tempfile.TemporaryDirectory() as d:
   base=Path(d)/'base.docx';make(base)
   paths=[]
   for real in (False,True):
    target=Path(d)/f'notes-{real}.docx';paths.append(target)
    with ZipFile(base) as src,ZipFile(target,'w') as out:
     for name in src.namelist():out.writestr(name,src.read(name))
     note='<w:footnote w:id="1"><w:p><w:r><w:t>Fictional note</w:t></w:r></w:p></w:footnote>' if real else ''
     out.writestr('word/footnotes.xml','<w:footnotes xmlns:w="'+W[1:-1]+'"><w:footnote w:id="-1" w:type="separator"><w:p><w:r><w:separator/></w:r></w:p></w:footnote>'+note+'</w:footnotes>')
   self.assertEqual(0,inventory_notes(paths[0])['regular_note_count'])
   self.assertEqual(1,inventory_notes(paths[1])['regular_note_count'])
   self.assertEqual('FAIL_STRUCTURAL',compare(inventory(paths[1]),inventory(paths[0]))['status'])
   self.assertEqual('PASS_STRUCTURAL',compare(inventory(paths[1]),inventory(paths[1]))['status'])
if __name__=='__main__':unittest.main()
