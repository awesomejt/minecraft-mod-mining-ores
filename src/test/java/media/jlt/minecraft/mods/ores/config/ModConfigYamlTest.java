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

class ModConfigYamlTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;

    @TempDir
    Path tempDirectory;

    @Test
    void missingConfigIsCreatedFromSanitizedDefaultsAsNestedYaml() throws IOException {
        Path yamlFile = tempDirectory.resolve("jlt_ores.yaml");
        Path jsonFile = tempDirectory.resolve("jlt_ores.json");

        ModConfig.ReloadResult result = ModConfig.loadOrMigrateYaml(yamlFile, jsonFile, LOGGER);

        assertTrue(result.success());
        assertTrue(result.config().enabled);
        assertTrue(Files.exists(yamlFile));
        String written = Files.readString(yamlFile);
        assertTrue(written.contains("gates:"));
        assertTrue(written.contains("economy:"));
        assertTrue(written.startsWith("gates:"), "expected 'gates' (declared first) at the top:\n" + written);
    }

    @Test
    void existingLegacyJsonMigratesValuesAndIsKeptAsBackup() throws IOException {
        Path yamlFile = tempDirectory.resolve("jlt_ores.yaml");
        Path jsonFile = tempDirectory.resolve("jlt_ores.json");
        Files.writeString(jsonFile, """
            {
              "maxMineDistance": 999,
              "durabilityMultiplier": 7
            }
            """);

        ModConfig.ReloadResult result = ModConfig.loadOrMigrateYaml(yamlFile, jsonFile, LOGGER);

        assertTrue(result.success());
        assertEquals(128, result.config().maxMineDistance);
        assertEquals(7, result.config().durabilityMultiplier);
        assertTrue(Files.exists(yamlFile));
        assertFalse(Files.exists(jsonFile));
        assertTrue(Files.exists(jsonFile.resolveSibling("jlt_ores.json.bak")));
        String dumped = Files.readString(yamlFile);
        assertTrue(dumped.contains("maxMineDistance: 128"));
        assertTrue(dumped.contains("durabilityMultiplier: 7"));
    }

    @Test
    void existingYamlTakesPrecedenceOverLegacyJsonWhenBothPresent() throws IOException {
        Path yamlFile = tempDirectory.resolve("jlt_ores.yaml");
        Path jsonFile = tempDirectory.resolve("jlt_ores.json");
        Files.writeString(jsonFile, "{ \"maxMineDistance\": 20 }");
        ModConfig.loadOrMigrateYaml(yamlFile, jsonFile, LOGGER);
        // Simulate a stray re-created legacy file after migration already happened once.
        Files.writeString(jsonFile, "{ \"maxMineDistance\": 999 }");

        ModConfig.ReloadResult result = ModConfig.loadOrMigrateYaml(yamlFile, jsonFile, LOGGER);

        assertTrue(result.success());
        assertEquals(20, result.config().maxMineDistance);
    }

    @Test
    void nestedToolTierMapAndEfficiencyDelaysRoundTrip() throws IOException {
        Path yamlFile = tempDirectory.resolve("jlt_ores.yaml");
        Path jsonFile = tempDirectory.resolve("jlt_ores.json");
        Files.writeString(yamlFile, """
            timing:
              blockBreakDelayTicksByEfficiencyLevel:
                "0": 20
                "5": 1
            scanBounds:
              maxMineDistance: 100
            toolTiers:
              diamond: netherite
            """);

        ModConfig.ReloadResult result = ModConfig.loadOrMigrateYaml(yamlFile, jsonFile, LOGGER);

        assertTrue(result.success());
        assertEquals(20, result.config().delayTicksForEfficiencyLevel(0));
        assertEquals(1, result.config().delayTicksForEfficiencyLevel(5));
        assertEquals("netherite", result.config().minimumTierByOreFamily.get(OreFamily.DIAMOND.configKey()));
    }

    @Test
    void malformedYamlFailsWithoutOverwritingInput() throws IOException {
        Path yamlFile = tempDirectory.resolve("jlt_ores.yaml");
        Path jsonFile = tempDirectory.resolve("jlt_ores.json");
        String malformed = "gates: [enabled: true\n";
        Files.writeString(yamlFile, malformed);

        ModConfig.ReloadResult result = ModConfig.loadOrMigrateYaml(yamlFile, jsonFile, LOGGER);

        assertFalse(result.success());
        assertNotNull(result.errorMessage());
        assertEquals(malformed, Files.readString(yamlFile));
    }

    @Test
    void dumpedYamlCarriesDocCommentsFromExistingDefaultDocs() throws IOException {
        Path yamlFile = tempDirectory.resolve("jlt_ores.yaml");
        Path jsonFile = tempDirectory.resolve("jlt_ores.json");

        ModConfig.loadOrMigrateYaml(yamlFile, jsonFile, LOGGER);

        String dumped = Files.readString(yamlFile);
        assertTrue(dumped.contains("# Default: true. Values: true|false. Master switch for all auto-mine behavior.\n  enabled:"),
            dumped);
    }

    @Test
    void defaultSerializationMatchesCheckedInYamlExample() throws IOException {
        Path yamlFile = tempDirectory.resolve("jlt_ores.yaml");
        Path jsonFile = tempDirectory.resolve("jlt_ores.json");

        ModConfig.loadOrMigrateYaml(yamlFile, jsonFile, LOGGER);

        String generated = Files.readString(yamlFile).strip();
        String example = Files.readString(Path.of("config/jlt_ores.example.yaml")).strip();
        assertEquals(example, generated);
    }
}
