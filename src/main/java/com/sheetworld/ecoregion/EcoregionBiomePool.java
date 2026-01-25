package com.sheetworld.ecoregion;

import com.sheetworld.Sheetworld;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.*;

/**
 * Manages biome pools for each ecoregion.
 * 
 * This is the SECOND level of biome selection:
 *   Ecoregion → Weighted Biome Selection → Specific Biome
 * 
 * Each ecoregion has a pool of candidate biomes with base weights.
 * The final selection considers:
 * - Base weight (how common is this biome in this ecoregion?)
 * - Local noise (deterministic variation based on position)
 * - Erosion (terrain roughness affects biome choice)
 * - Weirdness (vanilla parameter for variant selection)
 */
public class EcoregionBiomePool {
    
    private final Map<Ecoregion, List<PooledBiome>> pools = new EnumMap<>(Ecoregion.class);
    private final Map<Ecoregion, PooledBiome> fallbacks = new EnumMap<>(Ecoregion.class);
    private final HolderGetter<Biome> biomeGetter;
    
    // Initialize all pools empty
    {
        for (Ecoregion eco : Ecoregion.values()) {
            pools.put(eco, new ArrayList<>());
        }
    }
    
    public EcoregionBiomePool(HolderGetter<Biome> biomeGetter) {
        this.biomeGetter = biomeGetter;
        
        registerVanillaBiomes();
        
        Sheetworld.LOGGER.info("EcoregionBiomePool initialized with {} ecoregions", pools.size());
        for (Ecoregion eco : Ecoregion.values()) {
            Sheetworld.LOGGER.debug("  {}: {} biomes", eco.name(), pools.get(eco).size());
        }
    }
    
    /**
     * Register a biome to an ecoregion with a base weight.
     * Higher weights = more common in that ecoregion.
     */
    public void register(Ecoregion ecoregion, ResourceKey<Biome> biomeKey, int baseWeight) {
        register(ecoregion, biomeKey, baseWeight, null, BiomeModifiers.NONE);
    }
    
    public void register(Ecoregion ecoregion, ResourceKey<Biome> biomeKey, int baseWeight, 
                         String requiredMod, BiomeModifiers modifiers) {
        PooledBiome pooled = new PooledBiome(biomeKey, biomeGetter, baseWeight, requiredMod, modifiers);
        pools.get(ecoregion).add(pooled);
    }
    
    /**
     * Set the fallback biome for an ecoregion.
     * Used when no other biomes are available (e.g., mods not loaded).
     */
    public void setFallback(Ecoregion ecoregion, ResourceKey<Biome> biomeKey) {
        fallbacks.put(ecoregion, new PooledBiome(biomeKey, biomeGetter, 1, null, BiomeModifiers.NONE));
    }
    
    /**
     * Select a biome from the pool for the given ecoregion.
     * 
     * @param ecoregion     The ecoregion to select from
     * @param localNoise    Deterministic noise value for this position (-1 to 1)
     * @param erosion       Terrain erosion parameter (-1 to 1)
     * @param weirdness     Vanilla weirdness parameter (-1 to 1)
     * @return The selected biome holder
     */
    public Holder<Biome> selectBiome(Ecoregion ecoregion, double localNoise, double erosion, double weirdness) {
        List<PooledBiome> pool = pools.get(ecoregion);
        
        // Filter to available biomes and calculate adjusted weights
        List<WeightedSelection> candidates = pool.stream()
            .filter(PooledBiome::isAvailable)
            .map(pb -> new WeightedSelection(pb, pb.getAdjustedWeight(erosion, weirdness)))
            .filter(ws -> ws.weight > 0)
            .toList();
        
        if (candidates.isEmpty()) {
            // Use fallback
            PooledBiome fallback = fallbacks.get(ecoregion);
            if (fallback != null && fallback.isAvailable()) {
                return fallback.getBiome();
            }
            // Ultimate fallback - plains
            return biomeGetter.getOrThrow(Biomes.PLAINS);
        }
        
        // Calculate total weight
        double totalWeight = candidates.stream().mapToDouble(ws -> ws.weight).sum();
        
        // Use local noise to deterministically select
        // Map noise from [-1, 1] to [0, 1]
        double selector = (localNoise + 1.0) / 2.0;
        double threshold = selector * totalWeight;
        
        double cumulative = 0;
        for (WeightedSelection ws : candidates) {
            cumulative += ws.weight;
            if (cumulative >= threshold) {
                return ws.biome.getBiome();
            }
        }
        
        // Shouldn't reach here, but return last candidate
        return candidates.get(candidates.size() - 1).biome.getBiome();
    }
    
