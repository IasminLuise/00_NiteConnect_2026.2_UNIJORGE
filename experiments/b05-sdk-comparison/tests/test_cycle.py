import sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from cycle_probe import prove
class CycleProofTest(unittest.TestCase):
 def test_residual_yz_is_not_a_successful_reversal(self):
  original=['Heading','Body'];edited=['XYZHeading','Body']
  self.assertTrue(prove(original,edited,original)['complete_body_cycle'])
  failed=prove(original,edited,['YZHeading','Body'])
  self.assertTrue(failed['exact_insertion']);self.assertFalse(failed['exact_reversal']);self.assertFalse(failed['complete_body_cycle'])
  self.assertFalse(prove(original,['XYZHeading','Changed'],original)['complete_body_cycle'])
  self.assertFalse(prove(original,edited,original+['Extra'])['complete_body_cycle'])
if __name__=='__main__':unittest.main()
