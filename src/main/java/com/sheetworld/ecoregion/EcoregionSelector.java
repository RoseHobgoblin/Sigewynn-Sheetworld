package com.sheetworld.ecoregion;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Selects the appropriate ecoregion based on climate parameters.
 * 
 * This is the DETERMINISTIC first level of biome selection.
 * Given temperature, precipitation, and elevation, this returns
 * the single best-matching ecoregion.
 * 
 * The selection uses a priority system:
 * 1. Montane ecoregions (elevation > threshold) - checked first
 * 2. Aquatic ecoregions (wetland conditions) - checked second
 * 3. General ecoregions - best match by score
 */
public class EcoregionSelector {
    
    // Elevation thresholds (based on Tectonic's mountain_ridges values)
    private static final double ALPINE_THRESHOLD = 0.6;
    private static final double MONTANE_THRESHOLD = 0.4;
    private static final double FOOTHILL_THRESHOLD = 0.2;
    
    // Wetland detection
    private static final double WETLAND_PRECIP_THRESHOLD = 0.7;
    private static final double MANGROVE_TEMP_THRESHOLD = 0.4;
    
    /**
     * Select the ecoregion for given climate conditions.
     * 
     * @param temperature    Range -1 (polar) to +1 (equatorial)
     * @param precipitation  Range 0 (desert) to 1 (rainforest)
     * @param elevation      Range 0 (sea level) to 1 (mountain peak)
     *                       Derived from Tectonic's mountain_ridges
     * @return The best matching ecoregion
     */
    public static Ecoregion select(double temperature, double precipitation, double elevation) {
        
        // === PRIORITY 1: MONTANE OVERRIDE ===
        // High elevation overrides latitude-based climate
        if (elevation >= ALPINE_THRESHOLD) {
            // Above treeline
            if (precipitation > 0.5) {
                return Ecoregion.ALPINE_TUNDRA;
            } else {
                // Very dry high altitude = cold desert / rocky peaks
                return Ecoregion.ALPINE_TUNDRA;
            }
        }
        
        if (elevation >= MONTANE_THRESHOLD) {
            // Montane zone - forested if wet enough
            if (precipitation > 0.35) {
                return Ecoregion.MONTANE_FOREST;
            } else {
                // Dry mountains = alpine tundra conditions
                return Ecoregion.ALPINE_TUNDRA;
            }
        }
        
        // === PRIORITY 2: WETLAND / MANGROVE ===
        // Very wet, low-lying areas
        if (elevation < 0.15 && precipitation > WETLAND_PRECIP_THRESHOLD) {
            if (temperature > MANGROVE_TEMP_THRESHOLD) {
                return Ecoregion.MANGROVE;
            } else {
                return Ecoregion.WETLAND;
            }
        }
        
        // === PRIORITY 3: TEMPERATURE-PRECIPITATION GRID ===
        // This is the main selection for non-montane, non-wetland areas
        
        // Apply foothill temperature adjustment (lapse rate approximation)
        double adjustedTemp = temperature;
        if (elevation > FOOTHILL_THRESHOLD) {
            // Temperature decreases with elevation
            double elevationEffect = (elevation - FOOTHILL_THRESHOLD) * 0.5;
            adjustedTemp -= elevationEffect;
        }
        
        return selectByClimate(adjustedTemp, precipitation);
    }
    
    /**
     * Select ecoregion based purely on temperature and precipitation.
     * Used for lowland areas without elevation effects.
     */
    private static Ecoregion selectByClimate(double temperature, double precipitation) {
        
        // === POLAR / ICE ===
        if (temperature < -0.7) {
            return Ecoregion.ICE_SHEET_AND_POLAR_DESERT;
        }
        
        // === TUNDRA ===
        if (temperature < -0.4) {
            return Ecoregion.TUNDRA;
        }
        
        // === COLD ===
        if (temperature < -0.05) {
            if (precipitation > 0.5) {
                return Ecoregion.TAIGA;
            } else if (precipitation > 0.25) {
                return Ecoregion.TEMPERATE_STEPPE;
            } else {
                return Ecoregion.DRY_STEPPE;
            }
        }
        
        // === TEMPERATE ===
        if (temperature < 0.35) {
            if (precipitation > 0.6) {
                return Ecoregion.TEMPERATE_BROADLEAF_FOREST;
            } else if (precipitation > 0.35) {
                return Ecoregion.TEMPERATE_STEPPE;
            } else if (precipitation > 0.2) {
                return Ecoregion.DRY_STEPPE;
            } else {
                return Ecoregion.SEMIARID_DESERT;
            }
        }
        
        // === SUBTROPICAL ===
        if (temperature < 0.55) {
            if (precipitation > 0.65) {
                return Ecoregion.SUBTROPICAL_MOIST_FOREST;
            } else if (precipitation > 0.4) {
                return Ecoregion.MEDITERRANEAN;
            } else if (precipitation > 0.2) {
                return Ecoregion.XERIC_SHRUBLAND;
            } else {
                return Ecoregion.SEMIARID_DESERT;
            }
        }
        
        // === TROPICAL ===
        // Hot regions - the most diverse
        if (precipitation > 0.75) {
            return Ecoregion.TROPICAL_RAINFOREST;
        } else if (precipitation > 0.55) {
            return Ecoregion.TROPICAL_MOIST_BROADLEAF;
        } else if (precipitation > 0.4) {
            return Ecoregion.TROPICAL_DRY_FOREST;
        } else if (precipitation > 0.25) {
            return Ecoregion.TREE_SAVANNA;
        } else if (precipitation > 0.15) {
            return Ecoregion.GRASS_SAVANNA;
        } else {
            return Ecoregion.ARID_DESERT;
        }
    }
    
    /**
     * Alternative selection using best-fit scoring.
     * Useful for edge cases and debugging.
     */
    public static Ecoregion selectByScore(double temperature, double precipitation, double elevation) {
        return Stream.of(Ecoregion.values())
            .map(eco -> new ScoredEcoregion(eco, eco.getMatchScore(temperature, precipitation, elevation)))
            .filter(se -> se.score > 0)
            .max(Comparator.comparingDouble(se -> se.score))
            .map(se -> se.ecoregion)
            .orElse(Ecoregion.TEMPERATE_STEPPE); // Fallback
    }
    
    private static class ScoredEcoregion {
        final Ecoregion ecoregion;
        final double score;
        
        ScoredEcoregion(Ecoregion ecoregion, double score) {
            this.ecoregion = ecoregion;
            this.score = score;
        }
    }
    
    /**
     * Get debug information about ecoregion selection.
     */
    public static String getDebugInfo(double temperature, double precipitation, double elevation) {
        Ecoregion selected = select(temperature, precipitation, elevation);
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Climate: T=%.2f P=%.2f E=%.2f\n", temperature, precipitation, elevation));
        sb.append(String.format("Selected: %s\n", selected.getDisplayName()));
        sb.append(String.format("Terrain Features: %s\n", selected.getTerrainFeatures()));
        
        // Show top 3 alternatives by score
        sb.append("\nAlternatives by score:\n");
        Stream.of(Ecoregion.values())
            .map(eco -> new ScoredEcoregion(eco, eco.getMatchScore(temperature, precipitation, elevation)))
            .filter(se -> se.score > 0)
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .limit(3)
            .forEach(se -> sb.append(String.format("  %.3f: %s\n", se.score, se.ecoregion.getDisplayName())));
        
        return sb.toString();
    }
}
