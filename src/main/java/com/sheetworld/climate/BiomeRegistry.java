package com.sheetworld.climate;

import com.sheetworld.Sheetworld;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Climate-based biome registry and selection system.
 * 
 * Within each category, biomes compete based on climate fit score.
 * This allows modded biomes to seamlessly integrate by registering
 * into the appropriate category with their climate preferences.
 * 
 * TERRAIN CATEGORIES (hard gates via TerrainBiomeSelector):
 * - Mushroom, Deep Ocean, Ocean: Special cases
 * - Beach, Cliff, River: Coastal/water features  
 * - Peak, Slope, Shattered: Mountain terrain
 * - Land: EVERYTHING ELSE - climate does all the work
 */
public class BiomeRegistry {
    
    // Biome lists by terrain category
    private final List<BiomeCandidate> mushroomBiomes = new ArrayList<>();
    private final List<BiomeCandidate> deepOceanBiomes = new ArrayList<>();
    private final List<BiomeCandidate> oceanBiomes = new ArrayList<>();
    private final List<BiomeCandidate> beachBiomes = new ArrayList<>();
    private final List<BiomeCandidate> cliffBiomes = new ArrayList<>();
    private final List<BiomeCandidate> riverBiomes = new ArrayList<>();
    private final List<BiomeCandidate> peakBiomes = new ArrayList<>();
    private final List<BiomeCandidate> slopeBiomes = new ArrayList<>();
    private final List<BiomeCandidate> shatteredBiomes = new ArrayList<>();
    private final List<BiomeCandidate> landBiomes = new ArrayList<>();
    
    private final HolderGetter<Biome> biomeGetter;
    
    // Fallbacks
    private BiomeCandidate fallbackLand;
    private BiomeCandidate fallbackOcean;
    
    public BiomeRegistry(HolderGetter<Biome> biomeGetter) {
        this.biomeGetter = biomeGetter;
        
        ModCompat.init();
        
        registerVanillaBiomes();
        registerAtmosphericBiomes();
        registerEnvironmentalBiomes();
        registerAutumnityBiomes();
        
        int total = mushroomBiomes.size() + deepOceanBiomes.size() + oceanBiomes.size() +
                    beachBiomes.size() + cliffBiomes.size() + riverBiomes.size() +
                    peakBiomes.size() + slopeBiomes.size() + shatteredBiomes.size() +
                    landBiomes.size();
        
        Sheetworld.LOGGER.info("BiomeRegistry initialized with {} total candidates (land: {})",
            total, landBiomes.size());
    }
    
    // ==================== CATEGORY SELECTORS ====================
    
    public Holder<Biome> selectMushroomBiome(double temp) {
        return selectFromCategory(mushroomBiomes, temp, 0.5, 0.5, fallbackLand);
    }
    
    public Holder<Biome> selectDeepOceanBiome(double temp) {
        return selectFromCategory(deepOceanBiomes, temp, 0.5, 0.5, fallbackOcean);
    }
    
    public Holder<Biome> selectOceanBiome(double temp) {
        return selectFromCategory(oceanBiomes, temp, 0.5, 0.5, fallbackOcean);
    }
    
    public Holder<Biome> selectBeachBiome(double temp, double humid, double precip) {
        return selectFromCategory(beachBiomes, temp, humid, precip, fallbackLand);
    }
    
    public Holder<Biome> selectCliffBiome(double temp, double humid, double precip) {
        return selectFromCategory(cliffBiomes, temp, humid, precip, fallbackLand);
    }
    
    public Holder<Biome> selectRiverBiome(double temp, double humid, double precip) {
        return selectFromCategory(riverBiomes, temp, humid, precip, fallbackLand);
    }
    
    public Holder<Biome> selectPeakBiome(double temp, double humid, double precip) {
        return selectFromCategory(peakBiomes, temp, humid, precip, fallbackLand);
    }
    
    public Holder<Biome> selectSlopeBiome(double temp, double humid, double precip) {
        return selectFromCategory(slopeBiomes, temp, humid, precip, fallbackLand);
    }
    
