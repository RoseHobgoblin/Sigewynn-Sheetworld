package com.sheetworld.climate;

import com.sheetworld.ecoregion.Ecoregion;
import com.sheetworld.ecoregion.EcoregionBiomePool;
import com.sheetworld.ecoregion.EcoregionSelector;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Terrain-based biome selection using the ecoregion system.
 * 
 * This bridges the old terrain parameter approach with the new
 * two-level ecoregion selection system.
 * 
 * HIERARCHY:
 * 1. Ocean/Coast gates (continentalness)
 * 2. Ecoregion selection (temperature, precipitation, elevation)
 * 3. Biome pool selection (local noise, erosion, weirdness)
 */
public class TerrainBiomeSelector {
    
    private final EcoregionBiomePool biomePool;
    private final HolderGetter<Biome> biomeGetter;
    
    public TerrainBiomeSelector(HolderGetter<Biome> biomeGetter) {
        this.biomeGetter = biomeGetter;
        this.biomePool = new EcoregionBiomePool(biomeGetter);
    }
    
    /**
     * Main selection method.
     * 
     * @param continentalness Continentalness value for ocean/land gating
     * @param erosion Erosion value for biome weight adjustment
     * @param weirdness Weirdness value for variant selection
     * @param temperature Temperature (-1 to 1)
     * @param precipitation Precipitation (0 to 1)
     * @param elevation Elevation (0 to 1, from mountain ridges)
     */
    public Holder<Biome> selectBiome(
            double continentalness, double erosion, double weirdness,
            double temperature, double precipitation, double elevation) {
        
        // === OCEAN GATES ===
        if (continentalness < -0.45) {
            return selectDeepOceanBiome(temperature);
        }
        if (continentalness < -0.10) {
            return selectOceanBiome(temperature);
        }
        if (continentalness < -0.02) {
            return selectCoastBiome(temperature);
        }
        
        // === LAND - USE ECOREGION SYSTEM ===
        
        // Select ecoregion based on climate
        Ecoregion ecoregion = EcoregionSelector.select(temperature, precipitation, elevation);
        
        // Select biome from ecoregion pool
        return biomePool.selectBiome(ecoregion, weirdness, erosion, weirdness);
    }
    
    // === OCEAN BIOMES ===
    
    private Holder<Biome> selectDeepOceanBiome(double temperature) {
        if (temperature > 0.5) {
            return biomeGetter.getOrThrow(Biomes.DEEP_LUKEWARM_OCEAN);
        } else if (temperature > -0.2) {
            return biomeGetter.getOrThrow(Biomes.DEEP_OCEAN);
        } else if (temperature > -0.5) {
            return biomeGetter.getOrThrow(Biomes.DEEP_COLD_OCEAN);
        } else {
            return biomeGetter.getOrThrow(Biomes.DEEP_FROZEN_OCEAN);
        }
    }
    
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
}
