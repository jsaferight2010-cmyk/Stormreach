package com.stormreach.chase;

import com.stormreach.progress.StormAdvancements;
import com.stormreach.sim.EFScale;
import com.stormreach.tornado.TornadoEntity;
import com.stormreach.tornado.TornadoManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Storm Chaser's Logbook keeps itself: carry one and every tornado you see (within 600 blocks, with a clear line of
 * sight to its funnel) is logged - how many, the strongest, the closest you've been, your vehicle intercepts and probe
 * readings, and the balloons you've launched. Right-click it to read the log.
 */
public final class ChaserLog {
    private static final int MAX_IDS = 64;

    private ChaserLog() {}

    static ItemStack find(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(ChaseRegistry.CHASERS_LOGBOOK)) return inv.getItem(i);
        return ItemStack.EMPTY;
    }

    public static CompoundTag read(ItemStack book) {
        CustomData d = book.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    private static boolean addId(CompoundTag tag, String key, String id) {
        List<String> ids = new ArrayList<>(Arrays.asList(tag.getStringOr(key, "").split(",")));
        ids.removeIf(String::isEmpty);
        if (ids.contains(id)) return false;
        ids.add(id);
        while (ids.size() > MAX_IDS) ids.remove(0);
        tag.putString(key, String.join(",", ids));
        return true;
    }

    private static String shortId(TornadoEntity t) {
        return Long.toHexString(t.getUUID().getMostSignificantBits());
    }

    /** Every second: log what each logbook carrier can see. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator()) continue;
            ItemStack book = find(p);
            if (book.isEmpty()) continue;
            ServerLevel level = p.level();
            for (TornadoEntity t : TornadoManager.live(level)) {
                double d = Math.hypot(t.getX() - p.getX(), t.getZ() - p.getZ());
                double peak = t.currentPeakMph();
                if (d > 600 || peak < 65 || t.funnelReach() < 0.3f) continue;
                Vec3 eye = p.getEyePosition(), funnel = new Vec3(t.getX(), t.getY() + 12, t.getZ());
                if (level.clip(new ClipContext(eye, funnel, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.BLOCK) continue;
                EFScale ef = EFScale.fromGustMph(peak);
                CustomData.update(DataComponents.CUSTOM_DATA, book, tag -> {
                    if (addId(tag, "SeenIds", shortId(t))) tag.putInt("Seen", tag.getIntOr("Seen", 0) + 1);
                    if (ef != null && ef.ordinal() > tag.getIntOr("MaxEF", -1)) tag.putInt("MaxEF", ef.ordinal());
                    if (d < tag.getIntOr("Closest", 99999)) tag.putInt("Closest", (int) Math.round(d));
                });
                CompoundTag tag = read(book);
                StormAdvancements.award(p, "chaser_first");
                if (tag.getIntOr("Seen", 0) >= 10) StormAdvancements.award(p, "chaser_veteran");
                if (tag.getIntOr("MaxEF", -1) >= 4) StormAdvancements.award(p, "chaser_violent");
                if (d <= 50) StormAdvancements.award(p, "chaser_close");
            }
        }
    }

    static void recordIntercept(ServerPlayer p, TornadoEntity t, double d, boolean anchored) {
        if (!anchored || d > 40) return;
        ItemStack book = find(p);
        if (book.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, book, tag -> {
            if (addId(tag, "InterceptIds", shortId(t))) tag.putInt("Intercepts", tag.getIntOr("Intercepts", 0) + 1);
        });
    }

    static void recordProbe(ServerPlayer p, TornadoProbeBlockEntity probe) {
        ItemStack book = find(p);
        if (book.isEmpty() || !probe.firstReadBy(p.getUUID())) return;
        probe.setChanged();
        CustomData.update(DataComponents.CUSTOM_DATA, book, tag -> {
            tag.putInt("Probes", tag.getIntOr("Probes", 0) + 1);
            tag.putInt("ProbePeak", Math.max(tag.getIntOr("ProbePeak", 0), Math.round(probe.peakMph())));
        });
    }

    static void recordBalloon(ServerPlayer p) {
        ItemStack book = find(p);
        if (book.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, book, tag -> tag.putInt("Balloons", tag.getIntOr("Balloons", 0) + 1));
    }

    /** The log, as chat lines. */
    public static List<Component> summary(ItemStack book) {
        CompoundTag t = read(book);
        int maxEf = t.getIntOr("MaxEF", -1), closest = t.getIntOr("Closest", 99999);
        List<Component> out = new ArrayList<>();
        out.add(Component.translatable("item.stormreach.chasers_logbook.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        out.add(Component.translatable("item.stormreach.chasers_logbook.seen", t.getIntOr("Seen", 0),
                maxEf < 0 ? "-" : EFScale.values()[maxEf].name(), closest == 99999 ? "-" : closest + " m").withStyle(ChatFormatting.YELLOW));
        out.add(Component.translatable("item.stormreach.chasers_logbook.field", t.getIntOr("Intercepts", 0), t.getIntOr("Probes", 0),
                t.getIntOr("ProbePeak", 0), t.getIntOr("Balloons", 0)).withStyle(ChatFormatting.GRAY));
        return out;
    }
}
