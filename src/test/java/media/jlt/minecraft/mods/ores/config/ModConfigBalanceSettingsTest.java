package media.jlt.minecraft.mods.ores.config;

import media.jlt.minecraft.engine.balance.BalanceSettings;
import media.jlt.minecraft.engine.balance.DurabilityProtectionMode;
import media.jlt.minecraft.engine.balance.TaxMode;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModConfigBalanceSettingsTest {
    @Test
    void mapsEverySharedDefault() {
        BalanceSettings settings = new ModConfig().toBalanceSettings();

        assertEquals(TaxMode.HUNGER_ONLY, settings.taxMode());
        assertTrue(settings.enableHungerTax());
        assertEquals(1, settings.hungerTaxFloor());
        assertEquals(0, settings.xpTaxFloor());
        assertEquals(400, settings.hungerResumeTimeoutTicks());
        assertEquals(DurabilityProtectionMode.ALL, settings.durabilityProtectionMode());
        assertEquals(1, settings.durabilityProtectionFloor());
        assertEquals(3, settings.durabilityMultiplier());
        assertFalse(settings.enableDurabilityXpSubstitution());
        assertEquals(10, settings.durabilityXpSubstitutionWindow());
        assertEquals(3, settings.xpPerSubstitutedDurabilityPoint());
        assertTrue(settings.enableTimePenalty());
        assertEquals(Map.of(0, 16, 1, 8, 2, 4, 3, 2, 4, 1, 5, 0), settings.delayTicksByEfficiencyLevel());
        assertEquals(8, settings.maxInstantBlocksPerTick());
        assertEquals(64, settings.maxHarvestDistance());
    }

    @Test
    void mapsCustomValuesAndDefensivelyCopiesDelays() {
        ModConfig config = new ModConfig();
        config.xpTaxMode = "xp_after_hunger_depleted";
        config.enableHungerTax = false;
        config.hungerTaxFloor = 4;
        config.xpTaxFloor = 25;
        config.hungerResumeTimeoutTicks = 800;
        config.durabilityProtectionMode = "enchanted_only";
        config.durabilityProtectionFloor = 7;
        config.durabilityMultiplier = 6;
        config.enableDurabilityXpSubstitution = true;
        config.durabilityXpSubstitutionWindow = 30;
        config.xpPerSubstitutedDurabilityPoint = 5;
        config.enableTimePenalty = false;
        config.blockBreakDelayTicksByEfficiencyLevel = customDelays();
        config.maxInstantBlocksPerTick = 12;
        config.maxMineDistance = 80;

        BalanceSettings settings = config.toBalanceSettings();
        config.blockBreakDelayTicksByEfficiencyLevel.put("0", 40);

        assertEquals(TaxMode.XP_AFTER_HUNGER_DEPLETED, settings.taxMode());
        assertFalse(settings.enableHungerTax());
        assertEquals(4, settings.hungerTaxFloor());
        assertEquals(25, settings.xpTaxFloor());
        assertEquals(800, settings.hungerResumeTimeoutTicks());
        assertEquals(DurabilityProtectionMode.ENCHANTED_ONLY, settings.durabilityProtectionMode());
        assertEquals(7, settings.durabilityProtectionFloor());
        assertEquals(6, settings.durabilityMultiplier());
        assertTrue(settings.enableDurabilityXpSubstitution());
        assertEquals(30, settings.durabilityXpSubstitutionWindow());
        assertEquals(5, settings.xpPerSubstitutedDurabilityPoint());
        assertFalse(settings.enableTimePenalty());
        assertEquals(10, settings.delayTicksForEfficiencyLevel(0));
        assertEquals(5, settings.delayTicksForEfficiencyLevel(5));
        assertEquals(12, settings.maxInstantBlocksPerTick());
        assertEquals(80, settings.maxHarvestDistance());
    }

    private static Map<String, Integer> customDelays() {
        Map<String, Integer> delays = new LinkedHashMap<>();
        for (int level = 0; level <= 5; level++) {
            delays.put(Integer.toString(level), 10 - level);
        }
        return delays;
    }
}
