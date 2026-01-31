package com.sheetworld.climate;

import com.sheetworld.climate.TerrainParameters.ContinentalnessLevel;
import com.sheetworld.climate.TerrainParameters.PVLevel;
import com.sheetworld.ecoregion.Ecoregion;
import com.sheetworld.ecoregion.EcoregionBiomePool;
import com.sheetworld.ecoregion.EcoregionSelector;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * Terrain-based biome selection using vanilla's lookup table approach.
 *
 * This implements the hard gate system:
 * 1. Continentalness determines ocean/coast/land
 * 2. PV (peaks/valleys) determines terrain shape
 * 3. Erosion determines mountain vs flat within each category
 * 4. Climate (temp/humid/precip) picks the specific biome via ecoregion system
 *
 * TERRAIN GATES:
 * - Ocean/DeepOcean: Temperature-based ocean selection
 * - Coast: Beaches/cliffs based on erosion
 * - River: Valleys → river biomes
 * - Peak: ALPINE_TUNDRA ecoregion
 * - Slope: MONTANE_FOREST ecoregion
 * - Shattered: EcoregionSelector picks ecoregion
 * - Land: EcoregionSelector picks ecoregion based on TPH
 */
public class TerrainBiomeSelector {

    private final BiomeRegistry registry;
    private final EcoregionBiomePool biomePool;

    public TerrainBiomeSelector(BiomeRegistry registry, EcoregionBiomePool biomePool) {
        this.registry = registry;
        this.biomePool = biomePool;
    }
    
    /**
     * Main selection method - the lookup table.
     */
    public Holder<Biome> selectBiome(
            float continentalness, float erosion, float weirdness,
            double temp, double humid, double precip) {
        
        float pv = TerrainParameters.calculatePV(weirdness);
        ContinentalnessLevel contLevel = TerrainParameters.getContinentalnessLevel(continentalness);
        PVLevel pvLevel = TerrainParameters.getPVLevel(pv);
        int erosionLevel = TerrainParameters.getErosionLevel(erosion);
        
        // === OCEAN GATES (continentalness only) ===
        
        if (contLevel == ContinentalnessLevel.MUSHROOM) {
            return registry.selectMushroomBiome(temp);
        }
        
        if (contLevel == ContinentalnessLevel.DEEP_OCEAN) {
            return registry.selectDeepOceanBiome(temp);
        }
        
        if (contLevel == ContinentalnessLevel.OCEAN) {
            return registry.selectOceanBiome(temp);
        }
        
        // === COAST ===
        
        if (contLevel == ContinentalnessLevel.COAST) {
            return selectCoastalBiome(pvLevel, erosionLevel, temp, humid, precip);
        }
        
        // === RIVERS (valleys at any inland continentalness) ===
        
        if (pvLevel == PVLevel.VALLEYS) {
            return registry.selectRiverBiome(temp, humid, precip);
        }
        
        // === PEAKS (high PV + low erosion) ===

        if (pvLevel == PVLevel.PEAKS && erosionLevel <= 1) {
            return registry.selectPeakBiome(temp, humid, precip);
        }

        // === SLOPES ===
        // High PV with low erosion - temperature gated for montane
        if (pvLevel == PVLevel.HIGH && erosionLevel <= 1) {
            if (temp < 0.0) {
                // Cold elevated terrain → montane
                if (erosionLevel == 0 && contLevel != ContinentalnessLevel.NEAR_INLAND) {
                    return biomePool.selectBiome(Ecoregion.ALPINE_TUNDRA, temp, humid, precip);
                }
                return biomePool.selectBiome(Ecoregion.MONTANE_FOREST, temp, humid, precip);
            }
            // Warm/hot elevated terrain → regular ecoregion (savanna plateau, badlands, etc.)
            Ecoregion eco = EcoregionSelector.select(temp, precip, 0.0);
            return biomePool.selectBiome(eco, temp, humid, precip);
        }

        // Mid PV slopes - only cold temps get montane
        if (pvLevel == PVLevel.MID && erosionLevel <= 1 && temp < -0.3) {
            return biomePool.selectBiome(Ecoregion.MONTANE_FOREST, temp, humid, precip);
        }

        // === SHATTERED (E=5 at elevated terrain) ===
        if (erosionLevel == 5 && (pvLevel == PVLevel.MID || pvLevel == PVLevel.HIGH || pvLevel == PVLevel.PEAKS)) {
            Ecoregion eco = EcoregionSelector.select(temp, precip, 0.0);
            return biomePool.selectBiome(eco, temp, humid, precip);
        }

        // === LAND (everything else - including most HIGH/PEAKS terrain) ===
        Ecoregion eco = EcoregionSelector.select(temp, precip, 0.0);
        return biomePool.selectBiome(eco, temp, humid, precip);
    }
    
    /**
     * Coastal biome selection.
     * 
     * Coast is special because terrain shape matters more:
     * - Valleys = rivers meeting ocean
     * - Flat = beaches (including mangroves in hot+wet!)
     * - Rugged = cliffs/stony shore
     * - High terrain = land biomes reaching the sea
     */
    private Holder<Biome> selectCoastalBiome(PVLevel pvLevel, int erosionLevel,
            double temp, double humid, double precip) {

        // Rivers meeting the ocean
        if (pvLevel == PVLevel.VALLEYS) {
            return registry.selectRiverBiome(temp, humid, precip);
        }

        // Peaks at coast
        if (pvLevel == PVLevel.PEAKS) {
            return registry.selectPeakBiome(temp, humid, precip);
        }

        // High terrain at coast = land biomes reaching the sea
        if (pvLevel == PVLevel.HIGH) {
            Ecoregion eco = EcoregionSelector.select(temp, precip, 0.0);
            return biomePool.selectBiome(eco, temp, humid, precip);
        }

        // Low/Mid terrain at coast - beaches vs cliffs based on erosion
        if (erosionLevel <= 2) {
            // Rugged coast - cliffs
            return registry.selectCliffBiome(temp, humid, precip);
        } else {
            // Flat coast - beaches (mangroves handled inside selectBeachBiome by climate)
            return registry.selectBeachBiome(temp, humid, precip);
        }
    }
}
