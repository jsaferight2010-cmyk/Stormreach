package com.stormreach.front;

import com.stormreach.anim.AnimPulse;
import com.stormreach.registry.ModSfx;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rainfrog: a little blue-green frog of the marshes and coasts that knows when rain is coming. In the hours before
 * rain the whole marsh starts to croak, throats puffing, and once the rain arrives they come out to hop about in it.
 * A chorus of Rainfrogs is a forecast you can hear. Breed with Slimeballs. Swims well.
 */
public class Rainfrog extends Animal {
    private int chorusTimer;

    public Rainfrog(EntityType<? extends Rainfrog> type, Level level) {
        super(type, level);
        chorusTimer = 40 + random.nextInt(80);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 6).add(Attributes.MOVEMENT_SPEED, 0.26);
    }

    static boolean checkSpawn(LevelAccessor level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(BlockTags.DIRT) || below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.MUD) || below.is(BlockTags.SAND) || below.is(Blocks.CLAY);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.9) {
            @Override public boolean canUse() { return (level().isRaining() || random.nextInt(3) == 0) && super.canUse(); }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server) || !isAlive()) return;
        boolean soon = FrontArrivals.rainSoon(server), raining = server.isRainingAt(blockPosition());
        if (--chorusTimer > 0) return;
        if (soon || raining) {
            // the chorus: louder and more often the closer the rain
            playSound(ModSfx.CREATURE_FROG_CROAK, soon ? 1.6f : 1.0f, isBaby() ? 1.5f : 0.9f + random.nextFloat() * 0.2f);
            AnimPulse.fire(this, AnimPulse.CAST);
            chorusTimer = soon ? 50 + random.nextInt(60) : 120 + random.nextInt(160);
        } else {
            chorusTimer = 400 + random.nextInt(400);
        }
        if (raining && getHealth() < getMaxHealth() && random.nextInt(4) == 0) heal(1);
    }

    @Override public boolean isFood(ItemStack stack) { return stack.is(Items.SLIME_BALL); }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return FrontRegistry.RAINFROG.create(level, EntitySpawnReason.BREEDING); }
    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_FROG_CROAK; }
    @Override public int getAmbientSoundInterval() { return 400; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.CREATURE_SMALL_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_SMALL_DEATH; }
    @Override public float getVoicePitch() { return 0.8f + random.nextFloat() * 0.25f; }
    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return d > 6 && super.causeFallDamage(d - 6, m, s); }
}
