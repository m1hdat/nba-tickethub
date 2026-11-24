package com.example.nbatickethub.data;

import com.example.nbatickethub.model.*;

import org.junit.Test;

import static org.junit.Assert.*;

public class ModelTest {
    @Test
    public void moneyRejectsInvalidAmountsAndCurrency() {
        assertEquals(12345L, new Money(12345, "USD").amountMinor);
        assertThrows(IllegalArgumentException.class, () -> new Money(-1, "USD"));
        assertThrows(IllegalArgumentException.class, () -> new Money(1, "usd"));
        assertThrows(IllegalArgumentException.class, () -> new Money(1, "ZZZ"));
    }

    @Test
    public void invalidNamesIdsAndSameTeamsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Team("bad/id", "Name", "N"));
        assertThrows(IllegalArgumentException.class, () -> new Team("ok", " ", "N"));
        assertThrows(IllegalArgumentException.class,
                () -> new Game("g", "a", "a", "arena", "demo", mock()));
    }

    @Test
    public void boundsRejectNonfiniteAndZeroSize() {
        assertThrows(IllegalArgumentException.class, () -> new WorldRect(0, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new WorldRect(Float.NaN, 0, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new WorldRect(0, 0, Float.POSITIVE_INFINITY, 1));
    }

    @Test
    public void quadOwnsItsVerticesAndRejectsBowTie() {
        float[] input = {0, 0, 2, 0, 2, 2, 0, 2};
        SeatGeometry geometry = new SeatGeometry(input);
        input[0] = 100;
        geometry.copyVertices()[0] = 100;
        assertEquals(0, geometry.x(0), 0);
        assertThrows(IllegalArgumentException.class,
                () -> new SeatGeometry(new float[]{0, 0, 2, 2, 0, 2, 2, 0}));
        assertThrows(IndexOutOfBoundsException.class, () -> geometry.x(-1));
    }

    @Test
    public void annularBoundsIncludeCardinalExtrema() {
        AnnularSector sector = new AnnularSector(100, 100, 10, 20, 30, 150);
        assertTrue(sector.bounds.bottom >= 120);
        assertTrue(sector.bounds.left <= 100 + 20 * Math.cos(Math.toRadians(150)));
        assertThrows(IllegalArgumentException.class,
                () -> new AnnularSector(0, 0, 20, 10, 0, 360));
    }

    @Test
    public void mockCannotPretendToHaveRealSourceMetadata() {
        assertThrows(IllegalArgumentException.class,
                () -> new DataProvenance(DataProvenance.OriginType.MOCK, "v1", 1,
                        "Mock", "https://example.org", null));
        assertThrows(IllegalArgumentException.class,
                () -> new DataProvenance(DataProvenance.OriginType.REAL_SNAPSHOT, "v1", 0,
                        "Snapshot", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new DataProvenance(DataProvenance.OriginType.REAL_SNAPSHOT, "v1", 0,
                        "Snapshot", "https://example.org", "2026-02-30T12:00:00Z"));
    }

    private DataProvenance mock() {
        return new DataProvenance(DataProvenance.OriginType.MOCK, "test-v1", 1,
                "Synthetic data", null, null);
    }
}
