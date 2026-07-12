package media.jlt.minecraft.mods.ores;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FabricModMetadataTest {
    @Test
    void declaresStableModIdentityEntrypointAndDependencies() throws IOException {
        JsonObject metadata = JsonParser.parseString(
            Files.readString(Path.of("src/main/resources/fabric.mod.json"))
        ).getAsJsonObject();

        assertEquals(1, metadata.get("schemaVersion").getAsInt());
        assertEquals("jlt_ores", metadata.get("id").getAsString());
        assertEquals("${version}", metadata.get("version").getAsString());
        assertEquals("*", metadata.get("environment").getAsString());
        assertEquals(
            "media.jlt.minecraft.mods.ores.OresMod",
            metadata.getAsJsonObject("entrypoints").getAsJsonArray("main").get(0).getAsString()
        );

        JsonObject dependencies = metadata.getAsJsonObject("depends");
        assertEquals(">=0.19.3", dependencies.get("fabricloader").getAsString());
        assertEquals("~26.2", dependencies.get("minecraft").getAsString());
        assertEquals(">=25", dependencies.get("java").getAsString());
        assertEquals("*", dependencies.get("fabric-api").getAsString());
    }
}
