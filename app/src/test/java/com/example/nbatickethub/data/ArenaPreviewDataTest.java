package com.example.nbatickethub.data;

import com.example.nbatickethub.geometry.ArenaScene;
import com.example.nbatickethub.geometry.ArenaSceneCompiler;
import com.example.nbatickethub.geometry.ArenaOutlines;
import com.example.nbatickethub.geometry.SeatPathBatches;
import com.example.nbatickethub.geometry.ArenaSeatMarkers;
import com.example.nbatickethub.model.*;
import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class ArenaPreviewDataTest {
    @Test public void thirtyCourtsHaveTwoIndependentColorFreeArtworkLayouts() throws Exception {
        ArenaCatalog catalog = new ArenaCatalogReader().load(assets());
        assertEquals(30, catalog.themes.size());
        assertEquals(30, catalog.profiles.size());
        Set<String> teams = new HashSet<>(), woods = new HashSet<>(), graphics = new HashSet<>();
        Set<Float> markWidths = new HashSet<>();
        for (ArenaTheme theme : catalog.themes) {
            assertTrue(teams.add(theme.teamId));
            CourtDesign design = theme.court.design;
            assertTrue(design.woodFeature.length() > 5 && design.graphicFeature.length() > 5);
            assertEquals(theme.teamId.toUpperCase(Locale.ROOT), design.logo.fallback);
            assertNull("No unverified official logo asset may be claimed", design.logo.asset);
            markWidths.add(design.logo.width);
            List<String> wood = new ArrayList<>(), graphic = new ArrayList<>();
            for (CourtDesign.Primitive primitive : design.primitives) {
                String geometry = primitive.type + ":" + primitive.strokeWidth + ":"
                        + Arrays.toString(primitive.copyPoints());
                if (primitive.part.equals("wood")) { wood.add(geometry); }
                if (primitive.part.equals("decor")) { graphic.add(geometry); }
            }
            Collections.sort(wood); Collections.sort(graphic);
            assertFalse(wood.isEmpty()); assertFalse(graphic.isEmpty());
            assertTrue("Wood composition must differ beyond color/logo", woods.add(wood.toString()));
            assertTrue("Graphic composition must differ beyond color/logo", graphics.add(graphic.toString()));
            assertThrows(UnsupportedOperationException.class, () -> design.primitives.clear());
        }
        assertTrue(markWidths.size() >= 4);
    }

    @Test public void everyTeamMapsToHomeAndSharedHomesReuseGeometryWithDifferentPalettes() throws Exception {
        ArenaCatalog catalog = new ArenaCatalogReader().load(assets());
        Set<String> geometry = new HashSet<>();
        for (ArenaTheme theme : catalog.themes) {
            ArenaProfile profile = catalog.homeArena(theme.teamId);
            assertEquals(theme.teamId, profile.homeTeam.id);
            ArenaDataset data = new com.example.nbatickethub.data.mock.MockArenaRepository(assets()).load(profile.dataAsset);
            ArenaScene scene = new ArenaSceneCompiler().compile(profile, data);
            // Independent full projected-vertex signature, not just an arena identifier.
            java.security.MessageDigest signature = java.security.MessageDigest.getInstance("SHA-256");
            java.nio.ByteBuffer quad = java.nio.ByteBuffer.allocate(32);
            for (SeatGeometry seat : scene.seats) {
                quad.clear();
                for (int vertex = 0; vertex < 4; vertex++) { quad.putFloat(seat.x(vertex)); quad.putFloat(seat.y(vertex)); }
                signature.update(quad.array());
            }
            assertTrue(geometry.add(Base64.getEncoder().encodeToString(signature.digest())));
            assertTrue(theme.seatThemeNotice.contains("DESIGNED TEAM SEAT COLORS"));
            Set<Integer> colors = new HashSet<>();
            for (ArenaLevel level : data.levels.values()) {
                assertTrue(colors.add(theme.seatTier(level.order).seatBackground));
                assertTrue(ArenaTheme.contrast(0xffffffff, theme.statuses.available) >= 3);
                assertTrue(ArenaTheme.contrast(0xffffffff, theme.statuses.locked) >= 3);
            }
        }
        assertEquals(30, geometry.size());
        Map<String, String> shared = new LinkedHashMap<>();
        for (ArenaTheme theme : catalog.themes) { shared.put(theme.teamId, catalog.homeArena(theme.teamId).id); }
        shared.put("gsw", "msg"); // Synthetic shared-home fixture, not a factual NBA mapping.
        ArenaCatalog fixture = new ArenaCatalog(catalog.profiles, catalog.themes, shared);
        assertSame(fixture.homeArena("nyk"), fixture.homeArena("gsw"));
        ArenaProfile arena = fixture.homeArena("nyk");
        ArenaTheme knicks = fixture.resolveTheme(arena, "nyk-demo"), warriors = fixture.resolveTheme(arena, "gsw-demo");
        for (int order = 1; order <= 4; order++) {
            assertNotEquals(knicks.seatTier(order).seatBackground, warriors.seatTier(order).seatBackground);
        }
        shared.remove("gsw");
        assertThrows(IllegalArgumentException.class, () -> new ArenaCatalog(catalog.profiles, catalog.themes, shared));
    }

    @Test public void centerLogoContainsImageAspectRatioAndRejectsUnattributedAsset() {
        CourtDesign.Logo logo = new CourtDesign.Logo(null, null, null, "NYK", 20, 16);
        assertArrayEquals(new float[]{37,20,57,30}, logo.fit(200,100), .0001f);
        assertArrayEquals(new float[]{43,17,51,33}, logo.fit(100,200), .0001f);
        assertThrows(IllegalArgumentException.class, () -> logo.fit(0,100));
        assertThrows(IllegalArgumentException.class,
                () -> new CourtDesign.Logo("logos/test.png", null, null, "NYK", 20, 16));
    }

    @Test public void sharedCourtMarkingsStayInsideCourtAndIncludeCenterLine() {
        boolean centerLine = false;
        for (CourtDesign.Primitive primitive : com.example.nbatickethub.geometry.CourtMarkings.ALL) {
            float[] points = primitive.copyPoints();
            if (Arrays.equals(points, new float[]{47,0,47,50})) { centerLine = true; }
            for (int i = 0; i < points.length; i++) {
                assertTrue(points[i] >= -.001 && points[i] <= (i % 2 == 0 ? 94.001 : 50.001));
            }
        }
        assertTrue(centerLine);
    }

    @Test public void themesResolveByGameThenHomeTeamThenFallbackWithoutChangingGeometry() throws Exception {
        ArenaCatalog catalog = new ArenaCatalogReader().load(assets());
        for (ArenaProfile profile : catalog.profiles) {
            ArenaDataset data = new com.example.nbatickethub.data.mock.MockArenaRepository(assets()).load(profile.dataAsset);
            ArenaScene scene = new ArenaSceneCompiler().compile(profile, data);
            List<SeatGeometry> sameSeats = scene.seats;
            float[] before = sameSeats.get(0).copyVertices();
            assertEquals(profile.homeTeam.id, catalog.resolveTheme(profile, null).teamId);
            for (ArenaTheme theme : catalog.themes) {
                assertSame(theme, catalog.resolveTheme(profile, theme.id));
                assertSame(sameSeats, scene.seats);
                assertArrayEquals(before, scene.seats.get(0).copyVertices(), 0);
                assertSame(catalog.themes.get(0).statuses, theme.statuses);
                assertTrue(ArenaTheme.contrast(profile.stands.labelText, profile.stands.labelBackground) >= 4.5);
                for (ArenaLevel level : data.levels.values()) { assertNotNull(profile.stands.tier(level.order)); }
            }
            ArenaProfile fallback = new ArenaProfile(profile.id, profile.venueName,
                    new Team("unknown-home", "Synthetic home", "TEST"), profile.dataAsset,
                    profile.identitySource, profile.referenceSource, profile.retrievedOn,
                    profile.geometryNotice, profile.geometry, profile.stands, profile.defaultThemeId);
            assertEquals(profile.defaultThemeId, catalog.resolveTheme(fallback, null).id);
            ArenaProfile gameConfigured = new ArenaProfile(profile.id, profile.venueName, profile.homeTeam,
                    profile.dataAsset, profile.identitySource, profile.referenceSource, profile.retrievedOn,
                    profile.geometryNotice, profile.geometry, profile.stands, profile.defaultThemeId, "lal-demo");
            assertEquals("lal-demo", catalog.resolveTheme(gameConfigured, null).id);
            assertEquals("nyk-demo", catalog.resolveTheme(gameConfigured, "nyk-demo").id);
            // Home-team identity is not an input to venue geometry generation.
            ArenaScene rebound = new ArenaSceneCompiler().compile(fallback, data);
            assertArrayEquals(before, rebound.seats.get(0).copyVertices(), 0);
        }
        assertThrows(IllegalArgumentException.class, () -> catalog.resolveTheme(catalog.profiles.get(0), "missing"));
    }

    @Test public void rejectsLowContrastAndFalseOfficialThemeAndDanglingThemeReference() throws Exception {
        String json;
        try (InputStream stream = assets().open("venues/catalog.json")) {
            json = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        final String text = json;
        assertThrows(IOException.class, () -> new ArenaCatalogReader().read(new StringReader(
                text.replace("#24394B", "#FFFFFF"))));
        assertThrows(IOException.class, () -> new ArenaCatalogReader().read(new StringReader(
                text.replace("\"origin\":\"MOCK\"", "\"origin\":\"OFFICIAL\""))));
        assertThrows(IOException.class, () -> new ArenaCatalogReader().read(new StringReader(
                text.replace("\"defaultThemeId\":\"nyk-demo\"", "\"defaultThemeId\":\"missing\""))));
    }

    @Test public void statusMarkersRemainInsideEveryProjectedSeat() throws Exception {
        for (ArenaProfile profile : new ArenaCatalogReader().load(assets()).profiles) {
            ArenaDataset data = new com.example.nbatickethub.data.mock.MockArenaRepository(assets()).load(profile.dataAsset);
            for (SeatGeometry seat : new ArenaSceneCompiler().compile(profile, data).seats) {
                float[] marker = ArenaSeatMarkers.marker(seat);
                for (int edge = 0; edge < 4; edge++) {
                    int next = (edge + 1) % 4;
                    double dx = seat.x(next) - seat.x(edge), dy = seat.y(next) - seat.y(edge);
                    double distance = Math.abs(dx * (marker[1] - seat.y(edge))
                            - dy * (marker[0] - seat.x(edge))) / Math.hypot(dx, dy);
                    assertTrue("Status dot must fit within the seat", distance + .001 >= marker[2]);
                }
            }
        }
    }

    private AssetSource assets() {
        Path root = Paths.get("src/main/assets");
        if (!Files.isDirectory(root)) { root = Paths.get("app/src/main/assets"); }
        final Path base = root;
        return name -> Files.newInputStream(base.resolve(name));
    }

    @Test public void thirtyDistinctScenesHaveValidUniqueSeatsAndSimulatedInventory() throws Exception {
        ArenaCatalog catalog = new ArenaCatalogReader().load(assets());
        assertEquals(30, catalog.profiles.size());
        int[] counts = {16908, 17584, 19056};
        Set<String> ids = new HashSet<>();
        int venue = 0;
        for (ArenaProfile profile : catalog.profiles) {
            ArenaDataset data;
            try (Reader reader = new InputStreamReader(assets().open(profile.dataAsset), java.nio.charset.StandardCharsets.UTF_8)) {
                data = new com.example.nbatickethub.data.mock.MockArenaJsonReader().read(reader);
            }
            ArenaScene scene = new ArenaSceneCompiler().compile(profile, data);
            if (venue < 3) { assertEquals(counts[venue], scene.seats.size()); }
            venue++;
            assertEquals(data.capacity(), data.seatedCapacity());
            assertTrue(data.levels.size() >= 3);
            assertTrue(profile.geometryNotice.contains("SIMULATION"));
            for (Seat seat : data.seatOrder) {
                assertTrue(ids.add(seat.id));
                assertTrue(profile.geometry.displayBounds.encloses(scene.seats.get(seat.geometryIndex).bounds));
                // Projection is deterministic and leaves canonical geometry untouched.
                float[] point = new float[2];
                ArenaSceneCompiler.project(profile, data, data.geometry.get(seat.geometryIndex).x(0),
                        data.geometry.get(seat.geometryIndex).y(0), point);
                assertEquals(point[0], scene.seats.get(seat.geometryIndex).x(0), 0);
                assertWithinProjectedRow(profile, data.rows.get(seat.rowId).boundary,
                        scene.seats.get(seat.geometryIndex));
            }
        }
        assertNotEquals(catalog.profiles.get(0).geometry.bowlExponent, catalog.profiles.get(1).geometry.bowlExponent, 0);
        assertNotEquals(catalog.profiles.get(1).geometry.scaleX, catalog.profiles.get(2).geometry.scaleX, 0);
    }

    private void assertWithinProjectedRow(ArenaProfile p, AnnularSector row, SeatGeometry quad) {
        // Independent analytic inverse of the preparation projection, for containment tests only.
        // Interactive inverse Matrix/touch handling is intentionally absent from Phase 3A.
        for (int edge = 0; edge < 4; edge++) {
            int next = (edge + 1) % 4;
            for (int sample = 0; sample <= 4; sample++) {
                double t = sample / 4.0;
                double x = (quad.x(edge) * (1 - t) + quad.x(next) * t - p.geometry.displayBounds.centerX()) / p.geometry.scaleX;
                double y = (quad.y(edge) * (1 - t) + quad.y(next) * t - p.geometry.displayBounds.centerY()) / p.geometry.scaleY;
                double r = Math.pow(Math.pow(Math.abs(x), p.geometry.bowlExponent)
                        + Math.pow(Math.abs(y), p.geometry.bowlExponent), 1.0 / p.geometry.bowlExponent);
                double angle = Math.toDegrees(Math.atan2(y, x));
                if (angle < 0) { angle += 360; }
                assertTrue(r >= row.innerRadius - .01 && r <= row.outerRadius + .01);
                assertTrue(angle >= row.startDegrees - .01 && angle <= row.endDegrees + .01);
            }
        }
    }

    @Test public void layoutHasCompactConcourseReadableLabelsAndDistinctTierPalettes() throws Exception {
        ArenaCatalog catalog = new ArenaCatalogReader().load(assets());
        for (ArenaProfile p : catalog.profiles) {
            ArenaTheme theme = catalog.resolveTheme(p, null);
            ArenaDataset data = new com.example.nbatickethub.data.mock.MockArenaRepository(assets()).load(p.dataAsset);
            ArenaScene scene = new ArenaSceneCompiler().compile(p, data);
            float previous = 0, outer = 0;
            Set<Integer> fills = new HashSet<>(), inks = new HashSet<>();
            for (ArenaLevel level : data.levels.values()) {
                if (previous > 0) { assertTrue(level.footprint.innerRadius - previous <= 25); }
                previous = level.footprint.outerRadius;
                outer = Math.max(outer, previous);
                fills.add(theme.seatTier(level.order).section);
                inks.add(theme.seatTier(level.order).seatBackground);
                assertNotEquals(theme.statuses.booked, theme.seatTier(level.order).seatBackground);
                assertNotEquals(theme.statuses.locked, theme.seatTier(level.order).seatBackground);
            }
            assertEquals(data.levels.size(), fills.size());
            assertEquals(data.levels.size(), inks.size());
            for (ArenaLevel level : data.levels.values()) {
                double occupiedDegrees = 0;
                int count = 0;
                for (Section section : data.sections.values()) {
                    if (section.levelId.equals(level.id)) {
                        occupiedDegrees += section.boundary.endDegrees - section.boundary.startDegrees;
                        count++;
                    }
                }
                // Gallery intentionally contains two separate sideline strips.
                // Full bowls should not lose large wedges to decorative aisles.
                if (count >= 8) { assertTrue(occupiedDegrees >= 338); }
            }
            double courtFraction = p.geometry.courtLength / (2 * outer * p.geometry.scaleX);
            assertTrue(courtFraction >= .30 && courtFraction <= .42);
            for (Section section : data.sections.values()) {
                float[] point = ArenaOutlines.label(scene, section.boundary);
                double x = (point[0] - p.geometry.displayBounds.centerX()) / p.geometry.scaleX;
                double y = (point[1] - p.geometry.displayBounds.centerY()) / p.geometry.scaleY;
                double radius = Math.pow(Math.pow(Math.abs(x), p.geometry.bowlExponent)
                        + Math.pow(Math.abs(y), p.geometry.bowlExponent), 1.0 / p.geometry.bowlExponent);
                assertTrue(radius > section.boundary.innerRadius && radius < section.boundary.outerRadius);
            }
        }
    }

    @Test public void rayProjectionDoesNotStretchTinyChangesAtBowlAxesIntoSpikes() throws Exception {
        for (ArenaProfile p : new ArenaCatalogReader().load(assets()).profiles) {
            ArenaDataset data = new com.example.nbatickethub.data.mock.MockArenaRepository(assets()).load(p.dataAsset);
            float cx = data.arena.worldBounds.centerX(), cy = data.arena.worldBounds.centerY();
            for (int axis = 0; axis < 2; axis++) {
                float[] first = new float[2], adjacent = new float[2];
                ArenaSceneCompiler.project(p, data, cx + (axis == 0 ? 800 : 0), cy + (axis == 1 ? 800 : 0), first);
                ArenaSceneCompiler.project(p, data, cx + (axis == 0 ? 800 : .01f), cy + (axis == 1 ? 800 : .01f), adjacent);
                assertTrue(Math.hypot(first[0] - adjacent[0], first[1] - adjacent[1]) < .1);
            }
        }
    }

    @Test public void palettesAreDefensiveAndMalformedPaletteIsRejected() throws Exception {
        ArenaProfile p = new ArenaCatalogReader().load(assets()).profiles.get(0);
        ArenaTheme theme = new ArenaCatalogReader().load(assets()).resolveTheme(p, null);
        java.util.List<ArenaStands.Tier> tiers = new ArrayList<>();
        tiers.add(p.stands.tier(1));
        ArenaStands s = p.stands;
        ArenaStands copy = new ArenaStands(s.notice, s.background, s.concourse, s.labelBackground,
                s.labelText, s.rowLine, s.sectionBorder, tiers);
        tiers.clear();
        assertEquals(1, copy.tierCount());
        assertSame(s.tier(1), copy.tier(1));
        String json;
        try (InputStream stream = assets().open("venues/catalog.json")) {
            json = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        final String invalid = json.replace("#9DAFC4", "#GGGGGG");
        assertNotEquals(json, invalid);
        assertThrows(IOException.class, () -> new ArenaCatalogReader().read(new StringReader(invalid)));
    }

    @Test public void boundedRenderBatchesCoverEverySeatExactlyOnceWithoutChangingGeometry() throws Exception {
        for (ArenaProfile p : new ArenaCatalogReader().load(assets()).profiles) {
            ArenaDataset data = new com.example.nbatickethub.data.mock.MockArenaRepository(assets()).load(p.dataAsset);
            ArenaScene scene = new ArenaSceneCompiler().compile(p, data);
            java.util.BitSet seen = new java.util.BitSet(data.capacity());
            Map<String, GameSeat> inventory = data.inventory.values().iterator().next();
            for (SeatPathBatches.Batch batch : SeatPathBatches.build(scene)) {
                assertTrue(batch.size() > 0 && batch.size() <= SeatPathBatches.MAX_SEATS_PER_PATH);
                for (int i = 0; i < batch.size(); i++) {
                    int index = batch.geometryIndex(i);
                    assertFalse(seen.get(index));
                    seen.set(index);
                    Seat seat = data.seatOrder.get(index);
                    assertEquals(batch.sectionId, data.rows.get(seat.rowId).sectionId);
                    assertEquals(batch.status, inventory.get(seat.id).baseStatus);
                    assertEquals(index, seat.geometryIndex);
                }
            }
            assertEquals(data.capacity(), seen.cardinality());
            for (Section section : data.sections.values()) {
                float[] polygon = ArenaOutlines.section(scene, section.boundary);
                assertTrue(polygon.length >= 36 && polygon.length % 2 == 0);
                for (int i = 0; i < polygon.length; i += 2) {
                    assertTrue(polygon[i] >= 0 && polygon[i] <= p.geometry.displayBounds.right);
                    assertTrue(polygon[i + 1] >= 0 && polygon[i + 1] <= p.geometry.displayBounds.bottom);
                }
            }
        }
    }

    @Test public void batchPreparationHonorsCancellation() throws Exception {
        ArenaProfile p = new ArenaCatalogReader().load(assets()).profiles.get(0);
        ArenaDataset data = new com.example.nbatickethub.data.mock.MockArenaRepository(assets()).load(p.dataAsset);
        ArenaScene scene = new ArenaSceneCompiler().compile(p, data);
        try {
            Thread.currentThread().interrupt();
            assertThrows(InterruptedIOException.class, () -> SeatPathBatches.build(scene));
            assertThrows(InterruptedIOException.class, () -> ArenaOutlines.section(scene, data.sections.values().iterator().next().boundary));
        } finally { Thread.interrupted(); }
    }

    @Test public void sceneRejectsCourtThatIntrudesIntoStands() throws Exception {
        ArenaProfile p = new ArenaCatalogReader().load(assets()).profiles.get(0);
        ArenaDataset data = new com.example.nbatickethub.data.mock.MockArenaRepository(assets()).load(p.dataAsset);
        ArenaProfile oversized = new ArenaProfile(p.id, p.venueName, p.homeTeam, p.dataAsset,
                p.identitySource, p.referenceSource, p.retrievedOn, p.geometryNotice, new ArenaGeometry(p.geometry.displayBounds,
                p.geometry.scaleX, p.geometry.scaleY, p.geometry.bowlExponent, 5000, p.geometry.courtWidth,
                p.geometry.courtAngle), p.stands, p.defaultThemeId);
        assertThrows(IllegalArgumentException.class, () -> new ArenaSceneCompiler().compile(oversized, data));
    }

    @Test public void catalogRejectsUnknownSchemaAndFalseProvenance() throws Exception {
        String json;
        try (InputStream input = assets().open("venues/catalog.json")) {
            json = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        final String text = json;
        assertThrows(IOException.class, () -> new ArenaCatalogReader().read(new StringReader(
                text.replace("\"schemaVersion\":4", "\"schemaVersion\":4.00000001"))));
        assertThrows(IOException.class, () -> new ArenaCatalogReader().read(new StringReader(text.replace("MOCK", "OFFICIAL"))));
    }

    @Test public void catalogSupportsThirtyEntriesWithoutHardCodedVenueBranches() throws Exception {
        ArenaProfile p = new ArenaCatalogReader().load(assets()).profiles.get(0);
        List<ArenaProfile> profiles = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            profiles.add(new ArenaProfile("test-" + i, "Synthetic test venue " + i,
                    new Team("test-team-" + i, "Synthetic team " + i, "T" + i), p.dataAsset,
                    p.identitySource, p.referenceSource, p.retrievedOn, p.geometryNotice, p.geometry, p.stands, p.defaultThemeId));
        }
        ArenaCatalog catalog = new ArenaCatalog(profiles, new ArenaCatalogReader().load(assets()).themes);
        assertEquals(30, catalog.profiles.size());
        assertEquals("test-29", catalog.byHomeTeamId.get("test-team-29").id);
        profiles.add(profiles.get(0));
        assertThrows(IllegalArgumentException.class, () -> new ArenaCatalog(profiles, new ArenaCatalogReader().load(assets()).themes));
    }
}