    // ==================== VANILLA BIOME REGISTRATION ====================
    
    private void registerVanillaBiomes() {
        
        // === ICE SHEET / POLAR DESERT ===
        register(Ecoregion.ICE_SHEET_AND_POLAR_DESERT, Biomes.ICE_SPIKES, 10);
        register(Ecoregion.ICE_SHEET_AND_POLAR_DESERT, Biomes.SNOWY_PLAINS, 5,
            null, BiomeModifiers.HIGH_EROSION_BOOST);  // Flat ice plains
        setFallback(Ecoregion.ICE_SHEET_AND_POLAR_DESERT, Biomes.ICE_SPIKES);
        
        // === TUNDRA ===
        register(Ecoregion.TUNDRA, Biomes.SNOWY_PLAINS, 10);
        register(Ecoregion.TUNDRA, Biomes.SNOWY_TAIGA, 5,
            null, BiomeModifiers.LOW_EROSION_BOOST);  // Sparse trees on rougher terrain
        setFallback(Ecoregion.TUNDRA, Biomes.SNOWY_PLAINS);
        
        // === TAIGA ===
        register(Ecoregion.TAIGA, Biomes.TAIGA, 10);
        register(Ecoregion.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, 5,
            null, BiomeModifiers.WEIRD_BOOST);  // Rare old growth
        register(Ecoregion.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, 5,
            null, BiomeModifiers.WEIRD_BOOST);
        register(Ecoregion.TAIGA, Biomes.SNOWY_TAIGA, 3,
            null, BiomeModifiers.LOW_EROSION_BOOST);  // Higher/colder spots
        setFallback(Ecoregion.TAIGA, Biomes.TAIGA);
        
        // === TEMPERATE BROADLEAF FOREST ===
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, Biomes.FOREST, 10);
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, Biomes.BIRCH_FOREST, 6);
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, 3,
            null, BiomeModifiers.WEIRD_BOOST);
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, Biomes.DARK_FOREST, 4,
            null, BiomeModifiers.LOW_EROSION_BOOST);  // Sheltered valleys
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, Biomes.FLOWER_FOREST, 2,
            null, BiomeModifiers.WEIRD_BOOST);
        setFallback(Ecoregion.TEMPERATE_BROADLEAF_FOREST, Biomes.FOREST);
        
        // === TEMPERATE STEPPE ===
        register(Ecoregion.TEMPERATE_STEPPE, Biomes.PLAINS, 10);
        register(Ecoregion.TEMPERATE_STEPPE, Biomes.SUNFLOWER_PLAINS, 3,
            null, BiomeModifiers.WEIRD_BOOST);  // Ukraine-style
        register(Ecoregion.TEMPERATE_STEPPE, Biomes.MEADOW, 4,
            null, BiomeModifiers.HIGH_EROSION_BOOST);  // Flatter areas
        setFallback(Ecoregion.TEMPERATE_STEPPE, Biomes.PLAINS);
        
        // === SUBTROPICAL MOIST FOREST ===
        register(Ecoregion.SUBTROPICAL_MOIST_FOREST, Biomes.FOREST, 6);  // Placeholder
        register(Ecoregion.SUBTROPICAL_MOIST_FOREST, Biomes.DARK_FOREST, 5);
        register(Ecoregion.SUBTROPICAL_MOIST_FOREST, Biomes.SWAMP, 4,
            null, BiomeModifiers.HIGH_EROSION_BOOST);  // Low-lying wet areas
        setFallback(Ecoregion.SUBTROPICAL_MOIST_FOREST, Biomes.FOREST);
        
        // === MEDITERRANEAN ===
        // Vanilla doesn't have great options here - using savanna as proxy
        register(Ecoregion.MEDITERRANEAN, Biomes.SAVANNA, 8);  // Proxy for scrubland
        register(Ecoregion.MEDITERRANEAN, Biomes.PLAINS, 5);
        register(Ecoregion.MEDITERRANEAN, Biomes.FOREST, 3,
            null, BiomeModifiers.LOW_EROSION_BOOST);  // Sheltered woodland
        setFallback(Ecoregion.MEDITERRANEAN, Biomes.SAVANNA);
        
        // === TROPICAL RAINFOREST ===
        register(Ecoregion.TROPICAL_RAINFOREST, Biomes.JUNGLE, 10);
        register(Ecoregion.TROPICAL_RAINFOREST, Biomes.BAMBOO_JUNGLE, 4,
            null, BiomeModifiers.WEIRD_BOOST);
        setFallback(Ecoregion.TROPICAL_RAINFOREST, Biomes.JUNGLE);
        
        // === TROPICAL MOIST BROADLEAF ===
        register(Ecoregion.TROPICAL_MOIST_BROADLEAF, Biomes.JUNGLE, 8);
        register(Ecoregion.TROPICAL_MOIST_BROADLEAF, Biomes.SPARSE_JUNGLE, 6);
        register(Ecoregion.TROPICAL_MOIST_BROADLEAF, Biomes.BAMBOO_JUNGLE, 3);
        setFallback(Ecoregion.TROPICAL_MOIST_BROADLEAF, Biomes.JUNGLE);
        
        // === TROPICAL DRY FOREST ===
        register(Ecoregion.TROPICAL_DRY_FOREST, Biomes.SPARSE_JUNGLE, 10);
        register(Ecoregion.TROPICAL_DRY_FOREST, Biomes.SAVANNA, 5);
        setFallback(Ecoregion.TROPICAL_DRY_FOREST, Biomes.SPARSE_JUNGLE);
        
        // === TREE SAVANNA ===
        register(Ecoregion.TREE_SAVANNA, Biomes.SAVANNA, 10);
        register(Ecoregion.TREE_SAVANNA, Biomes.SAVANNA_PLATEAU, 5,
            null, BiomeModifiers.LOW_EROSION_BOOST);
        register(Ecoregion.TREE_SAVANNA, Biomes.SPARSE_JUNGLE, 3);
        setFallback(Ecoregion.TREE_SAVANNA, Biomes.SAVANNA);
        
        // === GRASS SAVANNA ===
        register(Ecoregion.GRASS_SAVANNA, Biomes.SAVANNA, 10);
        register(Ecoregion.GRASS_SAVANNA, Biomes.PLAINS, 4);  // Transition
        setFallback(Ecoregion.GRASS_SAVANNA, Biomes.SAVANNA);
        
        // === ARID DESERT ===
        register(Ecoregion.ARID_DESERT, Biomes.DESERT, 10);
        setFallback(Ecoregion.ARID_DESERT, Biomes.DESERT);
        
        // === SEMIARID DESERT ===
        register(Ecoregion.SEMIARID_DESERT, Biomes.DESERT, 6);
        register(Ecoregion.SEMIARID_DESERT, Biomes.BADLANDS, 4,
            null, BiomeModifiers.LOW_EROSION_BOOST);  // Eroded areas
        setFallback(Ecoregion.SEMIARID_DESERT, Biomes.DESERT);
        
        // === XERIC SHRUBLAND ===
        register(Ecoregion.XERIC_SHRUBLAND, Biomes.BADLANDS, 8);
        register(Ecoregion.XERIC_SHRUBLAND, Biomes.WOODED_BADLANDS, 5);
        register(Ecoregion.XERIC_SHRUBLAND, Biomes.ERODED_BADLANDS, 3,
            null, BiomeModifiers.WEIRD_BOOST);
        setFallback(Ecoregion.XERIC_SHRUBLAND, Biomes.BADLANDS);
        
        // === DRY STEPPE ===
        register(Ecoregion.DRY_STEPPE, Biomes.PLAINS, 8);
        register(Ecoregion.DRY_STEPPE, Biomes.SAVANNA, 4);
        setFallback(Ecoregion.DRY_STEPPE, Biomes.PLAINS);
        
        // === ALPINE TUNDRA ===
        register(Ecoregion.ALPINE_TUNDRA, Biomes.JAGGED_PEAKS, 6);
        register(Ecoregion.ALPINE_TUNDRA, Biomes.STONY_PEAKS, 6);
        register(Ecoregion.ALPINE_TUNDRA, Biomes.FROZEN_PEAKS, 5,
            null, BiomeModifiers.LOW_EROSION_BOOST);  // Higher/rougher
        register(Ecoregion.ALPINE_TUNDRA, Biomes.SNOWY_SLOPES, 4);
        setFallback(Ecoregion.ALPINE_TUNDRA, Biomes.STONY_PEAKS);
        
        // === MONTANE FOREST ===
        register(Ecoregion.MONTANE_FOREST, Biomes.GROVE, 8);
        register(Ecoregion.MONTANE_FOREST, Biomes.MEADOW, 6);
        register(Ecoregion.MONTANE_FOREST, Biomes.SNOWY_SLOPES, 4);
        register(Ecoregion.MONTANE_FOREST, Biomes.CHERRY_GROVE, 2,
            null, BiomeModifiers.WEIRD_BOOST);
        setFallback(Ecoregion.MONTANE_FOREST, Biomes.GROVE);
        
        // === MANGROVE ===
        register(Ecoregion.MANGROVE, Biomes.MANGROVE_SWAMP, 10);
        setFallback(Ecoregion.MANGROVE, Biomes.MANGROVE_SWAMP);
        
        // === WETLAND ===
        register(Ecoregion.WETLAND, Biomes.SWAMP, 10);
        setFallback(Ecoregion.WETLAND, Biomes.SWAMP);
    }
    
    // ==================== MODDED BIOME REGISTRATION ====================
    
    /**
     * Register Atmospheric mod biomes.
     * Call this after checking ModCompat.hasAtmospheric()
     */
    public void registerAtmosphericBiomes() {
        String mod = "atmospheric";
        
        // Rainforest - better jungle for TROPICAL_RAINFOREST
        register(Ecoregion.TROPICAL_RAINFOREST, moddedKey(mod, "rainforest"), 15, mod, BiomeModifiers.NONE);
        register(Ecoregion.TROPICAL_MOIST_BROADLEAF, moddedKey(mod, "sparse_rainforest"), 12, mod, BiomeModifiers.NONE);
        
        // Dunes - proper desert dunes for ARID_DESERT
        register(Ecoregion.ARID_DESERT, moddedKey(mod, "dunes"), 12, mod, BiomeModifiers.NONE);
        register(Ecoregion.ARID_DESERT, moddedKey(mod, "rocky_dunes"), 8, mod, BiomeModifiers.LOW_EROSION_BOOST);
        
        // Scrubland - perfect for MEDITERRANEAN and XERIC_SHRUBLAND
        register(Ecoregion.MEDITERRANEAN, moddedKey(mod, "scrubland"), 15, mod, BiomeModifiers.NONE);
        register(Ecoregion.XERIC_SHRUBLAND, moddedKey(mod, "scrubland"), 10, mod, BiomeModifiers.NONE);
        
        // Rosewood - subtropical forest
        register(Ecoregion.SUBTROPICAL_MOIST_FOREST, moddedKey(mod, "rosewood_forest"), 12, mod, BiomeModifiers.NONE);
        
        Sheetworld.LOGGER.info("Registered Atmospheric biomes to ecoregion pools");
    }
    
    /**
     * Register Environmental mod biomes.
     */
    public void registerEnvironmentalBiomes() {
        String mod = "environmental";
        
        // Marsh - wetland
        register(Ecoregion.WETLAND, moddedKey(mod, "marsh"), 12, mod, BiomeModifiers.NONE);
        
        // Blossom woods - temperate forest
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, moddedKey(mod, "blossom_woods"), 8, mod, BiomeModifiers.WEIRD_BOOST);
        register(Ecoregion.SUBTROPICAL_MOIST_FOREST, moddedKey(mod, "blossom_woods"), 6, mod, BiomeModifiers.NONE);
        
        Sheetworld.LOGGER.info("Registered Environmental biomes to ecoregion pools");
    }
    
    /**
     * Register Autumnity mod biomes.
     */
    public void registerAutumnityBiomes() {
        String mod = "autumnity";
        
        // Maple forest - temperate deciduous
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, moddedKey(mod, "maple_forest"), 8, mod, BiomeModifiers.NONE);
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, moddedKey(mod, "maple_forest_hills"), 4, mod, BiomeModifiers.LOW_EROSION_BOOST);
        
        // Pumpkin fields - temperate steppe
        register(Ecoregion.TEMPERATE_STEPPE, moddedKey(mod, "pumpkin_fields"), 6, mod, BiomeModifiers.WEIRD_BOOST);
        
        Sheetworld.LOGGER.info("Registered Autumnity biomes to ecoregion pools");
    }
    
    // ==================== HELPERS ====================
    
    private ResourceKey<Biome> moddedKey(String modId, String path) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(modId, path));
    }
    
    /**
     * A biome in a pool with its weight and modifiers.
     */
    private static class PooledBiome {
        private final ResourceKey<Biome> biomeKey;
        private final HolderGetter<Biome> biomeGetter;
        private final int baseWeight;
        private final String requiredMod;
        private final BiomeModifiers modifiers;
        
        private Holder<Biome> cachedBiome;
        private boolean availabilityChecked = false;
        private boolean isAvailable = false;
        
        PooledBiome(ResourceKey<Biome> biomeKey, HolderGetter<Biome> biomeGetter, 
                   int baseWeight, String requiredMod, BiomeModifiers modifiers) {
            this.biomeKey = biomeKey;
            this.biomeGetter = biomeGetter;
            this.baseWeight = baseWeight;
            this.requiredMod = requiredMod;
            this.modifiers = modifiers;
        }
        
        boolean isAvailable() {
            if (!availabilityChecked) {
                availabilityChecked = true;
                try {
                    cachedBiome = biomeGetter.getOrThrow(biomeKey);
                    isAvailable = true;
                } catch (Exception e) {
                    isAvailable = false;
                }
            }
            return isAvailable;
        }
        
        Holder<Biome> getBiome() {
            if (!availabilityChecked) {
                isAvailable();
            }
            return cachedBiome;
        }
        
        double getAdjustedWeight(double erosion, double weirdness) {
            double weight = baseWeight;
            
            switch (modifiers) {
                case HIGH_EROSION_BOOST:
                    // Boost for smooth/flat terrain
                    if (erosion > 0.3) weight *= 1.5;
                    break;
                case LOW_EROSION_BOOST:
                    // Boost for rough terrain
                    if (erosion < -0.2) weight *= 1.5;
                    break;
                case WEIRD_BOOST:
                    // Boost for high weirdness (rare variants)
                    if (Math.abs(weirdness) > 0.5) weight *= 2.0;
                    break;
                case NONE:
                default:
                    break;
            }
            
            return weight;
        }
    }
    
    private static class WeightedSelection {
        final PooledBiome biome;
        final double weight;
        
        WeightedSelection(PooledBiome biome, double weight) {
            this.biome = biome;
            this.weight = weight;
        }
    }
    
    /**
     * Modifiers that affect biome weight based on terrain parameters.
     */
    public enum BiomeModifiers {
        NONE,
        HIGH_EROSION_BOOST,   // Favors smooth/flat terrain
        LOW_EROSION_BOOST,    // Favors rough terrain
        WEIRD_BOOST           // Favors high weirdness (rare)
    }
    
    // ==================== DEBUG ====================
    
    public String getPoolDebug(Ecoregion ecoregion) {
        List<PooledBiome> pool = pools.get(ecoregion);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Ecoregion: %s\n", ecoregion.getDisplayName()));
        sb.append(String.format("Pool size: %d\n", pool.size()));
        
        for (PooledBiome pb : pool) {
            sb.append(String.format("  %s [weight=%d, available=%s]\n",
                pb.biomeKey.location(),
                pb.baseWeight,
                pb.isAvailable() ? "yes" : "no"));
        }
        
        return sb.toString();
    }
}
