const assert=require('node:assert/strict');
const {converged}=require('../scripts/focal_diagnostics.cjs');
assert.equal(converged([{at:0,count:6,fingerprint:'a'},{at:4000,count:6,fingerprint:'a'}],3000),true);
assert.equal(converged([{at:0,count:6,fingerprint:'a'},{at:4000,count:6,fingerprint:'b'}],3000),false);
assert.equal(converged([{at:0,count:6,fingerprint:'a'},{at:1000,count:6,fingerprint:'a'}],3000),false);
assert.equal(converged([{at:0,count:0,fingerprint:'a'},{at:4000,count:0,fingerprint:'a'}],3000),false);
assert.equal(converged([{at:0,count:6,fingerprint:'a'},{at:1000,count:6,fingerprint:'b'},{at:2000,count:6,fingerprint:'a'},{at:6000,count:6,fingerprint:'a'}],3000),true);
console.log('5 focal convergence controls passed');
