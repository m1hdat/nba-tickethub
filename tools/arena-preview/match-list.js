/* Civil dates in the device timezone, matching Android MatchListState. No UTC date slicing. */
(() => {
    'use strict';
    const key = value => [value.getFullYear(),String(value.getMonth()+1).padStart(2,'0'),String(value.getDate()).padStart(2,'0')].join('-');
    const atDay=(anchor,offset,hour)=>new Date(anchor.getFullYear(),anchor.getMonth(),anchor.getDate()+offset,hour,0,0,0);
    function schedule(definitions, anchor=new Date()) {
        const tomorrow=atDay(anchor,1,0).getTime();
        return definitions.map(game=>{
            const startsAt=game.remainingFraction ? anchor.getTime()+Math.max(1,Math.floor((tomorrow-anchor.getTime())*game.remainingFraction))
                : game.relativeMs ? anchor.getTime()+game.relativeMs : atDay(anchor,game.dayOffset,game.startHour).getTime();
            return {...game,startsAt,displayTime:new Intl.DateTimeFormat('vi-VN',{dateStyle:'short',timeStyle:'short'}).format(startsAt)};
        });
    }
    function defaults(){return {range:'seven',team:'',day:'',homeOnly:false};}
    function selectTeam(state,id){state.team=id;state.homeOnly=false;state.day='';state.range=id?'all':'seven';}
    function filter(games,state,now=new Date()) {
        const today=key(now),last=key(atDay(now,6,0));
        return games.filter(game=>{
            if(game.startsAt<=now.getTime())return false;
            if(state.team&&game.homeTeamId!==state.team&&(state.homeOnly||game.awayTeamId!==state.team))return false;
            const day=key(new Date(game.startsAt));
            return state.range==='all'||state.range==='today'&&day===today||state.range==='day'&&day===state.day
                ||state.range==='seven'&&day>=today&&day<=last;
        }).sort((a,b)=>a.startsAt-b.startsAt||a.id.localeCompare(b.id));
    }
    window.MatchList={key,atDay,schedule,defaults,selectTeam,filter};
})();
