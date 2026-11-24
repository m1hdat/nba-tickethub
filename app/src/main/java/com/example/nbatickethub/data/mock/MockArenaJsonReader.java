package com.example.nbatickethub.data.mock;

import com.example.nbatickethub.data.ArenaDataset;
import com.example.nbatickethub.data.json.StrictJson;
import com.example.nbatickethub.geometry.ArenaGeometryCompiler;
import com.example.nbatickethub.model.*;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Schema v1 mock manifest reader. It never presents generated inventory as real NBA data. */
public final class MockArenaJsonReader {
    private final int seatBudget;
    private final int offerBudget;

    public MockArenaJsonReader() { this(100_000, 500_000); }

    /** Resource budgets are configurable, not a 200-seat domain limit. */
    public MockArenaJsonReader(int seatBudget, int offerBudget) {
        if (seatBudget <= 0 || offerBudget <= 0) {
            throw new IllegalArgumentException("Load budgets must be positive");
        }
        this.seatBudget = seatBudget;
        this.offerBudget = offerBudget;
    }

    public ArenaDataset read(Reader reader) throws IOException {
        try { return decode(object(StrictJson.read(reader), "root")); }
        catch (IllegalArgumentException | ArithmeticException error) {
            throw new IOException("Invalid mock arena: " + error.getMessage(), error);
        }
    }

