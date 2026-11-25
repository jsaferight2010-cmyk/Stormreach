package com.stormreach.client.tornado;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.stormreach.Stormreach;
import com.stormreach.sim.FunnelShape;
import com.stormreach.sim.TornadoFormation;
import com.stormreach.sim.TornadoMorphology;
import com.stormreach.tornado.TornadoEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Draws a tornado as layered, turbulent meshes rather than a model:
 * <ul>
 * <li>the condensation funnel, a lumpy tube that spins faster near the ground and lowers from the cloud base
 *     with a pendant tip until it touches down ({@link FunnelShape}),</li>
 * <li>a looser outer sheath for depth,</li>
 * <li>the debris cloud at the base (dust and wreckage over land, white spray over water),</li>
 * <li>for supercell tornadoes, the lowered, rotating wall cloud the funnel hangs from,</li>
 * <li>for multiple-vortex tornadoes, suction vortices orbiting inside the main circulation.</li>
 * </ul>
 * Shape comes from the morphology, width and reach the server syncs, so every player sees the same tornado.
 */
public class TornadoRenderer extends EntityRenderer<TornadoEntity, TornadoRenderState> {
    private static final Identifier FUNNEL = Stormreach.id("textures/entity/tornado_funnel.png");
    private static final Identifier DEBRIS = Stormreach.id("textures/entity/tornado_debris.png");
    private static final Identifier WALL = Stormreach.id("textures/entity/tornado_wallcloud.png");

    public TornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public TornadoRenderState createRenderState() {
        return new TornadoRenderState();
    }

    @Override
    public void extractRenderState(TornadoEntity e, TornadoRenderState s, float partial) {
        super.extractRenderState(e, s, partial);
        s.width = Math.max(1.5f, e.visualWidth());
        s.cloudBase = e.cloudBase();
        s.reach = e.funnelReach();
        s.intensity = e.intensity();
        s.morph = e.morphology().ordinal();
        s.formation = e.formation().ordinal();
        s.subvortices = e.subvortices();
        s.rotation = -e.rotationSign(); // angles here grow from +x toward +z; WindField's cyclonic flow turns the other way
        s.seed = (e.getUUID().hashCode() & 1023) * 0.37f;
        s.spin = Mth.lerp(partial, e.clientSpinO, e.clientSpin);
        s.seeThrough = TornadoSeeThrough.active();
    }

    @Override
    protected AABB getBoundingBoxForCulling(TornadoEntity e, float partial) {
        double w = Math.max(16, e.visualWidth() * 4 + 40);
        return new AABB(e.getX() - w, e.getY() - 4, e.getZ() - w, e.getX() + w, e.getY() + e.cloudBase() + 16, e.getZ() + w);
    }

    @Override
    public void submit(TornadoRenderState s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(s, pose, collector, camera);
        int light = s.lightCoords;
        if (s.reach > 0.01f)
            collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(FUNNEL), (p, vc) -> TornadoMesh.funnelLayer(s, sink(p, vc, light)));
        if (TornadoMesh.hasDebris(s))
            collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(DEBRIS), (p, vc) -> TornadoMesh.debrisLayer(s, sink(p, vc, light)));
        if (TornadoMesh.hasWallCloud(s))
            collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(WALL), (p, vc) -> TornadoMesh.wallLayer(s, sink(p, vc, light)));
    }

    private static TornadoMesh.Sink sink(PoseStack.Pose p, VertexConsumer vc, int light) {
        return (x, y, z, u, v, argb) -> {
            float len = Math.max(0.001f, Mth.sqrt(x * x + z * z));
            vc.addVertex(p, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, x / len, 0, z / len);
        };
    }
}
