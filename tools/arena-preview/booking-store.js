/* Preview-only persistence. Android uses SQLite transactions and unique keys instead. */
(() => {
    'use strict';
    const key='nba-tickethub-simulated-orders-v1';
    let cachedText, cachedOrders;
    function list(){
        const raw=window.localStorage.getItem(key)||'[]';
        if(raw===cachedText)return cachedOrders;
        const orders=JSON.parse(raw);
        if(!Array.isArray(orders))throw Error('Không đọc được dữ liệu vé mô phỏng.');
        cachedText=raw;cachedOrders=orders;
        return orders;
    }
    function booked(matchId,id){return list().some(o=>o.matchId===matchId&&o.tickets.some(t=>t.seatId===id));}
    function save(draft,info){
        const orders=list(),existing=orders.find(o=>o.id===draft.id);
        if(existing)return existing;
        const conflicts=draft.tickets.filter(t=>info(t.seatId).status!==0||orders.some(o=>o.matchId===draft.matchId&&o.tickets.some(s=>s.seatId===t.seatId)));
        if(conflicts.length)throw Error('Ghế không còn trống (Khu / Hàng / Ghế): '+conflicts.map(t=>t.label).join(', ')+'. Chưa lưu đơn này. Hãy chọn lại.');
        // One setItem publishes the complete order; a quota/write error publishes nothing.
        window.localStorage.setItem(key,JSON.stringify([draft,...orders]));
        return draft;
    }
    window.PreviewBookings={list,booked,save};
})();
