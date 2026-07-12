package media.jlt.minecraft.mods.ores.logic;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OreFamilyTest {
    @Test
    void exposesAllFamiliesInStableConfigOrder() {
        assertEquals(
            List.of(
                "coal",
                "copper",
                "iron",
                "gold",
                "redstone",
                "lapis",
                "diamond",
                "emerald",
                "quartz",
                "nether_gold",
                "ancient_debris"
            ),
            List.of(OreFamily.values()).stream().map(OreFamily::configKey).toList()
        );
    }

    @Test
    void configKeysAreStableUnderTurkishDefaultLocale() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("iron", OreFamily.IRON.configKey());
            assertEquals("ancient_debris", OreFamily.ANCIENT_DEBRIS.configKey());
        } finally {
            Locale.setDefault(originalLocale);
        }
    }
}
