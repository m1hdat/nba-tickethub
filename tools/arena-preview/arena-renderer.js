// Adapted from preview/arena-preview.template.html; original paths, palette, court renderer.
// Coordinates come from the existing ../../preview/arena-data/ payloads, never a new seat generator.
(() => {
'use strict';
const source=window.MobileArenaSource, style=source.style;
const colors=['available','booked','locked'].map(key=>source.seatStatusPalette[key]);
const cache=new Map(),courtCache=new Map();
let preparations=0,galleryDirty=false;
const hidden=()=>false,neutral=()=>false;
function schedule(){window.dispatchEvent(new Event('arena-assets-ready'));}
function remember(map,id,value){map.set(id,value);while(map.size>2)map.delete(map.keys().next().value);}
const polygon=(points,close=true)=>{const path=new Path2D();path.moveTo(points[0],points[1]);for(let i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);if(close)path.closePath();return path;};
function artPath(p){if(p.type!=='ellipse')return polygon(p.points,p.type==='polygon');const [x,y,r,b]=p.points,path=new Path2D();path.ellipse((x+r)/2,(y+b)/2,(r-x)/2,(b-y)/2,0,0,2*Math.PI);return path;}
const linePaths={lines:new Path2D(),rims:new Path2D()};
for(const p of source.courtMarkings)(p.part==='key'?linePaths.rims:linePaths.lines).addPath(artPath(p));
function prepareCourt(t){
 if(courtCache.has(t.id))return courtCache.get(t.id);
 const result={paths:t.court.design.primitives.map(artPath),image:null};
 if(t.logoImage){const img=new Image();img.onload=()=>{result.image=img;galleryDirty=true;schedule();};img.src=t.logoImage.dataUrl;}
 courtCache.set(t.id,result);return result;
}
function prepare(a){
 const key=a.renderKey||a.id;if(cache.has(key))return cache.get(key);
 const markerPaths=(inset)=>a.batches.map(batch=>{const path=new Path2D();for(const index of batch.slice(1)){const [x,y,r]=a.seats[index][4];path.moveTo(x+r*inset,y);path.arc(x,y,r*inset,0,2*Math.PI);}return path;});
 const result={envelope:polygon(a.envelope),sections:a.sections.map(s=>polygon(s.polygon)),rows:a.rows.map(r=>polygon(r[2],false)),
  seats:a.batches.map(batch=>{const p=new Path2D();for(const index of batch.slice(1))p.addPath(polygon(a.seats[index][3]));return p;}),markers:markerPaths(1),dots:markerPaths(.72),
  rowSections:a.rows.map(r=>r[1]),batchSections:a.batches.map(batch=>a.rows.find(r=>r[0]===a.seats[batch[1]][1])[1]),
  batchLevels:a.batches.map(batch=>{const row=a.seats[batch[1]][1],section=a.rows.find(r=>r[0]===row)[1];return a.sections.find(s=>s.id===section).levelOrder;})};
 remember(cache,key,result);preparations++;return result;
}
function court(ctx,t){
 const c=t.court,prepared=prepareCourt(t),n=neutral(),wood=n?'#e4cc9d':c.wood;
 const fill=(color,x,y,w,h)=>{ctx.fillStyle=color;ctx.fillRect(x,y,w,h);};
 const part=name=>{for(let i=0;i<prepared.paths.length;i++){const p=c.design.primitives[i];if(p.part!==name)continue;
  const color=n?({wood:'#c2a67a',decor:'#b39a75',key:'#c5b17d',border:'#b5a17d'}[name]):p.color;
  if(p.strokeWidth){ctx.strokeStyle=color;ctx.lineWidth=p.strokeWidth;ctx.stroke(prepared.paths[i]);}else{ctx.fillStyle=color;ctx.fill(prepared.paths[i]);}}};
 fill(n?'#293d50':c.apron,-4,-4,102,58);part('border');fill(wood,0,0,94,50);
 ctx.save();ctx.beginPath();ctx.rect(0,0,94,50);ctx.clip();part('wood');part('decor');
 fill(n?'#586c80':c.paint,0,17,19,16);fill(n?'#586c80':c.paint,75,17,19,16);part('key');
 if(!hidden()){
  const mark=c.design.logo;
  if(prepared.image&&t.logoImage){const [x,y,r,b]=t.logoImage.bounds;ctx.drawImage(prepared.image,x,y,r-x,b-y);}
  else{ctx.fillStyle=c.center;ctx.font=`bold ${Math.min(mark.width/3.3,mark.height/2.5)}px system-ui`;ctx.textAlign='center';ctx.textBaseline='middle';ctx.fillText(mark.fallback,47,25);ctx.textBaseline='alphabetic';}
 }
 // Final line passes are above every team graphic and logo, with a dark visibility keyline.
 ctx.strokeStyle='#273a48';ctx.lineWidth=.7;ctx.stroke(linePaths.lines);
 ctx.strokeStyle=n?'#ffffff':c.lines;ctx.lineWidth=.34;ctx.stroke(linePaths.lines);
 ctx.strokeStyle='#e46b35';ctx.stroke(linePaths.rims);ctx.restore();
 if(!hidden()){ctx.fillStyle=c.lettering;ctx.font='bold 2.1px system-ui';ctx.textAlign='center';for(let side=0;side<2;side++){ctx.save();ctx.translate(side?96:-2,25);ctx.rotate(side?Math.PI/2:-Math.PI/2);ctx.fillText(c.baselineText,0,.7);ctx.restore();}}
}
function surface(target,w,h){
 const dpr=Math.min(window.devicePixelRatio||1,2,Math.sqrt(16000000/(w*h)));target.width=Math.round(w*dpr);target.height=Math.round(h*dpr);target.style.width=w+'px';target.style.height=h+'px';return {ctx:target.getContext('2d'),dpr};
}
function renderCourt(target,t,width,height,zoom=100){
 const scale=Math.min((width-24)/102,(height-24)/58)*zoom/100,w=zoom===100?width:102*scale+24,h=zoom===100?height:58*scale+24;
 const {ctx,dpr}=surface(target,w,h);target.style.background='#f5f7fa';
 ctx.setTransform(scale*dpr,0,0,scale*dpr,(w-94*scale)/2*dpr,(h-50*scale)/2*dpr);court(ctx,t);
}
function drawArena(ctx,a,p,t,scale,detail,visible=null){const stands=a.stands;
 ctx.fillStyle=stands.concourse;ctx.fill(p.envelope);
 for(let i=0;i<p.sections.length;i++){if(visible&&!visible.has(a.sections[i].id))continue;const tier=t.seatPalette[a.sections[i].levelOrder-1];ctx.fillStyle=tier.section;ctx.fill(p.sections[i]);ctx.strokeStyle=stands.sectionBorder;ctx.lineWidth=style.sectionStroke+1;ctx.stroke(p.sections[i]);ctx.strokeStyle=tier.accent;ctx.lineWidth=style.sectionStroke;ctx.stroke(p.sections[i]);}
 ctx.strokeStyle=stands.rowLine;ctx.lineWidth=style.rowStroke;if(detail>=1)for(let i=0;i<p.rows.length;i++)if(!visible||visible.has(p.rowSections[i]))ctx.stroke(p.rows[i]);
 if(detail>=2)for(let i=0;i<p.seats.length;i++){if(visible&&!visible.has(p.batchSections[i]))continue;ctx.fillStyle=t.seatPalette[p.batchLevels[i]-1].seatBackground;ctx.fill(p.seats[i]);ctx.fillStyle='#ffffff';ctx.fill(p.markers[i]);ctx.fillStyle=colors[a.batches[i][0]];ctx.fill(p.dots[i]);}
 ctx.textAlign='center';const fontSize=11/scale;ctx.font=`600 ${fontSize}px system-ui`;
 for(const s of a.sections){if(visible&&!visible.has(s.id))continue;const [x,y]=s.labelPoint,half=ctx.measureText(s.label).width/2+fontSize*.25;ctx.fillStyle=stands.labelBackground;ctx.beginPath();ctx.roundRect(x-half,y-fontSize*.65,half*2,fontSize*1.3,fontSize*.25);ctx.fill();ctx.fillStyle=stands.labelText;ctx.fillText(s.label,x,y+fontSize*.34);}
 ctx.save();ctx.translate(a.world[0]/2,a.world[1]/2);ctx.rotate(a.court[2]*Math.PI/180);ctx.scale(a.court[0]/94,a.court[1]/50);ctx.translate(-47,-25);court(ctx,t);ctx.restore();
}
window.MobileArenaRenderer={prepare,renderCourt,drawArena,polygon,court};
})();
