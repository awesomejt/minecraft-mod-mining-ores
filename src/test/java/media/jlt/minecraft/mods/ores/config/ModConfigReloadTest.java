package media.jlt.minecraft.mods.ores.config;

import media.jlt.minecraft.mods.ores.logic.OreFamily;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModConfigReloadTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;

    @TempDir
    Path tempDirectory;

    @Test
    void validConfigIsSanitizedAndRewritten() throws IOException {
        Path configFile = tempDirectory.resolve("jlt_ores.json");
        Files.writeString(configFile, """
            {
              "maxMineDistance": 999,
              "minimumTierByOreFamily": {
                "ancient_debris": "netherit"
              }
            }
            """);

        ModConfig.ReloadResult result = ModConfig.reload(configFile, LOGGER);

        assertTrue(result.success());
        assertEquals(128, result.config().maxMineDistance);
        assertEquals("netherite", result.config().minimumTierByOreFamily.get(OreFamily.ANCIENT_DEBRIS.configKey()));
        String rewritten = Files.readString(configFile);
        assertTrue(rewritten.contains("\"maxMineDistance\": 128"));
        assertTrue(rewritten.contains("\"ancient_debris\": \"netherite\""));
    }

    @Test
    void malformedConfigFailsWithoutOverwritingInput() throws IOException {
        Path configFile = tempDirectory.resolve("jlt_ores.json");
        String malformed = "{ \"enabled\": tru";
        Files.writeString(configFile, malformed);

        ModConfig.ReloadResult result = ModConfig.reload(configFile, LOGGER);

        assertFalse(result.success());
        assertNotNull(result.errorMessage());
        assertEquals(malformed, Files.readString(configFile));
    }

    @Test
    void missingConfigIsCreatedFromDefaults() throws IOException {
        Path configFile = tempDirectory.resolve("nested/jlt_ores.json");

        ModConfig.ReloadResult result = ModConfig.reload(configFile, LOGGER);

        assertTrue(result.success());
        assertTrue(result.config().enabled);
        assertTrue(Files.exists(configFile));
        assertTrue(Files.readString(configFile).contains("\"maxVeinBlocks\": 64"));
    }

    @Test
    void emptyAndJsonNullConfigsRegenerateDefaults() throws IOException {
        Path emptyConfig = tempDirectory.resolve("empty.json");
        Path nullConfig = tempDirectory.resolve("null.json");
        Files.writeString(emptyConfig, "");
        Files.writeString(nullConfig, "null");

        ModConfig.ReloadResult emptyResult = ModConfig.reload(emptyConfig, LOGGER);
        ModConfig.ReloadResult nullResult = ModConfig.reload(nullConfig, LOGGER);

        assertTrue(emptyResult.success());
        assertTrue(nullResult.success());
        assertEquals(64, emptyResult.config().maxMineDistance);
        assertEquals(64, nullResult.config().maxMineDistance);
        assertTrue(Files.readString(emptyConfig).contains("\"enabled\": true"));
        assertTrue(Files.readString(nullConfig).contains("\"enabled\": true"));
    }

    @Test
    void unwritableTargetReturnsFailureWithoutChangingBlockingFile() throws IOException {
        Path blockingFile = tempDirectory.resolve("not-a-directory");
        Files.writeString(blockingFile, "keep me");
        Path configFile = blockingFile.resolve("jlt_ores.json");

        ModConfig.ReloadResult result = ModConfig.reload(configFile, LOGGER);

        assertFalse(result.success());
        assertNotNull(result.errorMessage());
        assertEquals("keep me", Files.readString(blockingFile));
    }
}
