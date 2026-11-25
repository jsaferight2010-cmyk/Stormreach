package com.stormreach.entity;

import com.stormreach.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A bottled Squall. Shatters into a gust that throws everything nearby, including you, upward and outward. */
public class SquallBottleProjectile extends ThrowableItemProjectile {
    public SquallBottleProjectile(EntityType<? extends SquallBottleProjectile> type, Level level) {
        super(type, level);
    }

    public SquallBottleProjectile(Level level, LivingEntity owner, ItemStack stack) {
        super(com.stormreach.registry.ModEntities.SQUALL_BOTTLE, owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SQUALL_IN_A_BOTTLE;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            Vec3 at = result.getLocation();
            server.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            server.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 20, 0.6, 0.6, 0.6, 0.1);
            server.playSound(null, at.x, at.y, at.z, com.stormreach.registry.ModSfx.WEATHER_GUST, SoundSource.NEUTRAL, 1.0f, 0.8f);
            for (Entity e : server.getEntities(this, new AABB(at, at).inflate(4.5), e -> !e.isSpectator())) {
                Vec3 away = e.position().subtract(at);
                double dist = Math.max(0.5, away.length());
                Vec3 push = away.normalize().scale(1.4 / dist + 0.4);
                e.push(push.x, 0.9 / dist + 0.5, push.z);
                e.needsSync = true;
                e.resetFallDistance();
            }
            discard();
        }
    }
}
