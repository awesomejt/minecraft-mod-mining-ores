package media.jlt.minecraft.mods.ores;

import media.jlt.minecraft.engine.balance.BalanceSettings;
import media.jlt.minecraft.engine.balance.HarvestPlanner;
import media.jlt.minecraft.engine.scan.ConnectedBlockScanner;
import media.jlt.minecraft.engine.scan.Pos3i;
import media.jlt.minecraft.engine.scan.ScanLimits;
import media.jlt.minecraft.engine.schedule.HarvestScheduler;
import media.jlt.minecraft.engine.util.MessageRateLimiter;
import media.jlt.minecraft.engine.util.ToolTier;
import media.jlt.minecraft.mods.ores.config.ModConfig;
import media.jlt.minecraft.mods.ores.logic.OreFamilies;
import media.jlt.minecraft.mods.ores.logic.OreFamily;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

public final class OresMod implements ModInitializer {
    public static final String MOD_ID = "jlt_ores";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Set<UUID> ACTIVE_PLAYERS = new HashSet<>();
    private static final MessageRateLimiter HINT_LIMITER = new MessageRateLimiter(100);
    private static ModConfig config;
    private static final HarvestScheduler<ServerLevel, ItemStack> HARVEST_SCHEDULER =
        new HarvestScheduler<>(new OresHarvestAdapter(), () -> config.toBalanceSettings());
    private static long serverTick;

