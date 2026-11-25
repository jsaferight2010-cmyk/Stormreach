package com.stormreach.labyrinth;

import com.stormreach.registry.ModEffects;
import com.stormreach.registry.ModSounds;
import com.stormreach.sim.HidingRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Who is hiding in which locker, and whether they've given themselves away. Server-side only. */
public final class HidingTracker {
    public record Hide(BlockPos locker, Direction facing, Vec3 spot, long sinceTick, boolean[] moved) {}

    private static final Map<UUID, Hide> HIDING = new HashMap<>();
    /** Last game tick a guardian could see each player. */
    static final Map<UUID, Long> LAST_SEEN = new HashMap<>();

    private HidingTracker() {}

    public static boolean isHidden(ServerPlayer p) {
        return HIDING.containsKey(p.getUUID());
    }

    public static Hide hide(ServerPlayer p) {
        return HIDING.get(p.getUUID());
    }

    public static void enter(ServerPlayer p, BlockPos lower, Direction facing) {
        Vec3 spot = Vec3.atBottomCenterOf(lower).add(-facing.getStepX() * 0.15, 0, -facing.getStepZ() * 0.15);
        p.teleportTo(spot.x, spot.y, spot.z);
        p.setYRot(facing.toYRot());
        p.setDeltaMovement(Vec3.ZERO);
        HIDING.put(p.getUUID(), new Hide(lower, facing, spot, p.level().getGameTime(), new boolean[]{false}));
        p.addEffect(new MobEffectInstance(ModEffects.HIDDEN, 20 * 60 * 60, 0, false, false, true));
        p.level().playSound(null, lower, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6f, 0.7f);
        p.sendOverlayMessage(Component.translatable("block.stormreach.rusted_locker.hidden").withStyle(ChatFormatting.DARK_AQUA));
        com.stormreach.progress.StormAdvancements.award(p, "hide_locker");
    }

    public static void leave(ServerPlayer p, boolean stepOut) {
        Hide h = HIDING.remove(p.getUUID());
        p.removeEffect(ModEffects.HIDDEN);
        if (h != null && stepOut) {
            Vec3 out = Vec3.atBottomCenterOf(h.locker().relative(h.facing()));
            p.teleportTo(out.x, out.y, out.z);
            p.level().playSound(null, h.locker(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.6f, 0.7f);
        }
    }

    public static void discovered(ServerPlayer p) {
        leave(p, false);
        p.sendSystemMessage(Component.translatable("block.stormreach.rusted_locker.found").withStyle(ChatFormatting.DARK_RED));
    }

    /** Called every tick for each level. */
    public static void tick(ServerLevel level) {
        for (Iterator<Map.Entry<UUID, Hide>> it = HIDING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Hide> e = it.next();
            if (!(level.getEntity(e.getKey()) instanceof ServerPlayer p)) continue;
            Hide h = e.getValue();
            if (!level.getBlockState(h.locker()).is(LabyrinthRegistry.LOCKER)) {
                it.remove();
                p.removeEffect(ModEffects.HIDDEN);
                continue;
            }
            if (p.isShiftKeyDown()) {
                it.remove();
                leave(p, true);
                continue;
            }
            double moved = p.position().distanceTo(h.spot());
            if (moved > 0.6) {
                it.remove();
                p.removeEffect(ModEffects.HIDDEN);
                continue;
            }
            if (moved > 0.08) h.moved()[0] = true;
            if (level.getGameTime() % 20 == 0) {
                HollowKeeper g = LabyrinthTracker.nearestGuardian(level, p.position(), 10);
                if (g != null) p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.HEARTBEAT), SoundSource.PLAYERS,
                        p.getX(), p.getY(), p.getZ(), 1.0f, 1.0f, level.getRandom().nextLong()));
            }
        }
    }

    /** One search check by a guardian standing near a locker. */
    public static boolean searchCheck(ServerLevel level, HollowKeeper guardian, ServerPlayer p) {
        Hide h = hide(p);
        if (h == null) return true;
        double d = guardian.position().distanceTo(Vec3.atCenterOf(h.locker()));
        long seen = LAST_SEEN.getOrDefault(p.getUUID(), -10_000L);
        int since = (int) Math.min(Integer.MAX_VALUE, h.sinceTick() - seen);
        double chance = HidingRules.discoverChance(d, h.moved()[0], Math.max(0, since));
        boolean found = level.getRandom().nextDouble() < chance;
        if (!found && d < 4) com.stormreach.progress.StormAdvancements.award(p, "hide_survived");
        return found;
    }
}