    private ArenaDataset decode(Map<String, Object> root) throws IOException {
        keys(root, "schemaVersion", "geometryVersion", "provenance", "arena", "teams",
                "games", "levels", "sections");
        int version = integer(root, "schemaVersion");
        if (version != 1) { throw new IllegalArgumentException("Unsupported schemaVersion: " + version); }
        Map<String, Object> source = object(root.get("provenance"), "provenance");
        keys(source, "originType", "version", "simulationSeed", "notice", "sourceUrl", "collectedAt");
        if (!"MOCK".equals(text(source, "originType"))) {
            throw new IllegalArgumentException("Mock reader accepts MOCK provenance only");
        }
        DataProvenance provenance = new DataProvenance(DataProvenance.OriginType.MOCK,
                text(source, "version"), number(source, "simulationSeed").longValueExact(),
                text(source, "notice"), nullableText(source, "sourceUrl"),
                nullableText(source, "collectedAt"));
        Map<String, Object> arenaJson = object(root.get("arena"), "arena");
        keys(arenaJson, "id", "name", "worldBounds", "courtBounds");
        Arena arena = new Arena(text(arenaJson, "id"), text(arenaJson, "name"), version,
                text(root, "geometryVersion"), bounds(arenaJson.get("worldBounds")),
                bounds(arenaJson.get("courtBounds")), provenance);
        List<Team> teams = new ArrayList<>();
        for (Object value : array(root.get("teams"), "teams")) {
            Map<String, Object> item = object(value, "team");
            keys(item, "id", "displayName", "shortName");
            teams.add(new Team(text(item, "id"), text(item, "displayName"), text(item, "shortName")));
        }
        List<Game> games = new ArrayList<>();
        List<InventorySpec> inventories = new ArrayList<>();
        for (Object value : array(root.get("games"), "games")) {
            Map<String, Object> item = object(value, "game");
            keys(item, "id", "homeTeamId", "awayTeamId", "arenaId", "displayTime", "mockInventory");
            Game game = new Game(text(item, "id"), text(item, "homeTeamId"),
                    text(item, "awayTeamId"), text(item, "arenaId"), text(item, "displayTime"), provenance);
            Map<String, Object> inventory = object(item.get("mockInventory"), "mockInventory");
            keys(inventory, "priceFactorPercent", "bookedPercent", "lockedPercent");
            int factor = integer(inventory, "priceFactorPercent");
            int booked = integer(inventory, "bookedPercent");
            int locked = integer(inventory, "lockedPercent");
            if (factor <= 0 || booked < 0 || locked < 0 || (long) booked + locked > 100) {
                throw new IllegalArgumentException("Invalid mock inventory percentages");
            }
            games.add(game);
            inventories.add(new InventorySpec(game, factor, booked, locked));
        }
        List<ArenaLevel> levels = new ArrayList<>();
        Map<String, ArenaLevel> levelLookup = new HashMap<>();
        for (Object value : array(root.get("levels"), "levels")) {
            Map<String, Object> item = object(value, "level");
            keys(item, "id", "arenaId", "code", "name", "order", "innerRadius", "outerRadius");
            ArenaLevel level = new ArenaLevel(text(item, "id"), text(item, "arenaId"),
                    text(item, "code"), text(item, "name"), integer(item, "order"),
                    new AnnularSector(arena.worldBounds.centerX(), arena.worldBounds.centerY(),
                            decimal(item, "innerRadius"), decimal(item, "outerRadius"), 0, 360));
            if (levelLookup.put(level.id, level) != null) {
                throw new IllegalArgumentException("Duplicate level ID: " + level.id);
            }
            levels.add(level);
        }
        List<Section> sections = new ArrayList<>();
        List<ArenaGeometryCompiler.SectionLayout> layouts = new ArrayList<>();
        Map<String, Money> sectionPrices = new HashMap<>();
        long expectedCapacity = 0;
        for (Object value : array(root.get("sections"), "sections")) {
            Map<String, Object> item = object(value, "section");
            keys(item, "id", "levelId", "code", "name", "category", "startDegrees", "endDegrees",
                    "rowCount", "seatsPerRow", "mockBasePriceMinor", "currencyCode");
            ArenaLevel level = levelLookup.get(text(item, "levelId"));
            if (level == null) { throw new IllegalArgumentException("Unknown section levelId"); }
            Section section = new Section(text(item, "id"), level.id, text(item, "code"),
                    text(item, "name"), Section.Category.valueOf(text(item, "category")),
                    new AnnularSector(level.footprint.centerX, level.footprint.centerY,
                            level.footprint.innerRadius, level.footprint.outerRadius,
                            decimal(item, "startDegrees"), decimal(item, "endDegrees")));
            int rowCount = integer(item, "rowCount");
            int seatsPerRow = integer(item, "seatsPerRow");
            layouts.add(new ArenaGeometryCompiler.SectionLayout(section, rowCount, seatsPerRow));
            expectedCapacity = Math.addExact(expectedCapacity, (long) rowCount * seatsPerRow);
            Money price = new Money(number(item, "mockBasePriceMinor").longValueExact(),
                    text(item, "currencyCode"));
            if (sectionPrices.put(section.id, price) != null) {
                throw new IllegalArgumentException("Duplicate section ID: " + section.id);
            }
            sections.add(section);
        }
        long expectedOffers = Math.multiplyExact(expectedCapacity, games.size());
        if (expectedOffers > offerBudget) {
            throw new IllegalArgumentException("Generated inventory exceeds configured offer budget");
        }
        ArenaGeometryCompiler.Result compiled = new ArenaGeometryCompiler().compile(layouts, seatBudget);
        Map<String, SeatRow> rowLookup = new HashMap<>();
        for (SeatRow row : compiled.rows) { rowLookup.put(row.id, row); }
        List<GameSeat> offers = new ArrayList<>((int) expectedOffers);
        for (InventorySpec inventory : inventories) {
            for (Seat seat : compiled.seats) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedIOException("Inventory generation interrupted");
                }
                Money base = sectionPrices.get(rowLookup.get(seat.rowId).sectionId);
                // Integer half-up rounding; exact arithmetic rejects overflow instead of wrapping.
                long price = Math.addExact(Math.multiplyExact(base.amountMinor, inventory.factor), 50) / 100;
                int bucket = (int) Math.floorMod(provenance.simulationSeed
                        ^ inventory.game.id.hashCode() ^ seat.id.hashCode(), 100L);
                SeatStatus status = bucket < inventory.locked ? SeatStatus.LOCKED
                        : bucket < inventory.locked + inventory.booked ? SeatStatus.BOOKED
                        : SeatStatus.AVAILABLE;
                offers.add(new GameSeat(inventory.game.id, seat.id, status,
                        new Money(price, base.currencyCode), provenance));
            }
        }
        return new ArenaDataset(arena, teams, games, levels, sections, compiled.rows,
                compiled.seats, compiled.geometry, offers);
    }

    private static final class InventorySpec {
        final Game game;
        final int factor;
        final int booked;
        final int locked;

        InventorySpec(Game game, int factor, int booked, int locked) {
            this.game = game;
            this.factor = factor;
            this.booked = booked;
            this.locked = locked;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String field) {
        if (!(value instanceof Map)) { throw new IllegalArgumentException("Expected object: " + field); }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> array(Object value, String field) {
        if (!(value instanceof List)) { throw new IllegalArgumentException("Expected array: " + field); }
        return (List<Object>) value;
    }

    private static void keys(Map<String, Object> object, String... expected) {
        List<String> allowed = Arrays.asList(expected);
        for (String key : object.keySet()) {
            if (!allowed.contains(key)) { throw new IllegalArgumentException("Unknown field: " + key); }
        }
        for (String key : expected) {
            if (!object.containsKey(key)) { throw new IllegalArgumentException("Missing field: " + key); }
        }
    }

    private static String text(Map<String, Object> object, String field) {
        Object value = object.get(field);
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            throw new IllegalArgumentException("Expected nonblank string: " + field);
        }
        return (String) value;
    }

    private static String nullableText(Map<String, Object> object, String field) {
        return object.get(field) == null ? null : text(object, field);
    }

    private static BigDecimal number(Map<String, Object> object, String field) {
        Object value = object.get(field);
        if (!(value instanceof BigDecimal)) {
            throw new IllegalArgumentException("Expected number: " + field);
        }
        return (BigDecimal) value;
    }

    private static int integer(Map<String, Object> object, String field) {
        return number(object, field).intValueExact();
    }

    private static float decimal(Map<String, Object> object, String field) {
        float result = number(object, field).floatValue();
        if (!Float.isFinite(result)) { throw new IllegalArgumentException("Non-finite: " + field); }
        return result;
    }

    private static WorldRect bounds(Object value) {
        List<Object> numbers = array(value, "bounds");
        if (numbers.size() != 4) { throw new IllegalArgumentException("Bounds need four numbers"); }
        float[] coords = new float[4];
        for (int i = 0; i < 4; i++) {
            if (!(numbers.get(i) instanceof BigDecimal)) {
                throw new IllegalArgumentException("Bounds must be numeric");
            }
            coords[i] = ((BigDecimal) numbers.get(i)).floatValue();
        }
        return new WorldRect(coords[0], coords[1], coords[2], coords[3]);
    }
}
