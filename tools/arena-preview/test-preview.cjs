/* Host-side integration with real exported arena data. DOM/Canvas doubles do not prove pixels. */
'use strict';
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const here=__dirname, elements=new Map(),frames=[];
const context2d=new Proxy({measureText:t=>({width:String(t).length*6})},{get:(o,k)=>o[k]||(()=>{}),set:(o,k,v)=>(o[k]=v,true)});
class Element {
 constructor(id){this.id=id;this.hidden=false;this.disabled=false;this.style={setProperty(k,v){this[k]=v;}};this.children=[];this.listeners=new Map();this.dataset={};this.scrollTop=0;this.clientHeight=id==='arena'?720:500;this.value='';this.classes=new Set();this.classList={add:k=>this.classes.add(k),toggle:(k,on)=>on?this.classes.add(k):this.classes.delete(k)};}
 append(...children){this.children.push(...children);} replaceChildren(...children){this.children=children;} addEventListener(k,f){this.listeners.set(k,f);} setAttribute(k,v){this[k]=v;} click(){if(!this.disabled)this.listeners.get('click')?.({});} focus(){} remove(){} setPointerCapture(){} getContext(){return context2d;} getBoundingClientRect(){return{left:0,top:0,width:390,height:500};} showModal(){this.open=true;}close(){this.open=false;}
}
const html=fs.readFileSync(path.join(here,'index.html'),'utf8');
for(const [,id] of html.matchAll(/\bid="([^"]+)"/g))elements.set(id,new Element(id));
let sandbox;
const document={getElementById:id=>{assert.ok(elements.has(id),'Missing DOM: '+id);return elements.get(id);},createElement:tag=>new Element(tag),documentElement:new Element('root'),head:{append(script){vm.runInContext(fs.readFileSync(path.resolve(here,script.src),'utf8'),sandbox);script.onload();}}};
const storage=new Map();
const window={localStorage:{getItem:k=>storage.get(k)||null,setItem:(k,v)=>storage.set(k,v)},document,devicePixelRatio:1,innerHeight:800,addEventListener(){},dispatchEvent(){},setTimeout:()=>0,clearTimeout(){}};
sandbox=vm.createContext({window,document,console,Intl,Math,Number,Map,Set,Promise,Event:class{},Image:class{},requestAnimationFrame:f=>frames.push(f),Path2D:class{moveTo(){}lineTo(){}closePath(){}ellipse(){}arc(){}addPath(){}}});
for(const file of ['source-data.js','team-logos.js','arena-renderer.js','selection-model.js','seat-perspective.js','match-list.js','booking-store.js','seat-finder.js','mobile.js'])vm.runInContext(fs.readFileSync(path.join(here,file),'utf8'),sandbox,{filename:file});
const source=window.MobileArenaSource,$=id=>elements.get(id),app=window.MobilePreview,plain=v=>JSON.parse(JSON.stringify(v));
function flush(){let count=0;while(frames.length){assert.ok(count++<100);frames.shift()();}}
function payload(id){const text=fs.readFileSync(path.resolve(here,source.games.find(g=>g.arenaId===id).dataScript),'utf8');return JSON.parse(text.slice('window.ArenaPreviewData.receive('.length,-2));}
(async()=>{
 assert.equal(source.games.length,33);assert.equal(new Set(source.games.map(g=>g.homeTeamId)).size,30);
 assert.equal(app.state().route,'home');assert.equal($('team-grid').children.length,30);
 app.navigate('tickets');assert.ok($('my-tickets').children[0].textContent.includes('chưa có vé'));app.navigate('home');
 for(const team of source.themes){assert.ok(fs.existsSync(path.resolve(here,team.teamBadgeUrl)));assert.ok(!team.logoImage||Array.isArray(team.logoImage.bounds),'Poster marks must not overwrite the court logo contract');}
 assert.equal($('featured-match').children[0].dataset.matchId,'sample-nyk-bos');
 $('featured-match').children[0].click();assert.equal(app.state().route,'detail');assert.equal(app.state().detailMatchId,'sample-nyk-bos');
 $('detail-seats').click();await app.whenReady();flush();assert.equal(app.state().arenaId,'msg');assert.equal(app.state().homeTeamId,'nyk');
 app.back();assert.equal(app.state().route,'detail');app.back();assert.equal(app.state().route,'home');
 $('featured-match').children[0].children.at(-1).click();await app.whenReady();flush();assert.equal(app.state().arenaOrigin,'home');app.back();assert.equal(app.state().route,'home');
 app.chooseTeam('nyk');assert.equal(app.state().route,'schedule');assert.equal(app.state().matchFilters.range,'all');assert.ok(app.state().visibleMatches.includes('sample-bos'));
 $('schedule').scrollTop=280;app.openDetail('sample-bos');$('detail-seats').click();await app.whenReady();flush();
 const awayMatch=source.games.find(g=>g.id==='sample-bos');assert.equal(app.state().arenaId,awayMatch.arenaId);assert.equal(app.state().homeTeamId,'bos');
 app.back();app.back();assert.equal(app.state().route,'schedule');assert.equal($('schedule').scrollTop,280);assert.equal(app.state().matchFilters.team,'nyk');
 for(const game of source.games)assert.ok(fs.existsSync(path.resolve(here,game.dataScript)));
 assert.equal(source.games[0].arenaId,source.games[1].arenaId);assert.notEqual(source.games[0].awayTeamId,source.games[1].awayTeamId);
 const list=window.MatchList,now=new Date(),state=list.defaults();
 const visible=list.filter(source.games,state,now);assert.ok(visible.length);assert.ok(visible.every(g=>g.startsAt>now.getTime()));assert.ok(!visible.some(g=>g.id==='sample-started'));
 state.range='today';const today=list.filter(source.games,state,now);assert.equal(today.length,2);assert.ok(today.every(g=>list.key(new Date(g.startsAt))===list.key(now)));
 list.selectTeam(state,'nyk');assert.equal(state.range,'all');const team=list.filter(source.games,state,now);assert.ok(team.some(g=>g.id==='sample-nyk-lal-future'));assert.ok(team.some(g=>g.awayTeamId==='nyk'));
 state.homeOnly=true;assert.ok(list.filter(source.games,state,now).every(g=>g.homeTeamId==='nyk'));
 state.range='day';state.day=list.key(new Date(source.games[1].startsAt));assert.deepEqual(plain(list.filter(source.games,state,now).map(g=>g.id)),['sample-nyk-mia']);
 state.day='1900-01-01';assert.equal(list.filter(source.games,state,now).length,0);
 $('match-clear').click();$('match-today').click();assert.equal(app.state().visibleMatches.length,2);$('match-clear').click();assert.equal(app.state().matchFilters.range,'seven');
 const raw=payload('msg'),first=new window.PreviewSelection(raw,source.games[0]),second=new window.PreviewSelection(raw,source.games[1]);
 const available=raw.seats.find(s=>first.info(s[0]).status===0)[0],blocked=raw.seats.find(s=>first.info(s[0]).status!==0)[0];
 first.preview(available);assert.equal(first.selected.size,0);assert.equal(first.confirm(),'CHANGED');first.beginReplace();first.preview(blocked);assert.equal(first.confirm(),'UNAVAILABLE');assert.ok(first.selected.has(available));first.cancelReplace();
 assert.notEqual(first.info(available).priceMinor,second.info(available).priceMinor);
 first.category='VIP';first.budget=10000;for(const id of first.tickets()){assert.equal(first.info(id).kind,'VIP');assert.ok(first.info(id).priceMinor<=10000);}
 await app.selectGame(source.games[0].id);flush();assert.equal(app.state().loaded,true);
 const section=raw.sections[0],camera={...app.state().camera};
 app.inspectAt(section.labelPoint[0]*camera.scale+camera.x,section.labelPoint[1]*camera.scale+camera.y);flush();
 assert.equal(app.state().browsing,section.id);assert.ok(app.state().ticketCount>0);assert.equal(app.state().selected.length,0);assert.deepEqual(plain(app.state().camera),camera);
 const model=new window.PreviewSelection(raw,source.games[0]);model.browse(section.id);const tickets=model.tickets(),index=tickets.findIndex(id=>model.info(id).status===0);
 $('selected-list').children[index].children[0].click();flush();assert.equal(app.state().candidate,tickets[index]);assert.equal(app.state().selected.length,0);assert.equal($('toggle-seat').disabled,false);
 $('toggle-seat').click();flush();assert.deepEqual(plain(app.state().selected),[tickets[index]]);
 $('open-view').click();assert.equal($('view-dialog').open,true);$('close-view').click();assert.equal($('view-dialog').open,false);
 $('type-filter').value='VIP';$('type-filter').listeners.get('change')();flush();
 const saved={...app.state().camera};await app.selectGame(source.games[1].id);flush();assert.equal(app.state().browsing,section.id);assert.equal($('type-filter').value,'VIP');assert.equal(app.state().selected.length,0);assert.deepEqual(plain(app.state().camera),saved);
 await app.selectGame(source.games[2].id);flush();assert.equal(app.state().arenaId,'chase');assert.equal(app.state().homeTeamId,'gsw');assert.equal(app.state().browsing,null);assert.equal(app.state().camera.scale,app.state().camera.min);
 // Phase 6A: run the actual preview buttons and retain localStorage across reloading its store module.
 await app.selectGame(source.games[0].id);flush();
 app.inspectAt(section.labelPoint[0]*app.state().camera.scale+app.state().camera.x,section.labelPoint[1]*app.state().camera.scale+app.state().camera.y);flush();
 $('selected-list').children[index].children[0].click();$('toggle-seat').click();flush();
 const selected=plain(app.state().selected);assert.equal(selected.length,1);
 $('booking-continue').click();assert.equal($('booking-dialog').open,true);$('booking-back').click();assert.deepEqual(plain(app.state().selected),selected);
 $('booking-continue').click();await Promise.all([app.confirmBooking(),app.confirmBooking()]);flush();
 assert.equal(window.PreviewBookings.list().length,1);assert.equal(app.state().selected.length,0);assert.ok($('booking-message').textContent.includes('Mã đơn'));
 $('booking-confirm').click();assert.equal(app.state().route,'tickets');assert.equal($('my-tickets').children.length,1);
 const order=window.PreviewBookings.list()[0];
 vm.runInContext(fs.readFileSync(path.join(here,'booking-store.js'),'utf8'),sandbox);
 assert.equal(window.PreviewBookings.list()[0].id,order.id);
 const reloaded=new window.PreviewSelection(raw,source.games[0]);assert.equal(reloaded.info(selected[0]).status,1);
 const freeIds=raw.seats.map(s=>s[0]).filter(id=>reloaded.info(id).status===0).slice(0,2);
 const partial={...order,id:'SIM-partial',tickets:[{seatId:freeIds[0],label:'free'},{seatId:selected[0],label:'busy'}]};
 assert.throws(()=>window.PreviewBookings.save(partial,id=>reloaded.info(id)),/busy/);assert.equal(window.PreviewBookings.list().length,1);assert.equal(reloaded.info(freeIds[0]).status,0);
 const sameSeat=raw.seats.map(s=>s[0]).find(id=>reloaded.info(id).status===0&&second.info(id).status===0);
 window.PreviewBookings.save({...order,id:'SIM-a',tickets:[{seatId:sameSeat}]},id=>reloaded.info(id));
 window.PreviewBookings.save({...order,id:'SIM-b',matchId:source.games[1].id,tickets:[{seatId:sameSeat}]},id=>second.info(id));
 assert.equal(window.PreviewBookings.list().length,3);
 // 6B: run the shared panel with actual exported geometry and recheck competing bookings.
 await app.selectGame(source.games[0].id);flush();
 app.openFinder();$('finder-people').value='3';$('finder-budget').value='500';
 const groups=app.findSeats();assert.equal(groups.length,3);assert.equal(groups[0].ids.length,3);
 const m=new window.PreviewSelection(raw,source.games[0]);
 for(const group of groups){
   assert.equal(group.total,group.ids.reduce((v,id)=>v+m.info(id).priceMinor,0));
   const row=raw.seats[m.indices.get(group.ids[0])][1];
   for(let i=0;i<group.ids.length;i++){
     const seat=raw.seats[m.indices.get(group.ids[i])];assert.equal(seat[1],row);assert.equal(m.info(seat[0]).status,0);
     if(i)assert.ok(window.SeatFinder.adjacent(m,raw.seats[m.indices.get(group.ids[i-1])],seat));
   }
 }
 const firstCard=$('finder-results').children[0];firstCard.children[0].click();flush();assert.equal($('view-dialog').open,true);assert.equal(app.state().candidate,groups[0].ids[0]);$('close-view').click();
 firstCard.children[3].click();flush();assert.equal(app.state().selected.length,3);assert.equal($('finder-panel').hidden,true);
 $('booking-continue').click();assert.equal($('booking-dialog').open,true);assert.equal($('booking-content').children[0].children.length>0,true);$('booking-back').click();
 app.openFinder();$('finder-people').value='1';$('finder-budget').value='';$('finder-priority').value='price';
 const cheap=app.findSeats();assert.equal(cheap.length,3);assert.ok(cheap[0].total<=cheap[1].total);
 $('finder-priority').value='court';const near=app.findSeats();assert.ok(near[0].distance<=cheap[0].distance);assert.ok(near[0].distance<=near[1].distance);
 // Even consecutive seat numbers cannot cross a geometrically missing slot.
 const a=raw.seats[0],third=raw.seats[2],gap=[a[0]+':gap',a[1],0,third[3]];
 assert.equal(window.SeatFinder.adjacent(m,a,gap),false);
 const otherSection=raw.seats.find(s=>m.info(s[0]).sectionId!==m.info(a[0]).sectionId);assert.equal(window.SeatFinder.adjacent(m,a,otherSection),false);
 const previous=plain(app.state().selected);
 app.openFinder();$('finder-people').value='3';const proposal=app.findSeats()[0],button=$('finder-results').children[0].children[3];
 window.PreviewBookings.save({...order,id:'SIM-finder-conflict',matchId:source.games[0].id,tickets:[{seatId:proposal.ids[1]}]},id=>m.info(id));
 button.click();assert.deepEqual(plain(app.state().selected),previous);assert.ok($('finder-message').textContent.includes('Giữ nguyên'));
 assert.ok(app.findSeats().every(g=>!g.ids.includes(proposal.ids[1])));
 $('finder-budget').value='0';assert.equal(app.findSeats().length,0);assert.equal($('finder-budget').value,'0');assert.equal($('finder-people').value,'3');
 $('finder-budget').value='';$('finder-people').value='1000';assert.equal(app.findSeats().length,0);
 $('finder-people').value='1.5';assert.equal(app.findSeats().length,0);
 const nextModel=new window.PreviewSelection(raw,source.games[1]);
 const q={people:3,budget:Infinity,priority:'price'};
 const otherGroups=window.SeatFinder.search(nextModel,q);assert.ok(otherGroups.length);assert.ok(otherGroups.every(g=>g.matchId===source.games[1].id&&g.ids.every(id=>nextModel.info(id).status===0)));
 assert.equal(window.SeatFinder.select(nextModel,proposal),false);
 console.log('PASS: Phase 4–6A regression and 6B price/court, 3 seats, aisle geometry, totals, preview, atomic conflict, empty/invalid and independent matches. DOM/Canvas/storage doubles only.');
})().catch(error=>{console.error(error);process.exitCode=1;});
