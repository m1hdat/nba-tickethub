package com.example.nbatickethub.data;

import com.example.nbatickethub.data.json.StrictJson;
import com.example.nbatickethub.model.ArenaProfile;
import com.example.nbatickethub.model.ArenaGeometry;
import com.example.nbatickethub.model.ArenaStands;
import com.example.nbatickethub.model.CourtDesign;
import com.example.nbatickethub.model.ArenaTheme;
import com.example.nbatickethub.model.SeatStatusPalette;
import com.example.nbatickethub.model.Team;
import com.example.nbatickethub.model.WorldRect;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reads referenced identity separately from the MOCK geometry/inventory manifest. */
public final class ArenaCatalogReader {
    public ArenaCatalog load(AssetSource source) throws IOException {
        try (InputStream stream = source.open("venues/catalog.json");
             Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8.newDecoder()
                     .onMalformedInput(CodingErrorAction.REPORT))) {
            return read(reader);
        }
    }

    public ArenaCatalog read(Reader reader) throws IOException {
        try {
            Map<?, ?> root = object(StrictJson.read(reader));
            if (!BigDecimal.valueOf(4).equals(root.get("schemaVersion"))) {
                throw new IllegalArgumentException("Unsupported catalog schema");
            }
            Object entries = root.get("arenas");
            if (!(entries instanceof List)) { throw new IllegalArgumentException("Missing arena list"); }
            List<ArenaProfile> profiles = new ArrayList<>();
            for (Object value : (List<?>) entries) {
                Map<?, ?> item = object(value);
                if (!"MOCK".equals(text(item, "geometryOrigin"))
                        || !("CC0-1.0".equals(text(item, "identityLicense"))
                        || "FACTUAL_METADATA".equals(text(item, "identityLicense")))) {
                    throw new IllegalArgumentException("Expected MOCK geometry and explicit identity provenance");
                }
                Map<?, ?> team = object(item.get("homeTeam"));
                if (!text(team, "source").startsWith("https://")) {
                    throw new IllegalArgumentException("Home team metadata source missing");
                }
                Map<?, ?> view = object(item.get("geometry"));
                profiles.add(new ArenaProfile(text(item, "id"), text(item, "venueName"),
                        new Team(text(team, "id"), text(team, "name"), text(team, "shortName")),
                        text(item, "dataAsset"), text(item, "identitySource"),
                        text(item, "referenceSource"), text(item, "retrievedOn"),
                        text(item, "geometryNotice"),
                        new ArenaGeometry(new WorldRect(0, 0, number(view, "width"), number(view, "height")),
                        number(view, "scaleX"), number(view, "scaleY"), number(view, "bowlExponent"),
                        number(view, "courtLength"), number(view, "courtWidth"), number(view, "courtAngle")),
                        stands(object(item.get("stands"))), text(item, "defaultThemeId"), item.get("previewGameThemeId") == null
                        ? null : text(item, "previewGameThemeId")));
            }
            Map<?, ?> status = object(root.get("seatStatusPalette"));
            SeatStatusPalette statuses = new SeatStatusPalette(color(status, "available"),
                    color(status, "booked"), color(status, "locked"));
            List<ArenaTheme> themes = new ArrayList<>();
            for (Object value : array(root, "themes")) {
                Map<?, ?> theme = object(value), court = object(theme.get("court"));
                if (!"MOCK".equals(text(theme, "origin"))) {
                    throw new IllegalArgumentException("Expected explicit MOCK theme");
                }
                themes.add(new ArenaTheme(text(theme, "id"), text(theme, "teamId"), text(theme, "name"),
                        text(theme, "notice"), new ArenaTheme.Court(color(court, "wood"),
                        color(court, "woodAlternate"), color(court, "lines"), color(court, "apron"),
                        color(court, "paint"), color(court, "lettering"), color(court, "center"),
                        text(court, "baselineText"), text(court, "centerText"), design(object(court.get("design")))), statuses,
                        color(theme, "primaryColor"), color(theme, "secondaryColor"), text(theme, "seatThemeNotice")));
            }
            Map<?, ?> mapping = object(root.get("homeArenas"));
            Map<String, String> homes = new java.util.LinkedHashMap<>();
            for (Object key : mapping.keySet()) {
                if (!(key instanceof String)) { throw new IllegalArgumentException("Expected team ID"); }
                homes.put((String) key, text(mapping, (String) key));
            }
            return new ArenaCatalog(profiles, themes, homes);
        } catch (IllegalArgumentException error) {
            throw new IOException("Invalid arena catalog: " + error.getMessage(), error);
        }
    }

    private static ArenaStands stands(Map<?, ?> value) {
        if (!"MOCK".equals(text(value, "origin"))) { throw new IllegalArgumentException("Expected MOCK stands"); }
        List<ArenaStands.Tier> tiers = new ArrayList<>();
        for (Object item : array(value, "tiers")) {
            Map<?, ?> tier = object(item);
            tiers.add(new ArenaStands.Tier(color(tier, "section"), color(tier, "seatBackground"), color(tier, "accent")));
        }
        return new ArenaStands(text(value, "notice"), color(value, "background"), color(value, "concourse"),
                color(value, "labelBackground"), color(value, "labelText"), color(value, "rowLine"),
                color(value, "sectionBorder"), tiers);
    }

    private static CourtDesign design(Map<?, ?> value) {
        List<CourtDesign.Primitive> primitives = new ArrayList<>();
        for (Object item : array(value, "primitives")) {
            Map<?, ?> primitive = object(item);
            List<?> points = array(primitive, "points");
            float[] coords = new float[points.size()];
            for (int i = 0; i < coords.length; i++) {
                if (!(points.get(i) instanceof BigDecimal)) { throw new IllegalArgumentException("Expected coordinate"); }
                coords[i] = ((BigDecimal) points.get(i)).floatValue();
            }
            primitives.add(new CourtDesign.Primitive(text(primitive, "part"), text(primitive, "type"),
                    color(primitive, "color"), number(primitive, "strokeWidth"), coords));
        }
        Map<?, ?> logo = object(value.get("logo"));
        return new CourtDesign(text(value, "woodFeature"), text(value, "graphicFeature"), primitives,
                new CourtDesign.Logo(optionalText(logo, "asset"), optionalText(logo, "source"),
                        optionalText(logo, "license"), text(logo, "fallback"), number(logo, "width"), number(logo, "height")));
    }

    private static String optionalText(Map<?, ?> value, String key) {
        return value.get(key) == null ? null : text(value, key);
    }

    private static Map<?, ?> object(Object value) {
        if (!(value instanceof Map)) { throw new IllegalArgumentException("Expected object"); }
        return (Map<?, ?>) value;
    }

    private static String text(Map<?, ?> item, String key) {
        Object value = item.get(key);
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            throw new IllegalArgumentException("Expected string: " + key);
        }
        return (String) value;
    }

    private static float number(Map<?, ?> item, String key) {
        Object value = item.get(key);
        if (!(value instanceof BigDecimal)) { throw new IllegalArgumentException("Expected number: " + key); }
        float result = ((BigDecimal) value).floatValue();
        if (!Float.isFinite(result)) { throw new IllegalArgumentException("Non-finite value: " + key); }
        return result;
    }

    private static int color(Map<?, ?> item, String key) {
        String value = text(item, key);
        if (!value.matches("#[0-9a-fA-F]{6}")) { throw new IllegalArgumentException("Invalid color"); }
        return (int) (0xff000000L | Long.parseLong(value.substring(1), 16));
    }

    private static List<?> array(Map<?, ?> view, String key) {
        Object value = view.get(key);
        if (!(value instanceof List) || ((List<?>) value).isEmpty()) {
            throw new IllegalArgumentException("Expected nonempty array: " + key);
        }
        return (List<?>) value;
    }
}
