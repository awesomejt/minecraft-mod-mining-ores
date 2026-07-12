package media.jlt.minecraft.mods.ores;

import media.jlt.minecraft.mods.ores.logic.OreFamilies;
import media.jlt.minecraft.mods.ores.logic.OreFamily;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class OreMatchKind {
    private OreMatchKind() {
    }

    static int family(OreFamily family) {
        return family.ordinal();
    }

    static int exactBlock(Block block) {
        return ~BuiltInRegistries.BLOCK.getIdOrThrow(block);
    }

    static boolean matches(BlockState state, int matchKind) {
        if (matchKind < 0) {
            return state.getBlock() == BuiltInRegistries.BLOCK.byIdOrThrow(~matchKind);
        }
        OreFamily[] families = OreFamily.values();
        return matchKind < families.length
            && OreFamilies.classify(state).filter(families[matchKind]::equals).isPresent();
    }
}
