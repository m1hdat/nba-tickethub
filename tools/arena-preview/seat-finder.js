/* Search the exported Canvas geometry; inventory always belongs to model.game.id. */
(() => {
    'use strict';
    function center(seat) {
        const p=seat[3]; return [(p[0]+p[2]+p[4]+p[6])/4,(p[1]+p[3]+p[5]+p[7])/4];
    }
    function adjacent(model,a,b) {
        if(a[1]!==b[1])return false;
        const section=model.sections.get(model.rows.get(a[1])[1]);
        const envelope=model.data.envelope;
        let rx=0,ry=0;const cx=model.data.world[0]/2,cy=model.data.world[1]/2;
        for(let i=0;i<envelope.length;i+=2){rx=Math.max(rx,Math.abs(envelope[i]-cx));ry=Math.max(ry,Math.abs(envelope[i+1]-cy));}
        const x=center(a),y=center(b),ax=(x[0]-cx)/rx,ay=(x[1]-cy)/ry,bx=(y[0]-cx)/rx,by=(y[1]-cy)/ry;
        const angle=Math.abs(Math.atan2(ax*by-ay*bx,ax*bx+ay*by)*180/Math.PI);
        const pitch=(section.endDegrees-section.startDegrees)/section.seatsPerRow;
        return angle>pitch*.5&&angle<pitch*1.5;
    }
    function search(model,q) {
        if(!Number.isSafeInteger(q.people)||q.people<1||q.budget<0||Number.isNaN(q.budget))throw Error('Nhập số người từ 1 và ngân sách hợp lệ.');
        const best=[];let run=[],previous;
        const cx=model.data.world[0]/2,cy=model.data.world[1]/2,halfX=model.data.court[0]/2,halfY=model.data.court[1]/2;
        const order=(a,b)=>(q.priority==='court'?a.distance-b.distance||a.total-b.total:a.total-b.total||a.distance-b.distance)||a.ids[0].localeCompare(b.ids[0]);
        for(const seat of model.data.seats){
            const info=model.info(seat[0]);
            const eligible=info.status===0&&!window.PreviewBookings?.booked(model.game.id,info.id)&&info.priceMinor<=q.budget
                &&(!q.levelId||q.levelId===info.levelId)&&(!q.sectionId||q.sectionId===info.sectionId)
                &&(!q.category||q.category===info.kind)&&(q.people===1||info.kind!=='STANDING');
            if(!eligible||previous&&!adjacent(model,previous,seat))run=[];
            if(eligible)run.push(seat);previous=seat;
            if(run.length>q.people)run.shift();
            if(run.length!==q.people)continue;
            const ids=run.map(s=>s[0]),total=ids.reduce((v,id)=>v+model.info(id).priceMinor,0);
            const distance=run.reduce((v,s)=>{const [x,y]=center(s);return v+Math.hypot(Math.max(0,Math.abs(x-cx)-halfX),Math.max(0,Math.abs(y-cy)-halfY));},0)/q.people;
            best.push({matchId:model.game.id,ids,total,distance});best.sort(order);if(best.length>3)best.pop();
        }
        return best;
    }
    function select(model,result) {
        if(result.matchId!==model.game.id||!result.ids.length||result.ids.some(id=>model.info(id).status!==0||window.PreviewBookings?.booked(result.matchId,id)))return false;
        for(const id of result.ids)model.selected.add(id);
        model.replacing=null;model.preview(result.ids[0]);model.focused=result.ids[0];return true;
    }
    window.SeatFinder={search,select,adjacent};
})();