    public Holder<Biome> selectShatteredBiome(double temp, double humid, double precip) {
        return selectFromCategory(shatteredBiomes, temp, humid, precip, fallbackLand);
    }
    
    public Holder<Biome> selectLandBiome(double temp, double humid, double precip) {
        return selectFromCategory(landBiomes, temp, humid, precip, fallbackLand);
    }

    public Stream<Holder<Biome>> collectAllPossibleBiomes() {
        // Combine all category lists
        return Stream.of(
                mushroomBiomes,
                deepOceanBiomes,
                oceanBiomes,
                beachBiomes,
                cliffBiomes,
                riverBiomes,
                peakBiomes,
                slopeBiomes,
                landBiomes,
                shatteredBiomes
            )
            .flatMap(List::stream)                    // Stream all candidates
            .filter(BiomeCandidate::isAvailable)      // Only available ones (mod loaded + biome exists)
            .map(BiomeCandidate::getBiome)            // Get the Holder<Biome>
            .filter(java.util.Objects::nonNull)       // Filter out any nulls
            .distinct();                               // No duplicates
    }
    
    // ==================== SELECTION LOGIC ====================
    
    private Holder<Biome> selectFromCategory(List<BiomeCandidate> candidates,
            double temp, double humid, double precip, BiomeCandidate fallback) {
        
        Optional<BiomeCandidate> best = candidates.stream()
            .filter(BiomeCandidate::isAvailable)
            .map(c -> new ScoredCandidate(c, c.getFitScore(temp, humid, precip)))
            .filter(sc -> sc.score > 0)
            .max(Comparator.comparingDouble(sc -> sc.score))
            .map(sc -> sc.candidate);
        
        if (best.isPresent()) {
            Holder<Biome> biome = best.get().getBiome();
            if (biome != null) {
                return biome;
            }
        }
        
        return fallback != null ? fallback.getBiome() : null;
    }
    
    private static class ScoredCandidate {
        final BiomeCandidate candidate;
        final double score;
        
        ScoredCandidate(BiomeCandidate candidate, double score) {
            this.candidate = candidate;
            this.score = score;
        }
    }
    
    // ==================== REGISTRATION HELPERS ====================
    
