package com.sheetworld.ecoregion;

/**
 * WWF-inspired ecoregion categories for biome selection.
 * 
 * This is the FIRST level of biome selection:
 *   Climate Parameters → Ecoregion → Specific Biome
 * 
 * Each ecoregion represents a broad ecological category that can contain
 * multiple specific biomes (vanilla or modded). The ecoregion is selected
 * deterministically based on temperature, precipitation, and elevation.
 * 
 * The specific biome within an ecoregion is then selected based on
 * secondary factors (local noise, erosion, weirdness, etc.).
 */
public enum Ecoregion {
    
    // ==================== POLAR / COLD ====================
    
    /**
     * Permanent ice and extremely cold, dry conditions.
     * Vanilla: Ice Spikes
     * Real-world: Antarctica interior, Greenland ice sheet
     */
    ICE_SHEET_AND_POLAR_DESERT(
        "Ice Sheet & Polar Desert",
        -1.0, -0.7,   // temperature: extremely cold
        0.0, 0.3,     // precipitation: very dry (cold air holds little moisture)
        0.0, 0.4      // elevation: any (but typically flat ice)
    ),
    
    /**
     * Treeless cold regions with permafrost, mosses, lichens.
     * Vanilla: Snowy Plains, Snowy Slopes
     * Real-world: Siberian tundra, Canadian Arctic
     */
    TUNDRA(
        "Tundra",
        -0.75, -0.4,  // temperature: very cold
        0.1, 0.5,     // precipitation: low to moderate
        0.0, 0.5      // elevation: lowland tundra
    ),
    
    /**
     * Cold coniferous forests (boreal/taiga).
     * Vanilla: Taiga, Old Growth Taiga, Grove
     * Real-world: Siberia, Canada, Scandinavia
     */
    TAIGA(
        "Taiga",
        -0.5, -0.05,  // temperature: cold
        0.3, 0.8,     // precipitation: moderate to high
        0.0, 0.5      // elevation: lowland to moderate
    ),
    
    // ==================== TEMPERATE ====================
    
    /**
     * Deciduous and mixed forests of temperate regions.
     * Vanilla: Forest, Birch Forest, Dark Forest
     * Modded: Maple Forest (Autumnity), Blossom Woods (Environmental)
     * Real-world: Eastern US, Western Europe, East Asia
     */
    TEMPERATE_BROADLEAF_FOREST(
        "Temperate Broadleaf & Mixed Forest",
        -0.15, 0.35,  // temperature: cool to warm
        0.45, 0.9,    // precipitation: moderate to high
        0.0, 0.5      // elevation: lowland to moderate
    ),
    
    /**
     * Temperate grasslands with cold winters.
     * Vanilla: Plains, Sunflower Plains
     * Real-world: Ukraine, Kansas, Pampas
     */
    TEMPERATE_STEPPE(
        "Temperate Steppe",
        -0.2, 0.3,    // temperature: cold winters, warm summers
        0.2, 0.45,    // precipitation: semi-arid
        0.0, 0.4      // elevation: lowland plains
    ),
    
    // ==================== SUBTROPICAL ====================
    
    /**
     * Humid subtropical forests.
     * Vanilla: (sparse representation)
     * Modded: Rosewood Forest (Atmospheric)
     * Real-world: Southeastern US, Southern China, Eastern Australia
     */
    SUBTROPICAL_MOIST_FOREST(
        "Subtropical Moist Forest",
        0.25, 0.55,   // temperature: warm
        0.55, 0.9,    // precipitation: high
        0.0, 0.4      // elevation: lowland
    ),
    
    /**
     * Mediterranean climate - dry summers, wet winters.
     * Vanilla: (no good match)
     * Modded: Scrubland (Atmospheric)
     * Real-world: California, Mediterranean Basin, Chile
     */
    MEDITERRANEAN(
        "Mediterranean Vegetation",
        0.2, 0.5,     // temperature: warm
        0.25, 0.5,    // precipitation: moderate (seasonal)
        0.0, 0.4      // elevation: lowland to hills
    ),
    
    // ==================== TROPICAL ====================
    
    /**
     * Tropical rainforests with year-round rainfall.
     * Vanilla: Jungle, Bamboo Jungle
     * Modded: Rainforest (Atmospheric)
     * Real-world: Amazon, Congo, Southeast Asia
     * Terrain: JUNGLE PILLARS (武陵源 style)
     */
    TROPICAL_RAINFOREST(
        "Tropical Rainforest",
        0.6, 1.0,     // temperature: hot
        0.75, 1.0,    // precipitation: very high, year-round
        0.0, 0.4      // elevation: lowland
    ),
    
