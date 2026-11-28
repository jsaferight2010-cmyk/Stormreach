package com.stormreach.labyrinth;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 6.0: the invisible seat a hiding player sits on inside a Rusted Locker. Riding it is what holds you in place: no
 * per-tick teleports (which made the view snap), the client moves you with the seat smoothly, and it works for any
 * number of players in any number of lockers. It removes itself the moment nobody is sitting in it.
 */
public class LockerSeat extends Entity {
    public LockerSeat(EntityType<? extends LockerSeat> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {}

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && getPassengers().isEmpty() && tickCount > 5) discard();
    }

    @Override public boolean hurtServer(ServerLevel level, DamageSource s, float a) { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean shouldBeSaved() { return false; }   // never left behind in a saved world
    @Override protected void addAdditionalSaveData(ValueOutput out) {}
    @Override protected void readAdditionalSaveData(ValueInput in) {}
}
