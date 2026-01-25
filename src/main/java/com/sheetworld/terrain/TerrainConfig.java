package com.sheetworld.terrain;

/**
 * Configuration values for terrain generation.
 * 
 * This replaces Tectonic's ConfigHandler with a simpler static configuration.
 * Values can be adjusted to tune terrain characteristics.
 */
public class TerrainConfig {
    
    // === TERRAIN HEIGHT ===
    public static double MIN_OFFSET = -1.5;
    public static double MAX_OFFSET = 2.5;
    public static double VERTICAL_SCALE = 1.0;
    
    // === OCEAN DEPTHS ===
    public static double OCEAN_DEPTH = -0.5;
    public static double DEEP_OCEAN_DEPTH = -0.8;
    
    // === MOUNTAIN PARAMETERS ===
    public static double SLOPE_LOWER = 0.15;
    public static double SLOPE_UPPER = 0.65;
    public static double MOUNTAIN_HEIGHT = 1.2;
    
    // === FEATURE TOGGLES ===
    public static boolean JUNGLE_PILLARS = true;
    public static boolean ROLLING_HILLS = true;
    public static boolean DUNES = true;
    public static boolean BADLANDS_RIDGES = true;
    public static boolean UNDERGROUND_RIVERS = true;
    
    // === CAVE PARAMETERS ===
    public static boolean CAVE_CHEESE_ENABLED = true;
    public static boolean CAVE_NOODLE_ENABLED = true;
    public static boolean CAVE_SPAGHETTI_ENABLED = true;
    public static double CAVE_DEPTH_CUTOFF = -0.8;
    
    // === CLIMATE INTEGRATION ===
    public static double RAIN_SHADOW_STRENGTH = 0.7;
    public static double OROGRAPHIC_LIFT = 1.4;
    
    // === WORLD BOUNDS ===
    public static int HALF_SIZE = 5000;
    public static int BOUNDARY_FADE_START = 4500;
    
    // === NOISE PARAMETERS ===
    // These mirror Tectonic's noise scales
    public static double CONTINENT_NOISE_SCALE = 1.0;
    public static double EROSION_NOISE_SCALE = 1.0;
    public static double RIDGE_NOISE_SCALE = 1.0;
    
    // === JAGGEDNESS FACTORS ===
    public static double ISLAND_JAGGEDNESS_FACTOR = 0.8;
    public static double CONTINENT_JAGGEDNESS_FACTOR = 1.0;
    
    // === FLAT TERRAIN SKEW ===
    // Shifts region selector to favor certain terrain types
    public static double FLAT_TERRAIN_SKEW = 0.0;
    
    // === SNOW LINE ===
    public static int SNOW_START_OFFSET = 0;  // Blocks to shift snow line
    
    /**
     * Get a configuration value by key.
     * Used by ConfigConstant density function.
     */
    public static double getValue(String key) {
        return switch (key) {
            case "min_offset" -> MIN_OFFSET;
            case "max_offset" -> MAX_OFFSET;
            case "vertical_scale" -> VERTICAL_SCALE;
            case "ocean_depth" -> OCEAN_DEPTH;
            case "deep_ocean_depth" -> DEEP_OCEAN_DEPTH;
            case "slope_lower" -> SLOPE_LOWER;
            case "slope_upper" -> SLOPE_UPPER;
            case "mountain_height" -> MOUNTAIN_HEIGHT;
            case "cave_depth_cutoff" -> CAVE_DEPTH_CUTOFF;
            case "rain_shadow_strength" -> RAIN_SHADOW_STRENGTH;
            case "orographic_lift" -> OROGRAPHIC_LIFT;
            case "continent_noise_scale" -> CONTINENT_NOISE_SCALE;
            case "erosion_noise_scale" -> EROSION_NOISE_SCALE;
            case "ridge_noise_scale" -> RIDGE_NOISE_SCALE;
            case "island_jaggedness_factor" -> ISLAND_JAGGEDNESS_FACTOR;
            case "continent_jaggedness_factor" -> CONTINENT_JAGGEDNESS_FACTOR;
            case "flat_terrain_skew" -> FLAT_TERRAIN_SKEW;
            case "half_size" -> (double) HALF_SIZE;
            case "boundary_fade_start" -> (double) BOUNDARY_FADE_START;
            default -> {
                // Try boolean toggles (return 1.0 for true, 0.0 for false)
                yield switch (key) {
                    case "jungle_pillars" -> JUNGLE_PILLARS ? 1.0 : 0.0;
                    case "rolling_hills" -> ROLLING_HILLS ? 1.0 : 0.0;
                    case "dunes" -> DUNES ? 1.0 : 0.0;
                    case "badlands_ridges" -> BADLANDS_RIDGES ? 1.0 : 0.0;
                    case "underground_rivers" -> UNDERGROUND_RIVERS ? 1.0 : 0.0;
                    case "cave_cheese_enabled" -> CAVE_CHEESE_ENABLED ? 1.0 : 0.0;
                    case "cave_noodle_enabled" -> CAVE_NOODLE_ENABLED ? 1.0 : 0.0;
                    case "cave_spaghetti_enabled" -> CAVE_SPAGHETTI_ENABLED ? 1.0 : 0.0;
                    default -> 0.0;
                };
            }
        };
    }
    
    /**
     * Check if a feature is enabled.
     */
    public static boolean isFeatureEnabled(String feature) {
        return getValue(feature) > 0.5;
    }
}