    /**
     * Tropical forests with seasonal rainfall.
     * Vanilla: Sparse Jungle
     * Real-world: Monsoon forests of India, Southeast Asia
     */
    TROPICAL_MOIST_BROADLEAF(
        "Tropical & Subtropical Moist Broadleaf",
        0.5, 0.85,    // temperature: hot
        0.55, 0.8,    // precipitation: high but seasonal
        0.0, 0.4      // elevation: lowland
    ),
    
    /**
     * Tropical dry forests - pronounced dry season.
     * Vanilla: (limited)
     * Real-world: Yucatan, Madagascar dry forest
     */
    TROPICAL_DRY_FOREST(
        "Tropical & Subtropical Dry Forest",
        0.5, 0.8,     // temperature: hot
        0.3, 0.55,    // precipitation: moderate, very seasonal
        0.0, 0.4      // elevation: lowland
    ),
    
    // ==================== GRASSLAND / SAVANNA ====================
    
    /**
     * Tropical grasslands with scattered trees.
     * Vanilla: Savanna, Savanna Plateau
     * Real-world: East African savanna, Brazilian cerrado
     */
    TREE_SAVANNA(
        "Tree Savanna",
        0.45, 0.8,    // temperature: warm to hot
        0.25, 0.45,   // precipitation: moderate, seasonal
        0.0, 0.4      // elevation: lowland to plateau
    ),
    
    /**
     * Tropical grasslands with few trees.
     * Vanilla: Savanna (drier variant)
     * Real-world: Sahel, Australian outback edges
     */
    GRASS_SAVANNA(
        "Grass Savanna",
        0.4, 0.75,    // temperature: warm to hot
        0.15, 0.3,    // precipitation: low, seasonal
        0.0, 0.4      // elevation: lowland
    ),
    
    // ==================== ARID ====================
    
    /**
     * Hot deserts with minimal rainfall.
     * Vanilla: Desert
     * Modded: Dunes (Atmospheric)
     * Real-world: Sahara, Arabian, Sonoran
     * Terrain: DUNES
     */
    ARID_DESERT(
        "Arid Desert",
        0.5, 1.0,     // temperature: hot
        0.0, 0.15,    // precipitation: extremely low
        0.0, 0.4      // elevation: lowland
    ),
    
    /**
     * Semi-arid regions - slightly more rain than desert.
     * Vanilla: Badlands (kind of)
     * Modded: Rocky Dunes (Atmospheric)
     * Real-world: Mojave, Kalahari edges
     */
    SEMIARID_DESERT(
        "Semiarid Desert",
        0.3, 0.7,     // temperature: warm (can be cooler)
        0.08, 0.2,    // precipitation: very low
        0.0, 0.4      // elevation: lowland
    ),
    
    /**
     * Shrublands in arid climates.
     * Vanilla: Badlands variants
     * Modded: Scrubland (Atmospheric)
     * Real-world: Chihuahuan desert scrub, Australian mulga
     * Terrain: BADLANDS RIDGES
     */
    XERIC_SHRUBLAND(
        "Xeric Shrubland",
        0.35, 0.7,    // temperature: warm
        0.12, 0.28,   // precipitation: low
        0.0, 0.4      // elevation: lowland to hills
    ),
    
    /**
     * Cold/temperate semi-arid grasslands.
     * Vanilla: Plains (dry variant)
     * Real-world: Patagonia, Central Asian steppe
     */
    DRY_STEPPE(
        "Dry Steppe",
        -0.1, 0.4,    // temperature: variable
        0.1, 0.25,    // precipitation: low
        0.0, 0.4      // elevation: lowland
    ),
    
    // ==================== MONTANE ====================
    
    /**
     * High-altitude treeless zones.
     * Vanilla: Jagged Peaks, Stony Peaks, Frozen Peaks
     * Real-world: Tibetan Plateau, Andes paramo, Alps above treeline
     */
    ALPINE_TUNDRA(
        "Alpine Tundra",
        -1.0, 0.3,    // temperature: varies (cold at altitude)
        0.0, 0.6,     // precipitation: varies
        0.6, 1.0      // elevation: HIGH - this is the key discriminator
    ),
    
    /**
     * Mountain forests below treeline.
     * Vanilla: Grove, Meadow, Snowy Slopes
     * Real-world: Rocky Mountain forests, European Alps forests
     */
    MONTANE_FOREST(
        "Montane Forest",
        -0.5, 0.4,    // temperature: cool (altitude-adjusted)
        0.35, 0.85,   // precipitation: moderate to high
        0.4, 0.7      // elevation: MODERATE-HIGH
    ),
    
    // ==================== AQUATIC ====================
    
    /**
     * Coastal wetlands in tropical regions.
     * Vanilla: Mangrove Swamp
     * Real-world: Florida Everglades, Sundarbans
     */
    MANGROVE(
        "Mangrove",
        0.4, 1.0,     // temperature: warm to hot
        0.6, 1.0,     // precipitation: high
        0.0, 0.1      // elevation: sea level (coastal)
    ),
    
