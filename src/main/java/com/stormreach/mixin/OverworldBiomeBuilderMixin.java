package com.stormreach.mixin;

import com.mojang.datafixers.util.Pair;
import com.stormreach.StormreachConfig;
import com.stormreach.registry.ModBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

/**
 * Places Stormreach biomes in the Overworld. Each one takes a share of a vanilla biome's climate space,
 * so the vanilla biome still exists and other mods' biomes are untouched. Only affects new chunks.
 *  - Stormreach Coast: part of the ordinary beach band on cool-to-temperate, wet coasts
 *  - Squallwind Bluffs: half of the Stony Shore cliffs
 *  - Thunderspire Highlands: half of the Windswept Gravelly Hills
 *  - Brinewater Marsh: half of the Swamps
 */
@Mixin(OverworldBiomeBuilder.class)
public abstract class OverworldBiomeBuilderMixin {
    private static final ThreadLocal<Boolean> stormreach$splitting = ThreadLocal.withInitial(() -> false);

    @Inject(method = "pickBeachBiome", at = @At("RETURN"), cancellable = true)
    private void stormreach$stormyBeaches(int temperature, int humidity, CallbackInfoReturnable<ResourceKey<Biome>> cir) {
        if (!StormreachConfig.biomeEnabled || cir.getReturnValue() != Biomes.BEACH) return;
        boolean replace = switch (StormreachConfig.biomeFrequency) {
            case RARE -> temperature == 1 && humidity == 4;
            case NORMAL -> (temperature == 1 || temperature == 2) && humidity >= 3;
            case COMMON -> temperature >= 1 && temperature <= 3 && humidity >= 2;
        };
        if (replace) cir.setReturnValue(ModBiomes.STORMREACH_COAST);
    }

    @Inject(method = "addSurfaceBiome", at = @At("HEAD"), cancellable = true)
    private void stormreach$splitBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer,
                                        Climate.Parameter temperature, Climate.Parameter humidity, Climate.Parameter continentalness,
                                        Climate.Parameter erosion, Climate.Parameter weirdness, float offset, ResourceKey<Biome> biome,
                                        CallbackInfo ci) {
        if (stormreach$splitting.get()) return;
        ResourceKey<Biome> ours = null;
        if (biome == Biomes.STONY_SHORE && StormreachConfig.bluffsEnabled) ours = ModBiomes.SQUALLWIND_BLUFFS;
        else if (biome == Biomes.WINDSWEPT_GRAVELLY_HILLS && StormreachConfig.highlandsEnabled) ours = ModBiomes.THUNDERSPIRE_HIGHLANDS;
        else if (biome == Biomes.SWAMP && StormreachConfig.marshEnabled) ours = ModBiomes.BRINEWATER_MARSH;
        if (ours == null || weirdness.max() - weirdness.min() < 2) return;

        long mid = weirdness.min() + (weirdness.max() - weirdness.min()) / 2;
        OverworldBiomeBuilderInvoker self = (OverworldBiomeBuilderInvoker) (Object) this;
        stormreach$splitting.set(true);
        try {
            self.stormreach$addSurfaceBiome(consumer, temperature, humidity, continentalness, erosion,
                    new Climate.Parameter(weirdness.min(), mid), offset, biome);
            self.stormreach$addSurfaceBiome(consumer, temperature, humidity, continentalness, erosion,
                    new Climate.Parameter(mid, weirdness.max()), offset, ours);
        } finally {
            stormreach$splitting.set(false);
        }
        ci.cancel();
    }
}
