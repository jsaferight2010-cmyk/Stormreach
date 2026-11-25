package com.stormreach.wild;

import com.stormreach.registry.ModSfx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Sparkskink: a little coastal lizard that soaks up storm charge. Out in the rain (or struck by lightning) it charges:
 * its crest glows, it crackles with sparks, and every few minutes it sheds a Charged Scale. Breed with Glow Berries.
 */
public class Sparkskink extends Animal {
    private static final EntityDataAccessor<Boolean> CHARGED = SynchedEntityData.defineId(Sparkskink.class, EntityDataSerializers.BOOLEAN);
    private int shedTimer = 3000;

    public Sparkskink(EntityType<? extends Sparkskink> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 8).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    static boolean checkSpawn(LevelAccessor level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(BlockTags.SAND) || below.is(BlockTags.BASE_STONE_OVERWORLD) || below.is(Blocks.GRAVEL) || below.is(BlockTags.DIRT)
                || below.is(Blocks.GRASS_BLOCK)) && level.getRawBrightness(pos, 0) > 8;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(CHARGED, false);
    }

    public boolean isCharged() { return entityData.get(CHARGED); }
    public void setCharged(boolean c) { entityData.set(CHARGED, c); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.8));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        Level l = level();
        if (l instanceof ServerLevel server) {
            BlockPos p = blockPosition();
            boolean storm = l.isRainingAt(p) || (l.isThundering() && l.canSeeSky(p));
            if (storm && !isCharged() && random.nextInt(200) == 0) setCharged(true);
            if (isCharged() && !l.isRaining() && random.nextInt(2400) == 0) setCharged(false);
            if (isCharged() && !isBaby() && --shedTimer <= 0) {
                spawnAtLocation(server, new ItemStack(WildRegistry.CHARGED_SCALE));
                playSound(ModSfx.ITEM_POP, 1.0f, 1.4f);
                shedTimer = 3000 + random.nextInt(3000);
            }
        } else if (isCharged() && random.nextFloat() < 0.12f) {
            l.addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.5), getY() + 0.3, getRandomZ(0.5), 0, 0.02, 0);
        }
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        setCharged(true);
        heal(8);
        shedTimer = Math.min(shedTimer, 200);
    }

    @Override public boolean isFood(ItemStack stack) { return stack.is(Items.GLOW_BERRIES); }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return WildRegistry.SPARKSKINK.create(level, EntitySpawnReason.BREEDING); }
    @Override protected SoundEvent getAmbientSound() { return ModSfx.CREATURE_SKINK_CHIRP; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSfx.CREATURE_SMALL_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSfx.CREATURE_SMALL_DEATH; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("Charged", isCharged());
        out.putInt("Shed", shedTimer);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setCharged(in.getBooleanOr("Charged", false));
        shedTimer = in.getIntOr("Shed", 3000);
    }
}
