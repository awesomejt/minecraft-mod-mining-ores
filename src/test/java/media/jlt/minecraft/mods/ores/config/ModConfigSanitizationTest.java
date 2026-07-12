package media.jlt.minecraft.mods.ores.config;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModConfigSanitizationTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;

    @Test
    void defaultsMatchTheOresBalanceDesign() {
        ModConfig config = new ModConfig();

        assertEquals(3, config.durabilityMultiplier);
        assertEquals(1, config.hungerTaxFloor);
        assertEquals(64, config.maxVeinBlocks);
        assertEquals(8, config.maxHorizontalVeinRadius);
        assertEquals(64, config.maxMineDistance);
    }

    @Test
    void numericValuesClampToLowerBounds() {
        ModConfig config = extremeConfig(Integer.MIN_VALUE, Float.NEGATIVE_INFINITY);

        config.sanitize(LOGGER);

        assertNumericValues(config, 1, 1, 0f, 0, 0, 0, 0, 0, 20, 16, 4, 8);
    }

    @Test
    void numericValuesClampToUpperBounds() {
        ModConfig config = extremeConfig(Integer.MAX_VALUE, Float.POSITIVE_INFINITY);

        config.sanitize(LOGGER);

        assertNumericValues(config, 10, 64, 4f, 100, 1_000, 10_000, 20, 100_000, 7_200, 2_048, 64, 128);
    }

    @Test
    void invalidAndNullModesRestoreDefaults() {
        ModConfig config = new ModConfig();
        config.xpTaxMode = "invalid";
        config.durabilityProtectionMode = null;

        config.sanitize(LOGGER);

        assertEquals("hunger_only", config.xpTaxMode);
        assertEquals("all", config.durabilityProtectionMode);
    }

    @Test
    void modeNormalizationIsLocaleIndependent() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            ModConfig config = new ModConfig();
            config.xpTaxMode = " XP_ONLY ";
            config.durabilityProtectionMode = " ENCHANTED_ONLY ";

            config.sanitize(LOGGER);

            assertEquals("xp_only", config.xpTaxMode);
            assertEquals("enchanted_only", config.durabilityProtectionMode);
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    private static ModConfig extremeConfig(int integerValue, float floatValue) {
        ModConfig config = new ModConfig();
        config.durabilityMultiplier = integerValue;
        config.maxInstantBlocksPerTick = integerValue;
        config.exhaustionPerBlock = floatValue;
        config.xpCostPerBlock = integerValue;
        config.durabilityXpSubstitutionWindow = integerValue;
        config.xpPerSubstitutedDurabilityPoint = integerValue;
        config.durabilityProtectionFloor = integerValue;
        config.hungerTaxFloor = integerValue;
        config.xpTaxFloor = integerValue;
        config.hungerResumeTimeoutTicks = integerValue;
        config.maxVeinBlocks = integerValue;
        config.maxHorizontalVeinRadius = integerValue;
        config.maxMineDistance = integerValue;
        return config;
    }

    private static void assertNumericValues(
        ModConfig config,
        int durabilityMultiplier,
        int maxInstantBlocksPerTick,
        float exhaustionPerBlock,
        int xpCostPerBlock,
        int substitutionWindow,
        int durabilityFloor,
        int hungerFloor,
        int xpFloor,
        int hungerTimeout,
        int maxVeinBlocks,
        int horizontalRadius,
        int mineDistance
    ) {
        assertEquals(durabilityMultiplier, config.durabilityMultiplier);
        assertEquals(maxInstantBlocksPerTick, config.maxInstantBlocksPerTick);
        assertEquals(exhaustionPerBlock, config.exhaustionPerBlock);
        assertEquals(xpCostPerBlock, config.xpCostPerBlock);
        assertEquals(substitutionWindow, config.durabilityXpSubstitutionWindow);
        assertEquals(xpCostPerBlock, config.xpPerSubstitutedDurabilityPoint);
        assertEquals(durabilityFloor, config.durabilityProtectionFloor);
        assertEquals(hungerFloor, config.hungerTaxFloor);
        assertEquals(xpFloor, config.xpTaxFloor);
        assertEquals(hungerTimeout, config.hungerResumeTimeoutTicks);
        assertEquals(maxVeinBlocks, config.maxVeinBlocks);
        assertEquals(horizontalRadius, config.maxHorizontalVeinRadius);
        assertEquals(mineDistance, config.maxMineDistance);
    }
}
