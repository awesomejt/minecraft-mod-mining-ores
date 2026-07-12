package media.jlt.minecraft.mods.ores.logic;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OreFamiliesTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void classifiesSpecificNetherBlocksBeforeAnyTagLookup() {
        assertEquals(
            OreFamily.NETHER_GOLD,
            OreFamilies.classify(Blocks.NETHER_GOLD_ORE.defaultBlockState()).orElseThrow()
        );
        assertEquals(
            OreFamily.QUARTZ,
            OreFamilies.classify(Blocks.NETHER_QUARTZ_ORE.defaultBlockState()).orElseThrow()
        );
        assertEquals(
            OreFamily.ANCIENT_DEBRIS,
            OreFamilies.classify(Blocks.ANCIENT_DEBRIS.defaultBlockState()).orElseThrow()
        );
    }

    @Test
    void ordinaryBlockHasNoOreFamily() {
        assertTrue(OreFamilies.classify(Blocks.STONE.defaultBlockState()).isEmpty());
    }

    @Test
    void exactBlockModeDoesNotJoinStoneAndDeepslateVariants() {
        assertTrue(OreFamilies.matches(
            Blocks.IRON_ORE.defaultBlockState(),
            OreFamily.IRON,
            Blocks.IRON_ORE,
            false
        ));
        assertFalse(OreFamilies.matches(
            Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(),
            OreFamily.IRON,
            Blocks.IRON_ORE,
            false
        ));
    }

    @Test
    void familyModeMatchesSpecificFamilyAndRejectsAnother() {
        assertTrue(OreFamilies.matches(
            Blocks.NETHER_GOLD_ORE.defaultBlockState(),
            OreFamily.NETHER_GOLD,
            Blocks.NETHER_GOLD_ORE,
            true
        ));
        assertFalse(OreFamilies.matches(
            Blocks.NETHER_QUARTZ_ORE.defaultBlockState(),
            OreFamily.NETHER_GOLD,
            Blocks.NETHER_GOLD_ORE,
            true
        ));
    }

    @Test
    void tagBackedFamiliesUseTheirVanillaTagIdentifiers() {
        List<OreFamily> tagBacked = List.of(
            OreFamily.COAL,
            OreFamily.COPPER,
            OreFamily.IRON,
            OreFamily.GOLD,
            OreFamily.REDSTONE,
            OreFamily.LAPIS,
            OreFamily.DIAMOND,
            OreFamily.EMERALD
        );

        for (OreFamily family : tagBacked) {
            assertEquals(
                "minecraft:" + family.configKey() + "_ores",
                OreFamilies.tagForFamily(family).orElseThrow().location().toString()
            );
        }
        assertTrue(OreFamilies.tagForFamily(OreFamily.QUARTZ).isEmpty());
        assertTrue(OreFamilies.tagForFamily(OreFamily.NETHER_GOLD).isEmpty());
        assertTrue(OreFamilies.tagForFamily(OreFamily.ANCIENT_DEBRIS).isEmpty());
    }
}
