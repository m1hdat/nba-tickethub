/* HTML simulation of Android ArenaSelection; not Android ViewModel/lifecycle execution. */
(() => {
    'use strict';
    const hash = text => { let h=0; for(let i=0;i<text.length;i++)h=(Math.imul(h,31)+text.charCodeAt(i))|0; return h; };
    const mod=(n,d)=>((n%d)+d)%d;
    class PreviewSelection {
        constructor(data, game) {
            this.data = data; this.game = game;
            this.indices = new Map(data.seats.map((seat, i) => [seat[0], i]));
            this.rows = new Map(data.rows.map(row => [row[0], row]));
            this.sections = new Map(game.sections.map(section => [section.id, section]));
            this.levels = new Map(game.levels.map(level => [level.id, level]));
            this.selected = new Set(); this.focused = this.replacing = null;
            this.levelId = this.sectionId = this.browsing = this.candidate = this.category = null; this.budget=Infinity; this.infoCache=new Map();
        }
        info(id) {
            if(this.infoCache.has(id))return this.infoCache.get(id);
            const seat = this.data.seats[this.indices.get(id)], row = this.rows.get(seat[1]);
            const section = this.sections.get(row[1]), level = this.levels.get(section.levelId);
            const salt=hash(this.game.id), bucket=mod(salt^hash(id),100), rotated=(salt<<11)|(salt>>>21);
            const info={id, index: this.indices.get(id), sectionId: section.id, sectionCode: section.code,
                levelId: level.id, levelOrder: level.order, rowOrder: Number(row[0].match(/:R(\d+)$/)[1]),
                rowLabel: row[0].match(/:R\d+$/)[0].slice(1), number: Number(id.match(/:S(\d+)$/)[1]),
                status: bucket<8?2:bucket<26?1:0, kind:section.category==='STANDING'?'STANDING':mod(rotated^hash(id),7)===0?'VIP':section.category, currency: section.currencyCode,
                // Same integer half-up rule as MockArenaJsonReader, using existing mock manifest prices.
                priceMinor: Math.floor((Math.floor((section.mockBasePriceMinor * this.game.priceFactorPercent + 50) / 100)*(85+mod(salt,56))+50)/100)};
            this.infoCache.set(id,info);return info;
        }
        matches(id) {
            const info = this.info(id);
            return (!this.levelId || info.levelId === this.levelId) && (!this.sectionId || info.sectionId === this.sectionId) && (!this.category||info.kind===this.category) && info.priceMinor<=this.budget;
        }
        tap(id) {
            if (!id) return 'EMPTY';
            if (this.info(id).status !== 0) return 'UNAVAILABLE';
            if (!this.matches(id)) return 'OUTSIDE_FILTER';
            if (this.replacing) {
                if (id === this.replacing) { this.cancelReplace(); return 'CHANGED'; }
                if (this.selected.has(id)) return 'ALREADY_SELECTED';
                this.selected.delete(this.replacing); this.selected.add(id); this.replacing = null;
            } else if (!this.selected.delete(id)) this.selected.add(id);
            this.focused = id;
            return 'CHANGED';
        }
        browse(id) { if(this.sections.has(id)){this.browsing=id;this.candidate=null;} }
        preview(id) { this.candidate=id;this.browsing=this.info(id).sectionId; }
        confirm() { if(this.selected.has(this.candidate)&&!this.replacing)return 'ALREADY_SELECTED';return this.tap(this.candidate); }
        tickets() { return this.data.seats.map(s=>s[0]).filter(id=>this.info(id).sectionId===this.browsing&&this.matches(id)); }
        representative() { if(this.candidate)return this.candidate;const ids=this.tickets();return ids[Math.floor(ids.length/2)]||this.data.seats.find(s=>this.info(s[0]).sectionId===this.browsing)?.[0]; }
        beginReplace() { if (this.selected.has(this.focused)){this.replacing=this.focused;this.browsing=this.info(this.focused).sectionId;this.candidate=null;} }
        cancelReplace() { if (this.replacing) { this.focused = this.replacing; this.replacing = null; } }
        remove(id) {
            this.selected.delete(id);
            if (id === this.replacing) this.replacing = null;
            if (id === this.focused) this.focused = this.selected.values().next().value || null;
        }
        clearSelection() { this.selected.clear(); this.focused = this.replacing = null; }
        filterLevel(id) {
            if (id && !this.levels.has(id)) throw Error('Unknown level');
            this.levelId = id;
            if (this.sectionId && id && this.sections.get(this.sectionId).levelId !== id) this.sectionId = null;
        }
        filterSection(id) {
            if (id && (!this.sections.has(id) || (this.levelId && this.sections.get(id).levelId !== this.levelId))) throw Error('Invalid section');
            this.sectionId = id;
        }
        clearFilter() { this.levelId = this.sectionId = this.category = null; this.budget=Infinity; }
        focus(id) {
            if (!this.selected.has(id)) return;
            this.cancelReplace(); this.focused = id;
            if (!this.matches(id)) this.clearFilter();
        }
        totalMinor() { let value = 0; for (const id of this.selected) value += this.info(id).priceMinor; return value; }
        outsideCount() { let value = 0; for (const id of this.selected) if (!this.matches(id)) value++; return value; }
        visibleSections() {
            return new Set(this.data.seats.filter(s=>this.matches(s[0])).map(s=>this.info(s[0]).sectionId));
        }
    }
    PreviewSelection.hash=hash; window.PreviewSelection = PreviewSelection;
})();
