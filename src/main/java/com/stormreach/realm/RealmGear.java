package com.stormreach.realm;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

/** Set bonuses and special gear from the Tempest Realm. Runs on both client and server so gliding feels smooth. */
public final class RealmGear {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private RealmGear() {}

    public static void tick(LivingEntity e) {
        if (e.getItemBySlot(EquipmentSlot.FEET).is(RealmItems.CLOUDSTEP_BOOTS)) {
            e.resetFallDistance();
            Vec3 m = e.getDeltaMovement();
            if (!e.onGround() && e.isShiftKeyDown() && m.y < -0.12) {
                e.setDeltaMovement(m.x * 1.03, -0.12, m.z * 1.03);
                if (e.level() instanceof ServerLevel server && e.tickCount % 4 == 0) {
                    server.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY(), e.getZ(), 1, 0.2, 0, 0.2, 0.0);
                }
            }
        }
        if (fullSet(e, RealmItems.AURORITE_HELMET, RealmItems.AURORITE_CHESTPLATE, RealmItems.AURORITE_LEGGINGS, RealmItems.AURORITE_BOOTS)) {
            e.resetFallDistance();
        }
        if (e.level() instanceof ServerLevel && e.tickCount % 40 == 0 && TempestRealm.isRealm(e.level())
                && fullSet(e, RealmItems.VOLTITE_HELMET, RealmItems.VOLTITE_CHESTPLATE, RealmItems.VOLTITE_LEGGINGS, RealmItems.VOLTITE_BOOTS)) {
            e.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false));
            e.addEffect(new MobEffectInstance(MobEffects.HASTE, 60, 0, true, false));
        }
    }

    public static boolean fullAurorite(LivingEntity e) {
        return fullSet(e, RealmItems.AURORITE_HELMET, RealmItems.AURORITE_CHESTPLATE, RealmItems.AURORITE_LEGGINGS, RealmItems.AURORITE_BOOTS);
    }

    private static boolean fullSet(LivingEntity e, Item head, Item chest, Item legs, Item feet) {
        Item[] want = {head, chest, legs, feet};
        for (int i = 0; i < 4; i++) if (!e.getItemBySlot(ARMOR[i]).is(want[i])) return false;
        return true;
    }
}
