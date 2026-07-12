package media.jlt.minecraft.mods.ores.config;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModConfigEfficiencyDelayTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;

    @Test
    void defaultsFollowTheSharedEfficiencySchedule() {
        ModConfig config = new ModConfig();

        assertEquals(16, config.delayTicksForEfficiencyLevel(0));
        assertEquals(8, config.delayTicksForEfficiencyLevel(1));
        assertEquals(4, config.delayTicksForEfficiencyLevel(2));
        assertEquals(2, config.delayTicksForEfficiencyLevel(3));
        assertEquals(1, config.delayTicksForEfficiencyLevel(4));
        assertEquals(0, config.delayTicksForEfficiencyLevel(5));
        assertEquals(0, config.delayTicksForEfficiencyLevel(100));
    }

    @Test
    void sanitationRestoresMissingLevelsClampsValuesAndRemovesUnknownKeys() {
        ModConfig config = new ModConfig();
        config.blockBreakDelayTicksByEfficiencyLevel = new LinkedHashMap<>();
        config.blockBreakDelayTicksByEfficiencyLevel.put("0", -5);
        config.blockBreakDelayTicksByEfficiencyLevel.put("2", 99);
        config.blockBreakDelayTicksByEfficiencyLevel.put("9", 7);

        config.sanitize(LOGGER);

        assertEquals(Map.of("0", 0, "1", 8, "2", 40, "3", 2, "4", 1, "5", 0), config.blockBreakDelayTicksByEfficiencyLevel);
        assertFalse(config.blockBreakDelayTicksByEfficiencyLevel.containsKey("9"));
    }

    @Test
    void nullDelayMapRestoresEveryDefault() {
        ModConfig config = new ModConfig();
        config.blockBreakDelayTicksByEfficiencyLevel = null;

        config.sanitize(LOGGER);

        assertEquals(16, config.delayTicksForEfficiencyLevel(0));
        assertEquals(0, config.delayTicksForEfficiencyLevel(5));
    }
}
