package com.stormreach.backpack;

import com.stormreach.sim.BackpackTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Consumer;

/** A backpack. Contents live in the stack's container component, so they travel with the item everywhere. */
public class BackpackItem extends Item {
    public final BackpackTier tier;
    private final int fixedSlots;
    private final boolean builtInMagnet;

    public BackpackItem(Properties props, BackpackTier tier) {
        super(props);
        this.tier = tier;
        this.fixedSlots = 0;
        this.builtInMagnet = false;
    }

    /** Special backpacks (achievement rewards) with a fixed size and optional built-in magnetism. */
    public BackpackItem(Properties props, int fixedSlots, boolean builtInMagnet) {
        super(props);
        this.tier = null;
        this.fixedSlots = fixedSlots;
        this.builtInMagnet = builtInMagnet;
    }

    public int slots(Level level, ItemStack stack) {
        int cap = BackpackEnchants.level(level, stack, BackpackEnchants.CAPACITY);
        if (tier == null) return fixedSlots + Math.min(3, cap) * BackpackTier.CAPACITY_PER_LEVEL;
        return tier.slots(cap);
    }

    public int magnetLevel(Level level, ItemStack stack) {
        return Math.max(builtInMagnet ? 1 : 0, BackpackEnchants.level(level, stack, BackpackEnchants.MAGNETISM));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            // sneak + use: put it on your back (or swap with the one you're wearing)
            if (!level.isClientSide()) {
                BackpackEquipment.swapFromHand(player, hand);
                level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.9f);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            int slot = hand == InteractionHand.OFF_HAND ? 40 : sp.getInventory().getSelectedSlot();
            Backpacks.open(sp, slot);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player) || player.isSpectator()) return;
        int magnet = magnetLevel(level, stack);
        if (magnet <= 0 || level.getGameTime() % Math.max(5, com.stormreach.StormreachConfig.backpackMagnetInterval) != 0) return;
        if (player.containerMenu instanceof BackpackMenu open && open.isFor(stack)) return; // the open menu owns the contents
        double r = 2.5 + magnet * 2.0;
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(r),
                e -> e.isAlive() && !e.hasPickUpDelay() && !(e.getItem().getItem() instanceof BackpackItem));
        if (items.isEmpty()) return;
        BackpackStorage storage = BackpackStorage.read(stack, slots(level, stack));
        boolean changed = false;
        for (ItemEntity e : items) {
            ItemStack rest = storage.insert(e.getItem().copy());
            if (rest.getCount() != e.getItem().getCount()) {
                changed = true;
                if (rest.isEmpty()) e.discard();
                else e.setItem(rest);
            }
        }
        if (changed) storage.write(stack);
    }

    @Override
    public void onDestroyed(ItemEntity entity) {
        ItemContainerContents contents = entity.getItem().get(DataComponents.CONTAINER);
        if (contents != null) ItemUtils.onContainerDestroyed(entity, contents.nonEmptyItemCopyStream());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        ItemContainerContents c = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        long used = c.nonEmptyItemCopyStream().count();
        int base = tier == null ? fixedSlots : tier.baseSlots;
        tooltip.accept(Component.translatable("item.stormreach.backpack.slots", used, base).withStyle(ChatFormatting.GRAY));
        if (builtInMagnet) tooltip.accept(Component.translatable("item.stormreach.backpack.magnet").withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("item.stormreach.backpack.open").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.stormreach.backpack.worn_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
