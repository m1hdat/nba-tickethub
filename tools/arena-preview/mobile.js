/* HTML simulation of Android Phase 3/4. No Android build, lifecycle, booking or network API. */
(() => {
    'use strict';
    const $ = id => document.getElementById(id), source = window.MobileArenaSource;
    source.games=window.MatchList.schedule(source.games);
    const matchFilters=window.MatchList.defaults();
    let homeScroll=0,matchTimer,detailGame,detailOrigin='home',arenaOrigin='home';
    const pageScrolls=new Map();
    const renderer = window.MobileArenaRenderer, canvas = $('arena-canvas'), ctx = canvas.getContext('2d');
    const money = minor => new Intl.NumberFormat('vi-VN', {style:'currency',currency:'USD'}).format(minor/100);
    const selectedColor = '#117149', dataCache = new Map(), requests = new Map(), pointers = new Map(), cells = new Map();
    const camera = {scale:1,min:1,max:1,x:0,y:0};
    let game, theme, data, paths, model, visible, sectionCounts, minSeat = 1;
    let route = 'home', width = 390, height = 560, lod = 0, drawnCamera, drawnModel;
    let loaded = false, loading = false, generation = 0, ready = Promise.resolve(), queued = false;
    let sheetExpanded = false, sheetDock = 'bottom', helperIndex = -1, helperPath, focusPath, selectionPaths = [];
    const scrolls=new Map(); let scrollKey;
    let tapStart, tapAllowed = false, gesture, feedbackTimer;

    function ink(color) {
        const c = color.slice(1).match(/../g).map(x => parseInt(x,16)/255).map(x => x<=.04045?x/12.92:((x+.055)/1.055)**2.4);
        return c[0]*.2126+c[1]*.7152+c[2]*.0722>.179?'#000000':'#ffffff';
    }
    function node(tag, text, className) {
        const element = document.createElement(tag);
        if (text !== undefined) element.textContent = text;
        if (className) element.className = className;
        return element;
    }
    for (const key of ['available','booked','locked']) $(key).style.background = source.seatStatusPalette[key];
    const heading=id=>['bos','nyk','lal','phi','sas'].includes(id)?'Georgia,serif':['bkn','chi','mia','tor','lac'].includes(id)?'Arial Black,system-ui':'Impact,system-ui';
    const factor=id=>85+((window.PreviewSelection.hash(id)%56)+56)%56;
    function badge(team){
        const image=team.teamBadgeUrl||team.logoImage?.dataUrl;if(image){const mark=node('img');mark.src=image;mark.alt=team.name;mark.className='team-badge';return mark;}
        const mark=node('span',team.teamId.toUpperCase(),'team-badge fallback');mark.style.setProperty('--badge-team',team.primaryColor);mark.setAttribute('aria-label',team.name+' · Mã đội');return mark;
    }
    function renderMatches(){
        window.clearTimeout(matchTimer);
        const now=new Date(),list=window.MatchList.filter(source.games,matchFilters,now),cards=[];
        const chosen=source.themes.find(t=>t.teamId===matchFilters.team);
        $('match-team').replaceChildren(...(chosen?[badge(chosen),node('span',chosen.name)]:[node('span','Tất cả đội')]));
        $('match-home-label').hidden=!chosen;$('match-home-only').checked=matchFilters.homeOnly;
        $('match-seven').setAttribute('aria-pressed',String(matchFilters.range==='seven'));$('match-today').setAttribute('aria-pressed',String(matchFilters.range==='today'));
        $('match-day').value=matchFilters.day;
        const range=({seven:'7 ngày tới (gồm hôm nay)',today:'Hôm nay',day:matchFilters.day,all:'Mọi trận sắp tới'})[matchFilters.range];
        $('match-summary').textContent=range+' · '+(chosen?chosen.name:'Tất cả đội')+' · '+(matchFilters.homeOnly?'Chỉ sân nhà':'Nhà & khách')+' · '+Intl.DateTimeFormat().resolvedOptions().timeZone;
        $('match-empty').hidden=list.length>0;
        let lastDay;
        for(const match of list){
            const day=window.MatchList.key(new Date(match.startsAt));
            if(day!==lastDay){const title=node('h2',new Intl.DateTimeFormat('vi-VN',{weekday:'long',day:'numeric',month:'numeric',year:'numeric'}).format(match.startsAt),'match-day-heading');cards.push(title);lastDay=day;}
            const card=poster(match,'Chi tiết trận',()=>openDetail(match.id));
            card.children[card.children.length-1].id='game-'+match.id;
            card.addEventListener('click',()=>openDetail(match.id));cards.push(card);
        }
        const scroll=$('schedule').scrollTop;$('games').replaceChildren(...cards);$('schedule').scrollTop=scroll;
        const next=source.games.filter(g=>g.startsAt>now.getTime()).reduce((time,g)=>Math.min(time,g.startsAt-now.getTime()+1),60000);
        matchTimer=window.setTimeout(renderMatches,Math.max(1,next));
        renderLanding();
    }
    $('match-seven').addEventListener('click',()=>{matchFilters.range='seven';matchFilters.day='';renderMatches();});
    $('match-today').addEventListener('click',()=>{matchFilters.range='today';matchFilters.day='';renderMatches();});
    $('match-day').addEventListener('change',()=>{if($('match-day').value){matchFilters.range='day';matchFilters.day=$('match-day').value;renderMatches();}});
    $('match-clear').addEventListener('click',()=>{Object.assign(matchFilters,window.MatchList.defaults());renderMatches();});
    $('match-home-only').addEventListener('change',()=>{matchFilters.homeOnly=$('match-home-only').checked;renderMatches();});
    $('match-team').addEventListener('click',()=>$('team-dialog').showModal());
    $('close-teams').addEventListener('click',()=>$('team-dialog').close());
    for(const team of [null,...[...source.themes].sort((a,b)=>a.name.localeCompare(b.name))]){
        const button=node('button');if(team)button.append(badge(team));button.append(node('span',team?team.name:'Tất cả đội'));
        button.addEventListener('click',()=>{window.MatchList.selectTeam(matchFilters,team?.teamId||'');renderMatches();$('team-dialog').close();});$('team-options').append(button);
    }
    renderMatches();
    window.addEventListener('focus',renderMatches);
    function startingPrice(match) {
        return Math.min(...match.sections.map(v=>Math.floor((Math.floor((v.mockBasePriceMinor*match.priceFactorPercent+50)/100)*factor(match.id)+50)/100)));
    }
    function poster(match,action,open){
        const home=source.themes.find(t=>t.teamId===match.homeTeamId),away=source.themes.find(t=>t.teamId===match.awayTeamId),card=node('article',undefined,'game-card poster');
        card.dataset.matchId=match.id;
        card.style.setProperty('--away-color',away.primaryColor);card.style.setProperty('--home-color',home.primaryColor);
        const header=node('header');header.append(node('span','Lịch mẫu'));
        const pair=node('div',undefined,'match-pair');
        for(const team of [away,home]){const column=node('div');column.append(badge(team),node('h2',team.name));pair.append(column);if(team===away)pair.append(node('span','@','versus'));}
        const price=node('strong','Từ '+money(startingPrice(match)),'poster-price');
        card.append(header,pair,node('p',match.displayTime),node('p',match.venue),price);
        if(action){const button=node('button',action);button.disabled=match.startsAt<=Date.now();button.addEventListener('click',event=>{event.stopPropagation?.();open();});card.append(button);}
        return card;
    }
    function renderLanding(){
        const upcoming=source.games.filter(g=>g.startsAt>Date.now()).sort((a,b)=>a.startsAt-b.startsAt);
        $('featured-match').replaceChildren();$('upcoming-matches').replaceChildren();
        upcoming.slice(0,4).forEach((match,index)=>{
            const card=poster(match,index===0?'Chọn ghế':'Chi tiết trận',()=>index===0?selectGame(match.id):openDetail(match.id));
            // The poster artwork opens detail; the explicit CTA has its own direct-seat action.
            card.addEventListener('click',()=>openDetail(match.id));
            (index===0?$('featured-match'):$('upcoming-matches')).append(card);
        });
        if(!upcoming.length)$('featured-match').append(node('p','Không có trận sắp tới.'));
    }
    function chooseTeam(id){window.MatchList.selectTeam(matchFilters,id);homeScroll=0;pageScrolls.set('schedule',0);navigate('schedule');}
    for(const container of ['team-grid','home-team-grid'])for(const team of [...source.themes].sort((a,b)=>a.name.localeCompare(b.name))){
        const button=node('button',undefined,'team-tile');button.dataset.teamId=team.teamId;button.append(badge(team),node('span',team.name));button.addEventListener('click',()=>chooseTeam(team.teamId));$(container).append(button);
    }
    function openDetail(id){
        const match=source.games.find(g=>g.id===id);if(!match)return;
        if(route!=='detail')detailOrigin=route;
        if(detailGame?.id!==id)pageScrolls.set('detail',0);
        detailGame=match;
        $('detail-poster').replaceChildren(poster(match));
        $('detail-price').textContent='Từ '+money(startingPrice(match));
        $('detail-seats').disabled=match.startsAt<=Date.now();
        $('detail').style.setProperty('--detail-team',source.themes.find(t=>t.teamId===match.homeTeamId).primaryColor);
        navigate('detail');
    }
    function back(){navigate(route==='arena'?arenaOrigin:route==='detail'?detailOrigin:'home');}
    function navigate(name) {
        if(!['home','schedule','teams','detail','arena'].includes(name))throw Error('Unknown page');
        pageScrolls.set(route,(route==='detail'?$('detail-scroll'):$(route)).scrollTop);
        route=name;
        for(const id of ['home','schedule','teams','detail','arena'])$(id).hidden=route!==id;
        $('browse-nav').hidden=route==='arena'||route==='detail';
        for(const id of ['home','schedule','teams'])$('nav-'+id).setAttribute('aria-current',route===id?'page':'false');
        if(route==='arena')resize();
        else {if(route==='schedule')renderMatches();if(route==='home')renderLanding();(route==='detail'?$('detail-scroll'):$(route)).scrollTop=pageScrolls.get(route)||0;}
        $(route==='detail'?'detail-title':route==='arena'?'arena-title':route==='teams'?'teams-title':route==='schedule'?'schedule-title':'home-title').focus({preventScroll:true});
    }
    for(const page of ['home','schedule','teams'])$('nav-'+page).addEventListener('click',()=>navigate(page));
    $('all-matches').addEventListener('click',()=>navigate('schedule'));
    $('detail-back').addEventListener('click',back);$('arena-back').addEventListener('click',back);
    $('detail-seats').addEventListener('click',()=>{if(detailGame)selectGame(detailGame.id);});
    $('choose-game').addEventListener('click',()=>navigate('schedule'));
    window.ArenaPreviewData = {receive(value) {
        const pending = requests.get(value.id);
        if (!pending) return;
        const match = source.games.find(g => g.arenaId === value.id);
        if (!match || value.geometrySha256 !== match.geometrySha256) pending.reject(Error('Geometry snapshot không khớp.'));
        else { dataCache.set(value.id,value); pending.resolve(value); }
    }};
    function payload(match) {
        if (dataCache.has(match.arenaId)) return Promise.resolve(dataCache.get(match.arenaId));
        if (requests.has(match.arenaId)) return requests.get(match.arenaId).promise;
        let resolve,reject;
        const promise = new Promise((yes,no)=>{resolve=yes;reject=no;});
        requests.set(match.arenaId,{promise,resolve,reject});
        const script = document.createElement('script'); script.src = match.dataScript;
        script.onload = () => {
            if (!dataCache.has(match.arenaId)) reject(Error('Payload không chứa dữ liệu sân.'));
            requests.delete(match.arenaId); script.remove();
        };
        script.onerror = () => {
            requests.delete(match.arenaId); script.remove();
            reject(Error('Không tải được '+match.dataScript+'. Giữ thư mục preview/arena-data/ rồi thử lại.'));
        };
        document.head.append(script);
        return promise;
    }
    function selectGame(id, retry = false) {
        const match = source.games.find(g => g.id === id);
        if (!match) throw Error('Unknown mock game');
        if(match.startsAt<=Date.now())return Promise.reject(Error('Trận đã bắt đầu'));
        if(route!=='arena')arenaOrigin=route;
        if (!retry && game?.id === id && (loaded || loading)) { navigate('arena'); return ready; }
        const previous=model,oldGame=game,sameArena=loaded&&game?.arenaId===match.arenaId, savedCamera={...camera};
        const previousExpanded=sheetExpanded;
        const version = ++generation;
        game = match; theme = source.themes.find(t=>t.teamId===game.homeTeamId);
        document.documentElement.style.setProperty('--team',theme.primaryColor);
        document.documentElement.style.setProperty('--ink',ink(theme.primaryColor));
        $('arena-title').textContent = game.awayTeamId.toUpperCase()+' @ '+game.homeTeamId.toUpperCase();
        $('arena-title').style.fontFamily=heading(game.homeTeamId);
        document.documentElement.style.setProperty('--team-light',theme.primaryColor+'0c');
        $('arena-subtitle').textContent = game.venue+' · '+game.displayTime+' · Lịch mẫu';
        data = model = paths = null; loaded = false; loading = true; lod = 0; helperIndex = -1;
        pointers.clear(); gesture = null; tapAllowed = false; sheetExpanded = false;
        $('load-state').hidden = false; $('load-state').textContent = 'Đang tải sơ đồ hiện có…'; $('retry').hidden = true;
        $('level-filter').disabled = $('section-filter').disabled = true;
        $('seat-sheet').classList.add('collapsed'); $('sheet-body').hidden = $('sheet-actions').hidden = true;
        $('selection-count').textContent = 'Đang tải trận mẫu'; $('selection-total').textContent = game.venue;
        $('cancel-change').hidden = true; $('feedback').hidden = true;
        navigate('arena');
        ready = payload(match).then(value => {
            if (version !== generation) return;
            model=new window.PreviewSelection(value,game);
            if(sameArena&&previous){for(const key of ['levelId','sectionId','category','budget','browsing'])model[key]=previous[key];}
            data={...value,renderKey:game.id,sections:value.sections.map(v=>({...v,label:v.label.replace(/\*/g,'')})),seats:value.seats.map(seat=>{const copy=seat.slice();copy[2]=model.info(seat[0]).status;return copy;}),batches:[]};
            for(const batch of value.batches){const groups=[[],[],[]];for(const index of batch.slice(1))groups[data.seats[index][2]].push(index);groups.forEach((indices,status)=>{if(indices.length)data.batches.push([status,...indices]);});}
            paths=renderer.prepare(data);
            buildHitIndex(); loaded = true; loading = false;
            $('load-state').hidden = true;
            $('level-filter').disabled = $('section-filter').disabled = false;
            renderFilters(true);if(sameArena){limits();Object.assign(camera,savedCamera);clamp();}else fit();renderUI();showSheet(sameArena&&previousExpanded);
        }).catch(error => {
            if (version !== generation) return;
            loading = false; $('load-state').textContent = error.message; $('retry').hidden = false;
        });
        return ready;
    }
    $('retry').addEventListener('click',()=>selectGame(game.id,true));

    function buildHitIndex() {
        minSeat = Infinity; cells.clear(); sectionCounts = new Map();
        data.seats.forEach((seat,index) => {
            const q = seat[3], xs = [q[0],q[2],q[4],q[6]], ys = [q[1],q[3],q[5],q[7]];
            for(let v=0;v<4;v++) { const n=(v+1)%4; minSeat=Math.min(minSeat,Math.hypot(q[2*v]-q[2*n],q[2*v+1]-q[2*n+1])); }
            for(let x=Math.floor(Math.min(...xs)/24);x<=Math.floor(Math.max(...xs)/24);x++) {
                for(let y=Math.floor(Math.min(...ys)/24);y<=Math.floor(Math.max(...ys)/24);y++) {
                    const key=x+':'+y; if(!cells.has(key))cells.set(key,[]); cells.get(key).push(index);
                }
            }
            const section = model.rows.get(seat[1])[1];
            if (!sectionCounts.has(section)) sectionCounts.set(section,{total:0,available:0});
            const counts=sectionCounts.get(section); counts.total++; if(seat[2]===0)counts.available++;
        });
    }
    function hitWorld(x,y) {
        if (!loaded || !Number.isFinite(x) || !Number.isFinite(y)) return -1;
        const candidates=cells.get(Math.floor(x/24)+':'+Math.floor(y/24))||[];
        for(const index of candidates) {
            const q=data.seats[index][3]; let positive=false,negative=false;
            for(let v=0;v<4;v++) { const n=(v+1)%4;
                const cross=(q[2*n]-q[2*v])*(y-q[2*v+1])-(q[2*n+1]-q[2*v+1])*(x-q[2*v]);
                positive ||= cross>0; negative ||= cross<0;
            }
            if(!(positive&&negative)) return index;
        }
        return -1;
    }
    function sectionOptions() {
        const options=[node('option','Mọi khu')]; options[0].value='';
        for(const section of game.sections) if(!model.levelId||section.levelId===model.levelId) {
            const option=node('option',section.code); option.value=section.id; options.push(option);
        }
        $('section-filter').replaceChildren(...options); $('section-filter').value=model.sectionId||'';
    }
    function renderFilters(all = false) {
        if(all) {
            const options=[node('option','Mọi tầng')]; options[0].value='';
            for(const level of game.levels) { const option=node('option',level.code); option.value=level.id; options.push(option); }
            $('level-filter').replaceChildren(...options);
        }
        $('level-filter').value=model.levelId||''; sectionOptions();
    }
    $('level-filter').addEventListener('change',()=>{if(model){model.filterLevel($('level-filter').value||null);renderFilters();renderUI();}});
    $('section-filter').addEventListener('change',()=>{if(model){model.filterSection($('section-filter').value||null);renderUI();}});
    $('clear-filter').addEventListener('click',()=>{if(model){model.clearFilter();renderFilters();renderUI();}});
    function describe(id) { const info=model.info(id);return 'Khu '+info.sectionCode+' · Hàng '+info.rowLabel+' · Ghế '+info.number; }
    function rebuildOverlays() {
        selectionPaths=[];
        const groups=new Map();
        for(const id of model.selected) {
            const info=model.info(id); let group=groups.get(info.sectionId);
            if(!group||group.count===256) { group={path:new Path2D(),count:0,sectionId:info.sectionId}; groups.set(info.sectionId,group);selectionPaths.push(group); }
            group.path.addPath(renderer.polygon(data.seats[info.index][3]));group.count++;
        }
        const focus=model.candidate||model.focused;focusPath=focus&&model.matches(focus)?renderer.polygon(data.seats[model.indices.get(focus)][3]):null;
    }
    function renderUI() {
        if(!model)return;
        visible=model.visibleSections();rebuildOverlays();
        $('type-filter').value=model.category||'';$('budget-filter').value=String(model.budget);
        $('filter-status').textContent=model.replacing?'Đang đổi ghế · Giữ ghế cũ đến khi xác nhận':'Dữ liệu mẫu · Chạm khu để xem góc nhìn và vé';
        const section=model.sections.get(model.browsing),tickets=model.browsing?model.tickets():[],candidate=model.candidate?model.info(model.candidate):null;
        $('seat-sheet').hidden=!section;
        const available=tickets.filter(id=>model.info(id).status===0),minimum=available.length?Math.min(...available.map(id=>model.info(id).priceMinor)):null;
        $('selection-count').textContent=section?'Khu '+section.code+' · '+(minimum===null?'Không có vé phù hợp':'Từ '+money(minimum)):'Chạm khu';
        $('selection-total').textContent=model.selected.size+' ghế · '+money(model.totalMinor());
        $('cancel-change').hidden=!model.replacing;
        $('seat-title').textContent=candidate?describe(candidate.id):'';$('seat-title').hidden=!candidate;
        $('seat-meta').hidden=$('seat-price').hidden=$('seat-status').hidden=true;
        $('change-seat').disabled=!model.selected.has(model.focused)||!!model.replacing;
        $('change-seat').style.visibility=model.selected.size?'visible':'hidden';
        $('toggle-seat').disabled=!candidate||candidate.status!==0||!model.matches(candidate.id)||model.selected.has(candidate.id);
        $('toggle-seat').style.visibility=candidate?'visible':'hidden';$('toggle-seat').textContent=candidate&&model.selected.has(candidate.id)?'Đã chọn':'Chọn ghế';
        $('open-view').disabled=!model.representative();$('clear-selection').disabled=!model.selected.size;
        $('selection-outside').textContent=tickets.length+' vé trong khu · '+model.outsideCount()+' đã chọn ngoài lọc';
        const nextKey=game.arenaId+':'+model.browsing+':'+model.category+':'+model.budget;
        if(scrollKey)scrolls.set(scrollKey,$('selected-list').scrollTop);
        const oldScroll=scrolls.get(nextKey)||0;scrollKey=nextKey;
        const list=tickets.map(id=>{
            const info=model.info(id),li=node('li');li.classList.toggle('candidate',id===model.candidate);li.classList.toggle('unavailable',info.status!==0);
            const row=node('button',describe(id),'seat-row');row.setAttribute('aria-pressed',String(id===model.candidate));
            row.append(node('small',({VIP:'VIP',STANDARD:'Tiêu chuẩn',STANDING:'Khu đứng'})[info.kind]+' · '+['Trống','Đã bán','Khóa'][info.status]),node('strong',money(info.priceMinor)));
            row.addEventListener('click',()=>{model.preview(id);renderUI();});li.append(row);
            if(model.selected.has(id)){const remove=node('button','Bỏ','remove-row');remove.setAttribute('aria-label','Bỏ '+describe(id));remove.addEventListener('click',()=>{model.remove(id);renderUI();});li.append(remove);}
            return li;
        });
        if(!list.length)list.push(node('li','Không có vé khớp bộ lọc.'));
        $('selected-list').replaceChildren(...list);$('selected-list').scrollTop=oldScroll;
        const minima=new Map();for(const seat of data.seats){const info=model.info(seat[0]);if(info.status===0&&model.matches(info.id))minima.set(info.sectionId,Math.min(minima.get(info.sectionId)??Infinity,info.priceMinor));}
        data.sections.forEach((v,i)=>{v.label=v.code?.replace('SIM-','')||model.sections.get(v.id).code.replace('SIM-','');if(i%Math.max(1,Math.floor(data.sections.length/4))===0&&minima.has(v.id))v.label+=' · '+money(minima.get(v.id));});
        drawPerspective(false);schedule();
    }
    $('type-filter').addEventListener('change',()=>{if(model){model.category=$('type-filter').value||null;renderUI();}});
    $('budget-filter').addEventListener('change',()=>{if(model){model.budget=Number($('budget-filter').value);renderUI();}});
    function sectionAt(x,y){
        let nearest=-1,distance=Infinity;
        data.sections.forEach((s,i)=>{if(!visible.has(s.id))return;const dx=(x-s.labelPoint[0])*camera.scale,dy=(y-s.labelPoint[1])*camera.scale;
            if(Math.abs(dx)<=35&&Math.abs(dy)<=12&&dx*dx+dy*dy<distance){nearest=i;distance=dx*dx+dy*dy;}});
        if(nearest>=0)return nearest;
        return data.sections.findIndex(s=>{if(!visible.has(s.id))return false;const p=s.polygon;let inside=false;
            for(let a=0,b=p.length-2;a<p.length;b=a,a+=2)if((p[a+1]>y)!==(p[b+1]>y)&&x<(p[b]-p[a])*(y-p[a+1])/(p[b+1]-p[a+1])+p[a])inside=!inside;return inside;});
    }
    function feedback(text) {
        window.clearTimeout(feedbackTimer);$('feedback').hidden=false;$('feedback').textContent=text;
        feedbackTimer=window.setTimeout(()=>$('feedback').hidden=true,2600);
    }
    function inspectAt(x,y) {
        if(!loaded||x<0||x>=width||y<0||y>=height-64)return;
        if(drawnModel!==model||!drawnCamera||['scale','x','y'].some(k=>drawnCamera[k]!==camera[k]))return;
        const wx=(x-camera.x)/camera.scale,wy=(y-camera.y)/camera.scale,section=sectionAt(wx,wy);
        if(section>=0&&(lod!==2||data.sections[section].id!==model.browsing)){model.browse(data.sections[section].id);renderUI();showSheet(true);return;}
        const index=lod===2?hitWorld(wx,wy):-1;
        if(index>=0&&model.matches(data.seats[index][0])){model.preview(data.seats[index][0]);renderUI();showSheet(true);}
    }
    function showSheet(expand) {
        sheetExpanded=expand;
        sheetDock='bottom';$('seat-sheet').dataset.dock=sheetDock;
        $('seat-sheet').classList.toggle('collapsed',!expand);
        $('seat-sheet').style.setProperty('--sheet-height',Math.min(670,$('arena').clientHeight-72)+'px');
        $('sheet-body').hidden=$('sheet-actions').hidden=!expand;
        $('summary-button').setAttribute('aria-expanded',String(expand));$('toggle-sheet').textContent=expand?'Thu gọn':'Chi tiết';
        if(expand)drawPerspective(false);
    }
    $('toggle-sheet').addEventListener('click',()=>showSheet(!sheetExpanded));
    $('summary-button').addEventListener('click',()=>showSheet(!sheetExpanded));
    $('change-seat').addEventListener('click',()=>{if(model){model.beginReplace();renderUI();showSheet(true);}});
    $('cancel-change').addEventListener('click',()=>{if(model){model.cancelReplace();renderUI();showSheet(true);}});
    $('toggle-seat').addEventListener('click',()=>{if(model){const result=model.confirm();if(result==='UNAVAILABLE')feedback('Vé đã bán hoặc khóa. Giữ nguyên ghế cũ.');renderUI();}});
    $('clear-selection').addEventListener('click',()=>{if(model){model.clearSelection();renderUI();}});

    function drawPerspective(large) {
        const target=$(large?'large-perspective':'seat-perspective');
        if(!data||!model)return;
        const id=model.representative();const info=id?model.info(id):null;
        const w=target.getBoundingClientRect().width||width,h=w*9/16;
        window.SeatPerspectivePreview.draw(target,data,info,theme,w,h);
    }
    $('open-view').addEventListener('click',()=>{
        if(!model?.representative())return;
        $('large-view-title').textContent=describe(model.representative());$('view-dialog').showModal();drawPerspective(true);
    });
    $('close-view').addEventListener('click',()=>$('view-dialog').close());
    function resize() {
        if(route!=='arena')return;
        const r=canvas.getBoundingClientRect(),w=r.width||390,h=r.height||560;
        if(w===width&&h===height)return;
        const center=loaded?[(width/2-camera.x)/camera.scale,((height-64)/2-camera.y)/camera.scale,camera.scale/camera.min]:null;
        width=w;height=h;
        if(loaded){limits();camera.scale=Math.max(camera.min,Math.min(camera.max,camera.min*center[2]));camera.x=width/2-center[0]*camera.scale;camera.y=(height-64)/2-center[1]*camera.scale;clamp();showSheet(sheetExpanded);schedule();}
    }
    function limits(){camera.min=.9*Math.min(width/data.world[0],(height-64)/data.world[1]);camera.max=Math.max(camera.min,24/minSeat);}
    function clamp(){const axis=(offset,world,view)=>world*camera.scale<=view?(view-world*camera.scale)/2:Math.max(view-world*camera.scale,Math.min(0,offset));camera.x=axis(camera.x,data.world[0],width);camera.y=axis(camera.y,data.world[1],height-64);}
    function fit(){if(!loaded)return;limits();camera.scale=camera.min;camera.x=(width-data.world[0]*camera.scale)/2;camera.y=(height-64-data.world[1]*camera.scale)/2;clamp();schedule();}
    function zoom(factor,oldX=width/2,oldY=(height-64)/2,newX=oldX,newY=oldY){if(!loaded)return;const x=(oldX-camera.x)/camera.scale,y=(oldY-camera.y)/camera.scale;camera.scale=Math.max(camera.min,Math.min(camera.max,camera.scale*factor));camera.x=newX-x*camera.scale;camera.y=newY-y*camera.scale;clamp();schedule();}
    function center(index){const q=data.seats[index][3];return [(q[0]+q[2]+q[4]+q[6])/4,(q[1]+q[3]+q[5]+q[7])/4];}
    function screenForSeat(index){const p=center(index);return [p[0]*camera.scale+camera.x,p[1]*camera.scale+camera.y];}
    function focusSeat(index){if(!loaded)return;const p=screenForSeat(index);zoom(camera.max/camera.scale,p[0],p[1],width/2,height*.25);helperIndex=index;helperPath=renderer.polygon(data.seats[index][3]);$('review-tools').open=false;schedule();}
    function locate(kind){if(!model)return;const candidates=data.seats.map((s,index)=>({s,index,info:model.info(s[0])})).filter(v=>model.matches(v.s[0])&&v.info.status===(kind==='booked'?1:0));
        if(!candidates.length){feedback('Bộ lọc hiện tại không có ghế phù hợp. Thử Xóa lọc.');return;}
        const order=kind==='high'?Math.max(...candidates.map(v=>v.info.levelOrder)):Math.min(...candidates.map(v=>v.info.levelOrder));
        const target=candidates.find(v=>v.info.levelOrder===order&&!model.selected.has(v.s[0]))||candidates.find(v=>v.info.levelOrder===order);
        model.browse(target.info.sectionId);if(kind==='booked')model.preview(target.info.id);renderUI();showSheet(true);
    }
    $('focus-low').addEventListener('click',()=>locate('low'));$('focus-high').addEventListener('click',()=>locate('high'));$('focus-booked').addEventListener('click',()=>locate('booked'));
    $('zoom-in').addEventListener('click',()=>zoom(2));$('zoom-out').addEventListener('click',()=>zoom(.5));$('fit').addEventListener('click',fit);
    window.addEventListener('resize',resize);window.addEventListener('arena-assets-ready',schedule);
    if(typeof ResizeObserver!=='undefined')new ResizeObserver(resize).observe($('arena-stage'));
    function schedule(){if(!queued){queued=true;requestAnimationFrame(()=>{queued=false;draw();});}}
    function draw(){if(!loaded||route!=='arena')return;
        const dpr=Math.min(window.devicePixelRatio||1,2),w=Math.round(width*dpr),h=Math.round(height*dpr);
        if(canvas.width!==w||canvas.height!==h){canvas.width=w;canvas.height=h;}
        ctx.setTransform(dpr,0,0,dpr,0,0);ctx.fillStyle=data.stands.background;ctx.fillRect(0,0,width,height);
        ctx.save();ctx.beginPath();ctx.rect(0,0,width,height-64);ctx.clip();ctx.setTransform(dpr*camera.scale,0,0,dpr*camera.scale,dpr*camera.x,dpr*camera.y);
        const size=minSeat*camera.scale;
        if(lod===0&&size>=6*1.15)lod=1;if(lod===1&&size>=16*1.15)lod=2;
        if(lod===2&&size<16*.85)lod=1;if(lod===1&&size<6*.85)lod=0;
        renderer.drawArena(ctx,data,paths,theme,camera.scale,lod,visible);
        if(model.browsing){const index=data.sections.findIndex(s=>s.id===model.browsing);if(index>=0){ctx.strokeStyle=selectedColor;ctx.lineWidth=3/camera.scale;ctx.stroke(paths.sections[index]);}}
        if(lod===2){
            for(const group of selectionPaths)if(visible.has(group.sectionId)){ctx.strokeStyle='#fff';ctx.lineWidth=5/camera.scale;ctx.stroke(group.path);ctx.strokeStyle=selectedColor;ctx.lineWidth=3/camera.scale;ctx.stroke(group.path);}
            if(focusPath){ctx.strokeStyle='#fff';ctx.lineWidth=5/camera.scale;ctx.stroke(focusPath);ctx.strokeStyle='#142d52';ctx.lineWidth=2/camera.scale;ctx.stroke(focusPath);}
            if(helperIndex>=0&&model.matches(data.seats[helperIndex][0])&&!model.selected.has(data.seats[helperIndex][0])){ctx.setLineDash([3/camera.scale,2/camera.scale]);ctx.strokeStyle='#bc5d16';ctx.lineWidth=2/camera.scale;ctx.stroke(helperPath);ctx.setLineDash([]);}
        }
        ctx.restore();drawnCamera={...camera};drawnModel=model;
        $('zoom-label').textContent=camera.scale<=camera.min*1.01?'Toàn sân':Math.round(camera.scale/camera.min)+'×';
        $('tap-hint').textContent='Chạm khu để xem góc nhìn và vé · Kéo/pinch để khám phá';
    }
    function local(event){const r=canvas.getBoundingClientRect();return{x:event.clientX-r.left,y:event.clientY-r.top};}
    function baseline(){const p=[...pointers.values()];gesture=!p.length?null:p.length===1?{...p[0]}:{x:(p[0].x+p[1].x)/2,y:(p[0].y+p[1].y)/2,span:Math.hypot(p[1].x-p[0].x,p[1].y-p[0].y)};}
    canvas.addEventListener('pointerdown',event=>{if(!loaded||(event.button!==undefined&&event.button!==0))return;const p=local(event);if(p.y>=height-64)return;pointers.set(event.pointerId,p);canvas.setPointerCapture(event.pointerId);if(pointers.size===1){tapStart={...p,time:event.timeStamp};tapAllowed=true;}else tapAllowed=false;baseline();});
    canvas.addEventListener('pointermove',event=>{if(!pointers.has(event.pointerId))return;const p=local(event);pointers.set(event.pointerId,p);const old=gesture;if(Math.hypot(p.x-tapStart.x,p.y-tapStart.y)>7)tapAllowed=false;
        for(const sample of event.getCoalescedEvents?.()||[]){const s=local(sample);if(Math.hypot(s.x-tapStart.x,s.y-tapStart.y)>7)tapAllowed=false;}
        baseline();if(pointers.size>1){tapAllowed=false;if(old.span>0&&gesture.span>0)zoom(gesture.span/old.span,old.x,old.y,gesture.x,gesture.y);}else if(!tapAllowed){camera.x+=gesture.x-old.x;camera.y+=gesture.y-old.y;clamp();schedule();}});
    function finish(event,cancelled){if(!pointers.has(event.pointerId))return;const p=local(event),single=pointers.size===1;pointers.delete(event.pointerId);if(cancelled||!single)tapAllowed=false;
        if(!cancelled&&single&&tapAllowed&&Math.hypot(p.x-tapStart.x,p.y-tapStart.y)<=7&&event.timeStamp-tapStart.time<500)inspectAt(p.x,p.y);baseline();if(!pointers.size)tapAllowed=false;}
    canvas.addEventListener('pointerup',e=>finish(e,false));canvas.addEventListener('pointercancel',e=>finish(e,true));
    canvas.addEventListener('lostpointercapture',e=>{pointers.delete(e.pointerId);tapAllowed=false;baseline();});
    canvas.addEventListener('wheel',event=>{if(!loaded)return;event.preventDefault();const p=local(event);zoom(Math.exp(-event.deltaY*.003),p.x,p.y);},{passive:false});
    canvas.addEventListener('keydown',event=>{if(event.key==='+'||event.key==='=')zoom(2);else if(event.key==='-')zoom(.5);else if(event.key==='0')fit();else if(event.key==='Enter')inspectAt(width/2,height*.25);else return;event.preventDefault();});
    window.MobilePreview={selectGame,navigate,openDetail,chooseTeam,back,startingPrice,focusSeat,inspectAt,hitWorld,screenForSeat,whenReady:()=>ready,
        state:()=>({matchFilters:{...matchFilters},visibleMatches:window.MatchList.filter(source.games,matchFilters).map(g=>g.id),route,detailMatchId:detailGame?.id,arenaOrigin,loaded,gameId:game?.id,arenaId:game?.arenaId,themeId:theme?.id,homeTeamId:game?.homeTeamId,camera:{...camera},lod,helperIndex,sheetExpanded,sheetDock,
            selected:model?[...model.selected]:[],focused:model?.focused,replacing:model?.replacing,levelId:model?.levelId,sectionId:model?.sectionId,browsing:model?.browsing,candidate:model?.candidate,ticketCount:model?.browsing?model.tickets().length:0,totalMinor:model?.totalMinor(),info:model?.focused?model.info(model.focused):null})};
    navigate('home');
})();
