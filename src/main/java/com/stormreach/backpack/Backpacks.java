package com.stormreach.backpack;

import com.stormreach.Stormreach;
import com.stormreach.progress.StormAdvancements;
import com.stormreach.sim.BackpackTier;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Backpack registration, opening (from hand or the Open Backpack key), and Soulbound handling. */
public final class Backpacks {
    @SuppressWarnings("unchecked")
    public static final MenuType<BackpackMenu>[] MENUS = new MenuType[7];
    public static final List<Item> ITEMS = new ArrayList<>();
    public static final Map<BackpackTier, Item> BY_TIER = new HashMap<>();
    public static final Map<BackpackTier, Item> UPGRADES = new HashMap<>();
    public static Item STORM_CHASERS_PACK;

    static {
        for (int rows = 1; rows <= 6; rows++) {
            final int r = rows;
            MenuType<BackpackMenu>[] holder = MENUS;
            MENUS[rows] = Registry.register(BuiltInRegistries.MENU, Stormreach.id("backpack_" + rows),
                    new MenuType<>((id, inv) -> new BackpackMenu(holder[r], id, inv, r), FeatureFlags.VANILLA_SET));
        }
        for (BackpackTier t : BackpackTier.values()) {
            Item.Properties p = new Item.Properties().stacksTo(1).enchantable(t.ordinal() >= 2 ? 15 : 10)
                    .rarity(t.ordinal() >= 5 ? Rarity.EPIC : t.ordinal() >= 2 ? Rarity.RARE : Rarity.UNCOMMON);
            if (t.ordinal() >= 2) p.fireResistant();
            BY_TIER.put(t, item(t.id + "_backpack", q -> new BackpackItem(q, t), p));
            if (t.previous() != null) {
                UPGRADES.put(t, item(t.id + "_backpack_upgrade", Item::new, new Item.Properties().stacksTo(16)
                        .rarity(t.ordinal() >= 5 ? Rarity.EPIC : Rarity.RARE)));
            }
        }
        STORM_CHASERS_PACK = item("storm_chasers_pack", q -> new BackpackItem(q, 45, true),
                new Item.Properties().stacksTo(1).enchantable(12).rarity(Rarity.EPIC).fireResistant());
    }

    /** Client asks the server to open the first backpack in its inventory. */
    public record OpenBackpackPayload() implements CustomPacketPayload {
        public static final Type<OpenBackpackPayload> TYPE = new Type<>(Stormreach.id("open_backpack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenBackpackPayload> CODEC = StreamCodec.unit(new OpenBackpackPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static final Map<UUID, List<ItemStack>> SOULBOUND = new HashMap<>();
    private static final Map<UUID, net.minecraft.world.phys.Vec3> SOULBOUND_POS = new HashMap<>();
    private static final Map<UUID, ResourceKey<net.minecraft.world.level.Level>> SOULBOUND_DIM = new HashMap<>();

    private Backpacks() {}

    private static Item item(String name, Function<Item.Properties, Item> f, Item.Properties p) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        Item it = Registry.register(BuiltInRegistries.ITEM, key, f.apply(p.setId(key)));
        ITEMS.add(it);
        return it;
    }

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(OpenBackpackPayload.TYPE, OpenBackpackPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OpenBackpackPayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            ctx.server().execute(() -> {
                Inventory inv = p.getInventory();
                int[] order = new int[inv.getContainerSize()];
                // offhand first, then hotbar, then the main inventory
                int n = 0;
                order[n++] = 40;
                for (int i = 0; i < inv.getContainerSize(); i++) if (i != 40) order[n++] = i;
                for (int i : order) {
                    if (inv.getItem(i).getItem() instanceof BackpackItem) {
                        open(p, i);
                        return;
                    }
                }
                p.sendOverlayMessage(Component.translatable("item.stormreach.backpack.none"));
            });
        });
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (alive) return;
            List<ItemStack> kept = SOULBOUND.remove(oldPlayer.getUUID());
            SOULBOUND_POS.remove(oldPlayer.getUUID());
            SOULBOUND_DIM.remove(oldPlayer.getUUID());
            if (kept != null) for (ItemStack s : kept) if (!newPlayer.getInventory().add(s)) newPlayer.level().addFreshEntity(new ItemEntity(newPlayer.level(), newPlayer.getX(), newPlayer.getY(), newPlayer.getZ(), s));
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // anyone who died and never respawned gets their soulbound packs dropped where they died, never lost
            for (Map.Entry<UUID, List<ItemStack>> e : SOULBOUND.entrySet()) {
                ServerLevel level = server.getLevel(SOULBOUND_DIM.getOrDefault(e.getKey(), net.minecraft.world.level.Level.OVERWORLD));
                net.minecraft.world.phys.Vec3 pos = SOULBOUND_POS.get(e.getKey());
                if (level == null || pos == null) continue;
                for (ItemStack s : e.getValue()) level.addFreshEntity(new ItemEntity(level, pos.x, pos.y, pos.z, s));
            }
            SOULBOUND.clear();
        });
    }

    public static void open(ServerPlayer player, int invSlot) {
        ItemStack stack = player.getInventory().getItem(invSlot);
        if (!(stack.getItem() instanceof BackpackItem bp)) return;
        int capacity = bp.slots(player.level(), stack);
        int rows = BackpackTier.rows(capacity);
        boolean canSort = BackpackEnchants.level(player.level(), stack, BackpackEnchants.ORGANIZATION) > 0;
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> BackpackMenu.server(MENUS[rows], id, inv, stack, invSlot, capacity, canSort),
                stack.getHoverName()));
        StormAdvancements.award(player, "open_backpack");
    }

    /** Called from the Inventory.dropAll mixin before a dead player's items are dropped. */
    public static void keepSoulbound(Inventory inv) {
        if (!(inv.player instanceof ServerPlayer sp)) return;
        List<ItemStack> kept = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof BackpackItem && BackpackEnchants.level(sp.level(), s, BackpackEnchants.SOULBOUND) > 0) {
                kept.add(s);
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (!kept.isEmpty()) {
            SOULBOUND.computeIfAbsent(sp.getUUID(), k -> new ArrayList<>()).addAll(kept);
            SOULBOUND_POS.put(sp.getUUID(), sp.position());
            SOULBOUND_DIM.put(sp.getUUID(), sp.level().dimension());
        }
    }

    /** Resilience: the dropped pack can't be destroyed. */
    public static boolean indestructible(ItemEntity e) {
        ItemStack s = e.getItem();
        return s.getItem() instanceof BackpackItem && BackpackEnchants.level(e.level(), s, BackpackEnchants.RESILIENCE) > 0;
    }
}
