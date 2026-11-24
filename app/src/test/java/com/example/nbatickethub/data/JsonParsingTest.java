package com.example.nbatickethub.data;

import com.example.nbatickethub.data.json.StrictJson;
import com.example.nbatickethub.data.mock.MockArenaJsonReader;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

import static org.junit.Assert.*;

public class JsonParsingTest {
    @Test
    public void strictJsonPreservesExactNumbersAndEscapes() throws IOException {
        Map<?, ?> result = (Map<?, ?>) StrictJson.read(new StringReader(
                "{\"price\":9223372036854775807,\"name\":\"Vi\\u1ec7t\\n\\uD83C\\uDFC0\"}"));
        assertEquals(Long.MAX_VALUE, ((BigDecimal) result.get("price")).longValueExact());
        assertEquals("Việt\n🏀", result.get("name"));
        List<?> values = (List<?>) StrictJson.read(new StringReader(" [ true, false, null, 1e2 ] "));
        assertEquals(Boolean.TRUE, values.get(0));
        assertEquals(Boolean.FALSE, values.get(1));
        assertNull(values.get(2));
        assertEquals(100, ((BigDecimal) values.get(3)).intValueExact());
    }

    @Test
    public void rejectsMalformedAndAmbiguousJson() {
        for (String input : new String[]{"", "{", "[1,]", "{\"a\":1,}",
                "{\"a\":null,\"a\":2}", "01", "+1", "1.", "NaN", "true false",
                "\"\\uD800\"", "\"\\u１２３４\"", "\"unclosed", "\"line\nbreak\"", "/*comment*/{}"}) {
            assertThrows(input, IOException.class, () -> StrictJson.read(new StringReader(input)));
        }
    }

    @Test
    public void rejectsDeepOrOversizedManifests() {
        assertThrows(IOException.class, () -> StrictJson.read(new StringReader(
                "[".repeat(70) + "0" + "]".repeat(70))));
        assertThrows(IOException.class, () -> StrictJson.read(new StringReader(
                "\"" + "a".repeat(1_048_576) + "\"")));
    }

    @Test
    public void schemaRejectsMissingUnknownAndWrongTypedFields() throws IOException {
        String json = TestAssets.text("arena-small");
        invalid(json.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2"), "schemaVersion");
        invalid(json.replace("\"geometryVersion\":", "\"geometryVersoin\":"), "Unknown field");
        invalid(json.replace("\"seatsPerRow\": 5", "\"seatsPerRow\": \"5\""), "number");
        invalid(json.replace("\"rowCount\": 2", "\"rowCount\": 1.5"), "Invalid mock arena");
        invalid(json.replace("\"rowCount\": 2", "\"rowCount\": 2147483648"), "Invalid mock arena");
        invalid(json.replace("\"originType\": \"MOCK\"", "\"originType\": \"REAL_SNAPSHOT\""), "MOCK");
        invalid(json.replace("\"mockBasePriceMinor\": 13000", "\"mockBasePriceMinor\": -1"), "Negative");
        invalid(json.replace("\"innerRadius\": 340", "\"innerRadius\": 1e100"), "Non-finite");
    }

    @Test
    public void capacityAndInventoryBudgetsFailBeforeAllocation() throws IOException {
        String json = TestAssets.text("arena-small");
        assertThrows(IOException.class, () -> new MockArenaJsonReader(199, 1000)
                .read(new StringReader(json)));
        assertThrows(IOException.class, () -> new MockArenaJsonReader(1000, 399)
                .read(new StringReader(json)));
        invalid(json.replace("\"rowCount\": 2", "\"rowCount\": 0"), "positive");
        invalid(json.replace("\"priceFactorPercent\": 100", "\"priceFactorPercent\": 0"), "percentages");
        invalid(json.replace("\"bookedPercent\": 15", "\"bookedPercent\": 100"), "percentages");
        invalid(json.replace("\"mockBasePriceMinor\": 13000",
                "\"mockBasePriceMinor\": 9223372036854775807"), "overflow");
    }

    private void invalid(String json, String message) {
        IOException error = assertThrows(IOException.class, () -> TestAssets.parse(json));
        assertTrue(error.getMessage(), error.getMessage().contains(message));
    }
}
