package com.example.nbatickethub.data;

import com.example.nbatickethub.model.*;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ArenaDataTest {
    @Test
    public void smallFixtureCoversWholeArenaAndEverySeatHasTwoGameOffers() throws IOException {
        ArenaDataset data = TestAssets.small();
        assertEquals(200, data.capacity());
        assertEquals(3, data.levels.size());
        assertEquals(20, data.sections.size());
        assertEquals(40, data.rows.size());
        assertEquals(190, data.seatedCapacity());
        assertEquals(2, data.games.size());
        assertEquals(DataProvenance.OriginType.MOCK, data.arena.provenance.originType);
        assertNull(data.arena.provenance.sourceUrl);
        assertTrue(data.availableCount("demo-game-1") > 0);
        for (Seat seat : data.seatOrder) {
            SeatRow row = data.rows.get(seat.rowId);
            Section section = data.sections.get(row.sectionId);
            assertNotNull(data.levels.get(section.levelId));
            assertNotNull(data.inventory.get("demo-game-1").get(seat.id));
            assertNotNull(data.inventory.get("demo-game-2").get(seat.id));
        }
    }

    @Test
    public void physicalSeatCanHaveDifferentPriceAndStatusInDifferentGames() throws IOException {
        ArenaDataset data = TestAssets.small();
        Seat seat = data.seatOrder.get(0);
        GameSeat first = data.inventory.get("demo-game-1").get(seat.id);
        GameSeat second = data.inventory.get("demo-game-2").get(seat.id);
        assertEquals(first.seatId, second.seatId);
        assertEquals(13000, first.price.amountMinor);
        assertEquals(15600, second.price.amountMinor);
        assertTrue(data.seatOrder.stream().anyMatch(s -> data.inventory.get("demo-game-1")
                .get(s.id).baseStatus != data.inventory.get("demo-game-2").get(s.id).baseStatus));
    }

    @Test
    public void sameManifestProducesStableGeometryIdsAndInventory() throws IOException {
        ArenaDataset a = TestAssets.small();
        ArenaDataset b = TestAssets.small();
        assertEquals(a.seats.keySet(), b.seats.keySet());
        for (int i = 0; i < a.capacity(); i++) {
            assertArrayEquals(a.geometry.get(i).copyVertices(), b.geometry.get(i).copyVertices(), 0);
            String id = a.seatOrder.get(i).id;
            assertEquals(a.inventory.get("demo-game-1").get(id).baseStatus,
                    b.inventory.get("demo-game-1").get(id).baseStatus);
        }
    }

    @Test
    public void collectionsAndGeometryCannotBeMutatedByConsumer() throws IOException {
        ArenaDataset data = TestAssets.small();
        assertThrows(UnsupportedOperationException.class, () -> data.seats.clear());
        assertThrows(UnsupportedOperationException.class, () -> data.seatOrder.clear());
        assertThrows(UnsupportedOperationException.class, () -> data.inventory.clear());
        assertThrows(UnsupportedOperationException.class,
                () -> data.inventory.get("demo-game-1").clear());
        assertThrows(UnsupportedOperationException.class, () -> data.geometry.clear());
    }

    @Test
    public void duplicateIdsAndUnknownForeignKeysAreRejected() throws IOException {
        String json = TestAssets.text("arena-small");
        for (String invalid : Arrays.asList(
                json.replace("\"id\": \"demo-away\"", "\"id\": \"demo-home\""),
                json.replace("\"id\": \"demo-game-2\"", "\"id\": \"demo-game-1\""),
                json.replace("\"id\": \"lower-02\"", "\"id\": \"lower-01\""),
                json.replace("\"homeTeamId\": \"demo-home\"", "\"homeTeamId\": \"unknown\""),
                json.replace("\"levelId\": \"lower\"", "\"levelId\": \"unknown\""),
                json.replace("\"arenaId\": \"demo-arena\"", "\"arenaId\": \"unknown\""))) {
            assertThrows(IOException.class, () -> TestAssets.parse(invalid));
        }
    }

    @Test
    public void levelAndSectionOverlapAreRejected() throws IOException {
        String json = TestAssets.text("arena-small");
        assertThrows(IOException.class,
                () -> TestAssets.parse(json.replace("\"innerRadius\": 680", "\"innerRadius\": 500")));
        assertThrows(IOException.class,
                () -> TestAssets.parse(json.replace("\"startDegrees\": 48.0", "\"startDegrees\": 30")));
    }

    @Test
    public void repeatedSeatNumberBadRowAndWrongGeometryIndexAreRejected() throws IOException {
        ArenaDataset data = TestAssets.small();
        Seat seat = data.seatOrder.get(1);
        List<Seat> seats = new ArrayList<>(data.seatOrder);
        seats.set(1, new Seat(seat.id, seat.rowId, 1, seat.kind, seat.geometryIndex));
        assertThrows(IllegalArgumentException.class, () -> copy(data, seats, data.geometry, offers(data)));
        seats.set(1, new Seat(seat.id, "missing-row", 2, seat.kind, seat.geometryIndex));
        assertThrows(IllegalArgumentException.class, () -> copy(data, seats, data.geometry, offers(data)));
        seats.set(1, new Seat(seat.id, seat.rowId, 2, seat.kind, 0));
        assertThrows(IllegalArgumentException.class, () -> copy(data, seats, data.geometry, offers(data)));
    }

    @Test
    public void overlappingAndOutOfBoundsSeatGeometryAreRejected() throws IOException {
        ArenaDataset data = TestAssets.small();
        List<SeatGeometry> geometry = new ArrayList<>(data.geometry);
        geometry.set(1, geometry.get(0));
        assertThrows(IllegalArgumentException.class,
                () -> copy(data, data.seatOrder, geometry, offers(data)));
        geometry.set(1, SeatGeometry.rectangle(new WorldRect(-10, -10, -5, -5)));
        assertThrows(IllegalArgumentException.class,
                () -> copy(data, data.seatOrder, geometry, offers(data)));
    }

    @Test
    public void duplicateRowRangesAndGeometryOutsideItsRowAreRejected() throws IOException {
        ArenaDataset data = TestAssets.small();
        SeatRow first = data.rows.values().iterator().next();
        List<SeatRow> rows = new ArrayList<>(data.rows.values());
        rows.set(0, new SeatRow(first.id, first.sectionId, first.rowLabel, first.order,
                first.boundary, 1, first.seatCount));
        assertThrows(IllegalArgumentException.class, () -> new ArenaDataset(data.arena,
                new ArrayList<>(data.teams.values()), new ArrayList<>(data.games.values()),
                new ArrayList<>(data.levels.values()), new ArrayList<>(data.sections.values()),
                rows, data.seatOrder, data.geometry, offers(data)));
        List<SeatGeometry> geometry = new ArrayList<>(data.geometry);
        geometry.set(0, SeatGeometry.rectangle(data.arena.courtBounds));
        assertThrows(IllegalArgumentException.class,
                () -> copy(data, data.seatOrder, geometry, offers(data)));
    }

    @Test
    public void rotatedSeatsWithOverlappingAabbsButSeparateShapesAreValid() throws IOException {
        ArenaDataset data = TestAssets.small();
        List<SeatGeometry> geometry = new ArrayList<>(data.geometry);
        SeatGeometry original = geometry.get(0);
        float cx = original.bounds.centerX();
        float cy = original.bounds.centerY();
        float radius = (original.bounds.right - original.bounds.left) / 5;
        float offset = radius * 0.6f;
        geometry.set(0, diamond(cx - offset, cy - offset, radius));
        geometry.set(1, diamond(cx + offset, cy + offset, radius));
        assertTrue(geometry.get(0).bounds.overlaps(geometry.get(1).bounds));
        assertEquals(data.capacity(), copy(data, data.seatOrder, geometry, offers(data)).capacity());
        geometry.set(1, geometry.get(0));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> copy(data, data.seatOrder, geometry, offers(data)));
        assertTrue(error.getMessage(), error.getMessage().contains("Seats overlap"));
    }

    @Test
    public void unknownIncompleteDuplicateAndMixedCurrencyInventoryAreRejected() throws IOException {
        ArenaDataset data = TestAssets.small();
        List<GameSeat> offers = offers(data);
        GameSeat first = offers.get(0);
        offers.remove(0);
        assertThrows(IllegalArgumentException.class,
                () -> copy(data, data.seatOrder, data.geometry, offers));
        offers.add(new GameSeat(first.gameId, "unknown", first.baseStatus, first.price, first.provenance));
        assertThrows(IllegalArgumentException.class,
                () -> copy(data, data.seatOrder, data.geometry, offers));
        offers.set(offers.size() - 1, first);
        offers.add(first);
        assertThrows(IllegalArgumentException.class,
                () -> copy(data, data.seatOrder, data.geometry, offers));
        List<GameSeat> mixed = offers(data);
        mixed.set(0, new GameSeat(first.gameId, first.seatId, first.baseStatus,
                new Money(10, "EUR"), first.provenance));
        assertThrows(IllegalArgumentException.class,
                () -> copy(data, data.seatOrder, data.geometry, mixed));
    }

    @Test
    public void tenThousandAndThirtyThousandUseTheSamePipeline() throws IOException {
        for (String name : new String[]{"arena-10k", "arena-30k"}) {
            ArenaDataset data = TestAssets.parse(TestAssets.text(name));
            int expected = name.equals("arena-10k") ? 10000 : 30000;
            assertEquals(expected, data.capacity());
            assertEquals(expected * 95L / 100, data.seatedCapacity());
            assertEquals(expected, data.inventory.get("demo-game-1").size());
            assertEquals(expected, data.inventory.get("demo-game-2").size());
            assertEquals(3, data.levels.size());
            assertEquals(20, data.sections.size());
        }
    }

    @Test
    public void fiftyThousandIsADataCheckNotARenderingBenchmark() throws IOException {
        String json = TestAssets.text("arena-30k").replace("\"rowCount\": 30", "\"rowCount\": 50");
        ArenaDataset data = TestAssets.parse(json);
        assertEquals(50000, data.capacity());
        assertEquals(47500, data.seatedCapacity());
        assertEquals(1000, data.rows.size());
        assertEquals(50000, data.geometry.size());
    }

    @Test
    public void generationCanBeInterruptedWithoutPublishingPartialDataset() throws IOException {
        String json = TestAssets.text("arena-small");
        try {
            Thread.currentThread().interrupt();
            assertThrows(java.io.InterruptedIOException.class, () -> TestAssets.parse(json));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }

    private List<GameSeat> offers(ArenaDataset data) {
        List<GameSeat> result = new ArrayList<>();
        data.inventory.values().forEach(map -> result.addAll(map.values()));
        return result;
    }

    private SeatGeometry diamond(float x, float y, float radius) {
        return new SeatGeometry(new float[]{x, y - radius, x + radius, y,
                x, y + radius, x - radius, y});
    }

    private ArenaDataset copy(ArenaDataset data, List<Seat> seats, List<SeatGeometry> geometry,
                              List<GameSeat> offers) {
        return new ArenaDataset(data.arena, new ArrayList<>(data.teams.values()),
                new ArrayList<>(data.games.values()), new ArrayList<>(data.levels.values()),
                new ArrayList<>(data.sections.values()), new ArrayList<>(data.rows.values()),
                seats, geometry, offers);
    }
}
