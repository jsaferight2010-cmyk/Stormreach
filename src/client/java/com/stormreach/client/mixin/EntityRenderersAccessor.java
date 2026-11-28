package com.stormreach.client.mixin;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Reads vanilla's renderer table so each storm mob can reuse its vanilla mob's renderer. */
@Mixin(EntityRenderers.class)
public interface EntityRenderersAccessor {
    @Accessor("PROVIDERS")
    static Map<EntityType<?>, EntityRendererProvider<?>> stormreach$providers() {
        throw new AssertionError("mixin not applied");
    }
}
