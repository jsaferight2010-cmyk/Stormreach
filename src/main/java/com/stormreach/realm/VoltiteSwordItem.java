package com.stormreach.realm;

import com.stormreach.weather.StormWeather;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Voltite blades arc to one more nearby enemy every time they hit. */
public class VoltiteSwordItem extends Item {
    private final float arcDamage;

    public VoltiteSwordItem(Properties properties, float arcDamage) {
        super(properties);
        this.arcDamage = arcDamage;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        if (!(attacker.level() instanceof ServerLevel level)) return;
        LivingEntity next = null;
        double best = 25.0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(5.0),
                e -> e != target && e != attacker && e instanceof Enemy && e.isAlive())) {
            double d = e.distanceToSqr(target);
            if (d < best) { best = d; next = e; }
        }
        if (next == null || StormWeather.isLightningProof(next)) return;
        for (int i = 0; i <= 8; i++) {
            double t = i / 8.0;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX() + (next.getX() - target.getX()) * t,
                    target.getY(0.5) + (next.getY(0.5) - target.getY(0.5)) * t, target.getZ() + (next.getZ() - target.getZ()) * t, 2, 0.05, 0.05, 0.05, 0.0);
        }
        next.hurtServer(level, level.damageSources().lightningBolt(), arcDamage);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.stormreach.voltite_sword.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
