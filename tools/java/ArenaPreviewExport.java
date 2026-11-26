import com.example.nbatickethub.data.*;
import com.example.nbatickethub.data.mock.MockArenaRepository;
import com.example.nbatickethub.geometry.*;
import com.example.nbatickethub.model.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Exports the real Java geometry pipeline. Never regenerates coordinates in JavaScript. */
public final class ArenaPreviewExport {
    private static Writer out;
    private static void text(String value) throws IOException { out.write(value); }
    private static void string(String value) throws IOException {
        text("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '"') { text("\\" + c); }
            else if (c < 32 || c == '<' || c == '>' || c == '&') { text(String.format(Locale.ROOT, "\\u%04x", (int) c)); }
            else { out.write(c); }
        }
        text("\"");
    }
    private static void floats(float[] values) throws IOException {
        text("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) { text(","); }
            if (!Float.isFinite(values[i])) { throw new IOException("Non-finite geometry"); }
            text(Float.toString(values[i]));
        }
        text("]");
    }
    private static String digest(byte[] bytes) throws Exception {
        StringBuilder result = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) { result.append(String.format("%02x", b & 255)); }
        return result.toString();
    }
    private static String geometryDigest(ArenaScene scene) throws Exception {
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        byte[] bytes = new byte[4];
        for (SeatGeometry seat : scene.seats) {
            for (int i = 0; i < 8; i++) {
                int bits = Float.floatToIntBits(i % 2 == 0 ? seat.x(i / 2) : seat.y(i / 2));
                for (int b = 0; b < 4; b++) { bytes[b] = (byte) (bits >>> (b * 8)); }
                hash.update(bytes);
            }
        }
        StringBuilder result = new StringBuilder();
        for (byte b : hash.digest()) { result.append(String.format("%02x", b & 255)); }
        return result.toString();
    }

    private static void colors(String[] names, int[] values) throws IOException {
        for (int i = 0; i < names.length; i++) {
            if (i > 0) { text(","); }
            string(names[i]); text(":"); string(String.format("#%06x", values[i] & 0xffffff));
        }
    }
    private static void nullable(String value) throws IOException {
        if (value == null) { text("null"); } else { string(value); }
    }
    private static void primitive(CourtDesign.Primitive p) throws IOException {
        text("{\"part\":"); string(p.part); text(",\"type\":"); string(p.type);
        text(","); colors(new String[]{"color"}, new int[]{p.color});
        text(",\"strokeWidth\":" + p.strokeWidth + ",\"points\":"); floats(p.copyPoints()); text("}");
    }
    private static void stands(ArenaStands stands) throws IOException {
        text("{\"origin\":\"MOCK\",\"notice\":"); string(stands.notice); text(",");
        colors(new String[]{"background", "concourse", "labelBackground", "labelText", "rowLine", "sectionBorder"},
                new int[]{stands.background, stands.concourse, stands.labelBackground, stands.labelText,
                        stands.rowLine, stands.sectionBorder});
        text(",\"tiers\":[");
        for (int i = 1; i <= stands.tierCount(); i++) {
            if (i > 1) { text(","); } ArenaStands.Tier tier = stands.tier(i); text("{");
            colors(new String[]{"section", "seatBackground", "accent"},
                    new int[]{tier.section, tier.seatBackground, tier.accent}); text("}");
        }
        text("]}");
    }
    private static void theme(ArenaTheme theme, AssetSource assets) throws IOException {
        text("{\"id\":"); string(theme.id); text(",\"teamId\":"); string(theme.teamId);
        text(",\"name\":"); string(theme.name); text(",\"origin\":\"MOCK\",\"notice\":"); string(theme.notice);
        text(","); colors(new String[]{"primaryColor", "secondaryColor"}, new int[]{theme.primaryColor, theme.secondaryColor});
        text(",\"seatThemeNotice\":"); string(theme.seatThemeNotice);
        text(",\"seatPalette\":[");
        for (int i = 1; i <= 16; i++) {
            if (i > 1) { text(","); } ArenaStands.Tier tier = theme.seatTier(i); text("{");
            colors(new String[]{"section", "seatBackground", "accent"}, new int[]{tier.section, tier.seatBackground, tier.accent}); text("}");
        }
        text("],\"court\":{"); ArenaTheme.Court c = theme.court;
        colors(new String[]{"wood", "woodAlternate", "lines", "apron", "paint", "lettering", "center"},
                new int[]{c.wood, c.woodAlternate, c.lines, c.apron, c.paint, c.lettering, c.center});
        text(",\"baselineText\":"); string(c.baselineText); text(",\"centerText\":"); string(c.centerText);
        text(",\"design\":{\"woodFeature\":"); string(c.design.woodFeature);
        text(",\"graphicFeature\":"); string(c.design.graphicFeature); text(",\"primitives\":[");
        for (int i = 0; i < c.design.primitives.size(); i++) { if (i > 0) { text(","); } primitive(c.design.primitives.get(i)); }
        CourtDesign.Logo logo = c.design.logo;
        text("],\"logo\":{\"asset\":"); nullable(logo.asset); text(",\"source\":"); nullable(logo.source);
        text(",\"license\":"); nullable(logo.license); text(",\"fallback\":"); string(logo.fallback);
        text(",\"width\":" + logo.width + ",\"height\":" + logo.height + "}}}");
        text(",\"logoImage\":");
        boolean embedded = false;
        if (logo.asset != null) {
            try (InputStream input = assets.open(logo.asset)) {
                byte[] bytes = input.readNBytes(2_000_001);
                if (bytes.length <= 2_000_000) {
                    java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
                    if (image != null && (long) image.getWidth() * image.getHeight() <= 2_000_000) {
                        text("{\"dataUrl\":"); string("data:image/png;base64," + Base64.getEncoder().encodeToString(bytes));
                        text(",\"bounds\":"); floats(logo.fit(image.getWidth(), image.getHeight())); text("}"); embedded = true;
                    }
                }
            } catch (IOException error) { System.err.println("Logo unavailable, using code: " + theme.teamId); }
        }
        if (!embedded) { text("null"); }
        text("}");
    }

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args[0]);
        AssetSource assets = name -> Files.newInputStream(root.resolve(name));
        ArenaCatalog catalog = new ArenaCatalogReader().load(assets);
        String template = Files.readString(Paths.get(args[1]), StandardCharsets.UTF_8);
        if (!template.contains("<!--ARENA_DATA-->")) { throw new IOException("Template data marker missing"); }
        try (Writer writer = Files.newBufferedWriter(Paths.get(args[2]), StandardCharsets.UTF_8)) {
            out = writer;
            text(template.substring(0, template.indexOf("<!--ARENA_DATA-->")));
            text("<script id=\"arena-data\" type=\"application/json\">{\"schemaVersion\":4,\"catalogSha256\":");
            string(digest(Files.readAllBytes(root.resolve("venues/catalog.json"))));
            text(",\"pipelineSources\":{");
            Path javaRoot = root.getParent().resolve("java/com/example/nbatickethub");
            List<Path> sourceFiles = new ArrayList<>();
            for (String folder : new String[]{"data", "geometry", "model"}) {
                try (java.util.stream.Stream<Path> files = Files.walk(javaRoot.resolve(folder))) {
                    files.filter(file -> file.toString().endsWith(".java")).forEach(sourceFiles::add);
                }
            }
            sourceFiles.sort(Comparator.comparing(Path::toString));
            int sourceIndex = 0;
            for (Path file : sourceFiles) {
                if (sourceIndex++ > 0) { text(","); }
                string(javaRoot.relativize(file).toString().replace('\\', '/'));
                text(":"); string(digest(Files.readAllBytes(file)));
            }
            text("}");
            text(",\"style\":{");
            text("\"labelSize\":" + ArenaRenderStyle.LABEL_SIZE
                    + ",\"rowStroke\":" + ArenaRenderStyle.ROW_STROKE
                    + ",\"sectionStroke\":" + ArenaRenderStyle.SECTION_STROKE);
            text("},\"seatStatusPalette\":{");
            SeatStatusPalette status = catalog.themes.get(0).statuses;
            colors(new String[]{"available", "booked", "locked"},
                    new int[]{status.available, status.booked, status.locked});
            text("},\"themes\":[");
            for (int i = 0; i < catalog.themes.size(); i++) {
                if (i > 0) { text(","); } theme(catalog.themes.get(i), assets);
            }
            text("],\"courtMarkings\":[");
            for (int i = 0; i < CourtMarkings.ALL.size(); i++) { if (i > 0) { text(","); } primitive(CourtMarkings.ALL.get(i)); }
            text("],\"homeArenas\":{"); int homeIndex = 0;
            for (Map.Entry<String, ArenaProfile> entry : catalog.byHomeTeamId.entrySet()) {
                if (homeIndex++ > 0) { text(","); } string(entry.getKey()); text(":"); string(entry.getValue().id);
            }
            text("},\"arenas\":[");
            int arenaIndex = 0;
            for (ArenaProfile p : catalog.profiles) {
                long started = System.nanoTime();
                ArenaDataset data = new MockArenaRepository(assets).load(p.dataAsset);
                long loaded = System.nanoTime();
                ArenaScene scene = new ArenaSceneCompiler().compile(p, data);
                long projected = System.nanoTime();
                List<SeatPathBatches.Batch> batches = SeatPathBatches.build(scene);
                long batched = System.nanoTime();
                if (arenaIndex++ > 0) { text(","); }
                Writer htmlWriter = out;
                StringWriter arenaWriter = new StringWriter(); out = arenaWriter;
                text("{\"id\":"); string(p.id);
                text(",\"name\":"); string(p.venueName);
                text(",\"team\":"); string(p.homeTeam.displayName);
                text(",\"shortName\":"); string(p.homeTeam.shortName);
                text(",\"notice\":"); string(p.geometryNotice);
                text(",\"asset\":"); string(p.dataAsset);
                text(",\"sha256\":"); string(digest(Files.readAllBytes(root.resolve(p.dataAsset))));
                text(",\"geometrySha256\":"); string(geometryDigest(scene));
                text(",\"identitySource\":"); string(p.identitySource);
                text(",\"referenceSource\":"); string(p.referenceSource);
                text(",\"stands\":"); stands(p.stands);
                text(",\"world\":"); floats(new float[]{p.geometry.displayBounds.right, p.geometry.displayBounds.bottom});
                text(",\"court\":"); floats(new float[]{p.geometry.courtLength, p.geometry.courtWidth, p.geometry.courtAngle});
                text(",\"defaultThemeId\":"); string(catalog.resolveTheme(p, null).id);
                text(",\"levelCount\":" + data.levels.size() + ",\"rowCount\":" + data.rows.size());
                float outer = 0;
                for (ArenaLevel level : data.levels.values()) { outer = Math.max(outer, level.footprint.outerRadius); }
                text(",\"envelope\":"); floats(ArenaOutlines.arc(scene, new AnnularSector(data.arena.worldBounds.centerX(),
                        data.arena.worldBounds.centerY(), 1, outer + 12, 0, 360), outer + 12, false));
                text(",\"levels\":["); int levelIndex = 0;
                for (ArenaLevel level : data.levels.values()) {
                    if (levelIndex++ > 0) { text(","); }
                    text("{\"code\":"); string(level.code); text(",\"name\":"); string(level.name);
                    text(",\"order\":" + level.order + "}");
                }
                text("]");
                text(",\"sections\":[");
                int index = 0;
                for (Section section : data.sections.values()) {
                    if (index++ > 0) { text(","); }
                    text("{\"id\":"); string(section.id);
                    text(",\"levelOrder\":" + data.levels.get(section.levelId).order);
                    text(",\"label\":"); string(section.code.replace("SIM-", "") + "*");
                    text(",\"polygon\":"); floats(ArenaOutlines.section(scene, section.boundary));
                    text(",\"labelPoint\":"); floats(ArenaOutlines.label(scene, section.boundary)); text("}");
                }
                text("],\"rows\":["); index = 0;
                for (SeatRow row : data.rows.values()) {
                    if (index++ > 0) { text(","); }
                    text("["); string(row.id); text(","); string(row.sectionId); text(",");
                    floats(ArenaOutlines.arc(scene, row.boundary,
                            (row.boundary.innerRadius + row.boundary.outerRadius) / 2, false)); text("]");
                }
                text("],\"seats\":["); index = 0;
                Map<String, GameSeat> inventory = data.inventory.values().iterator().next();
                for (Seat seat : data.seatOrder) {
                    if (index++ > 0) { text(","); }
                    text("["); string(seat.id); text(","); string(seat.rowId);
                    text("," + inventory.get(seat.id).baseStatus.ordinal() + ",");
                    floats(scene.seats.get(seat.geometryIndex).copyVertices()); text(",");
                    floats(ArenaSeatMarkers.marker(scene.seats.get(seat.geometryIndex))); text("]");
                }
                text("],\"batches\":["); index = 0;
                for (SeatPathBatches.Batch batch : batches) {
                    if (index++ > 0) { text(","); }
                    text("[" + batch.status.ordinal());
                    for (int i = 0; i < batch.size(); i++) { text("," + batch.geometryIndex(i)); }
                    text("]");
                }
                text("]}");
                String arenaJson = arenaWriter.toString(); out = htmlWriter;
                String scriptPath = "arena-data/" + p.id + ".js";
                Path payloadPath = Paths.get(args[2]).getParent().resolve(scriptPath);
                Files.createDirectories(payloadPath.getParent());
                byte[] payload = ("window.ArenaPreviewData.receive(" + arenaJson + ");").getBytes(StandardCharsets.UTF_8);
                Files.write(payloadPath, payload);
                // Lightweight outlines are embedded; individual seats load only for the selected home.
                text(arenaJson.substring(0, arenaJson.indexOf(",\"rows\":[")));
                text(",\"capacity\":" + data.capacity() + ",\"dataScript\":"); string(scriptPath);
                text(",\"dataSha256\":"); string(digest(payload)); text("}");
                System.out.printf(Locale.ROOT,
                        "%s seats=%d sections=%d batches=%d loadValidateGenerate=%.1fms project=%.1fms batch=%.1fms jvmCliThread=%s%n",
                        p.id, data.capacity(), data.sections.size(), batches.size(), (loaded - started) / 1e6,
                        (projected - loaded) / 1e6, (batched - projected) / 1e6, Thread.currentThread().getName());
            }
            text("]}</script>");
            text(template.substring(template.indexOf("<!--ARENA_DATA-->") + "<!--ARENA_DATA-->".length()));
        }
    }
}
