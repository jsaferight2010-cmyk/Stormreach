package com.stormreach.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stormreach.StormreachConfig;
import com.stormreach.sky.WindTurbineBlock;
import com.stormreach.structure.Multiblock;
import com.stormreach.tornado.InstrumentBlock;
import com.stormreach.tornado.radar.DopplerRadarItem;
import com.stormreach.tornado.radar.RadarStructure;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 6.0: a holographic preview of a whole machine before you place it (Doppler radar, wind turbine, tornado siren,
 * weather station): the real model, drawn as glowing translucent cyan where it fits, red where something is in the
 * way, with the blocked cells outlined. Replaces the old particle box. Turn off with radarPlacementOutline=false.
 */
public final class PlacementHologram {
    private PlacementHologram() {}

    private static final int FULL_BRIGHT = 0xF000F0;

    public static void init() {
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            if (!StormreachConfig.radarPlacementOutline || mc.player == null || mc.level == null || mc.gui.hud.isHidden()) return;
            if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
            ItemStack held = ItemStack.EMPTY;
            for (InteractionHand h : InteractionHand.values()) if (held.isEmpty() && isMachine(mc.player.getItemInHand(h))) held = mc.player.getItemInHand(h);
            if (held.isEmpty()) return;
            Vec3 cam = ctx.levelState().cameraRenderState.pos;
            long t = mc.level.getGameTime();
            float pulse = 0.78f + 0.22f * (float) Math.sin(t * 0.15);
            PoseStack ps = ctx.poseStack();
            BlockPos at;
            List<BlockPos> blocked;
            Kind kind;
            float yaw = 0;
            if (held.getItem() instanceof DopplerRadarItem) {
                BlockPos base = DopplerRadarItem.baseFor(mc.level, hit.getBlockPos(), hit.getDirection());
                at = base.offset(com.stormreach.tornado.radar.RadarLayout.CONTROLLER);
                blocked = RadarStructure.blocked(mc.level, base);
                kind = Kind.RADAR;
            } else {
                BlockPos clicked = hit.getBlockPos();
                at = mc.level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
                var block = ((BlockItem) held.getItem()).getBlock();
                int height;
                if (block instanceof WindTurbineBlock) {
                    kind = Kind.TURBINE; height = WindTurbineBlock.TOWER;
                    Direction facing = mc.player.getDirection().getOpposite();
                    yaw = (180f - facing.toYRot()) * (float) Math.PI / 180f;
                } else {
                    InstrumentBlock ib = (InstrumentBlock) block;
                    kind = ib.kind() == InstrumentBlock.Kind.TORNADO_SIREN ? Kind.SIREN : Kind.STATION;
                    height = ib.partHeight(ib.defaultBlockState());
                }
                blocked = new java.util.ArrayList<>(Multiblock.blocked(mc.level, at, height));
                if (!mc.level.getBlockState(at).canBeReplaced()) blocked.add(at);
            }
            boolean fits = blocked.isEmpty();
            int alpha = (int) (110 * pulse);
            int tint = (alpha << 24) | (fits ? 0x40D8FF : 0xFF4040);
            final Kind k = kind; final float fy = yaw; final float spin = t * 0.02f;
            ps.pushPose();
            ps.translate(at.getX() + 0.5 - cam.x, at.getY() - cam.y, at.getZ() + 0.5 - cam.z);
            ctx.submitNodeCollector().submitCustomGeometry(ps, RenderTypes.entityTranslucentEmissive(Mesh.white()), (p, vc) -> {
                Mesh.Out o = Mesh.to(p, vc, FULL_BRIGHT).hologram(tint);
                switch (k) {
                    case RADAR -> Machines.dopplerRadar(o);
                    case TURBINE -> Machines.windTurbine(o, fy, spin, WindTurbineBlock.TOWER);
                    case SIREN -> Machines.siren(o, spin);
                    case STATION -> Machines.weatherStation(o, spin * 4, 0);
                }
            });
            ps.popPose();
            // blocked cells: solid red boxes so it is obvious what to clear
            for (BlockPos b : blocked) {
                ps.pushPose();
                ps.translate(b.getX() - cam.x, b.getY() - cam.y, b.getZ() - cam.z);
                ctx.submitNodeCollector().submitCustomGeometry(ps, RenderTypes.entityTranslucentEmissive(Mesh.white()),
                        (p, vc) -> Mesh.box(Mesh.to(p, vc, FULL_BRIGHT), -0.01f, -0.01f, -0.01f, 1.01f, 1.01f, 1.01f, 0x60FF2020));
                ps.popPose();
            }
        });
    }

    private enum Kind { RADAR, TURBINE, SIREN, STATION }

    static boolean isMachine(ItemStack s) {
        if (s.getItem() instanceof DopplerRadarItem) return true;
        if (!(s.getItem() instanceof BlockItem bi)) return false;
        return bi.getBlock() instanceof WindTurbineBlock || (bi.getBlock() instanceof InstrumentBlock ib && ib.partHeight(ib.defaultBlockState()) > 0);
    }
}