    @Override
    public void onInitialize() {
        config = ModConfig.load(MOD_ID, LOGGER);
        PlayerBlockBreakEvents.AFTER.register(OresMod::handleBlockBroken);
        ServerTickEvents.END_SERVER_TICK.register(OresMod::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(OresMod::onServerStopped);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            Commands.literal(MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("reload").executes(context -> reloadConfig(context.getSource())))
        ));
        LOGGER.info("{} initialized with config {}", MOD_ID, config.summary());
    }

    private static int reloadConfig(CommandSourceStack source) {
        ModConfig.ReloadResult result = ModConfig.reload(MOD_ID, LOGGER);
        if (!result.success()) {
            LOGGER.error("Config reload failed: {}", result.errorMessage());
            source.sendFailure(Component.translatable("command.jlt_ores.reload.failure", result.errorMessage()));
            return 0;
        }
        config = result.config();
        LOGGER.info("Config reloaded: {}", config.summary());
        source.sendSuccess(() -> Component.translatable("command.jlt_ores.reload.success"), true);
        return 1;
    }

    private static void onServerStopped(MinecraftServer server) {
        ACTIVE_PLAYERS.clear();
        HARVEST_SCHEDULER.clearAll();
        HINT_LIMITER.clearAll();
        serverTick = 0;
    }

    private static void onServerTick(MinecraftServer server) {
        serverTick++;
        HARVEST_SCHEDULER.tick(serverTick);
    }

    private static void handleBlockBroken(
        Level level,
        Player player,
        BlockPos pos,
        BlockState state,
        net.minecraft.world.level.block.entity.BlockEntity blockEntity
    ) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Optional<OreFamily> classified = OreFamilies.classify(state);
        if (classified.isEmpty() || !config.enabled) {
            return;
        }
        if (config.requireSneakForAutoMine && !serverPlayer.isShiftKeyDown()) {
            return;
        }
        ItemStack tool = serverPlayer.getMainHandItem();
        if (config.requirePickaxe && !tool.is(ItemTags.PICKAXES)) {
            hint(serverPlayer, "message.jlt_ores.hint.requires_pickaxe");
            return;
        }
        if (HARVEST_SCHEDULER.hasScheduledWork(serverPlayer.getUUID())) {
            hint(serverPlayer, "message.jlt_ores.hint.already_active");
            return;
        }
        if (!ACTIVE_PLAYERS.add(serverPlayer.getUUID())) {
            return;
        }
        try {
            runAutoMine(serverLevel, serverPlayer, pos, state.getBlock(), classified.get(), tool);
        } finally {
            ACTIVE_PLAYERS.remove(serverPlayer.getUUID());
        }
    }

    private static void runAutoMine(
        ServerLevel level,
        ServerPlayer player,
        BlockPos triggerPos,
        Block triggerBlock,
        OreFamily family,
        ItemStack tool
    ) {
        HARVEST_SCHEDULER.clearHungerPause(player.getUUID());
        Pos3i origin = toPos3i(triggerPos);
        List<Pos3i> vein = ConnectedBlockScanner.scan(
            origin,
            (x, y, z) -> (x != origin.x() || y != origin.y() || z != origin.z())
                && OreFamilies.matches(
                    level.getBlockState(new BlockPos(x, y, z)),
                    family,
                    triggerBlock,
                    config.matchDeepslateVariants
                ),
            new ScanLimits(config.maxVeinBlocks, config.maxHorizontalVeinRadius, OptionalInt.empty())
        );
        if (vein.isEmpty()) {
            hint(player, "message.jlt_ores.hint.no_vein");
            return;
        }

        ToolTier requiredTier = config.requiredTier(family);
        ToolTier toolTier = ToolTier.fromMaxDamage(tool.getMaxDamage());
        if (toolTier.isLowerThan(requiredTier)) {
            hint(
                player,
                "message.jlt_ores.hint.tier_required",
                Component.translatable("ore_family.jlt_ores." + family.configKey()),
                Component.translatable("tool_tier.jlt_ores." + requiredTier.configName())
            );
            return;
        }

        int costPerBlock = Math.max(1, config.durabilityMultiplier);
        BalanceSettings settings = config.toBalanceSettings();
        int remainingDurability = tool.getMaxDamage() - tool.getDamageValue();
        HarvestPlanner.PlanResult plan = HarvestPlanner.plan(
            vein.size(),
            player.isCreative(),
            tool.isDamageableItem(),
            remainingDurability,
            settings.isProtectionActive(tool.isDamageableItem(), tool.isEnchanted()),
            costPerBlock,
            settings,
            availableExperienceAboveFloor(player)
        );
        if (plan.outcome() == HarvestPlanner.Outcome.BLOCKED_DURABILITY_FLOOR) {
            hint(player, "message.jlt_ores.hint.durability_floor_blocked", config.durabilityProtectionFloor);
            return;
        }
        if (plan.outcome() == HarvestPlanner.Outcome.BLOCKED_INSUFFICIENT_DURABILITY) {
            hint(player, "message.jlt_ores.hint.vein_durability", plan.requiredDurability(), remainingDurability);
            return;
        }
        if (plan.trimmed()) {
            vein = new ArrayList<>(vein.subList(0, plan.blocksSupported()));
            hint(player, "message.jlt_ores.hint.durability_trimmed", vein.size());
        }
        if (plan.substitutedBlocks() > 0) {
            hint(player, "message.jlt_ores.hint.substitution_planned", plan.substitutedBlocks());
        }

        int durabilityChargedBlocks = Math.max(0, vein.size() - plan.substitutedBlocks());
        int matchKind = config.matchDeepslateVariants
            ? OreMatchKind.family(family)
            : OreMatchKind.exactBlock(triggerBlock);
        HARVEST_SCHEDULER.schedule(
            player.getUUID(),
            level,
            vein,
            Math.max(0, config.durabilityMultiplier - 1),
            computeDelayPerBlockTicks(player, tool),
            matchKind,
            config.exhaustionPerBlock,
            config.xpCostPerBlock,
            durabilityChargedBlocks,
            plan.xpCostPerSubstitutedBlock(),
            true,
            requiredTier,
            tool
        );
    }

    private static Pos3i toPos3i(BlockPos pos) {
        return new Pos3i(pos.getX(), pos.getY(), pos.getZ());
    }

    private static int availableExperienceAboveFloor(ServerPlayer player) {
        if (player.isCreative()) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, OresHarvestAdapter.estimateTotalExperience(player) - config.xpTaxFloor);
    }

    private static int computeDelayPerBlockTicks(ServerPlayer player, ItemStack tool) {
        if (!config.enableTimePenalty) {
            return 0;
        }
        return config.delayTicksForEfficiencyLevel(OresHarvestAdapter.efficiencyLevel(player, tool));
    }

    private static void hint(ServerPlayer player, String key, Object... args) {
        if (config.showGateFeedback && HINT_LIMITER.shouldShow(player.getUUID(), key, serverTick, args)) {
            player.sendOverlayMessage(Component.translatable(key, args));
        }
    }

    static boolean isPickaxeRequired() {
        return config.requirePickaxe;
    }

    static boolean addActivePlayer(UUID playerId) {
        return ACTIVE_PLAYERS.add(playerId);
    }

    static void removeActivePlayer(UUID playerId) {
        ACTIVE_PLAYERS.remove(playerId);
    }

    static void status(ServerPlayer player, String key, Object... args) {
        if (config.showGateFeedback) {
            player.sendSystemMessage(Component.translatable(key, args));
        }
    }
}
