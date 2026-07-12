package media.jlt.minecraft.mods.ores.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModConfigOrderingTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;

    @Test
    void defaultSerializationMatchesCheckedInExampleIncludingMapOrder() throws IOException {
        JsonObject example = JsonParser.parseString(
            Files.readString(Path.of("config/jlt_ores.example.json")).strip()
        ).getAsJsonObject();
        JsonObject generated = JsonParser.parseString(
            new ModConfig().sanitize(LOGGER).toJson()
        ).getAsJsonObject();

        assertEquals(example.toString(), generated.toString());
        assertEquals(
            keyOrder(example.getAsJsonObject("minimumTierByOreFamily")),
            keyOrder(generated.getAsJsonObject("minimumTierByOreFamily"))
        );
        assertEquals(
            keyOrder(example.getAsJsonObject("_docs")),
            keyOrder(generated.getAsJsonObject("_docs"))
        );
    }

    private static List<String> keyOrder(JsonObject object) {
        return object.keySet().stream().toList();
    }
}