    private ResourceKey<Biome> moddedKey(String modId, String path) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(modId, path));
    }
    
    // ==================== VANILLA BIOME REGISTRATION ====================
    
    private void registerVanillaBiomes() {
        
        // =====================================================================
        // MUSHROOM - isolated ocean islands
        // =====================================================================
        
        mushroomBiomes.add(BiomeCandidate.builder(Biomes.MUSHROOM_FIELDS, biomeGetter)
            .temperature(-1.0, 1.0, 0.0)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        // =====================================================================
        // DEEP OCEAN - temperature only
        // =====================================================================
        
        deepOceanBiomes.add(BiomeCandidate.builder(Biomes.DEEP_FROZEN_OCEAN, biomeGetter)
            .temperature(-1.0, -0.45, -0.7)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        deepOceanBiomes.add(BiomeCandidate.builder(Biomes.DEEP_COLD_OCEAN, biomeGetter)
            .temperature(-0.55, 0.0, -0.25)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        deepOceanBiomes.add(BiomeCandidate.builder(Biomes.DEEP_OCEAN, biomeGetter)
            .temperature(-0.1, 0.4, 0.15)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        deepOceanBiomes.add(BiomeCandidate.builder(Biomes.DEEP_LUKEWARM_OCEAN, biomeGetter)
            .temperature(0.3, 0.7, 0.5)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        fallbackOcean = BiomeCandidate.builder(Biomes.DEEP_OCEAN, biomeGetter)
            .temperature(ClimateRange.any())
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(0)
            .build();
        
        // =====================================================================
        // OCEAN - temperature only (with overlaps)
        // =====================================================================
        
        oceanBiomes.add(BiomeCandidate.builder(Biomes.FROZEN_OCEAN, biomeGetter)
            .temperature(-1.0, -0.4, -0.7)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        oceanBiomes.add(BiomeCandidate.builder(Biomes.COLD_OCEAN, biomeGetter)
            .temperature(-0.55, 0.0, -0.25)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        oceanBiomes.add(BiomeCandidate.builder(Biomes.OCEAN, biomeGetter)
            .temperature(-0.1, 0.4, 0.15)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        oceanBiomes.add(BiomeCandidate.builder(Biomes.LUKEWARM_OCEAN, biomeGetter)
            .temperature(0.3, 0.7, 0.5)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        oceanBiomes.add(BiomeCandidate.builder(Biomes.WARM_OCEAN, biomeGetter)
            .temperature(0.55, 1.0, 0.8)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        // =====================================================================
        // BEACH - flat coastline
        // =====================================================================
        
        beachBiomes.add(BiomeCandidate.builder(Biomes.SNOWY_BEACH, biomeGetter)
            .temperature(-1.0, -0.25, -0.55)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        beachBiomes.add(BiomeCandidate.builder(Biomes.BEACH, biomeGetter)
            .temperature(-0.35, 1.0, 0.3)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(8)
            .build());
        
        // MANGROVES - coastal wetland, hot + wet
        beachBiomes.add(BiomeCandidate.builder(Biomes.MANGROVE_SWAMP, biomeGetter)
            .temperature(0.4, 1.0, 0.7)
            .humidity(0.45, 1.0, 0.75)
            .precipitation(0.5, 1.0, 0.75)
            .priority(15)
            .build());
        
        // =====================================================================
        // CLIFF - rugged coastline
        // =====================================================================
        
        cliffBiomes.add(BiomeCandidate.builder(Biomes.STONY_SHORE, biomeGetter)
            .temperature(-1.0, 1.0, 0.0)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        // =====================================================================
        // RIVER - valleys
        // =====================================================================
        
        riverBiomes.add(BiomeCandidate.builder(Biomes.FROZEN_RIVER, biomeGetter)
            .temperature(-1.0, -0.25, -0.55)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        riverBiomes.add(BiomeCandidate.builder(Biomes.RIVER, biomeGetter)
            .temperature(-0.35, 1.0, 0.3)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        // =====================================================================
        // PEAK - mountain tops (temperature-driven, wide overlaps)
        // =====================================================================
        
        peakBiomes.add(BiomeCandidate.builder(Biomes.FROZEN_PEAKS, biomeGetter)
            .temperature(-1.0, -0.35, -0.65)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        peakBiomes.add(BiomeCandidate.builder(Biomes.JAGGED_PEAKS, biomeGetter)
            .temperature(-0.5, 0.0, -0.25)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        peakBiomes.add(BiomeCandidate.builder(Biomes.STONY_PEAKS, biomeGetter)
            .temperature(-0.1, 1.0, 0.35)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        // =====================================================================
        // SLOPE - mountain sides (WIDE OVERLAPPING RANGES)
        // =====================================================================
        
        // Snowy Slopes - cold + any moisture
        slopeBiomes.add(BiomeCandidate.builder(Biomes.SNOWY_SLOPES, biomeGetter)
            .temperature(-1.0, -0.15, -0.5)
            .humidity(0.0, 0.6, 0.3)
            .precipitation(0.0, 0.6, 0.3)
            .priority(10)
            .build());
        
        // Grove - cold + wet (forested snowy slopes)
        slopeBiomes.add(BiomeCandidate.builder(Biomes.GROVE, biomeGetter)
            .temperature(-0.7, 0.0, -0.35)
            .humidity(0.25, 1.0, 0.6)
            .precipitation(0.3, 1.0, 0.6)
            .priority(12)
            .build());
        
        // Meadow - temperate slopes, THE GENERIC SLOPE (wide range!)
        slopeBiomes.add(BiomeCandidate.builder(Biomes.MEADOW, biomeGetter)
            .temperature(-0.45, 0.5, 0.05)
            .humidity(0.15, 0.8, 0.45)
            .precipitation(0.15, 0.75, 0.4)
            .priority(8)  // Lower priority - specialized biomes win
            .build());
        
        // Cherry Grove - temperate + moderate-wet
        slopeBiomes.add(BiomeCandidate.builder(Biomes.CHERRY_GROVE, biomeGetter)
            .temperature(-0.2, 0.55, 0.2)
            .humidity(0.3, 0.85, 0.55)
            .precipitation(0.3, 0.8, 0.5)
            .priority(12)
            .build());

        slopeBiomes.add(BiomeCandidate.builder(Biomes.SPARSE_JUNGLE, biomeGetter)
            .temperature(0.45, 1.0, 0.65)
            .humidity(0.25, 0.75, 0.5)
            .precipitation(0.25, 0.65, 0.45)
            .priority(10)
            .build());
        
        // Savanna Plateau - warm + dry-moderate
        slopeBiomes.add(BiomeCandidate.builder(Biomes.SAVANNA_PLATEAU, biomeGetter)
            .temperature(0.2, 1.0, 0.55)
            .humidity(0.05, 0.55, 0.3)
            .precipitation(0.05, 0.5, 0.25)
            .priority(10)
            .build());
        
        // Badlands - hot + very dry
        slopeBiomes.add(BiomeCandidate.builder(Biomes.BADLANDS, biomeGetter)
            .temperature(0.3, 1.0, 0.65)
            .humidity(0.0, 0.4, 0.15)
            .precipitation(0.0, 0.3, 0.1)
            .priority(12)
            .build());
        
        // Wooded Badlands - hot + slightly less dry
        slopeBiomes.add(BiomeCandidate.builder(Biomes.WOODED_BADLANDS, biomeGetter)
            .temperature(0.25, 0.95, 0.55)
            .humidity(0.1, 0.5, 0.28)
            .precipitation(0.08, 0.4, 0.2)
            .priority(10)
            .build());
        
        // =====================================================================
        // SHATTERED - windswept terrain (WIDE RANGES)
        // =====================================================================
        
        // Windswept Hills - cool-temperate, moderate moisture
        shatteredBiomes.add(BiomeCandidate.builder(Biomes.WINDSWEPT_HILLS, biomeGetter)
            .temperature(-0.6, 0.6, -0.05)
            .humidity(0.0, 0.8, 0.35)
            .precipitation(0.0, 0.8, 0.35)
            .priority(8)
            .build());
        
        // Windswept Gravelly Hills - cool, drier
        shatteredBiomes.add(BiomeCandidate.builder(Biomes.WINDSWEPT_GRAVELLY_HILLS, biomeGetter)
            .temperature(-0.6, 0.3, -0.15)
            .humidity(0.0, 0.5, 0.25)
            .precipitation(0.05, 0.5, 0.25)
            .priority(8)
            .build());
        
        // Windswept Forest - temperate, wetter
        shatteredBiomes.add(BiomeCandidate.builder(Biomes.WINDSWEPT_FOREST, biomeGetter)
            .temperature(-0.45, 0.35, 0.0)
            .humidity(0.3, 0.8, 0.5)
            .precipitation(0.3, 0.75, 0.5)
            .priority(10)
            .build());
        
        // Windswept Savanna - hot, dry
        shatteredBiomes.add(BiomeCandidate.builder(Biomes.WINDSWEPT_SAVANNA, biomeGetter)
            .temperature(0.2, 1.0, 0.55)
            .humidity(0.0, 0.65, 0.25)
            .precipitation(0.0, 0.6, 0.2)
            .priority(10)
            .build());
        
        // =====================================================================
        // LAND - THE BIG ONE
        // ALL RANGES WIDENED AND OVERLAPPING!
        // =====================================================================
        
        // ===================
        // TROPICAL (hot)
        // ===================
        
        // Jungle - hot + wet (THE tropical wet forest)
        landBiomes.add(BiomeCandidate.builder(Biomes.JUNGLE, biomeGetter)
            .temperature(0.4, 1.0, 0.7)
            .humidity(0.4, 1.0, 0.7)
            .precipitation(0.5, 1.0, 0.75)
            .priority(12)
            .build());
        
        // Bamboo Jungle - hot + wet, variant
        landBiomes.add(BiomeCandidate.builder(Biomes.BAMBOO_JUNGLE, biomeGetter)
            .temperature(0.45, 1.0, 0.7)
            .humidity(0.45, 1.0, 0.7)
            .precipitation(0.45, 0.9, 0.65)
            .priority(10)
            .build());
        
        // Sparse Jungle - hot + moderate wet (jungle edge)
        landBiomes.add(BiomeCandidate.builder(Biomes.SPARSE_JUNGLE, biomeGetter)
            .temperature(0.5, 1.0, 0.7)
            .humidity(0.3, 0.7, 0.5)
            .precipitation(0.3, 0.6, 0.45)
            .priority(10)
            .build());
        
        // Savanna - warm-hot + dry-moderate (WIDE RANGE)
        landBiomes.add(BiomeCandidate.builder(Biomes.SAVANNA, biomeGetter)
            .temperature(0.35, 1.0, 0.55)
            .humidity(0.05, 0.55, 0.3)
            .precipitation(0.1, 0.5, 0.28)
            .priority(10)
            .build());

        // Savanna Plateau - drier than regular savanna
        landBiomes.add(BiomeCandidate.builder(Biomes.SAVANNA_PLATEAU, biomeGetter)
            .temperature(0.35, 1.0, 0.6)
            .humidity(0.0, 0.4, 0.2)
            .precipitation(0.05, 0.3, 0.15)  // Drier than savanna
            .priority(12)
            .build());
        
        // Desert - hot + very dry
        landBiomes.add(BiomeCandidate.builder(Biomes.DESERT, biomeGetter)
            .temperature(0.35, 1.0, 0.65)
            .humidity(0.0, 0.45, 0.15)
            .precipitation(0.0, 0.22, 0.08)
            .priority(14)
            .build());
        
        // Badlands - hot + extremely dry
        landBiomes.add(BiomeCandidate.builder(Biomes.BADLANDS, biomeGetter)
            .temperature(0.4, 1.0, 0.7)
            .humidity(0.0, 0.35, 0.1)
            .precipitation(0.0, 0.2, 0.05)
            .priority(14)
            .build());
        
        // Eroded Badlands - hottest + driest
        landBiomes.add(BiomeCandidate.builder(Biomes.ERODED_BADLANDS, biomeGetter)
            .temperature(0.5, 1.0, 0.75)
            .humidity(0.0, 0.3, 0.08)
            .precipitation(0.0, 0.15, 0.04)
            .priority(12)
            .build());
        
        // ===================
        // SUBTROPICAL (warm)
        // ===================
        
        // Forest - TEMPERATE DECIDUOUS (oak/mixed)
        // Warmer temperate, moderate-high moisture
        landBiomes.add(BiomeCandidate.builder(Biomes.FOREST, biomeGetter)
            .temperature(0.0, 0.55, 0.28)         // Warmer temperate
            .humidity(0.35, 0.85, 0.55)           // Humid air
            .precipitation(0.40, 0.80, 0.55)      // Good rainfall
            .priority(10)
            .build());

        // Flower Forest - warm + wetter
        landBiomes.add(BiomeCandidate.builder(Biomes.FLOWER_FOREST, biomeGetter)
            .temperature(0.05, 0.5, 0.28)
            .humidity(0.35, 0.85, 0.6)
            .precipitation(0.35, 0.75, 0.55)
            .priority(10)
            .build());
        
        // Dark Forest - warm + very wet
        landBiomes.add(BiomeCandidate.builder(Biomes.DARK_FOREST, biomeGetter)
            .temperature(0.0, 0.5, 0.25)
            .humidity(0.45, 1.0, 0.7)
            .precipitation(0.45, 0.9, 0.65)
            .priority(12)
            .build());
        
        // Swamp - warm + very wet + low areas
        landBiomes.add(BiomeCandidate.builder(Biomes.SWAMP, biomeGetter)
            .temperature(-0.1, 0.55, 0.2)
            .humidity(0.5, 1.0, 0.75)
            .precipitation(0.5, 1.0, 0.75)
            .priority(12)
            .build());
        
        // Plains - TEMPERATE GRASSLAND
        // Lower precip than forest, drier air (continental)
        // This is proxy for steppe, when there is no better candidate
        landBiomes.add(BiomeCandidate.builder(Biomes.PLAINS, biomeGetter)
            .temperature(-0.15, 0.45, 0.15)
            .humidity(0.05, 0.40, 0.22)
            .precipitation(0.15, 0.40, 0.28)
            .priority(10)
            .build());

        // Sunflower Plains - warmer, slightly wetter plains variant. Consider it Ukraine!
        landBiomes.add(BiomeCandidate.builder(Biomes.SUNFLOWER_PLAINS, biomeGetter)
            .temperature(0.05, 0.5, 0.28)
            .humidity(0.15, 0.50, 0.32)
            .precipitation(0.20, 0.48, 0.35)
            .priority(10)
            .build());
        
        // ===================
        // TEMPERATE (cool)
        // ===================
        
        // Birch Forest - COOL TEMPERATE / TRANSITIONAL
        // Cooler, tolerates drier/poorer conditions, toward boreal
        landBiomes.add(BiomeCandidate.builder(Biomes.BIRCH_FOREST, biomeGetter)
            .temperature(-0.35, 0.20, -0.08)
            .humidity(0.20, 0.70, 0.42)
            .precipitation(0.25, 0.70, 0.45)
            .priority(10)
            .build());

        // Old Growth Birch - same climate but wetter (old growth needs stability)
        landBiomes.add(BiomeCandidate.builder(Biomes.OLD_GROWTH_BIRCH_FOREST, biomeGetter)
            .temperature(-0.40, 0.15, -0.12)
            .humidity(0.40, 0.85, 0.58)
            .precipitation(0.40, 0.80, 0.55)
            .priority(12)                          // Wins over regular birch when wet
            .build());
        
        // ===================
        // BOREAL (cold)
        // ===================
        
        // Taiga - cold + moderate moisture (WIDE RANGE - main cold forest)
        landBiomes.add(BiomeCandidate.builder(Biomes.TAIGA, biomeGetter)
            .temperature(-0.6, 0.0, -0.3)
            .humidity(0.15, 0.75, 0.4)
            .precipitation(0.15, 0.7, 0.4)
            .priority(10)
            .build());
        
        // Old Growth Spruce Taiga - cold + wet
        landBiomes.add(BiomeCandidate.builder(Biomes.OLD_GROWTH_SPRUCE_TAIGA, biomeGetter)
            .temperature(-0.65, -0.05, -0.35)
            .humidity(0.35, 0.9, 0.6)
            .precipitation(0.35, 0.85, 0.55)
            .priority(12)
            .build());
        
        // Old Growth Pine Taiga - cold + moderate
        landBiomes.add(BiomeCandidate.builder(Biomes.OLD_GROWTH_PINE_TAIGA, biomeGetter)
            .temperature(-0.65, -0.05, -0.35)
            .humidity(0.2, 0.7, 0.45)
            .precipitation(0.2, 0.65, 0.4)
            .priority(10)
            .build());
        
        // ===================
        // POLAR (freezing) (-0.6 - -1.0)
        // ===================
        
        // Snowy Taiga - polar + moderate moisture
        landBiomes.add(BiomeCandidate.builder(Biomes.SNOWY_TAIGA, biomeGetter)
            .temperature(-1.0, -0.35, -0.65)
            .humidity(0.15, 0.7, 0.4)
            .precipitation(0.1, 0.65, 0.35)
            .priority(10)
            .build());
        
        // Snowy Plains - polar + dry-moderate (WIDENED)
        landBiomes.add(BiomeCandidate.builder(Biomes.SNOWY_PLAINS, biomeGetter)
            .temperature(-1.0, -0.3, -0.6)
            .humidity(0.0, 0.55, 0.25)
            .precipitation(0.0, 0.5, 0.2)
            .priority(8)
            .build());
        
        // Ice Spikes - very polar + dry
        landBiomes.add(BiomeCandidate.builder(Biomes.ICE_SPIKES, biomeGetter)
            .temperature(-1.0, -0.5, -0.75)
            .humidity(0.0, 0.4, 0.15)
            .precipitation(0.0, 0.35, 0.12)
            .priority(12)
            .build());
        
        // ===================
        // FALLBACK
        // ===================
        
        fallbackLand = BiomeCandidate.builder(Biomes.PLAINS, biomeGetter)
            .temperature(ClimateRange.any())
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(0)
            .build();
    }
    
    // ==================== ATMOSPHERIC BIOMES ====================
    
    private void registerAtmosphericBiomes() {
        if (!ModCompat.hasAtmospheric()) return;
        
        Sheetworld.LOGGER.info("Registering Atmospheric biomes...");
        
        // Rainforest - tropical wet
        landBiomes.add(BiomeCandidate.builder(moddedKey(ModCompat.ATMOSPHERIC, "rainforest"), biomeGetter)
            .mod(ModCompat.ATMOSPHERIC)
            .temperature(0.5, 1.0, 0.75)
            .humidity(0.55, 1.0, 0.8)
            .precipitation(0.65, 1.0, 0.85)
            .priority(20)
            .build());
        
        // Sparse Rainforest
        landBiomes.add(BiomeCandidate.builder(moddedKey(ModCompat.ATMOSPHERIC, "sparse_rainforest"), biomeGetter)
            .mod(ModCompat.ATMOSPHERIC)
            .temperature(0.4, 0.95, 0.65)
            .humidity(0.4, 0.85, 0.6)
            .precipitation(0.45, 0.8, 0.6)
            .priority(18)
            .build());
        
        // Rainforest Basin
        riverBiomes.add(BiomeCandidate.builder(moddedKey(ModCompat.ATMOSPHERIC, "rainforest_basin"), biomeGetter)
            .mod(ModCompat.ATMOSPHERIC)
            .temperature(0.5, 1.0, 0.75)
            .humidity(0.55, 1.0, 0.8)
            .precipitation(0.65, 1.0, 0.85)
            .priority(20)
            .build());
        
        // Sparse Rainforest Basin
        riverBiomes.add(BiomeCandidate.builder(moddedKey(ModCompat.ATMOSPHERIC, "sparse_rainforest_basin"), biomeGetter)
            .mod(ModCompat.ATMOSPHERIC)
            .temperature(0.4, 0.95, 0.65)
            .humidity(0.4, 0.85, 0.6)
            .precipitation(0.45, 0.8, 0.6)
            .priority(18)
            .build());
        
        // Dunes - hot desert
        landBiomes.add(BiomeCandidate.builder(moddedKey(ModCompat.ATMOSPHERIC, "dunes"), biomeGetter)
            .mod(ModCompat.ATMOSPHERIC)
            .temperature(0.5, 1.0, 0.75)
            .humidity(0.0, 0.3, 0.1)
            .precipitation(0.0, 0.18, 0.05)
            .priority(18)
            .build());
        
        // Rocky Dunes
        landBiomes.add(BiomeCandidate.builder(moddedKey(ModCompat.ATMOSPHERIC, "rocky_dunes"), biomeGetter)
            .mod(ModCompat.ATMOSPHERIC)
            .temperature(0.45, 1.0, 0.7)
            .humidity(0.0, 0.4, 0.18)
            .precipitation(0.0, 0.25, 0.1)
            .priority(16)
            .build());
        
        // Scrubland (warm + dry-moderate)
        // Very important. Forms the transition from desert to grassland.
        landBiomes.add(BiomeCandidate.builder(moddedKey(ModCompat.ATMOSPHERIC, "scrubland"), biomeGetter)
            .mod(ModCompat.ATMOSPHERIC)
            .temperature(0.15, 0.75, 0.45)
            .humidity(0.1, 0.5, 0.28)
            .precipitation(0.05, 0.38, 0.2)
            .priority(16)
            .build());
    }
    
    // ==================== ENVIRONMENTAL BIOMES ====================
    
    private void registerEnvironmentalBiomes() {
        if (!ModCompat.hasEnvironmental()) return;
        
        Sheetworld.LOGGER.info("Registering Environmental biomes...");
        
        // Marsh - wet land biome
        landBiomes.add(BiomeCandidate.builder(moddedKey(ModCompat.ENVIRONMENTAL, "marsh"), biomeGetter)
            .mod(ModCompat.ENVIRONMENTAL)
            .temperature(-0.15, 0.45, 0.15)
            .humidity(0.5, 1.0, 0.75)
            .precipitation(0.5, 0.95, 0.72)
            .priority(15)
            .build());
        
        landBiomes.add(BiomeCandidate.builder(moddedKey(ModCompat.ENVIRONMENTAL, "pine_barrens"), biomeGetter)
            .mod(ModCompat.ENVIRONMENTAL)
            .temperature(0.05, 0.55, 0.25)
            .humidity(0.2, 0.55, 0.3)
            .precipitation(0.2, 0.5, 0.28)
            .priority(15)
            .build());
    }
    
    // ==================== AUTUMNITY BIOMES ====================
    
    private void registerAutumnityBiomes() {
        if (!ModCompat.hasAutumnity()) return;
        
        Sheetworld.LOGGER.info("Registering Autumnity biomes...");
        
        // Maple Forest
        landBiomes.add(BiomeCandidate.builder(moddedKey(ModCompat.AUTUMNITY, "maple_forest"), biomeGetter)
            .mod(ModCompat.AUTUMNITY)
            .temperature(-0.2, 0.35, 0.08)
            .humidity(0.3, 0.75, 0.5)
            .precipitation(0.28, 0.68, 0.45)
            .priority(15)
            .build());
        
        // Maple Forest Hills - slope
        slopeBiomes.add(BiomeCandidate.builder(moddedKey(ModCompat.AUTUMNITY, "maple_forest_hills"), biomeGetter)
            .mod(ModCompat.AUTUMNITY)
            .temperature(-0.25, 0.3, 0.02)
            .humidity(0.25, 0.7, 0.45)
            .precipitation(0.22, 0.62, 0.4)
            .priority(12)
            .build());
        
        // Pumpkin Fields
        landBiomes.add(BiomeCandidate.builder(moddedKey(ModCompat.AUTUMNITY, "pumpkin_fields"), biomeGetter)
            .mod(ModCompat.AUTUMNITY)
            .temperature(-0.15, 0.3, 0.08)
            .humidity(0.18, 0.58, 0.35)
            .precipitation(0.15, 0.5, 0.32)
            .priority(18)
            .build());
    }
    
    // ==================== DEBUG ====================
    
    public String getSelectionDebug(double temp, double humid, double precip) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Climate: T=%.2f H=%.2f P=%.2f\n", temp, humid, precip));
        sb.append("Top land candidates:\n");
        
        landBiomes.stream()
            .filter(BiomeCandidate::isAvailable)
            .map(c -> new ScoredCandidate(c, c.getFitScore(temp, humid, precip)))
            .filter(sc -> sc.score > 0)
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .limit(5)
            .forEach(sc -> sb.append(String.format("  %.3f: %s%s\n", 
                sc.score, 
                sc.candidate.getBiomeId(),
                sc.candidate.isVanilla() ? "" : " [" + sc.candidate.getSourceMod() + "]")));
        
        return sb.toString();
    }
}
