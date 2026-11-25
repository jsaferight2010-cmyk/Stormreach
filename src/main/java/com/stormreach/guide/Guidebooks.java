package com.stormreach.guide;

import com.stormreach.Stormreach;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The two guidebooks every player gets once, the first time they join: the Stormreach Field Guide (everything in the
 * mod, with the rarity tables) and the Storm Crafter's Handbook (every recipe, drawn with the real item icons). A
 * full inventory doesn't lose them: they drop at your feet. They can always be re-crafted from a book and a Storm
 * Shard.
 */
public final class Guidebooks {
    private Guidebooks() {}

    public static final int FIELD_GUIDE = 0, CRAFTING = 1;
    /** Set by the client: opens the book screen. Does nothing on a dedicated server. */
    public static IntConsumer opener = k -> {};
    public static final String GIVEN_TAG = "stormreach.guidebooks_given";

    public static final Item FIELD_GUIDE_ITEM = register("stormreach_field_guide", FIELD_GUIDE);
    public static final Item CRAFTING_ITEM = register("storm_crafters_handbook", CRAFTING);

    public static class GuidebookItem extends Item {
        private final int kind;

        public GuidebookItem(Properties p, int kind) {
            super(p);
            this.kind = kind;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide()) opener.accept(kind);
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack s, TooltipContext ctx, TooltipDisplay d, Consumer<Component> tooltip, TooltipFlag f) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        }
    }

    private static Item register(String name, int kind) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        Item it = Registry.register(BuiltInRegistries.ITEM, key, new GuidebookItem(new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.UNCOMMON), kind));
        return it;
    }

    public static void init() {
        ServerPlayerEvents.JOIN.register(Guidebooks::giveOnce);
    }

    /** Gives both books if this player has never had them. Public for GameTests. */
    public static boolean giveOnce(ServerPlayer p) {
        if (p.entityTags().contains(GIVEN_TAG)) return false;
        p.addTag(GIVEN_TAG);
        for (Item book : new Item[]{FIELD_GUIDE_ITEM, CRAFTING_ITEM}) {
            ItemStack s = new ItemStack(book);
            if (!p.getInventory().add(s)) // full inventory: drop it at your feet, never lose it
                p.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(p.level(), p.getX(), p.getY(), p.getZ(), s));
        }
        p.sendSystemMessage(Component.translatable("item.stormreach.guidebooks.given").withStyle(ChatFormatting.AQUA));
        return true;
    }
}
