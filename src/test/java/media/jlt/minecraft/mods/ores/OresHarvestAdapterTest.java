package media.jlt.minecraft.mods.ores;

import media.jlt.minecraft.engine.schedule.HarvestFeedback;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OresHarvestAdapterTest {
    @Test
    void mapsEverySchedulerFeedbackReasonToAnOresTranslation() {
        Map<HarvestFeedback, String> expected = Map.of(
            HarvestFeedback.CANCELLED_DEATH, "message.jlt_ores.status.cancelled_death",
            HarvestFeedback.CANCELLED_DISTANCE, "message.jlt_ores.status.cancelled_distance",
            HarvestFeedback.CANCELLED_TOOL_MISSING, "message.jlt_ores.status.cancelled_tool_missing",
            HarvestFeedback.CANCELLED_HUNGER_TIMEOUT, "message.jlt_ores.status.cancelled_hunger_timeout",
            HarvestFeedback.HUNGER_PAUSED, "message.jlt_ores.status.hunger_paused",
            HarvestFeedback.HUNGER_RESUMED, "message.jlt_ores.status.hunger_resumed",
            HarvestFeedback.NO_SUBSTITUTION_XP, "message.jlt_ores.status.no_substitution_xp",
            HarvestFeedback.NO_XP, "message.jlt_ores.status.no_xp",
            HarvestFeedback.TOOL_BROKE, "message.jlt_ores.status.tool_broke",
            HarvestFeedback.DURABILITY_FLOOR, "message.jlt_ores.status.durability_floor"
        );

        assertEquals(HarvestFeedback.values().length, expected.size());
        expected.forEach((feedback, key) -> assertEquals(key, OresHarvestAdapter.feedbackTranslationKey(feedback)));
    }
}
