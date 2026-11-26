package com.example.nbatickethub.data.mock;

import com.example.nbatickethub.data.*;
import com.example.nbatickethub.model.*;
import java.util.*;

/** Authored sample schedule and deterministic inventory. Never a real NBA schedule. */
public final class MockMatchSchedule {
    private static final DataProvenance SOURCE = new DataProvenance(DataProvenance.OriginType.MOCK,
            "mobile-matches-v1", 20261125, "Sample matches, prices and availability; not real tickets", null, null);
    private MockMatchSchedule() { }

    public static List<Game> games(ArenaCatalog catalog) {
        return games(catalog, System.currentTimeMillis(), TimeZone.getDefault());
    }

    public static List<Game> games(ArenaCatalog catalog, long anchor, TimeZone zone) {
        List<Game> result = new ArrayList<>();
        Calendar tomorrow = Calendar.getInstance(zone); tomorrow.setTimeInMillis(anchor);
        tomorrow.add(Calendar.DAY_OF_YEAR, 1); tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0); tomorrow.set(Calendar.SECOND, 0); tomorrow.set(Calendar.MILLISECOND, 0);
        long remaining = tomorrow.getTimeInMillis() - anchor;
        add(result, catalog, "sample-nyk-bos", "nyk", "bos", anchor + Math.max(1, remaining / 3), zone);
        add(result, catalog, "sample-nyk-mia", "nyk", "mia", atDay(anchor, 1, 19, zone), zone);
        add(result, catalog, "sample-gsw-lal", "gsw", "lal", anchor + Math.max(1, remaining * 2 / 3), zone);
        int index = 0;
        for (ArenaTheme theme : catalog.themes) {
            if (theme.teamId.equals("nyk") || theme.teamId.equals("gsw")) { continue; }
            add(result, catalog, "sample-" + theme.teamId, theme.teamId, "nyk",
                    atDay(anchor, 2 + index % 12, 18 + index % 3, zone), zone);
            index++;
        }
        add(result, catalog, "sample-started", "nyk", "bos", anchor - 3600000, zone);
        add(result, catalog, "sample-nyk-lal-future", "nyk", "lal", atDay(anchor, 10, 20, zone), zone);
        return Collections.unmodifiableList(result);
    }

    private static long atDay(long anchor, int offset, int hour, TimeZone zone) {
        Calendar value = Calendar.getInstance(zone); value.setTimeInMillis(anchor);
        value.add(Calendar.DAY_OF_YEAR, offset); value.set(Calendar.HOUR_OF_DAY, hour);
        value.set(Calendar.MINUTE, 0); value.set(Calendar.SECOND, 0); value.set(Calendar.MILLISECOND, 0);
        return value.getTimeInMillis();
    }

    private static void add(List<Game> result, ArenaCatalog catalog, String id, String home, String away,
                            long start, TimeZone zone) {
        result.add(new Game(id, home, away, catalog.homeArena(home).id,
                com.example.nbatickethub.state.MatchListState.display(start, "dd/MM/yyyy · HH:mm (z)", zone), SOURCE, start));
    }

    @SuppressWarnings("unchecked")
    public static java.util.Map<String, Long> startingPrices(ArenaCatalog catalog, AssetSource assets) throws java.io.IOException {
        java.util.Map<String, Long> result = new java.util.LinkedHashMap<>();
        java.util.Map<String, Long> bases = new java.util.HashMap<>();
        for (Game game : games(catalog)) {
            if (!bases.containsKey(game.arenaId)) {
                try (java.io.Reader reader = new java.io.InputStreamReader(assets.open(catalog.byId.get(game.arenaId).dataAsset), java.nio.charset.StandardCharsets.UTF_8)) {
                    Map<String, Object> json = (Map<String, Object>) com.example.nbatickethub.data.json.StrictJson.read(reader);
                    long minimum = Long.MAX_VALUE;
                    for (Object entry : (List<?>) json.get("sections")) {
                        minimum = Math.min(minimum, ((java.math.BigDecimal) ((Map<?, ?>) entry).get("mockBasePriceMinor")).longValueExact());
                    }
                    Map<?, ?> first = (Map<?, ?>) ((List<?>) json.get("games")).get(0);
                    long factor = ((java.math.BigDecimal) ((Map<?, ?>) first.get("mockInventory")).get("priceFactorPercent")).longValueExact();
                    bases.put(game.arenaId, (minimum * factor + 50) / 100);
                }
            }
            result.put(game.id, (bases.get(game.arenaId) * (85 + Math.floorMod(game.id.hashCode(), 56)) + 50) / 100);
        }
        return result;
    }

    public static ArenaDataset inventory(ArenaDataset base, Game match, ArenaCatalog catalog) {
        if (!base.arena.id.equals(match.arenaId)) { throw new IllegalArgumentException("Match arena mismatch"); }
        List<Team> teams = new ArrayList<>();
        for (ArenaTheme theme : catalog.themes) {
            teams.add(new Team(theme.teamId, theme.name, theme.teamId.toUpperCase(Locale.ROOT)));
        }
        List<GameSeat> offers = new ArrayList<>();
        Map<String, GameSeat> original = base.inventory.values().iterator().next();
        int factor = 85 + Math.floorMod(match.id.hashCode(), 56);
        for (Seat seat : base.seatOrder) {
            int bucket = Math.floorMod(match.id.hashCode() ^ seat.id.hashCode(), 100);
            SeatStatus status = bucket < 8 ? SeatStatus.LOCKED : bucket < 26 ? SeatStatus.BOOKED : SeatStatus.AVAILABLE;
            Money price = original.get(seat.id).price;
            offers.add(new GameSeat(match.id, seat.id, status,
                    new Money((price.amountMinor * factor + 50) / 100, price.currencyCode), SOURCE,
                    base.sections.get(base.rows.get(seat.rowId).sectionId).category == Section.Category.STANDING
                            ? Section.Category.STANDING : Math.floorMod(Integer.rotateLeft(match.id.hashCode(), 11) ^ seat.id.hashCode(), 7) == 0
                            ? Section.Category.VIP : base.sections.get(base.rows.get(seat.rowId).sectionId).category));
        }
        return new ArenaDataset(base.arena, teams, Collections.singletonList(match), new ArrayList<>(base.levels.values()),
                new ArrayList<>(base.sections.values()), new ArrayList<>(base.rows.values()), base.seatOrder, base.geometry, offers);
    }
}
