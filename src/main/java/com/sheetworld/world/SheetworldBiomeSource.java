package com.sheetworld.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sheetworld.Sheetworld;
import com.sheetworld.climate.BiomeRegistry;
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
 * Cubeworld Biome Source
 * 
 * The world is one face of a cubic planet. The cube rotates on an axis passing
 * through the centers of the top (Hyperborea) and bottom (Hypernotia) faces.
 * 
 * GEOMETRY:
 *                HYPERBOREA (top face, polar)
 *                     ↑ rotation axis
 *                ┌────┴────┐
 *           ┌────┤ NORTH   ├────┐
 *           │WEST│ PLAYER  │EAST│  ← 4 equatorial faces
 *           │    │  FACE   │    │
 *           └────┤ SOUTH   ├────┘
 *                └────┬────┘
 *                     ↓
 *                HYPERNOTIA (bottom face, polar)
 * 
 * CLIMATE MODEL (Three-axis system):
 * 
 * 1. TEMPERATURE: Based on LATITUDE (N-S distance from center)
 *    - Center (Z=0) = equator = hottest (+1)
 *    - N/S edges = facing polar faces = coldest (-1)
 *    - Formula: T = 1 - 2 * (|z|/halfSize)^1.3
 * 
 * 2. HUMIDITY: Air moisture content
 *    - Base level from temperature (warm air holds more moisture)
 *    - Coastal boost (ocean proximity = more humid air)
 *    - Inland drying (continental interiors are dry)
 *    - Affects vegetation lushness, "feel" of biome
 * 
 * 3. PRECIPITATION: Rainfall amount
 *    - Wind patterns create checkerboard (planetary lungs)
 *    - Convergence zones (ITCZ, polar fronts) = more rain
 *    - Subtropical highs (~30% latitude) = DESERTS
 *    - Determines biome type on dry↔wet axis
 * 
 * BIOME SELECTION:
 * Uses BiomeRegistry for plug-and-play modded biome support.
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
    
    // The registry that handles biome selection
    private final BiomeRegistry biomeRegistry;
    
    // === CIRCULATION CELL BOUNDARIES (as fraction of distance to N/S edge) ===
    private static final double HADLEY_END = 0.30;    // Trade winds end / Subtropical high
    private static final double FERREL_END = 0.60;    // Westerlies end / Polar front
    // Beyond FERREL_END is polar easterlies
    
    // === OCEAN THRESHOLDS (tightened for realistic coastlines) ===
    private static final float DEEP_OCEAN_THRESHOLD = -0.45f;
    private static final float OCEAN_THRESHOLD = -0.10f;
    private static final float COAST_THRESHOLD = -0.02f;   // Very narrow beach band
    private static final float INLAND_THRESHOLD = 0.4f;    // Where continental drying kicks in
    
    public SheetworldBiomeSource(HolderGetter<Biome> biomeGetter, int worldSize) {
        this.biomeGetter = biomeGetter;
        this.worldSize = worldSize;
        this.halfSize = worldSize / 2;
        
        // Initialize the biome registry with all available biomes
        this.biomeRegistry = new BiomeRegistry(biomeGetter);
        
        Sheetworld.LOGGER.info("Cubeworld BiomeSource created: worldSize={}, halfSize={}", worldSize, halfSize);
    }
    
    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }
    
    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        // Return all vanilla biomes that might be used
        return Stream.of(
            // Hot
            biomeGetter.getOrThrow(Biomes.DESERT),
            biomeGetter.getOrThrow(Biomes.BADLANDS),
            biomeGetter.getOrThrow(Biomes.ERODED_BADLANDS),
            biomeGetter.getOrThrow(Biomes.WOODED_BADLANDS),
            biomeGetter.getOrThrow(Biomes.SAVANNA),
            biomeGetter.getOrThrow(Biomes.SAVANNA_PLATEAU),
            biomeGetter.getOrThrow(Biomes.JUNGLE),
            biomeGetter.getOrThrow(Biomes.SPARSE_JUNGLE),
            biomeGetter.getOrThrow(Biomes.BAMBOO_JUNGLE),
            biomeGetter.getOrThrow(Biomes.MANGROVE_SWAMP),
            // Warm
            biomeGetter.getOrThrow(Biomes.PLAINS),
            biomeGetter.getOrThrow(Biomes.SUNFLOWER_PLAINS),
            biomeGetter.getOrThrow(Biomes.FOREST),
            biomeGetter.getOrThrow(Biomes.FLOWER_FOREST),
            biomeGetter.getOrThrow(Biomes.BIRCH_FOREST),
            biomeGetter.getOrThrow(Biomes.DARK_FOREST),
            biomeGetter.getOrThrow(Biomes.SWAMP),
            // Cool
            biomeGetter.getOrThrow(Biomes.MEADOW),
            biomeGetter.getOrThrow(Biomes.CHERRY_GROVE),
            biomeGetter.getOrThrow(Biomes.OLD_GROWTH_BIRCH_FOREST),
            // Cold
            biomeGetter.getOrThrow(Biomes.TAIGA),
            biomeGetter.getOrThrow(Biomes.OLD_GROWTH_SPRUCE_TAIGA),
            biomeGetter.getOrThrow(Biomes.OLD_GROWTH_PINE_TAIGA),
            // Freezing
            biomeGetter.getOrThrow(Biomes.SNOWY_PLAINS),
            biomeGetter.getOrThrow(Biomes.SNOWY_TAIGA),
            biomeGetter.getOrThrow(Biomes.GROVE),
            biomeGetter.getOrThrow(Biomes.ICE_SPIKES),
            biomeGetter.getOrThrow(Biomes.SNOWY_SLOPES),
            // Beach/Coast
            biomeGetter.getOrThrow(Biomes.BEACH),
            biomeGetter.getOrThrow(Biomes.SNOWY_BEACH),
            biomeGetter.getOrThrow(Biomes.STONY_SHORE),
            // Ocean
            biomeGetter.getOrThrow(Biomes.WARM_OCEAN),
            biomeGetter.getOrThrow(Biomes.LUKEWARM_OCEAN),
            biomeGetter.getOrThrow(Biomes.OCEAN),
            biomeGetter.getOrThrow(Biomes.COLD_OCEAN),
            biomeGetter.getOrThrow(Biomes.FROZEN_OCEAN),
            // Deep Ocean
            biomeGetter.getOrThrow(Biomes.DEEP_LUKEWARM_OCEAN),
            biomeGetter.getOrThrow(Biomes.DEEP_OCEAN),
            biomeGetter.getOrThrow(Biomes.DEEP_COLD_OCEAN),
            biomeGetter.getOrThrow(Biomes.DEEP_FROZEN_OCEAN)
        );
    }
    
    // ==================== MAIN BIOME SELECTION ====================
    
    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        // Convert from quart coordinates (4 blocks) to block coordinates
        int x = quartX * 4;
        int z = quartZ * 4;
        
        // Check if outside world bounds - return ocean
        if (Math.abs(x) > halfSize || Math.abs(z) > halfSize) {
            double temp = getTemperature(z);
            return biomeRegistry.selectOceanBiome(temp, 0.5, 0.5);
        }
        
        // Sample continentalness from the density function system
        Climate.TargetPoint target = sampler.sample(quartX, quartY, quartZ);
        float continentalness = Climate.unquantizeCoord(target.continentalness());
        
        // Calculate all climate values
        double temperature = getTemperature(z);
        double humidity = getHumidity(x, z, continentalness);
        double precipitation = getPrecipitation(x, z);
        
        // Select biome based on continentalness and climate
        if (continentalness < DEEP_OCEAN_THRESHOLD) {
            return biomeRegistry.selectOceanBiome(temperature, humidity, precipitation);
        }
        if (continentalness < OCEAN_THRESHOLD) {
            return biomeRegistry.selectOceanBiome(temperature, humidity, precipitation);
        }
        if (continentalness < COAST_THRESHOLD) {
            return biomeRegistry.selectCoastBiome(temperature, humidity, precipitation);
        }
        
        // Land biome - use registry for best fit
        return biomeRegistry.selectLandBiome(temperature, humidity, precipitation);
    }
    
    // ==================== TEMPERATURE ====================
    
    /**
     * Calculate temperature based on LATITUDE (Z coordinate).
     * 
     * Range: -1 (polar) to +1 (equatorial)
     */
    private double getTemperature(int z) {
        double latitude = Math.abs(z) / (double) halfSize;
        latitude = Math.min(latitude, 1.0);
        return 1.0 - 2.0 * Math.pow(latitude, 1.3);
    }
    
    // ==================== HUMIDITY (Air Moisture Content) ====================
    
    /**
     * Calculate humidity - the moisture content of the air.
     * 
     * This affects vegetation lushness and the "feel" of biomes.
     * NOT the same as precipitation (rainfall).
     * 
     * Factors:
     * 1. Temperature: Warm air CAN hold more moisture, but doesn't always
     * 2. Ocean proximity: Coastal areas have humid air
     * 3. Continental interior: Deep inland = dry air
     * 
     * Range: 0.0 (bone dry) to 1.0 (saturated)
     */
    private double getHumidity(int x, int z, float continentalness) {
        double temp = getTemperature(z);
        double latitude = Math.abs(z) / (double) halfSize;
        
        // Base humidity: moderate, slightly higher in warm areas
        // Range: 0.35 (cold) to 0.55 (hot)
        double baseHumidity = 0.45 + (temp * 0.1);
        
        // Coastal boost: ocean is a moisture source
        // Near ocean (low continentalness) = humid air
        double coastalEffect = 0.0;
        if (continentalness < COAST_THRESHOLD) {
            // Right at coast - very humid
            coastalEffect = 0.25;
        } else if (continentalness < INLAND_THRESHOLD) {
            // Gradual decrease from coast to inland
            double t = (continentalness - COAST_THRESHOLD) / (INLAND_THRESHOLD - COAST_THRESHOLD);
            coastalEffect = 0.25 * (1.0 - t);
        } else {
            // Deep inland - continental drying (DRY air)
            double inlandFactor = Math.min((continentalness - INLAND_THRESHOLD) * 2.5, 1.0);
            coastalEffect = -0.2 * inlandFactor;
        }
        
        // Subtropical drying: the subtropical high pressure zones have DRY air
        // This is separate from precipitation - the air itself is dry
        double subtropicalDist = Math.abs(latitude - HADLEY_END);
        double subtropicalDrying = Math.exp(-subtropicalDist * subtropicalDist * 80) * -0.15;
        
        // Local variation for organic patterns
        double localVariation = getLocalVariation(x, z, 0.0023, 0.0019) * 0.08;
        
        return clamp(baseHumidity + coastalEffect + subtropicalDrying + localVariation, 0.0, 1.0);
    }
    
    // ==================== PRECIPITATION (Rainfall) ====================
    
    /**
     * Calculate precipitation - how much water falls from the sky.
     * 
     * This is the PRIMARY driver of biome type on the desert↔rainforest axis.
     * 
     * Factors:
     * 1. Wind patterns: The checkerboard effect (planetary lungs)
     *    - Upwind edges receive moisture = WET
     *    - Downwind edges lose moisture = DRY
     * 2. Convergence zones: Where air masses meet = uplift = RAIN
     *    - ITCZ at equator (trade winds converge) = RAINFOREST
     *    - Polar front at ~60% = storms
     * 3. Subtropical HIGH: Air DESCENDS = NO RAIN = DESERTS
     *    - This is the most important factor for creating deserts!
     * 
     * Range: 0.0 (no rain) to 1.0 (constant rain)
     */
    private double getPrecipitation(int x, int z) {
        double latitude = Math.abs(z) / (double) halfSize;
        latitude = Math.min(latitude, 1.0);
        
        double longitude = x / (double) halfSize;
        longitude = clamp(longitude, -1.0, 1.0);
        
        // === 1. WIND-DRIVEN PATTERNS (the planetary lungs checkerboard) ===
        // This is the main driver of E-W precipitation variation
        // Range: -1 (downwind/dry) to +1 (upwind/wet)
        double windEffect = getWindPrecipitationEffect(latitude, longitude);
        
        // === 2. CONVERGENCE / DIVERGENCE ZONES ===
        // These create the N-S precipitation bands
        double zonalEffect = getZonalPrecipitationEffect(latitude);
        
        // === 3. LOCAL VARIATION ===
        double localVariation = getLocalVariation(x, z, 0.0041, 0.0037) * 0.1;
        
        // === COMBINE ===
        // Start at 0.5 (neutral), wind shifts ±0.3, zones shift ±0.35
        // Total range: ~0.0 to ~1.0
        double precipitation = 0.5 + (windEffect * 0.3) + zonalEffect + localVariation;
        
        // Polar dampening - cold regions get less precipitation overall
        if (latitude > FERREL_END) {
            double polarFactor = (latitude - FERREL_END) / (1.0 - FERREL_END);
            precipitation = precipitation * (1.0 - polarFactor * 0.4);
        }
        
        return clamp(precipitation, 0.0, 1.0);
    }
    
    /**
     * Wind-driven precipitation effect (planetary lungs checkerboard).
     * 
     * Wind zones determine which edge receives moisture:
     * - Hadley (0-30%): Trade winds FROM east → East=wet, West=dry
     * - Ferrel (30-60%): Westerlies FROM west → West=wet, East=dry
     * - Polar (60-100%): Easterlies FROM east → East=wet, West=dry
     * 
     * @return -1 (downwind/dry) to +1 (upwind/wet)
     */
    private double getWindPrecipitationEffect(double latitude, double longitude) {
        double windEffect;
        
        if (latitude < HADLEY_END) {
            // TRADE WINDS - blow FROM east (toward west)
            // East edge (+longitude) receives moisture = wet
            windEffect = longitude;
        } else if (latitude < FERREL_END) {
            // WESTERLIES - blow FROM west (toward east)
            // West edge (-longitude) receives moisture = wet
            windEffect = -longitude;
        } else {
            // POLAR EASTERLIES - blow FROM east (toward west)
            windEffect = longitude;
        }
        
        // Smooth transitions at cell boundaries
        windEffect = smoothCellTransitions(windEffect, latitude, longitude);
        
        return windEffect;
    }
    
    /**
     * Zonal (N-S) precipitation effects from atmospheric circulation.
     * 
     * Key zones:
     * 1. ITCZ at equator: Trade winds CONVERGE, air rises, LOTS of rain
     * 2. Subtropical HIGH at ~30%: Air DESCENDS, NO rain, DESERTS form here!
     * 3. Polar FRONT at ~60%: Warm and cold air meet, storms, moderate rain
     * 
     * @return Precipitation modifier (can be strongly negative for deserts!)
     */
    private double getZonalPrecipitationEffect(double latitude) {
        // === ITCZ - Intertropical Convergence Zone ===
        // Very sharp peak right at equator - this is where rainforests are
        double itczBonus = Math.exp(-latitude * latitude * 100) * 0.35;
        
        // === SUBTROPICAL HIGH - THE DESERT MAKER ===
        // This is CRITICAL - strong HIGH pressure at ~30% latitude
        // Air descends, warms, holds onto moisture = NO RAIN
        // This creates Sahara, Arabian, Sonoran, Australian deserts IRL
        double subtropicalDist = Math.abs(latitude - HADLEY_END);
        double subtropicalDrying = Math.exp(-subtropicalDist * subtropicalDist * 60) * -0.4;
        
        // === POLAR FRONT ===
        // Where Ferrel (warm) meets Polar (cold) cells
        // Rising air and storms = moderate precipitation
        double polarFrontDist = Math.abs(latitude - FERREL_END);
        double polarFrontBonus = Math.exp(-polarFrontDist * polarFrontDist * 120) * 0.15;
        
        // === POLAR HIGH ===
        // Cold air descends at poles - less precipitation
        // (Already handled by polar dampening, but add slight effect)
        double polarDrying = 0.0;
        if (latitude > 0.8) {
            polarDrying = -0.1 * ((latitude - 0.8) / 0.2);
        }
        
        return itczBonus + subtropicalDrying + polarFrontBonus + polarDrying;
    }
    
    /**
     * Smooth transitions between circulation cells.
     */
    private double smoothCellTransitions(double windEffect, double latitude, double longitude) {
        double transitionWidth = 0.08;
        
        // Transition at Hadley/Ferrel boundary (30%)
        if (Math.abs(latitude - HADLEY_END) < transitionWidth) {
            double t = (latitude - HADLEY_END + transitionWidth) / (2 * transitionWidth);
            t = smoothstep(t);
            double hadleyEffect = longitude;
            double ferrelEffect = -longitude;
            windEffect = hadleyEffect * (1 - t) + ferrelEffect * t;
        }
        
        // Transition at Ferrel/Polar boundary (60%)
        if (Math.abs(latitude - FERREL_END) < transitionWidth) {
            double t = (latitude - FERREL_END + transitionWidth) / (2 * transitionWidth);
            t = smoothstep(t);
            double ferrelEffect = -longitude;
            double polarEffect = longitude;
            windEffect = ferrelEffect * (1 - t) + polarEffect * t;
        }
        
        return windEffect;
    }
    
    // ==================== UTILITY FUNCTIONS ====================
    
    private double smoothstep(double t) {
        t = clamp(t, 0.0, 1.0);
        return t * t * (3 - 2 * t);
    }
    
    private double getLocalVariation(int x, int z, double freqX, double freqZ) {
        double h1 = Math.sin(x * freqX + z * freqZ) * 0.5;
        double h2 = Math.sin(x * freqX * 1.7 - z * freqZ * 1.3 + 1.7) * 0.3;
        double h3 = Math.cos(x * freqX * 0.6 + z * freqZ * 0.8 + 2.3) * 0.2;
        return h1 + h2 + h3;
    }
    
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    // ==================== DEBUG INFO ====================
    
    /**
     * Get climate debug info for a position.
     */
    public String getClimateDebugInfo(int x, int z, float continentalness) {
        double latitude = Math.abs(z) / (double) halfSize;
        double longitude = x / (double) halfSize;
        double temp = getTemperature(z);
        double humid = getHumidity(x, z, continentalness);
        double precip = getPrecipitation(x, z);
        
        String windZone;
        if (latitude < HADLEY_END) windZone = "Hadley";
        else if (latitude < FERREL_END) windZone = "Ferrel";
        else windZone = "Polar";
        
        String terrainType;
        if (continentalness < DEEP_OCEAN_THRESHOLD) terrainType = "DeepOcean";
        else if (continentalness < OCEAN_THRESHOLD) terrainType = "Ocean";
        else if (continentalness < COAST_THRESHOLD) terrainType = "Coast";
        else terrainType = "Land";
        
        return String.format(
            "Lat:%.2f Lon:%.2f | T:%.2f H:%.2f P:%.2f | %s | Cont:%.2f(%s)",
            latitude, longitude, temp, humid, precip, windZone, continentalness, terrainType
        );
    }
    
    /**
     * Get detailed biome selection debug info.
     */
    public String getBiomeSelectionDebug(int x, int z, float continentalness) {
        double temp = getTemperature(z);
        double humid = getHumidity(x, z, continentalness);
        double precip = getPrecipitation(x, z);
        return biomeRegistry.getSelectionDebug(temp, humid, precip);
    }
    
    public int getWorldSize() {
        return worldSize;
    }
}
