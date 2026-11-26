package com.example.nbatickethub.state;

import com.example.nbatickethub.data.ArenaDataset;
import com.example.nbatickethub.model.*;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Domain state for one mock game. Blank/blocked taps never destroy the previous choice. */
public final class ArenaSelection {
    public enum Result { CHANGED, EMPTY, UNAVAILABLE, OUTSIDE_FILTER, ALREADY_SELECTED }
    private ArenaDataset dataset;
    private Game game;
    private final LinkedHashSet<String> selected = new LinkedHashSet<>();
    private String focusedId, replacingId, levelId, sectionId;
    private long totalMinor;
    private String browsingSection, candidateId, category;
    private long budget = Long.MAX_VALUE;
    private final java.util.Map<String, int[]> sectionCounts = new java.util.LinkedHashMap<>();

    public void bind(ArenaDataset value, Game match) {
        boolean same = game != null && game.id.equals(match.id) && game.arenaId.equals(match.arenaId)
                && dataset.arena.geometryVersion.equals(value.arena.geometryVersion);
        boolean sameArena = dataset != null && dataset.arena.id.equals(value.arena.id);
        dataset = value;
        game = match;
        sectionCounts.clear();
        for (Seat seat : value.seatOrder) {
            String section = value.rows.get(seat.rowId).sectionId;
            int[] counts = sectionCounts.computeIfAbsent(section, key -> new int[2]);
            counts[0]++;
            if (available(seat.id)) { counts[1]++; }
        }
        if (!same) {
            selected.clear(); focusedId = replacingId = candidateId = null;
            if (!sameArena) { levelId = sectionId = browsingSection = category = null; budget = Long.MAX_VALUE; }
        }
        selected.removeIf(id -> !available(id));
        if (focusedId != null && !available(focusedId)) { focusedId = selected.isEmpty() ? null : selected.iterator().next(); }
        if (replacingId != null && !selected.contains(replacingId)) { replacingId = null; }
        if (levelId != null && !dataset.levels.containsKey(levelId)) { levelId = sectionId = null; }
        if (sectionId != null && !dataset.sections.containsKey(sectionId)) { sectionId = null; }
        if (browsingSection != null && !dataset.sections.containsKey(browsingSection)) { browsingSection = candidateId = null; }
        recalculate();
    }

    public String browsingSection() { return browsingSection; }
    public String candidateId() { return candidateId; }
    public String category() { return category; }
    public long budget() { return budget; }
    public void filterTickets(String type, long max) { category = type; budget = max; }
    public void browseSection(String id) {
        if (!dataset.sections.containsKey(id)) { return; }
        browsingSection = id; candidateId = null;
    }
    public void preview(String id) {
        if (!dataset.seats.containsKey(id)) { return; }
        candidateId = id;
        browsingSection = dataset.rows.get(dataset.seats.get(id).rowId).sectionId;
    }
    public Result confirm() {
        if (candidateId == null) { return Result.EMPTY; }
        if (selected.contains(candidateId) && replacingId == null) { focusedId = candidateId; return Result.ALREADY_SELECTED; }
        return tap(candidateId);
    }
    public java.util.List<String> tickets() {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (browsingSection == null) { return result; }
        for (Seat seat : dataset.seatOrder) {
            if (browsingSection.equals(dataset.rows.get(seat.rowId).sectionId) && matches(seat.id)) { result.add(seat.id); }
        }
        return result;
    }
    public String representative() {
        if (candidateId != null) { return candidateId; }
        java.util.List<String> ids = tickets();
        if (!ids.isEmpty()) return ids.get(ids.size() / 2);
        for (Seat seat : dataset.seatOrder) {
            if (browsingSection != null && browsingSection.equals(dataset.rows.get(seat.rowId).sectionId)) return seat.id;
        }
        return null;
    }

    public Game game() { return game; }
    public ArenaDataset dataset() { return dataset; }
    public Set<String> selectedIds() { return Collections.unmodifiableSet(selected); }
    public String focusedId() { return focusedId; }
    public String replacingId() { return replacingId; }
    public String levelId() { return levelId; }
    public String sectionId() { return sectionId; }
    public long totalMinor() { return totalMinor; }
    public String currency() { return dataset.inventory.get(game.id).values().iterator().next().price.currencyCode; }
    public Section.Category ticketCategory(String id) {
        GameSeat ticket = offer(id);
        return ticket.ticketCategory != null ? ticket.ticketCategory
                : dataset.sections.get(dataset.rows.get(dataset.seats.get(id).rowId).sectionId).category;
    }
    public GameSeat offer(String id) { return dataset.inventory.get(game.id).get(id); }
    public boolean available(String id) {
        GameSeat offer = dataset == null || id == null ? null : offer(id);
        return offer != null && offer.baseStatus == SeatStatus.AVAILABLE;
    }

