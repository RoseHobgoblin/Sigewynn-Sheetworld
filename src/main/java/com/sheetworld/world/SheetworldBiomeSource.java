package com.sheetworld.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sheetworld.Sheetworld;
import com.sheetworld.ecoregion.Ecoregion;
import com.sheetworld.ecoregion.EcoregionBiomePool;
import com.sheetworld.ecoregion.EcoregionSelector;
import com.sheetworld.terrain.MountainRidgeSampler;
import com.sheetworld.terrain.TerrainConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.stream.Stream;

/**
 * Sheetworld Biome Source - Two-Level Selection System
 * 
 * LEVEL 1: Climate → Ecoregion (deterministic)
 *   - Temperature from latitude (Z coordinate)
 *   - Precipitation from atmospheric circulation + rain shadows
 *   - Elevation from mountain ridge sampling
 *   → Selects one of 18 WWF-inspired ecoregions
 * 
 * LEVEL 2: Ecoregion → Specific Biome (weighted)
 *   - Each ecoregion has a pool of candidate biomes
 *   - Selection uses local noise, erosion, weirdness
 *   → Selects specific vanilla or modded biome
 * 
 * This enables:
 *   - Climatically appropriate terrain features (jungle pillars in rainforests)
 *   - Proper montane ecoregions based on elevation
 *   - Rain shadow deserts on lee side of mountains
 *   - Modded biome integration without parameter tuning
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
    
    // Ecoregion biome pools - the two-level selection system
    private final EcoregionBiomePool biomePool;
    
    // Mountain ridge sampler for elevation and rain shadow
    private MountainRidgeSampler mountainSampler;
    
    // === ATMOSPHERIC CIRCULATION CELL BOUNDARIES ===
    private static final double HADLEY_END = 0.30;    // Trade winds end / Subtropical high
    private static final double FERREL_END = 0.60;    // Westerlies end / Polar front
    
    // === OCEAN THRESHOLDS ===
    private static final float DEEP_OCEAN_THRESHOLD = -0.45f;
    private static final float OCEAN_THRESHOLD = -0.10f;
    private static final float COAST_THRESHOLD = -0.02f;
    private static final float INLAND_THRESHOLD = 0.4f;
    
    public SheetworldBiomeSource(HolderGetter<Biome> biomeGetter, int worldSize) {
        this.biomeGetter = biomeGetter;
        this.worldSize = worldSize;
        this.halfSize = worldSize / 2;
        
        // Initialize the ecoregion biome pools
        this.biomePool = new EcoregionBiomePool(biomeGetter);
        
        // Mountain sampler will be set later when density functions are available
        this.mountainSampler = null;
        
        Sheetworld.LOGGER.info("SheetworldBiomeSource created with ecoregion system: worldSize={}", worldSize);
    }
    
    /**
     * Set the mountain ridge sampler.
     * Called during world creation when density functions are available.
     */
    public void setMountainSampler(DensityFunction ridgeFunction) {
        this.mountainSampler = new MountainRidgeSampler(ridgeFunction);
        Sheetworld.LOGGER.info("Mountain ridge sampler initialized for rain shadow calculations");
    }
    
    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }
    
    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        // Return all biomes that might be used across all ecoregions
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
            biomeGetter.getOrThrow(Biomes.STONY_SHORE)
        );
    }
    
    // ==================== MAIN BIOME SELECTION ====================
    
    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        // Convert from quart coordinates (4 blocks) to block coordinates
        int x = quartX * 4;
        int z = quartZ * 4;
        
        // Check world bounds - return ocean if outside
        if (Math.abs(x) > halfSize || Math.abs(z) > halfSize) {
            double temp = getTemperature(z);
            return selectOceanBiome(temp);
        }
        
        // Sample continentalness from noise system
        Climate.TargetPoint target = sampler.sample(quartX, quartY, quartZ);
        float continentalness = Climate.unquantizeCoord(target.continentalness());
        float erosion = Climate.unquantizeCoord(target.erosion());
        float weirdness = Climate.unquantizeCoord(target.weirdness());
        
        // === OCEAN / COAST CHECK ===
        if (continentalness < DEEP_OCEAN_THRESHOLD) {
            return selectOceanBiome(getTemperature(z));
        }
        if (continentalness < OCEAN_THRESHOLD) {
            return selectOceanBiome(getTemperature(z));
        }
        if (continentalness < COAST_THRESHOLD) {
            return selectCoastBiome(getTemperature(z));
        }
        
        // === LAND BIOME - USE ECOREGION SYSTEM ===
        
        // Calculate climate values
        double temperature = getTemperature(z);
        double basePrecipitation = getPrecipitation(x, z);
        
        // Get elevation from mountain ridge sampler
        double elevation = 0.0;
        if (mountainSampler != null) {
            elevation = mountainSampler.getElevation(x, z);
            
            // Apply rain shadow effect to precipitation
            double latitude = Math.abs(z) / (double) halfSize;
            MountainRidgeSampler.WindDirection windDir = MountainRidgeSampler.getWindDirection(latitude);
            double rainShadowMultiplier = mountainSampler.calculateRainShadow(x, z, windDir);
            basePrecipitation *= rainShadowMultiplier;
        }
        
        double precipitation = clamp(basePrecipitation, 0.0, 1.0);
        
        // LEVEL 1: Select ecoregion based on climate
        Ecoregion ecoregion = EcoregionSelector.select(temperature, precipitation, elevation);
        
        // LEVEL 2: Select specific biome from ecoregion pool
        // Use deterministic local noise based on position
        double localNoise = getLocalNoise(x, z);
        
        return biomePool.selectBiome(ecoregion, localNoise, erosion, weirdness);
    }
    
    // ==================== OCEAN / COAST SELECTION ====================
    
    private Holder<Biome> selectOceanBiome(double temperature) {
        if (temperature > 0.5) {
            return biomeGetter.getOrThrow(Biomes.WARM_OCEAN);
        } else if (temperature > 0.2) {
            return biomeGetter.getOrThrow(Biomes.LUKEWARM_OCEAN);
        } else if (temperature > -0.2) {
            return biomeGetter.getOrThrow(Biomes.OCEAN);
        } else if (temperature > -0.5) {
            return biomeGetter.getOrThrow(Biomes.COLD_OCEAN);
        } else {
            return biomeGetter.getOrThrow(Biomes.FROZEN_OCEAN);
        }
    }
    
    private Holder<Biome> selectCoastBiome(double temperature) {
        if (temperature < -0.3) {
            return biomeGetter.getOrThrow(Biomes.SNOWY_BEACH);
        } else {
            return biomeGetter.getOrThrow(Biomes.BEACH);
        }
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
        // Deterministic pseudo-random noise based on position
        double h1 = Math.sin(x * 0.0041 + z * 0.0037) * 0.5;
        double h2 = Math.sin(x * 0.0069 - z * 0.0048 + 1.7) * 0.3;
        double h3 = Math.cos(x * 0.0025 + z * 0.0032 + 2.3) * 0.2;
        return h1 + h2 + h3;
    }
    
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    // ==================== DEBUG ====================
    
    public String getEcoregionDebugInfo(int x, int z, float continentalness) {
        double temperature = getTemperature(z);
        double precipitation = getPrecipitation(x, z);
        double elevation = mountainSampler != null ? mountainSampler.getElevation(x, z) : 0.0;
        
        if (mountainSampler != null) {
            double latitude = Math.abs(z) / (double) halfSize;
            MountainRidgeSampler.WindDirection windDir = MountainRidgeSampler.getWindDirection(latitude);
            double rainShadow = mountainSampler.calculateRainShadow(x, z, windDir);
            precipitation *= rainShadow;
        }
        
        Ecoregion eco = EcoregionSelector.select(temperature, clamp(precipitation, 0, 1), elevation);
        
        return String.format(
            "Pos: %d, %d | T:%.2f P:%.2f E:%.2f | Ecoregion: %s | Terrain: %s",
            x, z, temperature, precipitation, elevation,
            eco.getDisplayName(),
            eco.getTerrainFeatures()
        );
    }
    
    public int getWorldSize() {
        return worldSize;
    }
}
