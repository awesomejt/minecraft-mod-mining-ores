package media.jlt.minecraft.mods.ores.logic;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class OreFamilies {
    private static final TagKey<Block> COAL_ORES = vanillaTag("coal_ores");
    private static final TagKey<Block> REDSTONE_ORES = vanillaTag("redstone_ores");
    private static final TagKey<Block> LAPIS_ORES = vanillaTag("lapis_ores");
    private static final TagKey<Block> DIAMOND_ORES = vanillaTag("diamond_ores");
    private static final TagKey<Block> EMERALD_ORES = vanillaTag("emerald_ores");

    private OreFamilies() {
    }

    public static Optional<OreFamily> classify(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.NETHER_GOLD_ORE) {
            return Optional.of(OreFamily.NETHER_GOLD);
        }
        if (block == Blocks.NETHER_QUARTZ_ORE) {
            return Optional.of(OreFamily.QUARTZ);
        }
        if (block == Blocks.ANCIENT_DEBRIS) {
            return Optional.of(OreFamily.ANCIENT_DEBRIS);
        }
        if (state.is(COAL_ORES)) {
            return Optional.of(OreFamily.COAL);
        }
        if (state.is(BlockTags.COPPER_ORES)) {
            return Optional.of(OreFamily.COPPER);
        }
        if (state.is(BlockTags.IRON_ORES)) {
            return Optional.of(OreFamily.IRON);
        }
        if (state.is(BlockTags.GOLD_ORES)) {
            return Optional.of(OreFamily.GOLD);
        }
        if (state.is(REDSTONE_ORES)) {
            return Optional.of(OreFamily.REDSTONE);
        }
        if (state.is(LAPIS_ORES)) {
            return Optional.of(OreFamily.LAPIS);
        }
        if (state.is(DIAMOND_ORES)) {
            return Optional.of(OreFamily.DIAMOND);
        }
        if (state.is(EMERALD_ORES)) {
            return Optional.of(OreFamily.EMERALD);
        }
        return Optional.empty();
    }

    public static boolean matches(
        BlockState candidate,
        OreFamily family,
        Block triggerBlock,
        boolean matchDeepslateVariants
    ) {
        if (!matchDeepslateVariants) {
            return candidate.getBlock() == triggerBlock;
        }
        return classify(candidate).filter(family::equals).isPresent();
    }

    static Optional<TagKey<Block>> tagForFamily(OreFamily family) {
        return switch (family) {
            case COAL -> Optional.of(COAL_ORES);
            case COPPER -> Optional.of(BlockTags.COPPER_ORES);
            case IRON -> Optional.of(BlockTags.IRON_ORES);
            case GOLD -> Optional.of(BlockTags.GOLD_ORES);
            case REDSTONE -> Optional.of(REDSTONE_ORES);
            case LAPIS -> Optional.of(LAPIS_ORES);
            case DIAMOND -> Optional.of(DIAMOND_ORES);
            case EMERALD -> Optional.of(EMERALD_ORES);
            case QUARTZ, NETHER_GOLD, ANCIENT_DEBRIS -> Optional.empty();
        };
    }

    private static TagKey<Block> vanillaTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(path));
    }
}
