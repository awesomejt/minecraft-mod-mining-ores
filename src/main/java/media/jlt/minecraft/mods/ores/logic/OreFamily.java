package media.jlt.minecraft.mods.ores.logic;

import java.util.Locale;

public enum OreFamily {
    COAL,
    COPPER,
    IRON,
    GOLD,
    REDSTONE,
    LAPIS,
    DIAMOND,
    EMERALD,
    QUARTZ,
    NETHER_GOLD,
    ANCIENT_DEBRIS;

    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
