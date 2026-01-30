package com.sheetworld.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sheetworld.Sheetworld;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * Sheetworld Chunk Generator - wraps NoiseBasedChunkGenerator with our settings.
 *
 * Terrain-aware biome selection works automatically because:
 * - noise_settings routes "ridges" to "sheetworld:terrain/mountain_ridges/ridges"
 * - Climate.Sampler.weirdness() returns the actual terrain height
 * - No manual wiring needed - it goes through the standard biome infrastructure
 */
public class SheetworldChunkGenerator extends NoiseBasedChunkGenerator {

    public static final MapCodec<SheetworldChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            RegistryOps.retrieveGetter(Registries.BIOME),
            RegistryOps.retrieveGetter(Registries.DENSITY_FUNCTION),  // Get density function registry
            SheetworldSettings.CODEC.fieldOf("settings").forGetter(gen -> gen.sheetworldSettings),
            NoiseGeneratorSettings.CODEC.fieldOf("noise_settings").forGetter(gen -> gen.generatorSettings())
        ).apply(instance, SheetworldChunkGenerator::new)
    );

    private final SheetworldSettings sheetworldSettings;

    // Static reference for density functions to access current settings
    private static SheetworldSettings CURRENT_SETTINGS = SheetworldSettings.DEFAULT;

    public SheetworldChunkGenerator(
            HolderGetter<Biome> biomeGetter,
            HolderGetter<DensityFunction> densityFunctionGetter,
            SheetworldSettings settings,
            Holder<NoiseGeneratorSettings> noiseSettings
    ) {
        // Create biome source with mountain sampler wired
        super(createBiomeSource(biomeGetter, densityFunctionGetter, settings), noiseSettings);
        this.sheetworldSettings = settings;

        // Update static reference for density functions
        CURRENT_SETTINGS = settings;

        Sheetworld.LOGGER.info("SheetworldChunkGenerator created with settings: worldSize={}, verticalScale={}",
            settings.worldSize().getId(), settings.verticalScale());
    }

    /**
     * Create the biome source.
     * Terrain-aware biomes work automatically via Climate.Sampler.weirdness()
     * which is routed to terrain ridges in noise_settings.
     */
    private static SheetworldBiomeSource createBiomeSource(
            HolderGetter<Biome> biomeGetter,
            HolderGetter<DensityFunction> densityFunctionGetter,
            SheetworldSettings settings
    ) {
        return new SheetworldBiomeSource(biomeGetter, settings.worldSize().getSize());
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /**
     * Get the Sheetworld settings for this generator.
     */
    public SheetworldSettings getSheetworldSettings() {
        return sheetworldSettings;
    }

    /**
     * Get the current settings (for density function access).
     * This is set when a chunk generator is created.
     */
    public static SheetworldSettings getCurrentSettings() {
        return CURRENT_SETTINGS;
    }

    /**
     * Set the current settings (used by config screen during world creation).
     */
    public static void setCurrentSettings(SheetworldSettings settings) {
        CURRENT_SETTINGS = settings;
        Sheetworld.LOGGER.debug("Current settings updated: worldSize={}", settings.worldSize().getId());
    }

    /**
     * Get a config value by key (for density functions).
     */
    public static double getConfigValue(String key) {
        return CURRENT_SETTINGS.getValue(key);
    }
}
