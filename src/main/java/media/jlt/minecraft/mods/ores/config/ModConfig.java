package media.jlt.minecraft.mods.ores.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import media.jlt.minecraft.engine.balance.BalanceSettings;
import media.jlt.minecraft.engine.balance.DurabilityProtectionMode;
import media.jlt.minecraft.engine.balance.TaxMode;
import media.jlt.minecraft.engine.config.JsonConfigStore;
import media.jlt.minecraft.engine.util.ToolTier;
import media.jlt.minecraft.mods.ores.logic.OreFamily;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public boolean requirePickaxe = true;
    public boolean requireSneakForAutoMine = true;
    public boolean showGateFeedback = true;
    public boolean enableTimePenalty = true;
    public boolean enableHungerTax = true;
    public boolean matchDeepslateVariants = true;

    public String xpTaxMode = "hunger_only";
    public String durabilityProtectionMode = "all";
    public int durabilityMultiplier = 3;
    public Map<String, Integer> blockBreakDelayTicksByEfficiencyLevel = defaultEfficiencyDelays();
    public int maxInstantBlocksPerTick = 8;
    public float exhaustionPerBlock = 0.4f;
    public int xpCostPerBlock = 1;
    public boolean enableDurabilityXpSubstitution = false;
    public int durabilityXpSubstitutionWindow = 10;
    public int xpPerSubstitutedDurabilityPoint = 3;
    public int durabilityProtectionFloor = 1;
    public int hungerTaxFloor = 1;
    public int xpTaxFloor = 0;
    public int hungerResumeTimeoutTicks = 400;

    public int maxVeinBlocks = 64;
    public int maxHorizontalVeinRadius = 8;
    public int maxMineDistance = 64;
    public Map<String, String> minimumTierByOreFamily = defaultTiers();
    public Map<String, String> _docs = defaultDocs();

    public static ModConfig load(String modId, Logger logger) {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(modId + ".json");
        ReloadResult result = fromStoreResult(configStore(configFile, logger).load());
        if (result.success()) {
            return result.config();
        }
        logger.error("Failed to load config at {}: {}. Using defaults.", configFile, result.errorMessage());
        return new ModConfig().sanitize(logger);
    }

    public static ReloadResult reload(String modId, Logger logger) {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(modId + ".json");
        return reload(configFile, logger);
    }

    static ReloadResult reload(Path configFile, Logger logger) {
        return fromStoreResult(configStore(configFile, logger).reload());
    }

    public ToolTier requiredTier(OreFamily family) {
        String configured = minimumTierByOreFamily == null
            ? null
            : minimumTierByOreFamily.get(family.configKey());
        return ToolTier.fromConfigName(configured).orElse(defaultTierFor(family));
    }

    public int delayTicksForEfficiencyLevel(int efficiencyLevel) {
        int normalizedLevel = clamp(efficiencyLevel, 0, 5);
        Integer configured = blockBreakDelayTicksByEfficiencyLevel.get(Integer.toString(normalizedLevel));
        return configured == null
            ? defaultEfficiencyDelays().get(Integer.toString(normalizedLevel))
            : configured;
    }

    public BalanceSettings toBalanceSettings() {
        Map<Integer, Integer> efficiencyDelays = new LinkedHashMap<>();
        for (int efficiencyLevel = 0; efficiencyLevel <= 5; efficiencyLevel++) {
            efficiencyDelays.put(efficiencyLevel, delayTicksForEfficiencyLevel(efficiencyLevel));
        }
        return new BalanceSettings(
            TaxMode.fromConfigName(xpTaxMode).orElse(TaxMode.HUNGER_ONLY),
            enableHungerTax,
            hungerTaxFloor,
            xpTaxFloor,
            hungerResumeTimeoutTicks,
            DurabilityProtectionMode.fromConfigName(durabilityProtectionMode)
                .orElse(DurabilityProtectionMode.ALL),
            durabilityProtectionFloor,
            durabilityMultiplier,
            enableDurabilityXpSubstitution,
            durabilityXpSubstitutionWindow,
            xpPerSubstitutedDurabilityPoint,
            enableTimePenalty,
            efficiencyDelays,
            maxInstantBlocksPerTick,
            maxMineDistance
        );
    }

    public String summary() {
        return "enabled=" + enabled
            + ", sneak=" + requireSneakForAutoMine
            + ", durabilityMultiplier=" + durabilityMultiplier
            + ", blockBreakDelayTicksByEfficiencyLevel=" + blockBreakDelayTicksByEfficiencyLevel
            + ", maxInstantBlocksPerTick=" + maxInstantBlocksPerTick
            + ", xpTaxMode=" + xpTaxMode
            + ", durabilityProtectionMode=" + durabilityProtectionMode
            + ", durabilityProtectionFloor=" + durabilityProtectionFloor
            + ", hungerTaxFloor=" + hungerTaxFloor
            + ", xpTaxFloor=" + xpTaxFloor
            + ", enableHungerTax=" + enableHungerTax
            + ", matchDeepslateVariants=" + matchDeepslateVariants
            + ", maxVeinBlocks=" + maxVeinBlocks
            + ", maxMineDistance=" + maxMineDistance;
    }

    ModConfig sanitize(Logger logger) {
        durabilityMultiplier = clamp(durabilityMultiplier, 1, 10);
        Map<String, Integer> configuredEfficiencyDelays = blockBreakDelayTicksByEfficiencyLevel;
        blockBreakDelayTicksByEfficiencyLevel = new LinkedHashMap<>();
        Map<String, Integer> defaultEfficiencyDelays = defaultEfficiencyDelays();
        for (int efficiencyLevel = 0; efficiencyLevel <= 5; efficiencyLevel++) {
            String key = Integer.toString(efficiencyLevel);
            Integer configured = configuredEfficiencyDelays == null ? null : configuredEfficiencyDelays.get(key);
            int fallback = defaultEfficiencyDelays.get(key);
            blockBreakDelayTicksByEfficiencyLevel.put(
                key,
                clamp(configured == null ? fallback : configured, 0, 40)
            );
        }
        maxInstantBlocksPerTick = clamp(maxInstantBlocksPerTick, 1, 64);
        exhaustionPerBlock = clampFloat(exhaustionPerBlock, 0f, 4f);
        xpCostPerBlock = clamp(xpCostPerBlock, 0, 100);
        durabilityXpSubstitutionWindow = clamp(durabilityXpSubstitutionWindow, 0, 1000);
        xpPerSubstitutedDurabilityPoint = clamp(xpPerSubstitutedDurabilityPoint, 0, 100);
        durabilityProtectionFloor = clamp(durabilityProtectionFloor, 0, 10000);
        hungerTaxFloor = clamp(hungerTaxFloor, 0, 20);
        xpTaxFloor = clamp(xpTaxFloor, 0, 100000);
        hungerResumeTimeoutTicks = clamp(hungerResumeTimeoutTicks, 20, 7200);
        maxVeinBlocks = clamp(maxVeinBlocks, 16, 2048);
        maxHorizontalVeinRadius = clamp(maxHorizontalVeinRadius, 4, 64);
        maxMineDistance = clamp(maxMineDistance, 8, 128);
        minimumTierByOreFamily = minimumTierByOreFamily == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(minimumTierByOreFamily);
        for (OreFamily family : OreFamily.values()) {
            String key = family.configKey();
            String configured = minimumTierByOreFamily.get(key);
            if (configured == null) {
                minimumTierByOreFamily.put(key, defaultTierFor(family).configName());
                continue;
            }
            ToolTier validated = ToolTier.fromConfigName(configured).orElse(null);
            if (validated == null) {
                ToolTier fallback = defaultTierFor(family);
                logger.warn(
                    "Invalid minimum tier '{}' for ore family '{}'; using default '{}'.",
                    configured,
                    key,
                    fallback.configName()
                );
                minimumTierByOreFamily.put(key, fallback.configName());
            } else {
                minimumTierByOreFamily.put(key, validated.configName());
            }
        }
        if (xpTaxMode == null) {
            xpTaxMode = "hunger_only";
        }
        xpTaxMode = xpTaxMode.trim().toLowerCase(Locale.ROOT);
        if (!xpTaxMode.equals("hunger_only")
            && !xpTaxMode.equals("xp_only")
            && !xpTaxMode.equals("xp_after_hunger_depleted")) {
            xpTaxMode = "hunger_only";
        }
        if (durabilityProtectionMode == null) {
            durabilityProtectionMode = "all";
        }
        durabilityProtectionMode = durabilityProtectionMode.trim().toLowerCase(Locale.ROOT);
        if (!durabilityProtectionMode.equals("off")
            && !durabilityProtectionMode.equals("all")
            && !durabilityProtectionMode.equals("enchanted_only")) {
            durabilityProtectionMode = "all";
        }
        _docs = _docs == null ? new LinkedHashMap<>() : new LinkedHashMap<>(_docs);
        for (Map.Entry<String, String> entry : defaultDocs().entrySet()) {
            _docs.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return this;
    }

    String toJson() {
        return GSON.toJson(this);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static JsonConfigStore<ModConfig> configStore(Path configFile, Logger logger) {
        Path configDirectory = configFile.getParent();
        if (configDirectory == null) {
            configDirectory = Path.of(".");
        }
        return new JsonConfigStore<>(
            configDirectory,
            configFile.getFileName().toString(),
            ModConfig.class,
            ModConfig::new,
            config -> config.sanitize(logger)
        );
    }

    private static ReloadResult fromStoreResult(JsonConfigStore.ReloadResult<ModConfig> result) {
        return result.success()
            ? ReloadResult.success(result.config())
            : ReloadResult.failure(result.errorMessage());
    }

    public record ReloadResult(ModConfig config, String errorMessage) {
        public static ReloadResult success(ModConfig config) {
            return new ReloadResult(config, null);
        }

        public static ReloadResult failure(String errorMessage) {
            return new ReloadResult(
                null,
                errorMessage == null || errorMessage.isBlank()
                    ? "Unknown configuration error"
                    : errorMessage
            );
        }

        public boolean success() {
            return config != null;
        }
    }

    private static Map<String, Integer> defaultEfficiencyDelays() {
        Map<String, Integer> delays = new LinkedHashMap<>();
        delays.put("0", 16);
        delays.put("1", 8);
        delays.put("2", 4);
        delays.put("3", 2);
        delays.put("4", 1);
        delays.put("5", 0);
        return delays;
    }

    private static Map<String, String> defaultTiers() {
        Map<String, String> tiers = new LinkedHashMap<>();
        for (OreFamily family : OreFamily.values()) {
            tiers.put(family.configKey(), defaultTierFor(family).configName());
        }
        return tiers;
    }

    private static ToolTier defaultTierFor(OreFamily family) {
        return switch (family) {
            case COAL, QUARTZ, NETHER_GOLD -> ToolTier.STONE;
            case COPPER, IRON, LAPIS -> ToolTier.IRON;
            case GOLD, REDSTONE, DIAMOND, EMERALD -> ToolTier.DIAMOND;
            case ANCIENT_DEBRIS -> ToolTier.NETHERITE;
        };
    }

    private static Map<String, String> defaultDocs() {
        Map<String, String> docs = new LinkedHashMap<>();
        docs.put("enabled", "Default: true. Values: true|false. Master switch for all auto-mine behavior.");
        docs.put("requirePickaxe", "Default: true. Values: true|false. If true, only pickaxes can trigger auto-mine.");
        docs.put("requireSneakForAutoMine", "Default: true. Values: true|false. If true, the player must hold Shift while mining.");
        docs.put("showGateFeedback", "Default: true. Values: true|false. Shows rate-limited action-bar gate hints and visible auto-mine status messages.");
        docs.put("enableTimePenalty", "Default: true. Values: true|false. If true, auto-mine applies per-block delay; false uses zero-delay batches capped by maxInstantBlocksPerTick.");
        docs.put("enableHungerTax", "Default: true. Values: true|false. If true, automated mining consumes exhaustion per block.");
        docs.put("matchDeepslateVariants", "Default: true. Values: true|false. If true, stone and deepslate variants of one ore family connect; false matches only the trigger block type.");
        docs.put("xpTaxMode", "Default: hunger_only. Values: hunger_only|xp_only|xp_after_hunger_depleted.");
        docs.put("durabilityProtectionMode", "Default: all. Values: off|all|enchanted_only. Controls when the durability floor is enforced.");
        docs.put("durabilityMultiplier", "Default: 3. Range: 1..10. Pickaxe durability cost multiplier for automated ore blocks.");
        docs.put("blockBreakDelayTicksByEfficiencyLevel", "Map keys: 0..5 for unenchanted through Efficiency V. Default ticks: 0=16, 1=8, 2=4, 3=2, 4=1, 5=0. Values are clamped to 0..40; missing levels restore defaults and unknown keys are removed.");
        docs.put("maxInstantBlocksPerTick", "Default: 8. Range: 1..64. Per-player block cap for delay-zero auto-mine.");
        docs.put("exhaustionPerBlock", "Default: 0.4. Range: 0.0..4.0. Exhaustion applied per automated ore block.");
        docs.put("xpCostPerBlock", "Default: 1. Range: 0..100. XP points charged per automated ore block when XP tax applies.");
        docs.put("enableDurabilityXpSubstitution", "Default: false. Values: true|false. Substitutes extra automation durability per successful ore break; vanilla base durability remains.");
        docs.put("durabilityXpSubstitutionWindow", "Default: 10. Range: 0..1000. Only durability within this many points above the floor may be substituted.");
        docs.put("xpPerSubstitutedDurabilityPoint", "Default: 3. Range: 0..100. XP charged after a successful break per substituted extra durability point.");
        docs.put("durabilityProtectionFloor", "Default: 1. Range: 0..10000. Auto-mine will not intentionally consume durability below this remaining amount.");
        docs.put("hungerTaxFloor", "Default: 1. Range: 0..20. Hunger tax preserves this food-level floor; 0 disables the floor.");
        docs.put("xpTaxFloor", "Default: 0. Range: 0..100000. XP tax will not consume below this total-XP floor.");
        docs.put("hungerResumeTimeoutTicks", "Default: 400. Range: 20..7200. Paused work resumes if hunger returns before this timeout.");
        docs.put("maxVeinBlocks", "Default: 64. Range: 16..2048. Hard cap for connected ore-vein scan size.");
        docs.put("maxHorizontalVeinRadius", "Default: 8. Range: 4..64. Maximum horizontal radius from the trigger block.");
        docs.put("maxMineDistance", "Default: 64. Range: 8..128. Cancels queued auto-mine when a target is farther than this distance.");
        docs.put("minimumTierByOreFamily", "Map keys: all documented ore-family names. Values: wood|stone|copper|iron|diamond|netherite. Invalid values reset to each family's default.");
        return docs;
    }
}
