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
        if (e instanceof net.minecraft.world.entity.player.Player p && e.level() instanceof ServerLevel && e.tickCount % 10 == 0) stormSurge(p);
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

    private static final net.minecraft.resources.Identifier SURGE_DAMAGE = com.stormreach.Stormreach.id("storm_surge_damage");
    private static final net.minecraft.resources.Identifier SURGE_MINING = com.stormreach.Stormreach.id("storm_surge_mining");

    /** A Stormreach weapon or tool (any item of the mod that is a tool or a weapon). */
    public static boolean stormGear(net.minecraft.world.item.ItemStack s) {
        return !s.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace().equals(com.stormreach.Stormreach.MOD_ID)
                && (s.has(net.minecraft.core.component.DataComponents.TOOL) || s.has(net.minecraft.core.component.DataComponents.WEAPON));
    }

    /**
     * 6.0 Storm Surge: Stormreach weapons and tools draw on the weather. Holding one in the rain gives +8% damage and
     * +15% mining speed; in a thunderstorm, +15% damage and +30% mining speed. Under cover it fades. Server-side
     * attribute modifiers, refreshed twice a second, so it works the same in single- and multiplayer.
     */
    public static double[] surge(boolean raining, boolean thundering) {
        return thundering ? new double[]{0.15, 0.30} : raining ? new double[]{0.08, 0.15} : new double[]{0, 0};
    }

    static void stormSurge(net.minecraft.world.entity.player.Player p) {
        var dmg = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        var dig = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED);
        if (dmg == null || dig == null) return;
        boolean wet = p.level().isRainingAt(p.blockPosition()) || p.level().isRainingAt(p.blockPosition().above());
        double[] k = stormGear(p.getMainHandItem()) && wet ? surge(true, p.level().isThundering()) : new double[]{0, 0};
        dmg.removeModifier(SURGE_DAMAGE);
        dig.removeModifier(SURGE_MINING);
        if (k[0] > 0) dmg.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(SURGE_DAMAGE, k[0],
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (k[1] > 0) dig.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(SURGE_MINING, k[1],
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
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
