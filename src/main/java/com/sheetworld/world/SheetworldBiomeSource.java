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
 * CLIMATE MODEL:
 * - Temperature: Based on LATITUDE (N-S distance from center)
 *   - Center (Z=0) = equator = hottest
 *   - N/S edges = facing polar faces = coldest
 *   - E/W edges = same latitude as center = warm
 * 
 * - Humidity: Based on atmospheric circulation cells (latitude bands)
 *   with moisture exchange at face edges ("planetary lungs")
 *   
 *   Wind zones (symmetric about equator):
 *   - 0-30% latitude: Trade winds / Hadley cell (wind FROM east)
 *   - 30-60% latitude: Westerlies / Ferrel cell (wind FROM west)
 *   - 60-100% latitude: Polar easterlies (wind FROM east)
 *   
 *   This creates a checkerboard wet/dry pattern at edges!
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
    
    // Ocean biomes (for world edge and internal seas)
    private final Holder<Biome> warmOcean;
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
        
        // Ocean biomes
        this.warmOcean = biomeRegistry.getOrThrow(Biomes.WARM_OCEAN);
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
            snowyPlains, snowyTaiga, grove,
            // Ocean
            warmOcean, ocean, coldOcean, frozenOcean,
            // Deep Ocean
            deepLukewarmOcean, deepOcean, deepColdOcean, deepFrozenOcean
        );
    }
    
    // === OCEAN THRESHOLDS ===
    // Continentalness ranges from about -1.2 (deep ocean) to +1.0 (inland)
    // These thresholds determine what counts as ocean vs land
    private static final float DEEP_OCEAN_THRESHOLD = -0.5f;   // Below this = deep ocean
    private static final float OCEAN_THRESHOLD = -0.2f;        // Below this = ocean
    private static final float COAST_THRESHOLD = 0.0f;         // Below this = coast/beach
    
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
        // This is what our continents.json override provides!
        Climate.TargetPoint target = sampler.sample(quartX, quartY, quartZ);
        float continentalness = Climate.unquantizeCoord(target.continentalness());
        
        // Calculate climate values
        double temperature = getTemperature(z);
        double humidity = getHumidity(x, z);
        
        // Ocean vs Land decision based on continentalness
        if (continentalness < DEEP_OCEAN_THRESHOLD) {
            return getDeepOceanForTemperature(temperature);
        }
        if (continentalness < OCEAN_THRESHOLD) {
            return getOceanForTemperature(temperature);
        }
        
        // Land biome - use Whittaker diagram
        return getBiomeForClimate(temperature, humidity);
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
    
    // ==================== HUMIDITY ====================
    
    /**
     * Calculate humidity based on:
     * 1. Atmospheric circulation cells (latitude-based wind zones)
     * 2. Longitude position (E-W) determines upwind/downwind
     * 3. Local variation for organic patterns
     * 
     * The key insight: winds blow E-W, carrying moisture from adjacent faces.
     * At face edges, air either ARRIVES (wet) or DEPARTS (dry).
     * 
     * @return humidity from 0.0 (arid) to 1.0 (wet)
     */
    private double getHumidity(int x, int z) {
        // Latitude: 0 at equator, 1 at N/S edges
        double latitude = Math.abs(z) / (double) halfSize;
        latitude = Math.min(latitude, 1.0);
        
        // Longitude: -1 at west edge, 0 at center, +1 at east edge
        double longitude = x / (double) halfSize;
        longitude = Math.max(-1.0, Math.min(1.0, longitude));
        
        // Get wind-based humidity modifier
        double windHumidity = getWindHumidityEffect(latitude, longitude);
        
        // Local variation for organic patterns
        double localVariation = getLocalHumidityVariation(x, z);
        
        // Combine: wind provides large-scale pattern (±0.35), local adds texture (±0.15)
        double humidity = 0.5 + (windHumidity * 0.35) + (localVariation * 0.15);
        
        // Polar dampening: cold air holds less moisture
        if (latitude > FERREL_END) {
            double polarFactor = (latitude - FERREL_END) / (1.0 - FERREL_END);
            // Compress humidity toward 0.3 in polar regions
            humidity = humidity * (1.0 - polarFactor * 0.5) + 0.3 * polarFactor * 0.5;
        }
        
        return Math.max(0.0, Math.min(1.0, humidity));
    }
    
    /**
     * Calculate wind-based humidity based on latitude bands and E-W position.
     * 
     * WIND ZONES (symmetric about equator):
     * - Hadley/Trade winds (0-30%): Wind FROM east (+X direction)
     *   → East edge receives moisture from eastern neighbor = WET
     *   → West edge loses moisture to western neighbor = DRY
     *   
     * - Ferrel/Westerlies (30-60%): Wind FROM west (-X direction)
     *   → West edge receives moisture from western neighbor = WET
     *   → East edge loses moisture to eastern neighbor = DRY
     *   
     * - Polar easterlies (60-100%): Wind FROM east (+X direction)
     *   → Same as Hadley: east = wet, west = dry
     * 
     * This creates the CHECKERBOARD pattern at edges!
     * 
     * @param latitude 0 (equator) to 1 (polar edge)
     * @param longitude -1 (west) to +1 (east)
     * @return humidity modifier from -1 (dry) to +1 (wet)
     */
    private double getWindHumidityEffect(double latitude, double longitude) {
        double windEffect;
        
        if (latitude < HADLEY_END) {
            // TRADE WINDS - blow FROM east (toward west)
            // Moisture arrives at east edge, leaves at west edge
            // longitude > 0 (east) = wet = positive
            windEffect = longitude;
            
        } else if (latitude < FERREL_END) {
            // WESTERLIES - blow FROM west (toward east)
            // Moisture arrives at west edge, leaves at east edge
            // longitude < 0 (west) = wet = positive when we negate
            windEffect = -longitude;
            
        } else {
            // POLAR EASTERLIES - blow FROM east (toward west)
            // Same as trade winds
            windEffect = longitude;
        }
        
        // Smooth transitions between cells to avoid harsh boundaries
        windEffect = smoothCellTransitions(windEffect, latitude, longitude);
        
        return windEffect;
    }
    
    /**
     * Smooth transitions between circulation cells.
     * At cell boundaries, wind direction reverses - we blend to avoid jarring changes.
     */
    private double smoothCellTransitions(double windEffect, double latitude, double longitude) {
        double transitionWidth = 0.08; // 8% latitude for smooth blending
        
        // Transition at Hadley/Ferrel boundary (30%)
        if (Math.abs(latitude - HADLEY_END) < transitionWidth) {
            double t = (latitude - HADLEY_END + transitionWidth) / (2 * transitionWidth);
            t = smoothstep(t);
            // Blend from Hadley (longitude) to Ferrel (-longitude)
            double hadleyEffect = longitude;
            double ferrelEffect = -longitude;
            windEffect = hadleyEffect * (1 - t) + ferrelEffect * t;
        }
        
        // Transition at Ferrel/Polar boundary (60%)
        if (Math.abs(latitude - FERREL_END) < transitionWidth) {
            double t = (latitude - FERREL_END + transitionWidth) / (2 * transitionWidth);
            t = smoothstep(t);
            // Blend from Ferrel (-longitude) to Polar (longitude)
            double ferrelEffect = -longitude;
            double polarEffect = longitude;
            windEffect = ferrelEffect * (1 - t) + polarEffect * t;
        }
        
        return windEffect;
    }
    
    /**
     * Attempt at smoothstep function for smoother interpolation.
     */
    private double smoothstep(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
    
    /**
     * Local humidity variation using pseudo-noise.
     * Creates organic patterns within the larger wind-driven zones.
     */
    private double getLocalHumidityVariation(int x, int z) {
        // Multiple octaves of sine waves for organic feel
        double h1 = Math.sin(x * 0.0031 + z * 0.0029) * 0.5;
        double h2 = Math.sin(x * 0.0047 - z * 0.0043 + 1.7) * 0.3;
        double h3 = Math.cos(x * 0.0017 + z * 0.0019 + 2.3) * 0.2;
        
        return h1 + h2 + h3;
    }
    
    // ==================== BIOME SELECTION ====================
    
    /**
     * Whittaker diagram-style biome selection.
     * 
     * Temperature: -1 (polar) to +1 (tropical)
     * Humidity: 0 (arid) to 1 (wet)
     */
    private Holder<Biome> getBiomeForClimate(double temp, double humid) {
        
        // === TROPICAL (temp > 0.5) ===
        if (temp > 0.5) {
            if (humid > 0.75) return mangroveSwamp;
            if (humid > 0.55) return jungle;
            if (humid > 0.4) return sparseJungle;
            if (humid > 0.25) return savanna;
            if (humid > 0.15) return desert;
            return badlands;
        }
        
        // === SUBTROPICAL (temp 0.2 to 0.5) ===
        if (temp > 0.2) {
            if (humid > 0.7) return swamp;
            if (humid > 0.55) return darkForest;
            if (humid > 0.4) return forest;
            if (humid > 0.25) return flowerForest;
            if (humid > 0.15) return plains;
            return savanna;
        }
        
        // === TEMPERATE (temp -0.2 to 0.2) ===
        if (temp > -0.2) {
            if (humid > 0.65) return oldGrowthBirch;
            if (humid > 0.5) return birchForest;
            if (humid > 0.35) return cherryGrove;
            if (humid > 0.2) return meadow;
            return plains;
        }
        
        // === BOREAL (temp -0.6 to -0.2) ===
        if (temp > -0.6) {
            if (humid > 0.6) return oldGrowthSpruce;
            if (humid > 0.4) return oldGrowthPine;
            if (humid > 0.2) return taiga;
            return snowyPlains;
        }
        
        // === POLAR (temp < -0.6) ===
        if (humid > 0.5) return grove;
        if (humid > 0.3) return snowyTaiga;
        return snowyPlains;
    }
    
    /**
     * Get appropriate ocean biome based on temperature
     */
    private Holder<Biome> getOceanForTemperature(double temp) {
        if (temp > 0.3) return warmOcean;
        if (temp > -0.2) return ocean;
        if (temp > -0.6) return coldOcean;
        return frozenOcean;
    }
    
    /**
     * Get appropriate deep ocean biome based on temperature
     */
    private Holder<Biome> getDeepOceanForTemperature(double temp) {
        if (temp > 0.3) return deepLukewarmOcean;  // No deep warm ocean in vanilla
        if (temp > -0.2) return deepOcean;
        if (temp > -0.6) return deepColdOcean;
        return deepFrozenOcean;
    }
    
    // ==================== DEBUG INFO ====================
    
    /**
     * Get climate debug info for a position (for F3 screen or other debug).
     * Note: This doesn't include continentalness since we'd need the sampler.
     */
    public String getClimateDebugInfo(int x, int z) {
        double latitude = Math.abs(z) / (double) halfSize;
        double longitude = x / (double) halfSize;
        double temp = getTemperature(z);
        double humid = getHumidity(x, z);
        
        String windZone;
        if (latitude < HADLEY_END) windZone = "Hadley (Trade winds, FROM east)";
        else if (latitude < FERREL_END) windZone = "Ferrel (Westerlies, FROM west)";
        else windZone = "Polar (Easterlies, FROM east)";
        
        return String.format(
            "Lat: %.2f, Lon: %.2f | Temp: %.2f, Humid: %.2f | %s",
            latitude, longitude, temp, humid, windZone
        );
    }
    
    /**
     * Get full climate debug info including continentalness.
     * @param continentalness the continentalness value from Climate.Sampler
     */
    public String getFullClimateDebugInfo(int x, int z, float continentalness) {
        String baseInfo = getClimateDebugInfo(x, z);
        String terrainType;
        if (continentalness < DEEP_OCEAN_THRESHOLD) terrainType = "Deep Ocean";
        else if (continentalness < OCEAN_THRESHOLD) terrainType = "Ocean";
        else if (continentalness < COAST_THRESHOLD) terrainType = "Coast";
        else terrainType = "Land";
        
        return String.format("%s | Cont: %.2f (%s)", baseInfo, continentalness, terrainType);
    }
    
    public int getWorldSize() {
        return worldSize;
    }
}
