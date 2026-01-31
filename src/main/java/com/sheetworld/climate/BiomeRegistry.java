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

/**
 * Climate-based biome registry and selection system.
 * 
 * Biomes are registered with their climate niches (temperature, humidity, precipitation).
 * Selection picks the best-fitting available biome for given climate conditions.
 * 
 * IMPORTANT: All biome resolution is LAZY to avoid load-order crashes with mods
 * like Blueprint that register biomes after our registry is constructed.
 */
public class BiomeRegistry {
    
    private final List<BiomeCandidate> landCandidates = new ArrayList<>();
    private final List<BiomeCandidate> oceanCandidates = new ArrayList<>();
    private final List<BiomeCandidate> coastCandidates = new ArrayList<>();
    
    private final HolderGetter<Biome> biomeGetter;
    
    // Lazy fallbacks
    private BiomeCandidate fallbackLandCandidate;
    private BiomeCandidate fallbackOceanCandidate;
    private BiomeCandidate fallbackCoastCandidate;
    
    public BiomeRegistry(HolderGetter<Biome> biomeGetter) {
        this.biomeGetter = biomeGetter;
        
        // Initialize mod compat detection
        ModCompat.init();
        
        // Register all biome CANDIDATES (no resolution yet!)
        registerVanillaBiomes();
        registerAtmosphericBiomes();
        registerEnvironmentalBiomes();
        registerAutumnityBiomes();
        
        Sheetworld.LOGGER.info("BiomeRegistry initialized with {} land, {} ocean, {} coast candidates (lazy resolution)",
            landCandidates.size(), oceanCandidates.size(), coastCandidates.size());
    }
    
    // ==================== BIOME SELECTION ====================
    
    public Holder<Biome> selectLandBiome(double temperature, double humidity, double precipitation) {
        Holder<Biome> fallback = fallbackLandCandidate != null ? fallbackLandCandidate.getBiome() : null;
        return selectBiome(landCandidates, temperature, humidity, precipitation, fallback);
    }
    
    public Holder<Biome> selectOceanBiome(double temperature, double humidity, double precipitation) {
        Holder<Biome> fallback = fallbackOceanCandidate != null ? fallbackOceanCandidate.getBiome() : null;
        return selectBiome(oceanCandidates, temperature, humidity, precipitation, fallback);
    }
    
    public Holder<Biome> selectCoastBiome(double temperature, double humidity, double precipitation) {
        Holder<Biome> fallback = fallbackCoastCandidate != null ? fallbackCoastCandidate.getBiome() : null;
        return selectBiome(coastCandidates, temperature, humidity, precipitation, fallback);
    }

    // ==================== TERRAIN BIOME SELECTORS ====================
    // These are called by TerrainBiomeSelector for specific terrain categories

    public Holder<Biome> selectMushroomBiome(double temp) {
        return biomeGetter.getOrThrow(Biomes.MUSHROOM_FIELDS);
    }

    public Holder<Biome> selectDeepOceanBiome(double temp) {
        if (temp > 0.2) return biomeGetter.getOrThrow(Biomes.DEEP_LUKEWARM_OCEAN);
        if (temp > -0.2) return biomeGetter.getOrThrow(Biomes.DEEP_OCEAN);
        if (temp > -0.5) return biomeGetter.getOrThrow(Biomes.DEEP_COLD_OCEAN);
        return biomeGetter.getOrThrow(Biomes.DEEP_FROZEN_OCEAN);
    }

    public Holder<Biome> selectOceanBiome(double temp) {
        if (temp > 0.5) return biomeGetter.getOrThrow(Biomes.WARM_OCEAN);
        if (temp > 0.2) return biomeGetter.getOrThrow(Biomes.LUKEWARM_OCEAN);
        if (temp > -0.2) return biomeGetter.getOrThrow(Biomes.OCEAN);
        if (temp > -0.5) return biomeGetter.getOrThrow(Biomes.COLD_OCEAN);
        return biomeGetter.getOrThrow(Biomes.FROZEN_OCEAN);
    }

    public Holder<Biome> selectRiverBiome(double temp, double humid, double precip) {
        if (temp < -0.3) return biomeGetter.getOrThrow(Biomes.FROZEN_RIVER);
        return biomeGetter.getOrThrow(Biomes.RIVER);
    }

