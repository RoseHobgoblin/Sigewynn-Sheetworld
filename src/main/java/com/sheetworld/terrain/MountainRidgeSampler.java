package com.sheetworld.terrain;

import com.sheetworld.densityfunction.SinglePointContext;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Utility class for sampling mountain ridge heights.
 * 
 * This enables rain shadow calculations by allowing us to query
 * the mountain height at any XZ position. The mountain ridge data
 * comes from Tectonic's coherent ridge system.
 */
public class MountainRidgeSampler {
    
    private final DensityFunction ridgeFunction;
    
    // Wind sampling offsets (in blocks)
    private static final int WIND_SAMPLE_DISTANCE = 500;
    
    /**
     * Create a sampler with the ridge density function.
     * 
     * @param ridgeFunction The density function for mountain ridges
     *                      (e.g., sheetworld:terrain/mountain_ridges/ridges)
     */
    public MountainRidgeSampler(DensityFunction ridgeFunction) {
        this.ridgeFunction = ridgeFunction;
    }
    
    /**
     * Sample the mountain height at a given XZ position.
     * 
     * @param x Block X coordinate
     * @param z Block Z coordinate
     * @return Mountain height value (0 = flat, 1 = peak)
     */
    public double sampleHeight(int x, int z) {
        if (ridgeFunction == null) {
            return 0.0;
        }
        return ridgeFunction.compute(new SinglePointContext(x, 0, z));
    }
    
    /**
     * Calculate the rain shadow effect at a position.
     * 
     * Checks if there are mountains upwind of this position.
     * Returns a multiplier for precipitation:
     * - < 1.0: In rain shadow (dry)
     * - = 1.0: No effect
     * - > 1.0: Windward side (wet, orographic lift)
     * 
     * @param x Block X coordinate
     * @param z Block Z coordinate
     * @param windDirection Wind direction (0 = from east, 1 = from west)
     *                      Based on atmospheric circulation cell
     * @return Precipitation multiplier
     */
    public double calculateRainShadow(int x, int z, WindDirection windDirection) {
        if (ridgeFunction == null) {
            return 1.0;
        }
        
        double localHeight = sampleHeight(x, z);
        
        // Sample upwind
        int upwindX = x + windDirection.getUpwindOffsetX();
        int upwindZ = z + windDirection.getUpwindOffsetZ();
        double upwindHeight = sampleHeight(upwindX, upwindZ);
        
        // Sample downwind
        int downwindX = x - windDirection.getUpwindOffsetX();
        int downwindZ = z - windDirection.getUpwindOffsetZ();
        double downwindHeight = sampleHeight(downwindX, downwindZ);
        
        // Calculate effect
        double shadowThreshold = 0.15;  // Minimum height difference to cause shadow
        
        // Rain shadow: mountains upwind block moisture
        if (upwindHeight > localHeight + shadowThreshold) {
            double shadowStrength = Math.min((upwindHeight - localHeight) * 2.0, 1.0);
            return 1.0 - (shadowStrength * TerrainConfig.RAIN_SHADOW_STRENGTH);
        }
        
        // Orographic lift: we're on the windward side of a mountain
        if (downwindHeight > localHeight + shadowThreshold) {
            double liftStrength = Math.min((downwindHeight - localHeight) * 2.0, 1.0);
            return 1.0 + (liftStrength * (TerrainConfig.OROGRAPHIC_LIFT - 1.0));
        }
        
        return 1.0;
    }
    
    /**
     * Get the wind direction for a given latitude.
     * Based on atmospheric circulation cells.
     */
    public static WindDirection getWindDirection(double latitude) {
        // latitude is 0-1 (0 = equator, 1 = pole)
        if (latitude < 0.30) {
            // Hadley cell: Trade winds from the EAST
            return WindDirection.FROM_EAST;
        } else if (latitude < 0.60) {
            // Ferrel cell: Westerlies from the WEST
            return WindDirection.FROM_WEST;
        } else {
            // Polar cell: Polar easterlies from the EAST
            return WindDirection.FROM_EAST;
        }
    }
    
    /**
     * Wind direction enum with offset calculations.
     */
    public enum WindDirection {
        FROM_EAST(-WIND_SAMPLE_DISTANCE, 0),   // Wind blows westward
        FROM_WEST(WIND_SAMPLE_DISTANCE, 0),    // Wind blows eastward
        FROM_NORTH(0, WIND_SAMPLE_DISTANCE),   // Wind blows southward
        FROM_SOUTH(0, -WIND_SAMPLE_DISTANCE);  // Wind blows northward
        
        private final int upwindOffsetX;
        private final int upwindOffsetZ;
        
        WindDirection(int upwindOffsetX, int upwindOffsetZ) {
            this.upwindOffsetX = upwindOffsetX;
            this.upwindOffsetZ = upwindOffsetZ;
        }
        
        public int getUpwindOffsetX() { return upwindOffsetX; }
        public int getUpwindOffsetZ() { return upwindOffsetZ; }
    }
    
    /**
     * Calculate elevation value for ecoregion selection.
     * Converts ridge height to 0-1 elevation parameter.
     */
    public double getElevation(int x, int z) {
        double ridgeHeight = sampleHeight(x, z);
        // Map ridge height (roughly -0.5 to 1.0) to elevation (0 to 1)
        return Math.max(0, Math.min(1, (ridgeHeight + 0.5) / 1.5));
    }
}
