import sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from cycle_probe import prove
from ooxml_gate import paragraphs,parse,W
class CycleProofTest(unittest.TestCase):
 def test_residual_yz_is_not_a_successful_reversal(self):
  original=['Heading','Body'];edited=['XYZHeading','Body']
  self.assertTrue(prove(original,edited,original)['complete_body_cycle'])
  failed=prove(original,edited,['YZHeading','Body'])
  self.assertTrue(failed['exact_insertion']);self.assertFalse(failed['exact_reversal']);self.assertFalse(failed['complete_body_cycle'])
  self.assertFalse(prove(original,['XYZHeading','Changed'],original)['complete_body_cycle'])
  self.assertFalse(prove(original,edited,original+['Extra'])['complete_body_cycle'])
 def test_body_control_markers_are_not_ignored(self):
  import cycle_probe
  extract=getattr(cycle_probe,'body_text',lambda xml:[p['text'] for p in paragraphs(parse(xml))])
  def body(inner):return ('<w:document xmlns:w="'+W[1:-1]+'"><w:body><w:p><w:r>'+inner+'</w:r></w:p></w:body></w:document>').encode()
  original=extract(body('<w:t>Heading</w:t>'))
  clean=extract(body('<w:t>XYZHeading</w:t>'))
  for marker in ('<w:tab/>','<w:br/>','<w:br w:type="page"/>','<w:cr/>'):
   edited=extract(body('<w:t>XYZ</w:t>'+marker+'<w:t>Heading</w:t>'))
   reverted=extract(body(marker+'<w:t>Heading</w:t>'))
   self.assertFalse(prove(original,edited,original)['exact_insertion'])
   self.assertFalse(prove(original,clean,reverted)['exact_reversal'])
if __name__=='__main__':unittest.main()
