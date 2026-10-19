/* Read-only focal instrumentation. No SDK model mutation. Detailed outputs stay private. */
function converged(samples, minimumMs) {
  if (samples.length < 2) return false;
  const last=samples.at(-1);
  if (!(last.count>0)) return false;
  let start=samples.length-1;
  while(start>0 && samples[start-1].count===last.count && samples[start-1].fingerprint===last.fingerprint)start--;
  return start>=0 && samples.slice(start).every(s=>s.count===last.count && s.fingerprint===last.fingerprint) && last.at-samples[start].at>=minimumMs;
}
async function pagination(page,out) {
  const fs=require('node:fs'),path=require('node:path');const samples=[];const started=Date.now();let stable=false;
  while(Date.now()-started<30000) {
    const sample=await page.locator('#editor').evaluate(root=>{
      const pages=[...root.querySelectorAll('.superdoc-page')];
      return {count:pages.length,fingerprint:JSON.stringify(pages.map(p=>[p.getBoundingClientRect().width,p.getBoundingClientRect().height,p.innerText]))};
    });samples.push({at:Date.now()-started,...sample});
    if(converged(samples,3000)){stable=true;break;}await page.waitForTimeout(500);
  }
  const detail=await page.locator('#editor').evaluate(root=>{
    function box(el){const r=el.getBoundingClientRect(),s=getComputedStyle(el);return {x:r.x,y:r.y,width:r.width,height:r.height,font:s.font,fontFamily:s.fontFamily,fontSize:s.fontSize,lineHeight:s.lineHeight,marginTop:s.marginTop,marginBottom:s.marginBottom,padding:s.padding,transform:s.transform,zoom:s.zoom};}
    return {locale:navigator.language,languages:navigator.languages,userAgent:navigator.userAgent,devicePixelRatio,viewport:[innerWidth,innerHeight],nativeCount:root.dataset.pageCount,events:JSON.parse(root.dataset.paginationEvents||'[]'),fonts:[...document.fonts].map(f=>({family:f.family,status:f.status,weight:f.weight})),fontChecks:['Times New Roman','Arial','Calibri'].map(f=>({family:f,available:document.fonts.check(`12px "${f}"`)})),pages:[...root.querySelectorAll('.superdoc-page')].map((p,i)=>({page:i+1,attributes:[...p.attributes].map(a=>[a.name,a.value]),geometry:box(p),fragments:[...p.querySelectorAll('.superdoc-fragment,.superdoc-table-fragment')].map(f=>({text:f.innerText,attributes:[...f.attributes].map(a=>[a.name,a.value]),geometry:box(f),lines:[...f.querySelectorAll('.superdoc-line')].map(l=>({text:l.innerText,geometry:box(l),spans:[...l.querySelectorAll('span')].slice(0,30).map(s=>({text:s.innerText,geometry:box(s)}))}))}))}))};
  });
  const pages=page.locator('#editor .superdoc-page');
  for(let i=0;i<await pages.count();i++){
    await pages.nth(i).scrollIntoViewIfNeeded();await page.waitForTimeout(250);
    detail.pages[i]=await pages.nth(i).evaluate((p,index)=>{
      function box(el){const r=el.getBoundingClientRect(),s=getComputedStyle(el);return {x:r.x,y:r.y,width:r.width,height:r.height,fontFamily:s.fontFamily,fontSize:s.fontSize,lineHeight:s.lineHeight,marginTop:s.marginTop,marginBottom:s.marginBottom};}
      return {page:index+1,geometry:box(p),attributes:[...p.attributes].map(a=>[a.name,a.value]),fragments:[...p.querySelectorAll('.superdoc-fragment,.superdoc-table-fragment')].map(f=>({text:f.innerText,attributes:[...f.attributes].map(a=>[a.name,a.value]),geometry:box(f),lines:[...f.querySelectorAll('.superdoc-line')].map(l=>({text:l.innerText,geometry:box(l),spans:[...l.querySelectorAll('span')].slice(0,30).map(s=>({text:s.innerText,geometry:box(s)}))}))}))};
    },i);
    for(const edge of ['top','bottom']){
      await pages.nth(i).evaluate((p,edge)=>{const root=document.querySelector('#editor');const a=p.getBoundingClientRect(),b=root.getBoundingClientRect();root.scrollTop+=edge==='top'?a.top-b.top:a.bottom-b.bottom;},edge);
      await page.waitForTimeout(150);await page.locator('#editor').screenshot({path:path.join(out,`page-${i+1}-${edge}.png`)});
    }
  }
  fs.writeFileSync(path.join(out,'pagination.json'),JSON.stringify({stable,samples,detail},null,2));
  return {stable,count:detail.pages.length,nativeCount:detail.nativeCount};
}
async function focusSnapshot(page){
  const snapshots=[];
  for(const frame of page.frames()) snapshots.push(await frame.evaluate(()=>{
    const roots=[document];for(let i=0;i<roots.length;i++)for(const el of roots[i].querySelectorAll('*'))if(el.shadowRoot)roots.push(el.shadowRoot);
    return {url:location.origin+location.pathname,active:{tag:document.activeElement?.tagName,className:document.activeElement?.className},surfaces:roots.flatMap(r=>[...r.querySelectorAll('input,textarea,[contenteditable],canvas')]).map(el=>({tag:el.tagName,type:el.getAttribute('type'),className:el.className,editable:el.getAttribute('contenteditable'),disabled:el.disabled,rect:{x:el.getBoundingClientRect().x,y:el.getBoundingClientRect().y,width:el.getBoundingClientRect().width,height:el.getBoundingClientRect().height}})),text:document.body?.innerText.slice(0,15000)};
  }));return snapshots;
}
module.exports={converged,pagination,focusSnapshot};
