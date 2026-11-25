package com.stormreach.story;

import com.stormreach.StormreachConfig;
import com.stormreach.progress.StormAdvancements;
import com.stormreach.realm.RealmEntities;
import com.stormreach.registry.ModEntities;
import com.stormreach.registry.ModSounds;
import com.stormreach.sim.BossPlan;
import com.stormreach.sim.DialogueScript;
import com.stormreach.sim.EFScale;
import com.stormreach.sim.TornadoFormation;
import com.stormreach.sim.TornadoMorphology;
import com.stormreach.sim.TornadoProfile;
import com.stormreach.tornado.TornadoEntity;
import com.stormreach.tornado.TornadoManager;
import com.stormreach.weather.StormWeather;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cyrrus, the Hollow Storm: the final boss, and the Stranger's true form. It rises out of the Hollow Shrine in a
 * scripted cinematic (voiced; see data/stormreach/cinematic/cyrrus_intro.txt), grows to four times a player's
 * size, then fights in five phases using the weather itself. Every attack is telegraphed first.
 */
public class CyrrusBoss extends Monster {
    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(CyrrusBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CINEMATIC = SynchedEntityData.defineId(CyrrusBoss.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bar = new ServerBossEvent(java.util.UUID.randomUUID(), Component.translatable("entity.stormreach.cyrrus"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_20);
    private final BossPlan plan = new BossPlan();
    private DialogueScript intro;
    private DialogueScript phaseLines;
    private double riseLeft;
    private int galeWindup, hailWindup;
    private int cueIndex;
    private int cinematicTick;
    private double targetScale = 1.0;
    private int phase = 1;
    private BlockPos altar;
    private final List<long[]> pendingStrikes = new ArrayList<>(); // {tick, x, y, z}
    private int crosswindTicks, maelstromTicks;
    private Vec3 crosswind = Vec3.ZERO;

    public CyrrusBoss(EntityType<? extends CyrrusBoss> type, Level level) {
        super(type, level);
        this.xpReward = 600;
        this.setPersistenceRequired();
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(600 * StormreachConfig.bossDifficulty / 100.0);
            setHealth(getMaxHealth());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 600).add(Attributes.ARMOR, 14).add(Attributes.ARMOR_TOUGHNESS, 6)
                .add(Attributes.ATTACK_DAMAGE, 14).add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.FOLLOW_RANGE, 80)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(FORM, 0);
        b.define(CINEMATIC, false);
    }

    /** 0 = the Stranger's shape, 1 = corrupted giant, 2 = final storm form. */
    public int form() {
        return entityData.get(FORM);
    }

    public boolean inCinematic() {
        return entityData.get(CINEMATIC);
    }

    public void beginCinematic(BlockPos altar) {
        this.altar = altar;
        entityData.set(CINEMATIC, true);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.0, true));
        targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        var scale = getAttribute(Attributes.SCALE);
        if (scale != null && Math.abs(scale.getBaseValue() - targetScale) > 0.01) {
            scale.setBaseValue(scale.getBaseValue() + Math.signum(targetScale - scale.getBaseValue()) * 0.02);
        }
        bar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 3 == 0) {
            level.sendParticles(form() == 0 ? ParticleTypes.SMOKE : com.stormreach.particle.ModParticles.STORM_SPARK, getX(), getY() + getBbHeight() * 0.6, getZ(),
                    3 + form() * 3, getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0.05);
        }
        if (inCinematic()) {
            runCinematic(level);
            return;
        }
        resolveStrikes(level);
        updatePhase(level);
        if (crosswindTicks > 0) { crosswindTicks--; blow(level, crosswind, 0.09); deflectProjectiles(level); }
        if (maelstromTicks > 0) maelstrom(level);
        if (galeWindup > 0) {
            level.sendParticles(ParticleTypes.GUST, getX(), getY() + 1, getZ(), 4, 2.5, 0.5, 2.5, 0);
            if (--galeWindup == 0) {
                level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
                for (ServerPlayer p : players(level, 18)) {
                    Vec3 away = p.position().subtract(position()).normalize().scale(2.2);
                    p.push(away.x, 0.9, away.z);
                    p.needsSync = true;
                    p.hurtServer(level, damageSources().mobAttack(this), 4f);
                }
            }
        }
        if (hailWindup > 0 && --hailWindup == 0) {
            for (ServerPlayer p : players(level, 40)) {
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, p.getX(), p.getY() + 8, p.getZ(), 60, 4, 1, 4, 0.1);
                if (level.canSeeSky(p.blockPosition().above()))
                    p.hurtServer(level, level.damageSources().source(StormWeather.HAIL), p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty() ? 6f : 3f);
            }
        }
        if (tickCount % 30 == 0 && getTarget() != null) {
            BossPlan.Attack a = plan.choose(phase, tickCount, new Random(level.getRandom().nextLong()));
            if (a != null) attack(level, a);
        }
    }

    // ---------------- cinematic ----------------
    private void runCinematic(ServerLevel level) {
        if (intro == null) {
            intro = ModSounds.loadScript("cyrrus_intro");
            for (ServerPlayer p : players(level, 64)) {
                p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, intro.length() + 40, 3, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, intro.length() + 40, 4, false, false));
            }
        }
        cinematicTick++;
        if (riseLeft > 0) { // emerge slowly out of the ground below the altar
            double step = Math.min(0.08, riseLeft);
            riseLeft -= step;
            setPos(getX(), getY() + step, getZ());
            if (tickCount % 3 == 0) level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 6, 0.6, 0.4, 0.6, 0.01);
        }
        while (cueIndex < intro.cues.size() && intro.cues.get(cueIndex).tick() <= cinematicTick) {
            DialogueScript.Cue cue = intro.cues.get(cueIndex++);
            Cinematics.speak(level, position().add(0, 2, 0), cue);
            for (String action : cue.actions()) act(level, action);
        }
        // turn to face the nearest player while talking
        Player p = level.getNearestPlayer(this, 64);
        if (p != null) getLookControl().setLookAt(p, 30, 30);
        if (cueIndex >= intro.cues.size() && cinematicTick > intro.length() + 20) act(level, "fight");
    }

    private void act(ServerLevel level, String action) {
        String[] kv = action.split(":");
        switch (kv[0]) {
            case "rise" -> riseLeft += Double.parseDouble(kv[1]);
            case "grow" -> targetScale = Double.parseDouble(kv[1]);
            case "thunder" -> {
                if (level.dimension() == Level.OVERWORLD) level.getServer().setWeatherParameters(0, 12000, true, true);
                level.playSound(null, blockPosition(), com.stormreach.registry.ModSfx.WEATHER_THUNDER, SoundSource.WEATHER, 6f, 0.5f);
            }
            case "rain" -> {
                if (level.dimension() == Level.OVERWORLD) level.getServer().setWeatherParameters(0, 12000, true, false);
                for (ServerPlayer p : players(level, 64)) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
            }
            case "darken" -> {
                for (ServerPlayer p : players(level, 64)) p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
                level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1, getZ(), 80, 3, 1, 3, 0.05);
            }
            case "strike_ring" -> {
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    StormWeather.strikeVisual(level, surface(level, getX() + Math.cos(a) * 9, getZ() + Math.sin(a) * 9));
                }
            }
            case "corrupt" -> entityData.set(FORM, 1);
            case "music" -> level.playSound(null, blockPosition(), ModSounds.CYRRUS_THEME, SoundSource.RECORDS, 4f, 1f);
            case "fight" -> {
                entityData.set(CINEMATIC, false);
                noPhysics = false;
                setNoGravity(false);
                if (form() == 0) entityData.set(FORM, 1);
                targetScale = Math.max(targetScale, 4.0);
                phaseLines = ModSounds.loadScript("cyrrus_phases");
            }
            default -> {}
        }
    }

    // ---------------- combat ----------------
    private void updatePhase(ServerLevel level) {
        int now = BossPlan.phaseFor(getHealth() / getMaxHealth());
        if (now <= phase) return;
        phase = now;
        if (phase == 5) entityData.set(FORM, 2);
        if (phaseLines != null) {
            for (DialogueScript.Cue cue : phaseLines.cues) if (cue.id().equals("phase" + phase)) Cinematics.speak(level, position().add(0, 3, 0), cue);
        }
        level.playSound(null, blockPosition(), com.stormreach.registry.ModSfx.CREATURE_TITAN_AMBIENT, SoundSource.HOSTILE, 5f, 0.6f + phase * 0.1f);
        for (ServerPlayer p : players(level, 96)) p.sendSystemMessage(Component.translatable("entity.stormreach.cyrrus.phase" + phase).withStyle(ChatFormatting.DARK_PURPLE));
    }

    private void attack(ServerLevel level, BossPlan.Attack a) {
        Entity target = getTarget();
        switch (a) {
            case LIGHTNING_VOLLEY -> {
                for (ServerPlayer p : players(level, 40)) telegraph(level, p.blockPosition(), 25);
            }
            case LIGHTNING_RING -> {
                for (int ring = 1; ring <= 3; ring++)
                    for (int i = 0; i < 8 + ring * 4; i++) {
                        double ang = i * Math.PI * 2 / (8 + ring * 4);
                        telegraph(level, surface(level, getX() + Math.cos(ang) * ring * 5, getZ() + Math.sin(ang) * ring * 5), 20 + ring * 10);
                    }
            }
            case DOWNPOUR -> {
                if (level.dimension() == Level.OVERWORLD) level.getServer().setWeatherParameters(0, 6000, true, level.isThundering());
                for (ServerPlayer p : players(level, 48)) {
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0));
                    p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0));
                }
                level.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + 12, getZ(), 400, 16, 4, 16, 0);
            }
            case GALE_PUSH -> { // telegraph: a second of rising gusts and a charging sound, then the blast
                galeWindup = 24;
                playSound(com.stormreach.registry.ModSfx.WEATHER_GUST, 4f, 0.6f);
            }
            case CROSSWIND -> {
                double ang = level.getRandom().nextDouble() * Math.PI * 2;
                crosswind = new Vec3(Math.cos(ang), 0, Math.sin(ang));
                crosswindTicks = 120;
                for (ServerPlayer p : players(level, 64)) p.sendSystemMessage(Component.translatable("entity.stormreach.cyrrus.crosswind").withStyle(ChatFormatting.GRAY));
            }
            case HAIL_BARRAGE -> { // telegraph: white flecks gather over each player; get under a roof (or wear a helmet)
                hailWindup = 40;
                for (ServerPlayer p : players(level, 40)) level.sendParticles(ParticleTypes.WHITE_ASH, p.getX(), p.getY() + 6, p.getZ(), 80, 3, 0.5, 3, 0);
            }
            case SUMMON_SQUALLS -> summon(level, ModEntities.SQUALL, 3);
            case SUMMON_HOST -> {
                summon(level, RealmEntities.STATIC_WISP, 2);
                summon(level, RealmEntities.STORMHAWK, 1);
                summon(level, ModEntities.BRINE_WRAITH, 2);
            }
            case SPAWN_TORNADO -> {
                if (target != null) {
                    Random r = new Random(level.getRandom().nextLong());
                    TornadoProfile p = TornadoProfile.rollWith(r, TornadoFormation.SUPERCELL, phase >= 5 ? EFScale.EF2 : EFScale.EF1,
                            r.nextBoolean() ? TornadoMorphology.ROPE : TornadoMorphology.CONE, 0.25, 0.08, TornadoManager.steeringHeadingDeg());
                    TornadoEntity.spawn(level, target.position().add(r.nextInt(21) - 10, 0, r.nextInt(21) - 10), p);
                }
            }
            case LIGHTNING_CAGE -> {
                if (target != null) for (int i = 0; i < 10; i++) {
                    double ang = i * Math.PI / 5;
                    telegraph(level, surface(level, target.getX() + Math.cos(ang) * 4, target.getZ() + Math.sin(ang) * 4), 30);
                    telegraph(level, surface(level, target.getX() + Math.cos(ang) * 4, target.getZ() + Math.sin(ang) * 4), 60);
                }
            }
            case BLINK -> {
                if (target != null) {
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 2, getZ(), 40, 1, 2, 1, 0.02);
                    double ang = level.getRandom().nextDouble() * Math.PI * 2;
                    BlockPos to = surface(level, target.getX() + Math.cos(ang) * 10, target.getZ() + Math.sin(ang) * 10);
                    teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
                    playSound(com.stormreach.registry.ModSfx.MAGIC_WARP, 3f, 0.5f);
                }
            }
            case MAELSTROM -> {
                maelstromTicks = 70;
                for (ServerPlayer p : players(level, 64)) p.sendSystemMessage(Component.translatable("entity.stormreach.cyrrus.maelstrom").withStyle(ChatFormatting.DARK_RED));
                playSound(com.stormreach.registry.ModSfx.CREATURE_TITAN_CHARGE, 4f, 0.5f);
            }
        }
    }

    /** Telegraph: sparks and a hum on the spot, then a real strike after the delay. */
    private void telegraph(ServerLevel level, BlockPos pos, int delay) {
        level.sendParticles(com.stormreach.particle.ModParticles.STORM_SPARK, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 25, 0.5, 0.1, 0.5, 0.05);
        pendingStrikes.add(new long[]{tickCount + delay, pos.getX(), pos.getY(), pos.getZ()});
    }

    private void resolveStrikes(ServerLevel level) {
        pendingStrikes.removeIf(s -> {
            if (tickCount < s[0]) {
                if ((s[0] - tickCount) % 10 == 0) level.sendParticles(com.stormreach.particle.ModParticles.STORM_SPARK, s[1] + 0.5, s[2] + 0.2, s[3] + 0.5, 6, 0.4, 0.1, 0.4, 0.05);
                return false;
            }
            StormWeather.strike(level, new BlockPos((int) s[1], (int) s[2], (int) s[3]), null);
            return true;
        });
    }

    private void blow(ServerLevel level, Vec3 dir, double strength) {
        for (ServerPlayer p : players(level, 48)) {
            p.push(dir.x * strength, 0, dir.z * strength);
            p.needsSync = true;
        }
        if (tickCount % 4 == 0) level.sendParticles(ParticleTypes.CLOUD, getX() - dir.x * 10, getY() + 2, getZ() - dir.z * 10, 20, 8, 2, 8, 0.4);
    }

    /** Crosswinds bend arrows and other projectiles in flight. */
    private void deflectProjectiles(ServerLevel level) {
        for (Projectile pr : level.getEntitiesOfClass(Projectile.class, getBoundingBox().inflate(32))) {
            pr.setDeltaMovement(pr.getDeltaMovement().add(crosswind.scale(0.06)));
            pr.needsSync = true;
        }
    }

    private void maelstrom(ServerLevel level) {
        maelstromTicks--;
        for (ServerPlayer p : players(level, 24)) {
            Vec3 in = position().subtract(p.position()).normalize().scale(0.14);
            p.push(in.x, 0.02, in.z);
            p.needsSync = true;
        }
        if (maelstromTicks % 10 == 0) {
            double ang = level.getRandom().nextDouble() * Math.PI * 2;
            StormWeather.strike(level, surface(level, getX() + Math.cos(ang) * 7, getZ() + Math.sin(ang) * 7), null);
        }
        if (maelstromTicks == 0) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
            for (ServerPlayer p : players(level, 10)) {
                Vec3 away = p.position().subtract(position()).normalize().scale(3);
                p.push(away.x, 1.2, away.z);
                p.needsSync = true;
                p.hurtServer(level, damageSources().mobAttack(this), 12f);
            }
        }
    }

    private void summon(ServerLevel level, EntityType<? extends Mob> type, int n) {
        for (int i = 0; i < n; i++) {
            Mob m = type.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (m == null) continue;
            BlockPos at = surface(level, getX() + level.getRandom().nextInt(13) - 6, getZ() + level.getRandom().nextInt(13) - 6);
            m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            if (getTarget() instanceof net.minecraft.world.entity.LivingEntity t) m.setTarget(t);
            level.addFreshEntity(m);
        }
    }

    private BlockPos surface(ServerLevel level, double x, double z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
        return new BlockPos((int) Math.floor(x), top <= level.getMinY() ? (int) getY() : top, (int) Math.floor(z));
    }

    private List<ServerPlayer> players(ServerLevel level, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) if (!p.isSpectator() && p.distanceToSqr(this) < r * r) out.add(p);
        return out;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            Cinematics.playDetached(level, position().add(0, 2, 0), "cyrrus_defeat");
            for (ServerPlayer p : players(level, 96)) StormAdvancements.award(p, "defeat_cyrrus");
            if (level.dimension() == Level.OVERWORLD) level.getServer().setWeatherParameters(12000, 0, false, false);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (inCinematic()) return false;
        if (source.is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT)) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override public void thunderHit(ServerLevel level, LightningBolt bolt) {}
    @Override public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }
    @Override protected SoundEvent getAmbientSound() { return com.stormreach.registry.ModSfx.WEATHER_WIND; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return com.stormreach.registry.ModSfx.CREATURE_TITAN_HURT; }
    @Override protected SoundEvent getDeathSound() { return com.stormreach.registry.ModSfx.CREATURE_TITAN_DEATH; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Form", form());
        out.putInt("Phase", phase);
        out.putInt("Scale100", (int) Math.round(targetScale * 100));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(FORM, in.getIntOr("Form", 1));
        phase = in.getIntOr("Phase", 1);
        targetScale = in.getIntOr("Scale100", 400) / 100.0;
        phaseLines = ModSounds.loadScript("cyrrus_phases");
    }

    public static AABB area(BlockPos p) { return new AABB(p).inflate(64); }
}