    public Holder<Biome> selectPeakBiome(double temp, double humid, double precip) {
        if (temp < -0.3) return biomeGetter.getOrThrow(Biomes.FROZEN_PEAKS);
        if (temp < 0.3) return biomeGetter.getOrThrow(Biomes.JAGGED_PEAKS);
        return biomeGetter.getOrThrow(Biomes.STONY_PEAKS);
    }

    public Holder<Biome> selectSlopeBiome(double temp, double humid, double precip) {
        if (temp < -0.3) return biomeGetter.getOrThrow(Biomes.SNOWY_SLOPES);
        if (temp < 0.0 && precip > 0.4) return biomeGetter.getOrThrow(Biomes.GROVE);
        if (temp < 0.3) return biomeGetter.getOrThrow(Biomes.MEADOW);
        return biomeGetter.getOrThrow(Biomes.SAVANNA_PLATEAU);
    }

    public Holder<Biome> selectShatteredBiome(double temp, double humid, double precip) {
        if (temp < -0.2) return biomeGetter.getOrThrow(Biomes.WINDSWEPT_GRAVELLY_HILLS);
        if (temp < 0.3 && precip > 0.4) return biomeGetter.getOrThrow(Biomes.WINDSWEPT_FOREST);
        return biomeGetter.getOrThrow(Biomes.WINDSWEPT_HILLS);
    }

    public Holder<Biome> selectCliffBiome(double temp, double humid, double precip) {
        return biomeGetter.getOrThrow(Biomes.STONY_SHORE);
    }

    public Holder<Biome> selectBeachBiome(double temp, double humid, double precip) {
        if (temp < -0.3) return biomeGetter.getOrThrow(Biomes.SNOWY_BEACH);
        if (temp > 0.5 && precip > 0.6) return biomeGetter.getOrThrow(Biomes.MANGROVE_SWAMP);
        return biomeGetter.getOrThrow(Biomes.BEACH);
    }

