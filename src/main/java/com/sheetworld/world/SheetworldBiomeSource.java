package com.sheetworld.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sheetworld.Sheetworld;
import com.sheetworld.climate.BiomeRegistry;
import com.sheetworld.climate.TerrainBiomeSelector;
import com.sheetworld.climate.TerrainParameters;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * Sheetworld Biome Source - Clean Terrain-Based Selection
 *
 * Uses TerrainBiomeSelector for proper vanilla-style terrain gating:
 * 1. Continentalness → Ocean/Coast/Land
 * 2. PV (Peaks/Valleys) → Mountain/River/Flat
 * 3. Erosion → Rugged/Smooth terrain variants
 * 4. Climate (temp/humid/precip) → Specific biome within category
 *
 * The "LAND" category (everything that passes terrain gates) uses
 * the BiomeRegistry's climate-based selection system.
 */
public class SheetworldBiomeSource extends BiomeSource {

    public static final MapCodec<SheetworldBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            RegistryOps.retrieveGetter(Registries.BIOME),
            Codec.intRange(1000, 100000).fieldOf("world_size").forGetter(source -> source.worldSize)
        ).apply(instance, SheetworldBiomeSource::new)
    );

    private final HolderGetter<Biome> biomeGetter;
    private final int worldSize;
    private final int halfSize;

    // The terrain-based biome selector handles ALL gating logic
    private final TerrainBiomeSelector terrainSelector;

    // === ATMOSPHERIC CIRCULATION CELL BOUNDARIES ===
    private static final double HADLEY_END = 0.30;
    private static final double FERREL_END = 0.60;

    public SheetworldBiomeSource(HolderGetter<Biome> biomeGetter, int worldSize) {
        this.biomeGetter = biomeGetter;
        this.worldSize = worldSize;
        this.halfSize = worldSize / 2;

        // Create the registry and selector
        BiomeRegistry registry = new BiomeRegistry(biomeGetter);
        this.terrainSelector = new TerrainBiomeSelector(registry);

        Sheetworld.LOGGER.info("SheetworldBiomeSource created with TerrainBiomeSelector: worldSize={}", worldSize);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(
            // Polar/Ice
            biomeGetter.getOrThrow(Biomes.ICE_SPIKES),
            biomeGetter.getOrThrow(Biomes.SNOWY_PLAINS),
            biomeGetter.getOrThrow(Biomes.FROZEN_PEAKS),
            // Tundra
            biomeGetter.getOrThrow(Biomes.SNOWY_TAIGA),
            biomeGetter.getOrThrow(Biomes.SNOWY_SLOPES),
            // Taiga
            biomeGetter.getOrThrow(Biomes.TAIGA),
            biomeGetter.getOrThrow(Biomes.OLD_GROWTH_SPRUCE_TAIGA),
            biomeGetter.getOrThrow(Biomes.OLD_GROWTH_PINE_TAIGA),
            biomeGetter.getOrThrow(Biomes.GROVE),
            // Temperate Forest
            biomeGetter.getOrThrow(Biomes.FOREST),
            biomeGetter.getOrThrow(Biomes.BIRCH_FOREST),
            biomeGetter.getOrThrow(Biomes.OLD_GROWTH_BIRCH_FOREST),
            biomeGetter.getOrThrow(Biomes.DARK_FOREST),
            biomeGetter.getOrThrow(Biomes.FLOWER_FOREST),
            // Temperate Steppe
            biomeGetter.getOrThrow(Biomes.PLAINS),
            biomeGetter.getOrThrow(Biomes.SUNFLOWER_PLAINS),
            biomeGetter.getOrThrow(Biomes.MEADOW),
            // Tropical
            biomeGetter.getOrThrow(Biomes.JUNGLE),
            biomeGetter.getOrThrow(Biomes.SPARSE_JUNGLE),
            biomeGetter.getOrThrow(Biomes.BAMBOO_JUNGLE),
            biomeGetter.getOrThrow(Biomes.MANGROVE_SWAMP),
            // Savanna
            biomeGetter.getOrThrow(Biomes.SAVANNA),
            biomeGetter.getOrThrow(Biomes.SAVANNA_PLATEAU),
            // Desert/Arid
            biomeGetter.getOrThrow(Biomes.DESERT),
            biomeGetter.getOrThrow(Biomes.BADLANDS),
            biomeGetter.getOrThrow(Biomes.ERODED_BADLANDS),
            biomeGetter.getOrThrow(Biomes.WOODED_BADLANDS),
            // Wetland
            biomeGetter.getOrThrow(Biomes.SWAMP),
            // Montane
            biomeGetter.getOrThrow(Biomes.JAGGED_PEAKS),
            biomeGetter.getOrThrow(Biomes.STONY_PEAKS),
            biomeGetter.getOrThrow(Biomes.CHERRY_GROVE),
            // Windswept
            biomeGetter.getOrThrow(Biomes.WINDSWEPT_HILLS),
            biomeGetter.getOrThrow(Biomes.WINDSWEPT_FOREST),
            biomeGetter.getOrThrow(Biomes.WINDSWEPT_GRAVELLY_HILLS),
            // River
            biomeGetter.getOrThrow(Biomes.RIVER),
            biomeGetter.getOrThrow(Biomes.FROZEN_RIVER),
            // Ocean
            biomeGetter.getOrThrow(Biomes.WARM_OCEAN),
            biomeGetter.getOrThrow(Biomes.LUKEWARM_OCEAN),
            biomeGetter.getOrThrow(Biomes.OCEAN),
            biomeGetter.getOrThrow(Biomes.COLD_OCEAN),
            biomeGetter.getOrThrow(Biomes.FROZEN_OCEAN),
            biomeGetter.getOrThrow(Biomes.DEEP_LUKEWARM_OCEAN),
            biomeGetter.getOrThrow(Biomes.DEEP_OCEAN),
            biomeGetter.getOrThrow(Biomes.DEEP_COLD_OCEAN),
            biomeGetter.getOrThrow(Biomes.DEEP_FROZEN_OCEAN),
            // Coast
            biomeGetter.getOrThrow(Biomes.BEACH),
            biomeGetter.getOrThrow(Biomes.SNOWY_BEACH),
            biomeGetter.getOrThrow(Biomes.STONY_SHORE),
            // Special
            biomeGetter.getOrThrow(Biomes.MUSHROOM_FIELDS)
        );
    }

    // ==================== MAIN BIOME SELECTION ====================

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int x = quartX * 4;
        int z = quartZ * 4;

        // World bounds check - return ocean if outside
        if (Math.abs(x) > halfSize || Math.abs(z) > halfSize) {
            return terrainSelector.selectBiome(-1.0f, 0, 0, getTemperature(z), 0.5, 0.5);
        }

        // Get terrain parameters from sampler
        Climate.TargetPoint target = sampler.sample(quartX, quartY, quartZ);
        float continentalness = Climate.unquantizeCoord(target.continentalness());
        float erosion = Climate.unquantizeCoord(target.erosion());
        float weirdness = Climate.unquantizeCoord(target.weirdness());

        // Calculate climate (latitude-based temp, circulation-based precip)
        double temperature = getTemperature(z);
        double humidity = getPrecipitation(x, z);
        double precipitation = humidity;

        // Let TerrainBiomeSelector handle ALL the gating logic
        return terrainSelector.selectBiome(
            continentalness, erosion, weirdness,
            temperature, humidity, precipitation
        );
    }

    // ==================== CLIMATE CALCULATIONS ====================

    /**
     * Temperature based on latitude (Z coordinate).
     * Range: -1 (polar) to +1 (equatorial)
     */
    private double getTemperature(int z) {
        double latitude = Math.abs(z) / (double) halfSize;
        latitude = Math.min(latitude, 1.0);
        return 1.0 - 2.0 * Math.pow(latitude, 1.3);
    }

    /**
     * Base precipitation based on atmospheric circulation.
     * Range: 0.0 (desert) to 1.0 (rainforest)
     */
    private double getPrecipitation(int x, int z) {
        double latitude = Math.abs(z) / (double) halfSize;
        latitude = Math.min(latitude, 1.0);
        double longitude = x / (double) halfSize;
        longitude = clamp(longitude, -1.0, 1.0);

        // Wind-driven patterns (planetary lungs)
        double windEffect = getWindPrecipitationEffect(latitude, longitude);

        // Convergence/divergence zones
        double zonalEffect = getZonalPrecipitationEffect(latitude);

        // Local variation
        double localVariation = getLocalNoise(x, z) * 0.1;

        // Combine
        double precipitation = 0.5 + (windEffect * 0.3) + zonalEffect + localVariation;

        // Polar dampening
        if (latitude > FERREL_END) {
            double polarFactor = (latitude - FERREL_END) / (1.0 - FERREL_END);
            precipitation *= (1.0 - polarFactor * 0.4);
        }

        return clamp(precipitation, 0.0, 1.0);
    }

    private double getWindPrecipitationEffect(double latitude, double longitude) {
        if (latitude < HADLEY_END) {
            return longitude;  // Trade winds from east
        } else if (latitude < FERREL_END) {
            return -longitude; // Westerlies from west
        } else {
            return longitude;  // Polar easterlies from east
        }
    }

    private double getZonalPrecipitationEffect(double latitude) {
        // ITCZ at equator
        double itczBonus = Math.exp(-latitude * latitude * 100) * 0.35;

        // Subtropical high (desert maker) at ~30%
        double subtropicalDist = Math.abs(latitude - HADLEY_END);
        double subtropicalDrying = Math.exp(-subtropicalDist * subtropicalDist * 60) * -0.4;

        // Polar front at ~60%
        double polarFrontDist = Math.abs(latitude - FERREL_END);
        double polarFrontBonus = Math.exp(-polarFrontDist * polarFrontDist * 120) * 0.15;

        return itczBonus + subtropicalDrying + polarFrontBonus;
    }

    // ==================== UTILITY FUNCTIONS ====================

    private double getLocalNoise(int x, int z) {
        double h1 = Math.sin(x * 0.0041 + z * 0.0037) * 0.5;
        double h2 = Math.sin(x * 0.0069 - z * 0.0048 + 1.7) * 0.3;
        double h3 = Math.cos(x * 0.0025 + z * 0.0032 + 2.3) * 0.2;
        return h1 + h2 + h3;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // ==================== DEBUG ====================

    public String getTerrainDebugInfo(int x, int z, float continentalness, float erosion, float weirdness) {
        double temperature = getTemperature(z);
        double precipitation = getPrecipitation(x, z);

        return TerrainParameters.getDebugString(continentalness, erosion, weirdness) +
            String.format(" | Climate: T=%.2f P=%.2f", temperature, precipitation);
    }

    public int getWorldSize() {
        return worldSize;
    }
}
