package com.sheetworld.climate;

import com.sheetworld.climate.TerrainParameters.ContinentalnessLevel;
import com.sheetworld.climate.TerrainParameters.PVLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * Terrain-based biome selection using vanilla's lookup table approach.
 * 
 * This implements the hard gate system:
 * 1. Continentalness determines ocean/coast/land
 * 2. PV (peaks/valleys) determines terrain shape  
 * 3. Erosion determines mountain vs flat within each category
 * 4. Climate (temp/humid/precip) picks the specific biome
 * 
 * SIMPLIFIED CATEGORIES:
 * - Ocean/DeepOcean: Temperature only
 * - Beach: Coastal + flat + climate (includes mangroves!)
 * - Cliff: Coastal + rugged
 * - River: Valleys at any continentalness
 * - Peak: High PV + low erosion
 * - Slope: Mountain sides
 * - Shattered: Windswept terrain
 * - Land: EVERYTHING ELSE - climate does all the work here
 */
public class TerrainBiomeSelector {
    
    private final BiomeRegistry registry;
    
    public TerrainBiomeSelector(BiomeRegistry registry) {
        this.registry = registry;
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
        
        // === SLOPES (mountain sides) ===
        
        if ((pvLevel == PVLevel.PEAKS || pvLevel == PVLevel.HIGH) && erosionLevel <= 3) {
            return registry.selectSlopeBiome(temp, humid, precip);
        }
        
        if (pvLevel == PVLevel.MID && erosionLevel <= 1) {
            return registry.selectSlopeBiome(temp, humid, precip);
        }
        
        // === SHATTERED (high erosion + mid-high PV = windswept) ===
        
        if (erosionLevel == 5 && (pvLevel == PVLevel.MID || pvLevel == PVLevel.HIGH)) {
            return registry.selectShatteredBiome(temp, humid, precip);
        }
        
        // === LAND (everything else - climate does the work) ===
        
        return registry.selectLandBiome(temp, humid, precip);
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
        
        // Peaks/High terrain at coast = land biomes or slopes
        if (pvLevel == PVLevel.PEAKS) {
            return registry.selectSlopeBiome(temp, humid, precip);
        }
        
        if (pvLevel == PVLevel.HIGH) {
            return registry.selectLandBiome(temp, humid, precip);
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
