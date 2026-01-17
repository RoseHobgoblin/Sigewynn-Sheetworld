package com.sheetworld.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sheetworld.Sheetworld;
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
 *    - Affects vegetation lushness, "feel" of biome
 * 
 * 3. PRECIPITATION: Rainfall amount
 *    - Wind patterns create checkerboard (planetary lungs)
 *    - Convergence zones (ITCZ, polar fronts) = more rain
 *    - Determines biome type on dry↔wet axis
 *    
 *    Wind zones (symmetric about equator):
 *    - 0-30% latitude: Trade winds / Hadley cell (wind FROM east)
 *    - 30-60% latitude: Westerlies / Ferrel cell (wind FROM west)
 *    - 60-100% latitude: Polar easterlies (wind FROM east)
 */
public class SheetworldBiomeSource extends BiomeSource {
    
    public static final MapCodec<SheetworldBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            RegistryOps.retrieveGetter(Registries.BIOME),
            Codec.intRange(1000, 100000).fieldOf("world_size").forGetter(source -> source.worldSize)
        ).apply(instance, SheetworldBiomeSource::new)
    );
    
    private final HolderGetter<Biome> biomeRegistry;
    private final int worldSize;
    private final int halfSize;
    
    // === CIRCULATION CELL BOUNDARIES (as fraction of distance to N/S edge) ===
    private static final double HADLEY_END = 0.30;    // Trade winds end
    private static final double FERREL_END = 0.60;    // Westerlies end
    // Beyond FERREL_END is polar easterlies
    
    // === OCEAN THRESHOLDS ===
    private static final float DEEP_OCEAN_THRESHOLD = -0.5f;
    private static final float OCEAN_THRESHOLD = -0.2f;
    private static final float COAST_THRESHOLD = 0.05f;
    private static final float INLAND_THRESHOLD = 0.5f;
    
    // === BIOME HOLDERS ===
    
    // Hot biomes (temperature > 0.5) - equatorial
    private final Holder<Biome> desert;
    private final Holder<Biome> badlands;
    private final Holder<Biome> savanna;
    private final Holder<Biome> jungle;
    private final Holder<Biome> sparseJungle;
    private final Holder<Biome> mangroveSwamp;
    
    // Warm biomes (temperature 0.2 to 0.5) - subtropical
    private final Holder<Biome> plains;
    private final Holder<Biome> forest;
    private final Holder<Biome> birchForest;
    private final Holder<Biome> darkForest;
    private final Holder<Biome> swamp;
    private final Holder<Biome> flowerForest;
    
    // Cool biomes (temperature -0.2 to 0.2) - temperate
    private final Holder<Biome> meadow;
    private final Holder<Biome> cherryGrove;
    private final Holder<Biome> oldGrowthBirch;
    
    // Cold biomes (temperature -0.6 to -0.2) - boreal
    private final Holder<Biome> taiga;
    private final Holder<Biome> oldGrowthSpruce;
    private final Holder<Biome> oldGrowthPine;
    
    // Freezing biomes (temperature < -0.6) - polar
    private final Holder<Biome> snowyPlains;
    private final Holder<Biome> snowyTaiga;
    private final Holder<Biome> grove;
    private final Holder<Biome> iceSpikes;
    
    // Beach/Coast biomes
    private final Holder<Biome> beach;
    private final Holder<Biome> snowyBeach;
    private final Holder<Biome> stonyShore;
    
    // Ocean biomes
    private final Holder<Biome> warmOcean;
    private final Holder<Biome> lukewarmOcean;
    private final Holder<Biome> ocean;
    private final Holder<Biome> coldOcean;
    private final Holder<Biome> frozenOcean;
    
    // Deep ocean biomes
    private final Holder<Biome> deepLukewarmOcean;
    private final Holder<Biome> deepOcean;
    private final Holder<Biome> deepColdOcean;
    private final Holder<Biome> deepFrozenOcean;
    
    public SheetworldBiomeSource(HolderGetter<Biome> biomeRegistry, int worldSize) {
        this.biomeRegistry = biomeRegistry;
        this.worldSize = worldSize;
        this.halfSize = worldSize / 2;
        
        Sheetworld.LOGGER.info("Cubeworld BiomeSource created: worldSize={}, halfSize={}", worldSize, halfSize);
        
        // Hot biomes
        this.desert = biomeRegistry.getOrThrow(Biomes.DESERT);
        this.badlands = biomeRegistry.getOrThrow(Biomes.BADLANDS);
        this.savanna = biomeRegistry.getOrThrow(Biomes.SAVANNA);
        this.jungle = biomeRegistry.getOrThrow(Biomes.JUNGLE);
        this.sparseJungle = biomeRegistry.getOrThrow(Biomes.SPARSE_JUNGLE);
        this.mangroveSwamp = biomeRegistry.getOrThrow(Biomes.MANGROVE_SWAMP);
        
        // Warm biomes
        this.plains = biomeRegistry.getOrThrow(Biomes.PLAINS);
        this.forest = biomeRegistry.getOrThrow(Biomes.FOREST);
        this.birchForest = biomeRegistry.getOrThrow(Biomes.BIRCH_FOREST);
        this.darkForest = biomeRegistry.getOrThrow(Biomes.DARK_FOREST);
        this.swamp = biomeRegistry.getOrThrow(Biomes.SWAMP);
        this.flowerForest = biomeRegistry.getOrThrow(Biomes.FLOWER_FOREST);
        
        // Cool biomes
        this.meadow = biomeRegistry.getOrThrow(Biomes.MEADOW);
        this.cherryGrove = biomeRegistry.getOrThrow(Biomes.CHERRY_GROVE);
        this.oldGrowthBirch = biomeRegistry.getOrThrow(Biomes.OLD_GROWTH_BIRCH_FOREST);
        
        // Cold biomes
        this.taiga = biomeRegistry.getOrThrow(Biomes.TAIGA);
        this.oldGrowthSpruce = biomeRegistry.getOrThrow(Biomes.OLD_GROWTH_SPRUCE_TAIGA);
        this.oldGrowthPine = biomeRegistry.getOrThrow(Biomes.OLD_GROWTH_PINE_TAIGA);
        
        // Freezing biomes
        this.snowyPlains = biomeRegistry.getOrThrow(Biomes.SNOWY_PLAINS);
        this.snowyTaiga = biomeRegistry.getOrThrow(Biomes.SNOWY_TAIGA);
        this.grove = biomeRegistry.getOrThrow(Biomes.GROVE);
        this.iceSpikes = biomeRegistry.getOrThrow(Biomes.ICE_SPIKES);
        
        // Beach/Coast biomes
        this.beach = biomeRegistry.getOrThrow(Biomes.BEACH);
        this.snowyBeach = biomeRegistry.getOrThrow(Biomes.SNOWY_BEACH);
        this.stonyShore = biomeRegistry.getOrThrow(Biomes.STONY_SHORE);
        
        // Ocean biomes
        this.warmOcean = biomeRegistry.getOrThrow(Biomes.WARM_OCEAN);
        this.lukewarmOcean = biomeRegistry.getOrThrow(Biomes.LUKEWARM_OCEAN);
        this.ocean = biomeRegistry.getOrThrow(Biomes.OCEAN);
        this.coldOcean = biomeRegistry.getOrThrow(Biomes.COLD_OCEAN);
        this.frozenOcean = biomeRegistry.getOrThrow(Biomes.FROZEN_OCEAN);
        
        // Deep ocean biomes
        this.deepLukewarmOcean = biomeRegistry.getOrThrow(Biomes.DEEP_LUKEWARM_OCEAN);
        this.deepOcean = biomeRegistry.getOrThrow(Biomes.DEEP_OCEAN);
        this.deepColdOcean = biomeRegistry.getOrThrow(Biomes.DEEP_COLD_OCEAN);
        this.deepFrozenOcean = biomeRegistry.getOrThrow(Biomes.DEEP_FROZEN_OCEAN);
    }
    
    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }
    
    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(
            // Hot
            desert, badlands, savanna, jungle, sparseJungle, mangroveSwamp,
            // Warm
            plains, forest, birchForest, darkForest, swamp, flowerForest,
            // Cool
            meadow, cherryGrove, oldGrowthBirch,
            // Cold
            taiga, oldGrowthSpruce, oldGrowthPine,
            // Freezing
            snowyPlains, snowyTaiga, grove, iceSpikes,
            // Beach/Coast
            beach, snowyBeach, stonyShore,
            // Ocean
            warmOcean, lukewarmOcean, ocean, coldOcean, frozenOcean,
            // Deep Ocean
            deepLukewarmOcean, deepOcean, deepColdOcean, deepFrozenOcean
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
            return getOceanForTemperature(getTemperature(z));
        }
        
        // Sample continentalness from the density function system
        Climate.TargetPoint target = sampler.sample(quartX, quartY, quartZ);
        float continentalness = Climate.unquantizeCoord(target.continentalness());
        
        // Calculate all climate values
        double temperature = getTemperature(z);
        double humidity = getHumidity(x, z, continentalness);
        double precipitation = getPrecipitation(x, z);
        
        // Ocean vs Land decision based on continentalness
        if (continentalness < DEEP_OCEAN_THRESHOLD) {
            return getDeepOceanForTemperature(temperature);
        }
        if (continentalness < OCEAN_THRESHOLD) {
            return getOceanForTemperature(temperature);
        }
        if (continentalness < COAST_THRESHOLD) {
            return getCoastBiome(temperature, precipitation);
        }
        
        // Land biome - use Whittaker diagram with all three climate axes
        return getBiomeForClimate(temperature, humidity, precipitation);
    }
    
    // ==================== TEMPERATURE ====================
    
    /**
     * Calculate temperature based on LATITUDE (Z coordinate).
     * 
     * Z = 0 is the equator (hottest)
     * Z = ±halfSize is the polar edge (coldest)
     * 
     * Uses T = 1 - 2 * (|z|/halfSize)^1.3 for gradual tropical zone
     * 
     * @param z the Z coordinate (north-south position)
     * @return temperature from +1 (tropical) to -1 (polar)
     */
    private double getTemperature(int z) {
        double latitude = Math.abs(z) / (double) halfSize;
        latitude = Math.min(latitude, 1.0);
        
        // Non-linear falloff: keeps equatorial zone warmer longer
        return 1.0 - 2.0 * Math.pow(latitude, 1.3);
    }
    
    // ==================== HUMIDITY (Air Moisture Content) ====================
    
    /**
     * Calculate humidity - the moisture content of the air.
     * 
     * This is NOT the same as precipitation! Humidity affects:
     * - Vegetation lushness and density
     * - The "feel" of a biome (muggy vs crisp)
     * - Fog and mist potential
     * 
     * Factors:
     * 1. Temperature: Warm air holds more moisture (Clausius-Clapeyron)
     * 2. Coastal proximity: Ocean = moisture source
     * 3. Local variation: Small-scale differences
     * 
     * @return humidity from 0.0 (bone dry air) to 1.0 (saturated)
     */
    private double getHumidity(int x, int z, float continentalness) {
        double temp = getTemperature(z);
        
        // 1. Temperature-based moisture capacity
        //    Warm air can hold more water vapor
        //    At equator (temp=1): base ~0.7
        //    At poles (temp=-1): base ~0.3
        double baseHumidity = 0.5 + (temp * 0.2);
        
        // 2. Coastal/Ocean proximity boost
        //    Near ocean = constant moisture source = humid air
        //    Deep inland = air has lost moisture = drier
        double coastalEffect = 0.0;
        if (continentalness < COAST_THRESHOLD) {
            // Very close to ocean - humid
            coastalEffect = 0.2;
        } else if (continentalness < INLAND_THRESHOLD) {
            // Gradual decrease from coast to inland
            double t = (continentalness - COAST_THRESHOLD) / (INLAND_THRESHOLD - COAST_THRESHOLD);
            coastalEffect = 0.2 * (1.0 - t);
        } else {
            // Deep inland - continental/dry air
            double inlandFactor = Math.min((continentalness - INLAND_THRESHOLD) * 2, 1.0);
            coastalEffect = -0.1 * inlandFactor;
        }
        
        // 3. Local variation for organic patterns
        double localVariation = getLocalVariation(x, z, 0.0023, 0.0019) * 0.1;
        
        return clamp(baseHumidity + coastalEffect + localVariation, 0.0, 1.0);
    }
    
    // ==================== PRECIPITATION (Rainfall) ====================
    
    /**
     * Calculate precipitation - how much water falls from the sky.
     * 
     * This determines the biome type on the desert↔rainforest axis.
     * 
     * Factors:
     * 1. Wind patterns: The checkerboard effect from circulation cells
     *    - Upwind edges receive moisture from neighboring faces
     *    - Downwind edges lose moisture
     * 2. Convergence zones: Where air masses meet = uplift = rain
     *    - ITCZ at equator (trade winds converge)
     *    - Polar fronts at ~60% latitude
     * 3. Base moisture: Can't rain without humidity
     * 4. Polar dampening: Cold air = less total precipitation
     * 
     * @return precipitation from 0.0 (no rain) to 1.0 (constant rain)
     */
    private double getPrecipitation(int x, int z) {
        double latitude = Math.abs(z) / (double) halfSize;
        latitude = Math.min(latitude, 1.0);
        
        double longitude = x / (double) halfSize;
        longitude = clamp(longitude, -1.0, 1.0);
        
        // 1. Wind-driven patterns (the planetary lungs checkerboard)
        double windEffect = getWindPrecipitationEffect(latitude, longitude);
        
        // 2. Convergence zones bonus
        double convergenceBonus = getConvergenceBonus(latitude);
        
        // 3. Base precipitation tied to temperature (moisture capacity)
        double temp = getTemperature((int)(Math.signum(z) * latitude * halfSize));
        double moistureBase = 0.3 + (temp + 1.0) * 0.15;  // 0.3 at poles, 0.6 at equator
        
        // 4. Local variation for organic patterns
        double localVariation = getLocalVariation(x, z, 0.0041, 0.0037) * 0.12;
        
        // Combine all factors
        double precipitation = moistureBase + (windEffect * 0.3) + (convergenceBonus * 0.25) + localVariation;
        
        // 5. Polar dampening - cold regions get less overall precipitation
        //    (what does fall is often snow, but total water is less)
        if (latitude > FERREL_END) {
            double polarFactor = (latitude - FERREL_END) / (1.0 - FERREL_END);
            precipitation = precipitation * (1.0 - polarFactor * 0.35);
        }
        
        return clamp(precipitation, 0.0, 1.0);
    }
    
    /**
     * Calculate wind-driven precipitation effect based on circulation cells.
     * 
     * The "planetary lungs" concept:
     * - Air flows between cube faces carrying moisture
     * - Upwind edge (where air ARRIVES) = wet
     * - Downwind edge (where air DEPARTS) = dry
     * 
     * WIND ZONES (symmetric about equator):
     * - Hadley/Trade (0-30%): Wind FROM east → East wet, West dry
     * - Ferrel/Westerlies (30-60%): Wind FROM west → West wet, East dry
     * - Polar (60-100%): Wind FROM east → East wet, West dry
     * 
     * This creates the CHECKERBOARD pattern!
     */
    private double getWindPrecipitationEffect(double latitude, double longitude) {
        double windEffect;
        
        if (latitude < HADLEY_END) {
            // TRADE WINDS - blow FROM east (toward west)
            // Moisture arrives from eastern neighbor face
            // East (+longitude) = wet, West (-longitude) = dry
            windEffect = longitude;
            
        } else if (latitude < FERREL_END) {
            // WESTERLIES - blow FROM west (toward east)
            // Moisture arrives from western neighbor face
            // West (-longitude) = wet, East (+longitude) = dry
            windEffect = -longitude;
            
        } else {
            // POLAR EASTERLIES - blow FROM east (toward west)
            // Same pattern as trade winds
            windEffect = longitude;
        }
        
        // Smooth transitions at cell boundaries
        windEffect = smoothCellTransitions(windEffect, latitude, longitude);
        
        return windEffect;
    }
    
    /**
     * Calculate bonus precipitation at convergence zones.
     * 
     * Where air masses meet, air is forced upward, cools, and releases moisture.
     * 
     * Two main convergence zones:
     * 1. ITCZ (Intertropical Convergence Zone) at equator
     *    - Trade winds from N and S hemispheres meet
     *    - Creates the rainforest belt
     * 2. Polar Front at ~60% latitude
     *    - Warm Ferrel air meets cold Polar air
     *    - Creates stormy, wet conditions
     */
    private double getConvergenceBonus(double latitude) {
        // ITCZ - sharp peak at equator (latitude = 0)
        // Gaussian-like falloff, width ~10% of world
        double itczBonus = Math.exp(-latitude * latitude * 80) * 0.35;
        
        // Polar Front at Ferrel/Polar boundary (~60%)
        // Broader zone of frontal precipitation
        double polarFrontDist = Math.abs(latitude - FERREL_END);
        double polarFrontBonus = Math.exp(-polarFrontDist * polarFrontDist * 150) * 0.2;
        
        // Secondary convergence at Hadley/Ferrel boundary (~30%)
        // Subtropical high pressure - actually causes LESS rain (subsiding air)
        // But the edges of this zone can have rain
        double subtropicalDist = Math.abs(latitude - HADLEY_END);
        double subtropicalEffect = Math.exp(-subtropicalDist * subtropicalDist * 200) * -0.1;
        
        return itczBonus + polarFrontBonus + subtropicalEffect;
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
    
    // ==================== BIOME SELECTION ====================
    
    /**
     * Whittaker diagram-style biome selection using all three climate axes.
     * 
     * @param temp Temperature: -1 (polar) to +1 (tropical)
     * @param humidity Air moisture: 0 (dry air) to 1 (humid air)
     * @param precip Precipitation: 0 (no rain) to 1 (constant rain)
     */
    private Holder<Biome> getBiomeForClimate(double temp, double humidity, double precip) {
        
        // === TROPICAL (temp > 0.5) ===
        if (temp > 0.5) {
            if (precip > 0.7) {
                // Extreme rainfall
                return humidity > 0.6 ? mangroveSwamp : jungle;
            }
            if (precip > 0.5) {
                // High rainfall - rainforest
                return jungle;
            }
            if (precip > 0.35) {
                // Moderate-high rainfall - seasonal forest
                return sparseJungle;
            }
            if (precip > 0.2) {
                // Low-moderate rainfall - savanna
                return savanna;
            }
            // Low rainfall - desert
            // Humidity affects desert type: humid air = sandy, dry air = badlands
            return humidity > 0.35 ? desert : badlands;
        }
        
        // === SUBTROPICAL (temp 0.2 to 0.5) ===
        if (temp > 0.2) {
            if (precip > 0.65) {
                // Very wet - swamp
                return swamp;
            }
            if (precip > 0.5) {
                // Wet - dense forest
                return humidity > 0.5 ? darkForest : forest;
            }
            if (precip > 0.35) {
                // Moderate - forest
                return humidity > 0.55 ? forest : flowerForest;
            }
            if (precip > 0.2) {
                // Low-moderate - grassland/light forest
                return humidity > 0.4 ? flowerForest : plains;
            }
            // Low rainfall - dry grassland to savanna
            return humidity > 0.3 ? plains : savanna;
        }
        
        // === TEMPERATE (temp -0.2 to 0.2) ===
        if (temp > -0.2) {
            if (precip > 0.6) {
                // Wet temperate - lush forest
                return oldGrowthBirch;
            }
            if (precip > 0.45) {
                // Moderate-wet - deciduous forest
                return humidity > 0.5 ? birchForest : cherryGrove;
            }
            if (precip > 0.3) {
                // Moderate - mixed
                return humidity > 0.45 ? cherryGrove : meadow;
            }
            // Low precipitation - grassland
            return humidity > 0.35 ? meadow : plains;
        }
        
        // === BOREAL (temp -0.6 to -0.2) ===
        if (temp > -0.6) {
            if (precip > 0.5) {
                // Wet boreal - dense taiga
                return humidity > 0.5 ? oldGrowthSpruce : oldGrowthPine;
            }
            if (precip > 0.3) {
                // Moderate - taiga
                return humidity > 0.4 ? oldGrowthPine : taiga;
            }
            // Low precipitation - sparse taiga to snowy plains
            return humidity > 0.3 ? taiga : snowyPlains;
        }
        
        // === POLAR (temp < -0.6) ===
        if (precip > 0.4) {
            // "Wet" polar - snowy forest
            return humidity > 0.4 ? grove : snowyTaiga;
        }
        if (precip > 0.25) {
            // Moderate - snowy taiga
            return snowyTaiga;
        }
        // Low precipitation - tundra/ice
        return humidity > 0.35 ? snowyPlains : iceSpikes;
    }
    
    /**
     * Get coastal/beach biome based on temperature and precipitation.
     */
    private Holder<Biome> getCoastBiome(double temp, double precip) {
        if (temp < -0.4) {
            return snowyBeach;
        }
        if (temp > 0.3 && precip < 0.3) {
            // Hot and dry coast - could be stony
            return stonyShore;
        }
        return beach;
    }
    
    /**
     * Get appropriate ocean biome based on temperature.
     */
    private Holder<Biome> getOceanForTemperature(double temp) {
        if (temp > 0.5) return warmOcean;
        if (temp > 0.2) return lukewarmOcean;
        if (temp > -0.2) return ocean;
        if (temp > -0.6) return coldOcean;
        return frozenOcean;
    }
    
    /**
     * Get appropriate deep ocean biome based on temperature.
     */
    private Holder<Biome> getDeepOceanForTemperature(double temp) {
        if (temp > 0.3) return deepLukewarmOcean;
        if (temp > -0.2) return deepOcean;
        if (temp > -0.6) return deepColdOcean;
        return deepFrozenOcean;
    }
    
    // ==================== UTILITY FUNCTIONS ====================
    
    /**
     * Attempt at smoothstep function for smoother interpolation.
     */
    private double smoothstep(double t) {
        t = clamp(t, 0.0, 1.0);
        return t * t * (3 - 2 * t);
    }
    
    /**
     * Local variation using pseudo-noise for organic patterns.
     */
    private double getLocalVariation(int x, int z, double freqX, double freqZ) {
        double h1 = Math.sin(x * freqX + z * freqZ) * 0.5;
        double h2 = Math.sin(x * freqX * 1.7 - z * freqZ * 1.3 + 1.7) * 0.3;
        double h3 = Math.cos(x * freqX * 0.6 + z * freqZ * 0.8 + 2.3) * 0.2;
        return h1 + h2 + h3;
    }
    
    /**
     * Clamp a value between min and max.
     */
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
    
    public int getWorldSize() {
        return worldSize;
    }
}