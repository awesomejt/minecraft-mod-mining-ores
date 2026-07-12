package media.jlt.minecraft.mods.ores;

import media.jlt.minecraft.engine.balance.ExperienceMath;
import media.jlt.minecraft.engine.port.ActivePlayer;
import media.jlt.minecraft.engine.port.HarvestAdapter;
import media.jlt.minecraft.engine.port.ToolSnapshot;
import media.jlt.minecraft.engine.scan.Pos3i;
import media.jlt.minecraft.engine.schedule.HarvestFeedback;
import media.jlt.minecraft.engine.util.InventoryIdentity;
import media.jlt.minecraft.engine.util.ToolTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.UUID;

final class OresHarvestAdapter implements HarvestAdapter<ServerLevel, ItemStack> {
    @Override
    public ActivePlayer<ItemStack> resolve(UUID playerId, ServerLevel level) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        return player == null || player.level() != level ? null : new OreActivePlayer(player);
    }

    @Override
    public boolean blockMatches(ServerLevel level, Pos3i pos, int matchKind) {
        return OreMatchKind.matches(level.getBlockState(toBlockPos(pos)), matchKind);
    }

    @Override
    public boolean breakBlock(
        ActivePlayer<ItemStack> activePlayer,
        int toolSlot,
        ItemStack boundTool,
        Pos3i pos,
        boolean applyBaseDurability,
        int extraDurability
    ) {
        if (!(activePlayer instanceof OreActivePlayer orePlayer)) {
            return false;
        }
        return breakAndDropWithBoundTool(
            orePlayer.player,
            toolSlot,
            boundTool,
            toBlockPos(pos),
            applyBaseDurability,
            extraDurability
        );
    }

    @Override
    public void feedback(ActivePlayer<ItemStack> activePlayer, HarvestFeedback reason, Object... args) {
        if (activePlayer instanceof OreActivePlayer orePlayer) {
            OresMod.status(orePlayer.player, feedbackTranslationKey(reason), args);
        }
    }

    static String feedbackTranslationKey(HarvestFeedback feedback) {
        return switch (feedback) {
            case CANCELLED_DEATH -> "message.jlt_ores.status.cancelled_death";
            case CANCELLED_DISTANCE -> "message.jlt_ores.status.cancelled_distance";
            case CANCELLED_TOOL_MISSING -> "message.jlt_ores.status.cancelled_tool_missing";
            case CANCELLED_HUNGER_TIMEOUT -> "message.jlt_ores.status.cancelled_hunger_timeout";
            case HUNGER_PAUSED -> "message.jlt_ores.status.hunger_paused";
            case HUNGER_RESUMED -> "message.jlt_ores.status.hunger_resumed";
            case NO_SUBSTITUTION_XP -> "message.jlt_ores.status.no_substitution_xp";
            case NO_XP -> "message.jlt_ores.status.no_xp";
            case TOOL_BROKE -> "message.jlt_ores.status.tool_broke";
            case DURABILITY_FLOOR -> "message.jlt_ores.status.durability_floor";
        };
    }

    static int estimateTotalExperience(ServerPlayer player) {
        int pointsIntoLevel = (int) Math.floor(player.experienceProgress * player.getXpNeededForNextLevel());
        return ExperienceMath.totalForLevel(player.experienceLevel) + pointsIntoLevel;
    }

    static int efficiencyLevel(ServerPlayer player, ItemStack tool) {
        var enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var efficiency = enchantments.getOrThrow(Enchantments.EFFICIENCY);
        return EnchantmentHelper.getItemEnchantmentLevel(efficiency, tool);
    }

    private static BlockPos toBlockPos(Pos3i pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }

    private static int findBoundToolSlot(Inventory inventory, ItemStack boundTool) {
        if (boundTool.isEmpty()) {
            return -1;
        }
        return InventoryIdentity.findSlot(inventory.getContainerSize(), inventory::getItem, boundTool);
    }

    private static boolean isValidScheduledTool(ItemStack tool, ToolTier requiredTier) {
        if (tool.isEmpty()) {
            return false;
        }
        if (OresMod.isPickaxeRequired() && !tool.is(ItemTags.PICKAXES)) {
            return false;
        }
        return !ToolTier.fromMaxDamage(tool.getMaxDamage()).isLowerThan(requiredTier);
    }

    private static boolean breakAndDrop(ServerPlayer player, BlockPos pos, boolean applyBaseDurability) {
        UUID playerId = player.getUUID();
        boolean addedGuard = OresMod.addActivePlayer(playerId);
        ItemStack originalTool = player.getMainHandItem();
        ItemStack durabilitySnapshot = applyBaseDurability || player.isCreative()
            ? ItemStack.EMPTY
            : originalTool.copy();
        try {
            boolean broken = player.gameMode.destroyBlock(pos);
            if (broken && !durabilitySnapshot.isEmpty()) {
                restoreBaseDurability(player, originalTool, durabilitySnapshot);
            }
            return broken;
        } finally {
            if (addedGuard) {
                OresMod.removeActivePlayer(playerId);
            }
        }
    }

    private static boolean breakAndDropWithBoundTool(
        ServerPlayer player,
        int boundToolSlot,
        ItemStack boundTool,
        BlockPos pos,
        boolean applyBaseDurability,
        int extraDurability
    ) {
        Inventory inventory = player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();
        if (boundToolSlot == selectedSlot) {
            boolean broken = breakAndDrop(player, pos, applyBaseDurability);
            if (broken) {
                applyExtraDurabilityCost(player, boundTool, extraDurability);
            }
            return broken;
        }

        ItemStack selectedItem = inventory.getSelectedItem();
        inventory.setItem(boundToolSlot, ItemStack.EMPTY);
        inventory.setSelectedItem(boundTool);
        try {
            boolean broken = breakAndDrop(player, pos, applyBaseDurability);
            if (broken) {
                applyExtraDurabilityCost(player, boundTool, extraDurability);
            }
            return broken;
        } finally {
            ItemStack resultingTool = inventory.getSelectedItem();
            inventory.setSelectedItem(selectedItem);
            inventory.setItem(boundToolSlot, resultingTool);
            inventory.setChanged();
        }
    }

    private static void restoreBaseDurability(
        ServerPlayer player,
        ItemStack originalTool,
        ItemStack durabilitySnapshot
    ) {
        ItemStack heldTool = player.getMainHandItem();
        if (heldTool == originalTool && !heldTool.isEmpty()) {
            heldTool.setDamageValue(durabilitySnapshot.getDamageValue());
            return;
        }
        if (heldTool.isEmpty()) {
            player.setItemSlot(EquipmentSlot.MAINHAND, durabilitySnapshot);
        }
    }

    private static void applyExtraDurabilityCost(ServerPlayer player, ItemStack tool, int extraDurability) {
        if (extraDurability > 0 && !player.isCreative() && !tool.isEmpty()) {
            tool.hurtAndBreak(extraDurability, player, EquipmentSlot.MAINHAND);
        }
    }

    private static final class OreActivePlayer implements ActivePlayer<ItemStack> {
        private final ServerPlayer player;

        private OreActivePlayer(ServerPlayer player) {
            this.player = player;
        }

        @Override
        public UUID id() {
            return player.getUUID();
        }

        @Override
        public boolean isDeadOrDying() {
            return player.isDeadOrDying();
        }

        @Override
        public double x() {
            return player.getX();
        }

        @Override
        public double y() {
            return player.getY();
        }

        @Override
        public double z() {
            return player.getZ();
        }

        @Override
        public boolean isCreative() {
            return player.isCreative();
        }

        @Override
        public int foodLevel() {
            return player.getFoodData().getFoodLevel();
        }

        @Override
        public float saturation() {
            return player.getFoodData().getSaturationLevel();
        }

        @Override
        public void addExhaustion(float amount) {
            player.getFoodData().addExhaustion(amount);
        }

        @Override
        public int totalExperience() {
            return estimateTotalExperience(player);
        }

        @Override
        public void giveExperiencePoints(int delta) {
            player.giveExperiencePoints(delta);
        }

        @Override
        public int findToolSlot(ItemStack boundTool) {
            return findBoundToolSlot(player.getInventory(), boundTool);
        }

        @Override
        public boolean isValidTool(ItemStack tool, ToolTier requiredTier) {
            return isValidScheduledTool(tool, requiredTier);
        }

        @Override
        public ToolSnapshot toolSnapshot(ItemStack tool) {
            return new ToolSnapshot(
                tool.isDamageableItem(),
                tool.isEnchanted(),
                tool.getMaxDamage(),
                tool.getDamageValue(),
                tool.isEmpty()
            );
        }

        @Override
        public int efficiencyLevel(ItemStack tool) {
            return OresHarvestAdapter.efficiencyLevel(player, tool);
        }
    }
}
