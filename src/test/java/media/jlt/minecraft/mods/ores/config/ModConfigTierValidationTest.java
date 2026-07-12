package media.jlt.minecraft.mods.ores.config;

import media.jlt.minecraft.engine.util.ToolTier;
import media.jlt.minecraft.mods.ores.logic.OreFamily;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModConfigTierValidationTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;

    @Test
    void defaultsMatchEveryDocumentedFamilyGate() {
        ModConfig config = new ModConfig();

        assertEquals(ToolTier.STONE, config.requiredTier(OreFamily.COAL));
        assertEquals(ToolTier.STONE, config.requiredTier(OreFamily.QUARTZ));
        assertEquals(ToolTier.STONE, config.requiredTier(OreFamily.NETHER_GOLD));
        assertEquals(ToolTier.IRON, config.requiredTier(OreFamily.COPPER));
        assertEquals(ToolTier.IRON, config.requiredTier(OreFamily.IRON));
        assertEquals(ToolTier.IRON, config.requiredTier(OreFamily.LAPIS));
        assertEquals(ToolTier.DIAMOND, config.requiredTier(OreFamily.GOLD));
        assertEquals(ToolTier.DIAMOND, config.requiredTier(OreFamily.REDSTONE));
        assertEquals(ToolTier.DIAMOND, config.requiredTier(OreFamily.DIAMOND));
        assertEquals(ToolTier.DIAMOND, config.requiredTier(OreFamily.EMERALD));
        assertEquals(ToolTier.NETHERITE, config.requiredTier(OreFamily.ANCIENT_DEBRIS));
    }

    @Test
    void invalidTierRestoresThatFamilyDefault() {
        ModConfig config = new ModConfig();
        config.minimumTierByOreFamily.put("ancient_debris", "netherit");

        config.sanitize(LOGGER);

        assertEquals("netherite", config.minimumTierByOreFamily.get("ancient_debris"));
        assertEquals(ToolTier.NETHERITE, config.requiredTier(OreFamily.ANCIENT_DEBRIS));
    }

    @Test
    void validTierIsNormalizedAndPreserved() {
        ModConfig config = new ModConfig();
        config.minimumTierByOreFamily.put("coal", " WOOD ");

        config.sanitize(LOGGER);

        assertEquals("wood", config.minimumTierByOreFamily.get("coal"));
        assertEquals(ToolTier.WOOD, config.requiredTier(OreFamily.COAL));
    }

    @Test
    void nullMapRestoresAllFamiliesInEnumOrder() {
        ModConfig config = new ModConfig();
        config.minimumTierByOreFamily = null;

        config.sanitize(LOGGER);

        assertEquals(
            List.of(OreFamily.values()).stream().map(OreFamily::configKey).toList(),
            List.copyOf(config.minimumTierByOreFamily.keySet())
        );
    }

    @Test
    void sanitationPreservesCustomOrderAndAppendsMissingFamilies() {
        ModConfig config = new ModConfig();
        config.minimumTierByOreFamily = new LinkedHashMap<>();
        config.minimumTierByOreFamily.put("diamond", "diamond");
        config.minimumTierByOreFamily.put("custom_ore", "netherite");

        config.sanitize(LOGGER);

        assertEquals("diamond", config.minimumTierByOreFamily.keySet().iterator().next());
        assertEquals("custom_ore", List.copyOf(config.minimumTierByOreFamily.keySet()).get(1));
        assertEquals("coal", List.copyOf(config.minimumTierByOreFamily.keySet()).get(2));
    }
}
