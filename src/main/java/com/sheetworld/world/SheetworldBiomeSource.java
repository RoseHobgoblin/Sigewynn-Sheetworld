package com.sheetworld.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sheetworld.Sheetworld;
import com.sheetworld.climate.BiomeRegistry;
import com.sheetworld.climate.ModCompat;
import com.sheetworld.climate.TerrainBiomeSelector;
import com.sheetworld.climate.TerrainParameters;
import com.sheetworld.ecoregion.EcoregionBiomePool;
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

    // === HUMIDITY THRESHOLD ===
    // Continental drying kicks in past this continentalness value
    private static final float INLAND_THRESHOLD = 0.3f;

    // === RAIN SHADOW SETTINGS ===
    // Distance upwind to check for blocking mountains (in blocks)
    private static final int RAIN_SHADOW_DISTANCE = 400;
    // How many sample points upwind to check
    private static final int RAIN_SHADOW_SAMPLES = 3;
    // PV threshold for "peak" detection (high weirdness = peaks)
    private static final float PEAK_PV_THRESHOLD = 0.4f;
    // Erosion threshold - low erosion = rugged/mountainous
    private static final float PEAK_EROSION_THRESHOLD = 0.2f;
    // Maximum precipitation reduction from rain shadow
    private static final double MAX_RAIN_SHADOW_REDUCTION = 0.5;

    public SheetworldBiomeSource(HolderGetter<Biome> biomeGetter, int worldSize) {
        this.biomeGetter = biomeGetter;
        this.worldSize = worldSize;
        this.halfSize = worldSize / 2;

        // Create the registry and biome pool
        BiomeRegistry registry = new BiomeRegistry(biomeGetter);
        EcoregionBiomePool biomePool = new EcoregionBiomePool(biomeGetter);

        // Register modded biomes if available
        if (ModCompat.hasAtmospheric()) {
            biomePool.registerAtmosphericBiomes();
        }
        if (ModCompat.hasEnvironmental()) {
            biomePool.registerEnvironmentalBiomes();
        }
        if (ModCompat.hasAutumnity()) {
            biomePool.registerAutumnityBiomes();
        }

        this.terrainSelector = new TerrainBiomeSelector(registry, biomePool);

        Sheetworld.LOGGER.info("SheetworldBiomeSource created with TerrainBiomeSelector + EcoregionBiomePool: worldSize={}", worldSize);
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

        // Calculate climate (latitude-based temp, continentalness-based humidity, circulation-based precip)
        double temperature = getTemperature(z);
        double humidity = getHumidity(x, z, continentalness);
        double precipitation = getPrecipitation(x, z, sampler, quartY);

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
     * Humidity - the moisture content of the air.
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

    /**
     * Base precipitation based on atmospheric circulation + rain shadow effects.
     * Range: 0.0 (desert) to 1.0 (rainforest)
     *
     * Rain shadows occur when moist air hits mountains and is forced upward,
     * dumping precipitation on the windward side. The leeward side receives
     * significantly less rainfall.
     */
    private double getPrecipitation(int x, int z, Climate.Sampler sampler, int quartY) {
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

        // Rain shadow effect - check for blocking peaks upwind
        if (sampler != null) {
            double rainShadowReduction = calculateRainShadow(x, z, latitude, sampler, quartY);
            precipitation *= (1.0 - rainShadowReduction);
        }

        return clamp(precipitation, 0.0, 1.0);
    }

    /**
     * Calculate rain shadow effect by sampling terrain upwind.
     *
     * Wind direction is determined by atmospheric circulation:
     * - 0-30% latitude: Trade winds blow FROM the east (sample east)
     * - 30-60% latitude: Westerlies blow FROM the west (sample west)
     * - 60-100% latitude: Polar easterlies FROM the east (sample east)
     *
     * @return reduction factor from 0.0 (no shadow) to MAX_RAIN_SHADOW_REDUCTION
     */
    private double calculateRainShadow(int x, int z, double latitude, Climate.Sampler sampler, int quartY) {
        // Determine wind direction based on latitude
        // windDirX: positive = wind comes from east, negative = wind comes from west
        int windDirX;
        if (latitude < HADLEY_END) {
            windDirX = 1;  // Trade winds - sample to the east (upwind)
        } else if (latitude < FERREL_END) {
            windDirX = -1; // Westerlies - sample to the west (upwind)
        } else {
            windDirX = 1;  // Polar easterlies - sample to the east (upwind)
        }

        // Sample upwind at multiple distances to find blocking peaks
        double maxBlockingFactor = 0.0;
        int stepDistance = RAIN_SHADOW_DISTANCE / RAIN_SHADOW_SAMPLES;

        for (int i = 1; i <= RAIN_SHADOW_SAMPLES; i++) {
            int sampleX = x + (windDirX * stepDistance * i);
            int sampleZ = z;

            // Convert to quart coordinates for sampler
            int sampleQuartX = sampleX / 4;
            int sampleQuartZ = sampleZ / 4;

            // Sample terrain at upwind position
            Climate.TargetPoint upwindTarget = sampler.sample(sampleQuartX, quartY, sampleQuartZ);
            float upwindWeirdness = Climate.unquantizeCoord(upwindTarget.weirdness());
            float upwindErosion = Climate.unquantizeCoord(upwindTarget.erosion());

            // Calculate PV from weirdness
            float upwindPV = TerrainParameters.calculatePV(upwindWeirdness);

            // Check if this is a peak (high PV + low erosion = rugged mountain)
            if (upwindPV > PEAK_PV_THRESHOLD && upwindErosion < PEAK_EROSION_THRESHOLD) {
                // This is a blocking peak!
                // Closer peaks have more effect, and higher/more rugged peaks block more
                double distanceFactor = 1.0 - ((double)(i - 1) / RAIN_SHADOW_SAMPLES);
                double peakStrength = (upwindPV - PEAK_PV_THRESHOLD) / (1.0f - PEAK_PV_THRESHOLD);
                double erosionFactor = 1.0 - (upwindErosion / PEAK_EROSION_THRESHOLD);

                double blockingFactor = distanceFactor * peakStrength * erosionFactor;
                maxBlockingFactor = Math.max(maxBlockingFactor, blockingFactor);
            }
        }

        return maxBlockingFactor * MAX_RAIN_SHADOW_REDUCTION;
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

    private double getLocalVariation(int x, int z, double freqX, double freqZ) {
        double h1 = Math.sin(x * freqX + z * freqZ) * 0.5;
        double h2 = Math.sin(x * freqX * 1.7 - z * freqZ * 1.3 + 1.7) * 0.3;
        double h3 = Math.cos(x * freqX * 0.6 + z * freqZ * 0.8 + 2.3) * 0.2;
        return h1 + h2 + h3;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // ==================== DEBUG ====================

    public String getTerrainDebugInfo(int x, int z, float continentalness, float erosion, float weirdness) {
        double temperature = getTemperature(z);
        double humidity = getHumidity(x, z, continentalness);
        double precipitation = getPrecipitation(x, z, null, 0);  // No rain shadow in debug (no sampler)

        return TerrainParameters.getDebugString(continentalness, erosion, weirdness) +
            String.format(" | Climate: T=%.2f H=%.2f P=%.2f", temperature, humidity, precipitation);
    }

    public int getWorldSize() {
        return worldSize;
    }
}
