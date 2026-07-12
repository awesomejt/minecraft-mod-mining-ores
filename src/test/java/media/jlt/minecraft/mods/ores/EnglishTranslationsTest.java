package media.jlt.minecraft.mods.ores;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import media.jlt.minecraft.engine.schedule.HarvestFeedback;
import media.jlt.minecraft.engine.util.ToolTier;
import media.jlt.minecraft.mods.ores.logic.OreFamily;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnglishTranslationsTest {
    @Test
    void containsEveryFamilyTierAndSchedulerStatus() throws Exception {
        JsonObject translations;
        try (var stream = getClass().getResourceAsStream("/assets/jlt_ores/lang/en_us.json")) {
            assertNotNull(stream);
            translations = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject();
        }

        for (OreFamily family : OreFamily.values()) {
            assertHasText(translations, "ore_family.jlt_ores." + family.configKey());
        }
        for (ToolTier tier : ToolTier.values()) {
            assertHasText(translations, "tool_tier.jlt_ores." + tier.configName());
        }
        for (HarvestFeedback feedback : HarvestFeedback.values()) {
            assertHasText(translations, OresHarvestAdapter.feedbackTranslationKey(feedback));
        }
        for (String key : new String[] {
            "command.jlt_ores.reload.success",
            "command.jlt_ores.reload.failure",
            "message.jlt_ores.hint.requires_pickaxe",
            "message.jlt_ores.hint.already_active",
            "message.jlt_ores.hint.no_vein",
            "message.jlt_ores.hint.tier_required",
            "message.jlt_ores.hint.durability_floor_blocked",
            "message.jlt_ores.hint.durability_trimmed",
            "message.jlt_ores.hint.substitution_planned",
            "message.jlt_ores.hint.vein_durability"
        }) {
            assertHasText(translations, key);
        }
    }

    private static void assertHasText(JsonObject translations, String key) {
        assertTrue(translations.has(key), () -> "Missing translation: " + key);
        assertTrue(!translations.get(key).getAsString().isBlank(), () -> "Blank translation: " + key);
    }
}