    public boolean matches(String id) {
        Seat seat = dataset.seats.get(id);
        if (seat == null) { return false; }
        Section section = dataset.sections.get(dataset.rows.get(seat.rowId).sectionId);
        return (levelId == null || levelId.equals(section.levelId))
                && (sectionId == null || sectionId.equals(section.id))
                && (category == null || category.equals(ticketCategory(id).name()))
                && offer(id).price.amountMinor <= budget;
    }

    public Result tap(String id) {
        if (id == null) { return Result.EMPTY; }
        if (!available(id)) { return Result.UNAVAILABLE; }
        if (!matches(id)) { return Result.OUTSIDE_FILTER; }
        if (replacingId != null) {
            if (replacingId.equals(id)) { cancelReplace(); return Result.CHANGED; }
            if (selected.contains(id)) { return Result.ALREADY_SELECTED; }
            selected.remove(replacingId);
            selected.add(id);
            replacingId = null;
        } else if (!selected.remove(id)) { selected.add(id); }
        focusedId = id;
        recalculate();
        return Result.CHANGED;
    }

    public void beginReplace() {
        if (focusedId != null && selected.contains(focusedId)) {
            replacingId = focusedId;
            browsingSection = dataset.rows.get(dataset.seats.get(focusedId).rowId).sectionId;
            candidateId = null;
        }
    }
    public void cancelReplace() {
        if (replacingId != null) { focusedId = replacingId; replacingId = null; }
    }
    public void remove(String id) {
        selected.remove(id);
        if (id.equals(replacingId)) { replacingId = null; }
        if (id.equals(focusedId)) { focusedId = selected.isEmpty() ? null : selected.iterator().next(); }
        recalculate();
    }
    public void clearSelection() { selected.clear(); focusedId = replacingId = null; totalMinor = 0; }
    /** Validate the complete proposal before mutating any previous selection. */
    public boolean selectSuggestion(SeatFinder.Suggestion suggestion, java.util.function.Predicate<String> stillAvailable) {
        if (!game.id.equals(suggestion.matchId) || suggestion.ids.isEmpty()) return false;
        for (String id : suggestion.ids) {
            if (!available(id) || !stillAvailable.test(id)) return false;
        }
        selected.addAll(suggestion.ids);
        replacingId = null;
        preview(suggestion.ids.get(0)); focusedId = candidateId;
        recalculate(); return true;
    }
    public void focus(String id) {
        if (!selected.contains(id)) { return; }
        cancelReplace();
        focusedId = id;
        if (!matches(id)) { clearFilter(); }
    }
    public void filterLevel(String id) {
        if (id != null && !dataset.levels.containsKey(id)) { throw new IllegalArgumentException("Unknown level"); }
        levelId = id;
        if (sectionId != null && id != null && !dataset.sections.get(sectionId).levelId.equals(id)) { sectionId = null; }
    }
    public void filterSection(String id) {
        if (id != null && (!dataset.sections.containsKey(id) || (levelId != null
                && !dataset.sections.get(id).levelId.equals(levelId)))) { throw new IllegalArgumentException("Invalid section"); }
        sectionId = id;
    }
    public void clearFilter() { levelId = sectionId = category = null; budget = Long.MAX_VALUE; }
    public int matchingCount(boolean availableOnly) {
        if (category != null || budget != Long.MAX_VALUE) {
            int matching = 0;
            for (Seat seat : dataset.seatOrder) if (matches(seat.id) && (!availableOnly || available(seat.id))) matching++;
            return matching;
        }
        int count = 0;
        for (java.util.Map.Entry<String, int[]> entry : sectionCounts.entrySet()) {
            Section section = dataset.sections.get(entry.getKey());
            if ((levelId == null || levelId.equals(section.levelId)) && (sectionId == null || sectionId.equals(section.id))) {
                count += entry.getValue()[availableOnly ? 1 : 0];
            }
        }
        return count;
    }
    public int outsideFilterCount() {
        int count = 0;
        for (String id : selected) { if (!matches(id)) { count++; } }
        return count;
    }
    private void recalculate() {
        long total = 0;
        String currency = null;
        for (String id : selected) {
            Money price = offer(id).price;
            if (currency != null && !currency.equals(price.currencyCode)) { throw new IllegalStateException("Mixed currencies"); }
            currency = price.currencyCode;
            total = Math.addExact(total, price.amountMinor);
        }
        totalMinor = total;
    }
}
