package media.jlt.minecraft.mods.ores;

import media.jlt.minecraft.mods.ores.logic.OreFamily;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OreMatchKindTest {
    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void familyEncodingMatchesTheEncodedFamily() {
        int matchKind = OreMatchKind.family(OreFamily.NETHER_GOLD);

        assertTrue(OreMatchKind.matches(Blocks.NETHER_GOLD_ORE.defaultBlockState(), matchKind));
        assertFalse(OreMatchKind.matches(Blocks.NETHER_QUARTZ_ORE.defaultBlockState(), matchKind));
    }

    @Test
    void exactEncodingMatchesOnlyTheTriggerBlock() {
        int matchKind = OreMatchKind.exactBlock(Blocks.IRON_ORE);

        assertTrue(OreMatchKind.matches(Blocks.IRON_ORE.defaultBlockState(), matchKind));
        assertFalse(OreMatchKind.matches(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(), matchKind));
    }

    @Test
    void unknownFamilyEncodingDoesNotMatch() {
        assertFalse(OreMatchKind.matches(Blocks.IRON_ORE.defaultBlockState(), OreFamily.values().length));
    }
}