    private Holder<Biome> selectBiome(List<BiomeCandidate> candidates, 
                                       double temperature, double humidity, double precipitation,
                                       Holder<Biome> fallback) {
        Optional<BiomeCandidate> best = candidates.stream()
            .filter(BiomeCandidate::isAvailable)  // This now includes lazy resolution check
            .map(c -> new ScoredCandidate(c, c.getFitScore(temperature, humidity, precipitation)))
            .filter(sc -> sc.score > 0)
            .max(Comparator.comparingDouble(sc -> sc.score))
            .map(sc -> sc.candidate);
        
        if (best.isPresent()) {
            Holder<Biome> biome = best.get().getBiome();
            if (biome != null) {
                return biome;
            }
        }
        return fallback;
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
    
    /**
     * Create a resource key for a modded biome.
     */
    private ResourceKey<Biome> moddedKey(String modId, String path) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(modId, path));
    }
    
    private void registerLand(BiomeCandidate candidate) {
        landCandidates.add(candidate);
    }
    
    private void registerOcean(BiomeCandidate candidate) {
        oceanCandidates.add(candidate);
    }
    
    private void registerCoast(BiomeCandidate candidate) {
        coastCandidates.add(candidate);
    }
    
    // ==================== VANILLA BIOMES ====================
    
    private void registerVanillaBiomes() {
        
        // ======================================================================
        // TROPICAL (temp > 0.5)
        // ======================================================================
        
        registerLand(BiomeCandidate.builder(Biomes.MANGROVE_SWAMP, biomeGetter)
            .temperature(0.5, 1.0, 0.75)
            .humidity(0.5, 1.0, 0.7)
            .precipitation(0.7, 1.0, 0.85)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.JUNGLE, biomeGetter)
            .temperature(0.5, 1.0, 0.75)
            .humidity(0.4, 1.0, 0.6)
            .precipitation(0.6, 1.0, 0.75)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.BAMBOO_JUNGLE, biomeGetter)
            .temperature(0.55, 1.0, 0.75)
            .humidity(0.45, 1.0, 0.65)
            .precipitation(0.55, 0.9, 0.7)
            .priority(8)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.SPARSE_JUNGLE, biomeGetter)
            .temperature(0.45, 1.0, 0.65)
            .humidity(0.3, 0.7, 0.5)
            .precipitation(0.35, 0.6, 0.45)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.SAVANNA, biomeGetter)
            .temperature(0.35, 1.0, 0.6)
            .humidity(0.15, 0.5, 0.35)
            .precipitation(0.15, 0.4, 0.28)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.SAVANNA_PLATEAU, biomeGetter)
            .temperature(0.4, 1.0, 0.65)
            .humidity(0.1, 0.45, 0.3)
            .precipitation(0.1, 0.35, 0.2)
            .priority(8)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.DESERT, biomeGetter)
            .temperature(0.4, 1.0, 0.7)
            .humidity(0.0, 0.45, 0.25)
            .precipitation(0.0, 0.2, 0.1)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.BADLANDS, biomeGetter)
            .temperature(0.45, 1.0, 0.7)
            .humidity(0.0, 0.3, 0.15)
            .precipitation(0.0, 0.15, 0.05)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.ERODED_BADLANDS, biomeGetter)
            .temperature(0.5, 1.0, 0.8)
            .humidity(0.0, 0.25, 0.1)
            .precipitation(0.0, 0.12, 0.04)
            .priority(8)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.WOODED_BADLANDS, biomeGetter)
            .temperature(0.4, 0.9, 0.6)
            .humidity(0.1, 0.4, 0.25)
            .precipitation(0.1, 0.28, 0.18)
            .priority(8)
            .build());
        
        // ======================================================================
        // SUBTROPICAL (temp 0.2 to 0.5)
        // ======================================================================
        
        registerLand(BiomeCandidate.builder(Biomes.SWAMP, biomeGetter)
            .temperature(0.1, 0.55, 0.35)
            .humidity(0.5, 1.0, 0.75)
            .precipitation(0.55, 1.0, 0.7)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.DARK_FOREST, biomeGetter)
            .temperature(0.1, 0.5, 0.3)
            .humidity(0.45, 0.85, 0.65)
            .precipitation(0.45, 0.75, 0.6)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.FOREST, biomeGetter)
            .temperature(0.05, 0.5, 0.28)
            .humidity(0.35, 0.7, 0.5)
            .precipitation(0.35, 0.6, 0.48)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.FLOWER_FOREST, biomeGetter)
            .temperature(0.1, 0.45, 0.28)
            .humidity(0.4, 0.7, 0.55)
            .precipitation(0.3, 0.55, 0.42)
            .priority(8)
            .build());
        
        // Plains - wide fallback
        BiomeCandidate plains = BiomeCandidate.builder(Biomes.PLAINS, biomeGetter)
            .temperature(-0.1, 0.5, 0.2)
            .humidity(0.2, 0.55, 0.38)
            .precipitation(0.2, 0.45, 0.32)
            .priority(5)
            .build();
        registerLand(plains);
        fallbackLandCandidate = plains;
        
        registerLand(BiomeCandidate.builder(Biomes.SUNFLOWER_PLAINS, biomeGetter)
            .temperature(0.05, 0.45, 0.25)
            .humidity(0.3, 0.6, 0.45)
            .precipitation(0.25, 0.5, 0.38)
            .priority(6)
            .build());
        
        // ======================================================================
        // TEMPERATE (temp -0.2 to 0.2)
        // ======================================================================
        
        registerLand(BiomeCandidate.builder(Biomes.OLD_GROWTH_BIRCH_FOREST, biomeGetter)
            .temperature(-0.2, 0.25, 0.0)
            .humidity(0.45, 0.8, 0.6)
            .precipitation(0.5, 0.8, 0.62)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.BIRCH_FOREST, biomeGetter)
            .temperature(-0.2, 0.25, 0.0)
            .humidity(0.35, 0.65, 0.5)
            .precipitation(0.38, 0.6, 0.48)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.CHERRY_GROVE, biomeGetter)
            .temperature(-0.15, 0.3, 0.08)
            .humidity(0.35, 0.65, 0.5)
            .precipitation(0.32, 0.55, 0.42)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.MEADOW, biomeGetter)
            .temperature(-0.25, 0.25, 0.0)
            .humidity(0.25, 0.55, 0.4)
            .precipitation(0.22, 0.45, 0.32)
            .priority(10)
            .build());
        
        // Dry temperate - fills gap
        registerLand(BiomeCandidate.builder(Biomes.WINDSWEPT_HILLS, biomeGetter)
            .temperature(-0.3, 0.2, -0.05)
            .humidity(0.15, 0.45, 0.3)
            .precipitation(0.15, 0.35, 0.25)
            .priority(8)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.WINDSWEPT_GRAVELLY_HILLS, biomeGetter)
            .temperature(-0.35, 0.15, -0.1)
            .humidity(0.1, 0.35, 0.22)
            .precipitation(0.1, 0.28, 0.18)
            .priority(8)
            .build());
        
        // ======================================================================
        // BOREAL (temp -0.6 to -0.2)
        // ======================================================================
        
        registerLand(BiomeCandidate.builder(Biomes.OLD_GROWTH_SPRUCE_TAIGA, biomeGetter)
            .temperature(-0.6, -0.15, -0.38)
            .humidity(0.4, 0.8, 0.55)
            .precipitation(0.45, 0.75, 0.55)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.OLD_GROWTH_PINE_TAIGA, biomeGetter)
            .temperature(-0.6, -0.15, -0.38)
            .humidity(0.3, 0.65, 0.45)
            .precipitation(0.32, 0.58, 0.42)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.TAIGA, biomeGetter)
            .temperature(-0.55, -0.1, -0.32)
            .humidity(0.25, 0.55, 0.4)
            .precipitation(0.25, 0.5, 0.35)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.WINDSWEPT_FOREST, biomeGetter)
            .temperature(-0.5, -0.05, -0.28)
            .humidity(0.2, 0.5, 0.35)
            .precipitation(0.18, 0.4, 0.28)
            .priority(8)
            .build());
        
        // ======================================================================
        // POLAR (temp < -0.6)
        // ======================================================================
        
        registerLand(BiomeCandidate.builder(Biomes.GROVE, biomeGetter)
            .temperature(-1.0, -0.5, -0.72)
            .humidity(0.3, 0.7, 0.48)
            .precipitation(0.3, 0.6, 0.42)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.SNOWY_TAIGA, biomeGetter)
            .temperature(-1.0, -0.5, -0.75)
            .humidity(0.2, 0.55, 0.38)
            .precipitation(0.2, 0.48, 0.32)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.SNOWY_PLAINS, biomeGetter)
            .temperature(-1.0, -0.45, -0.7)
            .humidity(0.1, 0.45, 0.28)
            .precipitation(0.1, 0.35, 0.22)
            .priority(8)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.SNOWY_SLOPES, biomeGetter)
            .temperature(-1.0, -0.4, -0.65)
            .humidity(0.15, 0.5, 0.32)
            .precipitation(0.15, 0.42, 0.28)
            .priority(8)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.ICE_SPIKES, biomeGetter)
            .temperature(-1.0, -0.6, -0.85)
            .humidity(0.0, 0.3, 0.12)
            .precipitation(0.0, 0.2, 0.08)
            .priority(10)
            .build());
        
        registerLand(BiomeCandidate.builder(Biomes.FROZEN_PEAKS, biomeGetter)
            .temperature(-1.0, -0.55, -0.8)
            .humidity(0.05, 0.35, 0.18)
            .precipitation(0.05, 0.25, 0.12)
            .priority(8)
            .build());
        
        // ======================================================================
        // OCEAN BIOMES
        // ======================================================================
        
        registerOcean(BiomeCandidate.builder(Biomes.WARM_OCEAN, biomeGetter)
            .temperature(0.5, 1.0, 0.75)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        registerOcean(BiomeCandidate.builder(Biomes.LUKEWARM_OCEAN, biomeGetter)
            .temperature(0.2, 0.5, 0.35)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        BiomeCandidate ocean = BiomeCandidate.builder(Biomes.OCEAN, biomeGetter)
            .temperature(-0.2, 0.2, 0.0)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build();
        registerOcean(ocean);
        fallbackOceanCandidate = ocean;
        
        registerOcean(BiomeCandidate.builder(Biomes.COLD_OCEAN, biomeGetter)
            .temperature(-0.6, -0.2, -0.4)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        registerOcean(BiomeCandidate.builder(Biomes.FROZEN_OCEAN, biomeGetter)
            .temperature(-1.0, -0.6, -0.8)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        registerOcean(BiomeCandidate.builder(Biomes.DEEP_LUKEWARM_OCEAN, biomeGetter)
            .temperature(0.2, 0.6, 0.4)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(8)
            .build());
        
        registerOcean(BiomeCandidate.builder(Biomes.DEEP_OCEAN, biomeGetter)
            .temperature(-0.25, 0.25, 0.0)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(8)
            .build());
        
        registerOcean(BiomeCandidate.builder(Biomes.DEEP_COLD_OCEAN, biomeGetter)
            .temperature(-0.65, -0.15, -0.4)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(8)
            .build());
        
        registerOcean(BiomeCandidate.builder(Biomes.DEEP_FROZEN_OCEAN, biomeGetter)
            .temperature(-1.0, -0.55, -0.75)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(8)
            .build());
        
        // ======================================================================
        // COAST BIOMES
        // ======================================================================
        
        BiomeCandidate beach = BiomeCandidate.builder(Biomes.BEACH, biomeGetter)
            .temperature(-0.35, 1.0, 0.3)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build();
        registerCoast(beach);
        fallbackCoastCandidate = beach;
        
        registerCoast(BiomeCandidate.builder(Biomes.SNOWY_BEACH, biomeGetter)
            .temperature(-1.0, -0.35, -0.6)
            .humidity(ClimateRange.any())
            .precipitation(ClimateRange.any())
            .priority(10)
            .build());
        
        registerCoast(BiomeCandidate.builder(Biomes.STONY_SHORE, biomeGetter)
            .temperature(-0.3, 0.6, 0.15)
            .humidity(0.0, 0.4, 0.2)
            .precipitation(0.0, 0.35, 0.15)
            .priority(8)
            .build());
    }
    
    // ==================== ATMOSPHERIC BIOMES ====================
    
    private void registerAtmosphericBiomes() {
        if (!ModCompat.hasAtmospheric()) return;
        
        Sheetworld.LOGGER.info("Registering Atmospheric biome candidates (lazy)...");
        
        // Rainforest
        registerLand(BiomeCandidate.builder(moddedKey(ModCompat.ATMOSPHERIC, "rainforest"), biomeGetter)
            .mod(ModCompat.ATMOSPHERIC)
            .temperature(0.55, 1.0, 0.78)
            .humidity(0.55, 1.0, 0.75)
            .precipitation(0.7, 1.0, 0.85)
            .priority(20)
            .build());
        
        // Sparse Rainforest
        registerLand(BiomeCandidate.builder(moddedKey(ModCompat.ATMOSPHERIC, "sparse_rainforest"), biomeGetter)
            .mod(ModCompat.ATMOSPHERIC)
            .temperature(0.45, 0.9, 0.68)
            .humidity(0.4, 0.75, 0.58)
            .precipitation(0.5, 0.75, 0.62)
            .priority(18)
            .build());
        
        // Dunes
        registerLand(BiomeCandidate.builder(moddedKey(ModCompat.ATMOSPHERIC, "dunes"), biomeGetter)
            .mod(ModCompat.ATMOSPHERIC)
            .temperature(0.55, 1.0, 0.8)
            .humidity(0.0, 0.3, 0.12)
            .precipitation(0.0, 0.15, 0.05)
            .priority(18)
            .build());
        
        // Rocky Dunes
        registerLand(BiomeCandidate.builder(moddedKey(ModCompat.ATMOSPHERIC, "rocky_dunes"), biomeGetter)
            .mod(ModCompat.ATMOSPHERIC)
            .temperature(0.5, 1.0, 0.72)
            .humidity(0.05, 0.35, 0.18)
            .precipitation(0.02, 0.2, 0.1)
            .priority(15)
            .build());
        
        // Scrubland - CRITICAL gap filler
        registerLand(BiomeCandidate.builder(moddedKey(ModCompat.ATMOSPHERIC, "scrubland"), biomeGetter)
            .mod(ModCompat.ATMOSPHERIC)
            .temperature(0.25, 0.7, 0.48)
            .humidity(0.12, 0.42, 0.28)
            .precipitation(0.1, 0.32, 0.2)
            .priority(20)
            .fillsGap()
            .build());
        
        // Rosewood Forest
        registerLand(BiomeCandidate.builder(moddedKey(ModCompat.ATMOSPHERIC, "rosewood_forest"), biomeGetter)
            .mod(ModCompat.ATMOSPHERIC)
            .temperature(0.15, 0.55, 0.35)
            .humidity(0.4, 0.72, 0.55)
            .precipitation(0.38, 0.62, 0.5)
            .priority(15)
            .build());
    }
    
    // ==================== ENVIRONMENTAL BIOMES ====================
    
    private void registerEnvironmentalBiomes() {
        if (!ModCompat.hasEnvironmental()) return;
        
        Sheetworld.LOGGER.info("Registering Environmental biome candidates (lazy)...");
        
        // Marsh
        registerLand(BiomeCandidate.builder(moddedKey(ModCompat.ENVIRONMENTAL, "marsh"), biomeGetter)
            .mod(ModCompat.ENVIRONMENTAL)
            .temperature(-0.05, 0.4, 0.18)
            .humidity(0.55, 1.0, 0.75)
            .precipitation(0.55, 0.88, 0.7)
            .priority(18)
            .build());
        
        // Blossom Woods
        registerLand(BiomeCandidate.builder(moddedKey(ModCompat.ENVIRONMENTAL, "blossom_woods"), biomeGetter)
            .mod(ModCompat.ENVIRONMENTAL)
            .temperature(0.1, 0.45, 0.28)
            .humidity(0.45, 0.75, 0.6)
            .precipitation(0.4, 0.65, 0.52)
            .priority(15)
            .build());
        
        // Blossom Hills
        registerLand(BiomeCandidate.builder(moddedKey(ModCompat.ENVIRONMENTAL, "blossom_hills"), biomeGetter)
            .mod(ModCompat.ENVIRONMENTAL)
            .temperature(0.05, 0.4, 0.22)
            .humidity(0.4, 0.7, 0.55)
            .precipitation(0.35, 0.6, 0.48)
            .priority(12)
            .build());
    }
    
    // ==================== AUTUMNITY BIOMES ====================
    
    private void registerAutumnityBiomes() {
        if (!ModCompat.hasAutumnity()) return;
        
        Sheetworld.LOGGER.info("Registering Autumnity biome candidates (lazy)...");
        
        // Maple Forest
        registerLand(BiomeCandidate.builder(moddedKey(ModCompat.AUTUMNITY, "maple_forest"), biomeGetter)
            .mod(ModCompat.AUTUMNITY)
            .temperature(-0.15, 0.28, 0.08)
            .humidity(0.35, 0.65, 0.5)
            .precipitation(0.32, 0.58, 0.45)
            .priority(15)
            .build());
        
        // Maple Forest Hills
        registerLand(BiomeCandidate.builder(moddedKey(ModCompat.AUTUMNITY, "maple_forest_hills"), biomeGetter)
            .mod(ModCompat.AUTUMNITY)
            .temperature(-0.2, 0.22, 0.02)
            .humidity(0.3, 0.6, 0.45)
            .precipitation(0.28, 0.52, 0.4)
            .priority(12)
            .build());
        
        // Pumpkin Fields - gap filler
        registerLand(BiomeCandidate.builder(moddedKey(ModCompat.AUTUMNITY, "pumpkin_fields"), biomeGetter)
            .mod(ModCompat.AUTUMNITY)
            .temperature(-0.1, 0.25, 0.08)
            .humidity(0.22, 0.5, 0.35)
            .precipitation(0.2, 0.42, 0.3)
            .priority(18)
            .fillsGap()
            .build());
    }
    
    // ==================== DEBUG ====================
    
    public String getSelectionDebug(double temperature, double humidity, double precipitation) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Climate: T=%.2f H=%.2f P=%.2f\n", temperature, humidity, precipitation));
        sb.append("Top candidates:\n");
        
        landCandidates.stream()
            .filter(BiomeCandidate::isAvailable)
            .map(c -> new ScoredCandidate(c, c.getFitScore(temperature, humidity, precipitation)))
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
