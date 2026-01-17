package com.sheetworld.climate;

/**
 * Represents a range for a climate parameter (temperature, humidity, or precipitation).
 * 
 * The range has a min/max defining where the biome CAN exist,
 * and an optional "ideal" center point for scoring.
 */
public class ClimateRange {
    private final double min;
    private final double max;
    private final double ideal;  // The "sweet spot" for this biome
    
    /**
     * Create a range where the ideal is the center.
     */
    public ClimateRange(double min, double max) {
        this.min = min;
        this.max = max;
        this.ideal = (min + max) / 2.0;
    }
    
    /**
     * Create a range with an explicit ideal point.
     */
    public ClimateRange(double min, double max, double ideal) {
        this.min = min;
        this.max = max;
        this.ideal = ideal;
    }
    
    /**
     * Check if a value falls within this range.
     */
    public boolean contains(double value) {
        return value >= min && value <= max;
    }
    
    /**
     * Get how well a value fits this range.
     * Returns 1.0 at ideal, decreasing toward 0.0 at edges.
     * Returns 0.0 if outside range.
     */
    public double getFit(double value) {
        if (!contains(value)) {
            return 0.0;
        }
        
        // Distance from ideal, normalized by range width
        double halfWidth = (max - min) / 2.0;
        if (halfWidth <= 0) {
            return 1.0;  // Point range
        }
        
        double distanceFromIdeal = Math.abs(value - ideal);
        double maxDistance = Math.max(ideal - min, max - ideal);
        
        if (maxDistance <= 0) {
            return 1.0;
        }
        
        // Linear falloff from ideal (1.0) to edge (0.2)
        // We don't go all the way to 0 so edge biomes still have some score
        double fit = 1.0 - (distanceFromIdeal / maxDistance) * 0.8;
        return Math.max(0.0, fit);
    }
    
    /**
     * Get the "softness" score for values just outside the range.
     * This allows for smooth transitions - a value slightly outside
     * can still partially match.
     * 
     * @param value The value to check
     * @param softness How far outside the range to still consider (e.g., 0.1)
     * @return Score from 0.0 (far outside) to 1.0 (inside ideal)
     */
    public double getSoftFit(double value, double softness) {
        if (contains(value)) {
            return getFit(value);
        }
        
        // How far outside are we?
        double outside;
        if (value < min) {
            outside = min - value;
        } else {
            outside = value - max;
        }
        
        if (outside > softness) {
            return 0.0;
        }
        
        // Linear falloff in the soft zone
        return (1.0 - outside / softness) * 0.3;  // Max 0.3 for soft matches
    }
    
    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getIdeal() { return ideal; }
    
    @Override
    public String toString() {
        return String.format("[%.2f - %.2f (ideal: %.2f)]", min, max, ideal);
    }
    
    // === Static factory methods for common patterns ===
    
    /** Full range - biome can exist anywhere for this parameter */
    public static ClimateRange any() {
        return new ClimateRange(-2.0, 2.0, 0.0);
    }
    
    /** Tropical range */
    public static ClimateRange tropical() {
        return new ClimateRange(0.5, 1.0, 0.75);
    }
    
    /** Subtropical range */
    public static ClimateRange subtropical() {
        return new ClimateRange(0.2, 0.5, 0.35);
    }
    
    /** Temperate range */
    public static ClimateRange temperate() {
        return new ClimateRange(-0.2, 0.2, 0.0);
    }
    
    /** Boreal/cold range */
    public static ClimateRange boreal() {
        return new ClimateRange(-0.6, -0.2, -0.4);
    }
    
    /** Polar/freezing range */
    public static ClimateRange polar() {
        return new ClimateRange(-1.0, -0.6, -0.8);
    }
    
    /** Arid (low precipitation) */
    public static ClimateRange arid() {
        return new ClimateRange(0.0, 0.25, 0.1);
    }
    
    /** Semi-arid */
    public static ClimateRange semiArid() {
        return new ClimateRange(0.15, 0.4, 0.27);
    }
    
    /** Moderate precipitation */
    public static ClimateRange moderate() {
        return new ClimateRange(0.3, 0.6, 0.45);
    }
    
    /** Wet */
    public static ClimateRange wet() {
        return new ClimateRange(0.5, 0.8, 0.65);
    }
    
    /** Very wet */
    public static ClimateRange veryWet() {
        return new ClimateRange(0.7, 1.0, 0.85);
    }
}
