package com.stormreach.tornado;

import com.stormreach.Stormreach;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class TornadoTypes {
    public static final EntityType<TornadoEntity> TORNADO = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id("tornado")),
            EntityType.Builder.<TornadoEntity>of(TornadoEntity::new, MobCategory.MISC).sized(2.0f, 8.0f).fireImmune()
                    .clientTrackingRange(32).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id("tornado"))));

    private TornadoTypes() {}

    public static void init() {}
}
