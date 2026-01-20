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
 * =============================================================================
 * BIOME SELECTION: TWO-PHASE SYSTEM
 * =============================================================================
 * 
 * PHASE 1: TERRAIN LOOKUP TABLE (hard gates)
 *   Vanilla parameters (continentalness, erosion, PV) determine terrain CATEGORY:
 *   
 *   CONTINENTALNESS → Ocean vs Land
 *     < -1.05  → Mushroom islands (rare, isolated)
 *     < -0.455 → Deep ocean
 *     < -0.19  → Ocean
 *     < -0.11  → Coast (beaches, cliffs, mangroves)
 *     ≥ -0.11  → Land
 *   
 *   PV (Peaks & Valleys) + EROSION → Terrain shape
 *     VALLEYS → Rivers
 *     PEAKS + low erosion → Mountain peaks
 *     HIGH + low erosion → Mountain slopes
 *     MID/HIGH + erosion 5 → Windswept/shattered
 *     Everything else → Normal land (climate decides)
 * 
 * PHASE 2: CLIMATE SELECTION (soft scoring)
 *   Within each terrain category, our physics-based climate picks the biome.
 *   This is where our custom system shines!
 * 
 * =============================================================================
 * CLIMATE MODEL (Three-axis system)
 * =============================================================================
 * 
 * 1. TEMPERATURE: Based on LATITUDE (N-S distance from center)
 *    - Center (Z=0) = equator = hottest (+1)
 *    - N/S edges = facing polar faces = coldest (-1)
 *    - Formula: T = 1 - 2 * (|z|/halfSize)^1.3
 *    - The exponent 1.3 keeps the tropical zone wider, matching Earth
 * 
 * 2. HUMIDITY: Air moisture content
 *    - Base level from temperature (warm air holds more moisture)
 *    - Coastal boost (ocean proximity = more humid air)
 *    - Inland drying (continental interiors are dry)
 *    - Subtropical drying (high pressure at ~30% latitude)
 *    - Affects vegetation lushness, "feel" of biome
 *    - NOT the same as precipitation!
 * 
 * 3. PRECIPITATION: Rainfall amount
 *    - Wind patterns create the "planetary lungs" checkerboard:
 *      * Hadley cell (0-30%): Trade winds FROM east → East=wet, West=dry
 *      * Ferrel cell (30-60%): Westerlies FROM west → West=wet, East=dry
 *      * Polar cell (60-100%): Easterlies FROM east → East=wet, West=dry
 *    - Convergence zones add rain:
 *      * ITCZ at equator = trade winds converge = RAINFORESTS
 *      * Polar front at ~60% = storms
 *    - Subtropical HIGH at ~30% = air descends = NO RAIN = DESERTS!
 *    - This is the primary driver of the desert↔rainforest axis
 * 
 * =============================================================================
 * MODDED BIOME SUPPORT
 * =============================================================================
 * 
 * The BiomeRegistry allows modded biomes to seamlessly integrate:
 * - Biomes register with their climate preferences (temp/humid/precip ranges)
 * - Higher priority modded biomes win over vanilla when climate matches
 * - Gap-filling biomes (like Atmospheric's Scrubland) slide into niches
 *   that vanilla leaves empty
 * 
 * Example: Plains → Scrubland → Desert transition becomes smooth when
 * Atmospheric is installed, instead of the hard Plains→Desert jump.
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
    
    // The biome selection systems
    private final BiomeRegistry biomeRegistry;
    private final TerrainBiomeSelector terrainSelector;
    
    // === CIRCULATION CELL BOUNDARIES (as fraction of distance to N/S edge) ===
    // These define where wind patterns change, creating the "planetary lungs"
    private static final double HADLEY_END = 0.30;    // Trade winds end / Subtropical high
    private static final double FERREL_END = 0.60;    // Westerlies end / Polar front
    // Beyond FERREL_END is polar easterlies
    
    // === HUMIDITY THRESHOLD ===
    // Continental drying kicks in past this continentalness value
    private static final float INLAND_THRESHOLD = 0.3f;
    
    public SheetworldBiomeSource(HolderGetter<Biome> biomeGetter, int worldSize) {
        this.biomeGetter = biomeGetter;
        this.worldSize = worldSize;
        this.halfSize = worldSize / 2;
        
        // Initialize the biome systems
        this.biomeRegistry = new BiomeRegistry(biomeGetter);
        this.terrainSelector = new TerrainBiomeSelector(biomeRegistry);
        
        Sheetworld.LOGGER.info("Cubeworld BiomeSource created: worldSize={}, halfSize={}", worldSize, halfSize);
    }
    
    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }
    
    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomeRegistry.collectAllPossibleBiomes();
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
            return biomeRegistry.selectOceanBiome(temp);
        }
        
        // Sample vanilla terrain parameters from density functions
        Climate.TargetPoint target = sampler.sample(quartX, quartY, quartZ);
        float continentalness = Climate.unquantizeCoord(target.continentalness());
        float erosion = Climate.unquantizeCoord(target.erosion());
        float weirdness = Climate.unquantizeCoord(target.weirdness());
        
        // Calculate our physics-based climate values
        double temperature = getTemperature(z);
        double humidity = getHumidity(x, z, continentalness);
        double precipitation = getPrecipitation(x, z);
        
        // Use terrain lookup + climate scoring to select biome
        return terrainSelector.selectBiome(
            continentalness, erosion, weirdness,
            temperature, humidity, precipitation
        );
    }
    
    // ==================== TEMPERATURE ====================
    
    /**
     * Calculate temperature based on LATITUDE (Z coordinate).
     * 
     * This models the basic physics of a rotating cube:
     * - The equator (Z=0) receives the most direct sunlight
     * - The poles (Z=±halfSize) receive oblique sunlight
     * 
     * The exponent 1.3 creates a wider tropical zone, similar to Earth
     * where the tropics extend to about 23.5° latitude.
     * 
     * @param z the Z coordinate (north-south position)
     * @return temperature from +1 (tropical equator) to -1 (polar edge)
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
     * NOT the same as precipitation (rainfall)!
     * 
     * A coastal desert can have humid air (fog) but no rain.
     * An inland forest can have dry air but regular rain.
     * 
     * Factors:
     * 1. Temperature: Warm air CAN hold more moisture (Clausius-Clapeyron)
     * 2. Ocean proximity: Coastal areas have humid air (moisture source)
     * 3. Continental interior: Deep inland = dry air (moisture depleted)
     * 4. Subtropical high: Air descending at ~30% latitude is dry
     * 
     * @return humidity from 0.0 (bone dry) to 1.0 (saturated)
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
        if (continentalness < TerrainParameters.COAST_THRESHOLD) {
            // Right at coast - very humid
            coastalEffect = 0.25;
        } else if (continentalness < INLAND_THRESHOLD) {
            // Gradual decrease from coast to inland
            double t = (continentalness - TerrainParameters.COAST_THRESHOLD) / 
                       (INLAND_THRESHOLD - TerrainParameters.COAST_THRESHOLD);
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
     * THE PLANETARY LUNGS:
     * Air flows between cube faces in circulation cells, carrying moisture.
     * Where air ARRIVES (upwind edge) = wet
     * Where air DEPARTS (downwind edge) = dry
     * This creates a checkerboard pattern across the world!
     * 
     * CONVERGENCE ZONES:
     * Where air masses meet, they're forced upward, cool, and release moisture.
     * - ITCZ at equator: Trade winds from N and S converge = RAINFORESTS
     * - Polar front at ~60%: Warm Ferrel air meets cold Polar air = storms
     * 
     * THE DESERT MAKER:
     * At ~30% latitude, air from the Hadley cell DESCENDS.
     * Descending air warms, holds onto its moisture = NO RAIN.
     * This creates the Sahara, Arabian, Sonoran, Australian deserts IRL.
     * 
     * @return precipitation from 0.0 (no rain) to 1.0 (constant rain)
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
        // (Cold air holds less moisture, even when it does precipitate)
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
     * This creates a CHECKERBOARD pattern:
     *   - NE and SW quadrants tend wet (upwind)
     *   - NW and SE quadrants tend dry (downwind)
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
     * 
     * Without smoothing, there would be hard lines at 30% and 60% latitude
     * where precipitation abruptly changes. This creates gradual transitions.
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
    
    /**
     * Attempt at smoothstep function for smoother interpolation.
     * Maps [0,1] to [0,1] with zero derivative at endpoints.
     */
    private double smoothstep(double t) {
        t = clamp(t, 0.0, 1.0);
        return t * t * (3 - 2 * t);
    }
    
    /**
     * Local variation using pseudo-noise for organic patterns.
     * Creates small-scale variation so biome boundaries aren't perfectly straight.
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
        
        return String.format(
            "Lat:%.2f Lon:%.2f | T:%.2f H:%.2f P:%.2f | %s | Cont:%.2f",
            latitude, longitude, temp, humid, precip, windZone, continentalness
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