    /**
     * Freshwater wetlands.
     * Vanilla: Swamp
     * Modded: Marsh (Environmental)
     * Real-world: Pantanal, Okavango Delta
     */
    WETLAND(
        "Wetland",
        -0.2, 0.6,    // temperature: varies
        0.6, 1.0,     // precipitation: high
        0.0, 0.2      // elevation: low-lying
    );
    
    // ==================== FIELDS ====================
    
    private final String displayName;
    private final double minTemp, maxTemp;
    private final double minPrecip, maxPrecip;
    private final double minElevation, maxElevation;
    
    Ecoregion(String displayName,
              double minTemp, double maxTemp,
              double minPrecip, double maxPrecip,
              double minElevation, double maxElevation) {
        this.displayName = displayName;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.minPrecip = minPrecip;
        this.maxPrecip = maxPrecip;
        this.minElevation = minElevation;
        this.maxElevation = maxElevation;
    }
    
    // ==================== ACCESSORS ====================
    
    public String getDisplayName() {
        return displayName;
    }
    
    public double getMinTemp() { return minTemp; }
    public double getMaxTemp() { return maxTemp; }
    public double getMinPrecip() { return minPrecip; }
    public double getMaxPrecip() { return maxPrecip; }
    public double getMinElevation() { return minElevation; }
    public double getMaxElevation() { return maxElevation; }
    
    /**
     * Check if this ecoregion matches the given climate conditions.
     * Returns a score from 0 (no match) to 1 (perfect match).
     */
    public double getMatchScore(double temperature, double precipitation, double elevation) {
        // Check hard bounds first
        if (temperature < minTemp || temperature > maxTemp) return 0.0;
        if (precipitation < minPrecip || precipitation > maxPrecip) return 0.0;
        if (elevation < minElevation || elevation > maxElevation) return 0.0;
        
        // Calculate how well the values fit within the ranges
        double tempFit = calculateFit(temperature, minTemp, maxTemp);
        double precipFit = calculateFit(precipitation, minPrecip, maxPrecip);
        double elevFit = calculateFit(elevation, minElevation, maxElevation);
        
        // Geometric mean gives a balanced score
        return Math.cbrt(tempFit * precipFit * elevFit);
    }
    
    /**
     * Calculate how well a value fits in a range.
     * Returns 1.0 at the center, decreasing toward edges.
     */
    private double calculateFit(double value, double min, double max) {
        double center = (min + max) / 2.0;
        double halfRange = (max - min) / 2.0;
        if (halfRange == 0) return 1.0;
        
        double distFromCenter = Math.abs(value - center);
        double normalizedDist = distFromCenter / halfRange;
        
        // Smooth falloff from center to edge
        return 1.0 - (normalizedDist * normalizedDist * 0.5);
    }
    
    /**
     * Check if this ecoregion is elevation-dependent (montane).
     */
    public boolean isMontane() {
        return this == ALPINE_TUNDRA || this == MONTANE_FOREST;
    }
    
    /**
     * Check if this ecoregion is coastal/aquatic.
     */
    public boolean isAquatic() {
        return this == MANGROVE || this == WETLAND;
    }
    
    /**
     * Get the terrain feature set associated with this ecoregion.
     * This connects to Tectonic's region system.
     */
    public TerrainFeatureSet getTerrainFeatures() {
        return switch (this) {
            case TROPICAL_RAINFOREST, TROPICAL_MOIST_BROADLEAF -> TerrainFeatureSet.JUNGLE_PILLARS;
            case ARID_DESERT -> TerrainFeatureSet.DUNES;
            case XERIC_SHRUBLAND, SEMIARID_DESERT -> TerrainFeatureSet.BADLANDS_RIDGES;
            case TEMPERATE_STEPPE, DRY_STEPPE, GRASS_SAVANNA -> TerrainFeatureSet.ROLLING_HILLS;
            case MEDITERRANEAN, TREE_SAVANNA -> TerrainFeatureSet.PLATEAUS;
            case ALPINE_TUNDRA, MONTANE_FOREST -> TerrainFeatureSet.MOUNTAIN_TERRAIN;
            default -> TerrainFeatureSet.BASE_TERRAIN;
        };
    }
    
    /**
     * Terrain feature sets that map to Tectonic's region system.
     */
    public enum TerrainFeatureSet {
        BASE_TERRAIN,       // Default terrain
        JUNGLE_PILLARS,     // Heart region - 武陵源 style karst pillars
        DUNES,              // Diamond region - sand dune systems
        BADLANDS_RIDGES,    // Club region - eroded ridges and mesas
        ROLLING_HILLS,      // Heart region - gentle hills
        PLATEAUS,           // Club region - flat-topped mesas
        MOUNTAIN_TERRAIN    // Full mountain ridge system
    }
}
