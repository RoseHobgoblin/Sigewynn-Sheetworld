package com.sheetworld.ecoregion;

import com.sheetworld.Sheetworld;
import com.sheetworld.climate.ClimateRange;
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
 *   Ecoregion → Climate Fit Scoring → Specific Biome
 *
 * Each ecoregion has a pool of candidate biomes with climate preferences.
 * The final selection picks the biome with the best climate fit score,
 * similar to how BiomeRegistry works but within an ecoregion's pool.
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
     * Register a biome to an ecoregion with climate preferences.
     */
    public void register(Ecoregion ecoregion, ResourceKey<Biome> biomeKey, ClimatePrefs prefs) {
        register(ecoregion, biomeKey, prefs, null);
    }

    public void register(Ecoregion ecoregion, ResourceKey<Biome> biomeKey, ClimatePrefs prefs, String requiredMod) {
        PooledBiome pooled = new PooledBiome(biomeKey, biomeGetter, prefs, requiredMod);
        pools.get(ecoregion).add(pooled);
    }

    /**
     * Set the fallback biome for an ecoregion.
     * Used when no other biomes match or are available.
     */
    public void setFallback(Ecoregion ecoregion, ResourceKey<Biome> biomeKey) {
        fallbacks.put(ecoregion, new PooledBiome(biomeKey, biomeGetter, ClimatePrefs.any(), null));
    }
    
    /**
     * Select a biome from the pool for the given ecoregion.
     *
     * Uses climate fit scoring to pick the best biome from the pool.
     * The biome with the highest fit score wins - deterministic, no randomness.
     *
     * @param ecoregion     The ecoregion to select from
     * @param temp          Temperature (-1 polar to +1 equatorial)
     * @param humid         Humidity (0 to 1)
     * @param precip        Precipitation (0 desert to 1 rainforest)
     * @return The selected biome holder
     */
    public Holder<Biome> selectBiome(Ecoregion ecoregion, double temp, double humid, double precip) {
        List<PooledBiome> pool = pools.get(ecoregion);

        PooledBiome best = null;
        double bestScore = -1;

        for (PooledBiome pb : pool) {
            if (pb.isAvailable()) {
                double score = pb.getFitScore(temp, humid, precip);
                if (score > bestScore) {
                    bestScore = score;
                    best = pb;
                }
            }
        }

        if (best != null) {
            return best.getBiome();
        }

        // Fallback if no biome scored positively
        PooledBiome fallback = fallbacks.get(ecoregion);
        if (fallback != null && fallback.isAvailable()) {
            return fallback.getBiome();
        }

        // Ultimate fallback - plains
        return biomeGetter.getOrThrow(Biomes.PLAINS);
    }
    
    // ==================== VANILLA BIOME REGISTRATION ====================

    private void registerVanillaBiomes() {

        // === ICE SHEET / POLAR DESERT ===
        register(Ecoregion.ICE_SHEET_AND_POLAR_DESERT, Biomes.ICE_SPIKES, ClimatePrefs.generic());
        register(Ecoregion.ICE_SHEET_AND_POLAR_DESERT, Biomes.SNOWY_PLAINS, ClimatePrefs.dry());
        setFallback(Ecoregion.ICE_SHEET_AND_POLAR_DESERT, Biomes.ICE_SPIKES);

        // === TUNDRA ===
        register(Ecoregion.TUNDRA, Biomes.SNOWY_PLAINS, ClimatePrefs.generic());
        register(Ecoregion.TUNDRA, Biomes.SNOWY_TAIGA, ClimatePrefs.wet());
        setFallback(Ecoregion.TUNDRA, Biomes.SNOWY_PLAINS);

        // === TAIGA ===
        register(Ecoregion.TAIGA, Biomes.TAIGA, ClimatePrefs.generic());
        register(Ecoregion.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, ClimatePrefs.veryWet());
        register(Ecoregion.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, ClimatePrefs.wet());
        register(Ecoregion.TAIGA, Biomes.SNOWY_TAIGA, ClimatePrefs.cool());
        setFallback(Ecoregion.TAIGA, Biomes.TAIGA);

        // === TEMPERATE BROADLEAF FOREST ===
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, Biomes.FOREST, ClimatePrefs.generic());
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, Biomes.BIRCH_FOREST, ClimatePrefs.cool());
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST,
            ClimatePrefs.of(new ClimateRange(-0.4, 0.1, -0.1), ClimateRange.wet(), ClimateRange.wet(), 14));
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, Biomes.DARK_FOREST, ClimatePrefs.veryWet());
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, Biomes.FLOWER_FOREST,
            ClimatePrefs.of(ClimateRange.any(), ClimateRange.moderate(), ClimateRange.moderate(), 10));
        setFallback(Ecoregion.TEMPERATE_BROADLEAF_FOREST, Biomes.FOREST);

        // === TEMPERATE STEPPE ===
        register(Ecoregion.TEMPERATE_STEPPE, Biomes.PLAINS, ClimatePrefs.generic());
        register(Ecoregion.TEMPERATE_STEPPE, Biomes.SUNFLOWER_PLAINS, ClimatePrefs.warm());
        register(Ecoregion.TEMPERATE_STEPPE, Biomes.MEADOW, ClimatePrefs.wet());
        setFallback(Ecoregion.TEMPERATE_STEPPE, Biomes.PLAINS);

        // === SUBTROPICAL MOIST FOREST ===
        register(Ecoregion.SUBTROPICAL_MOIST_FOREST, Biomes.FOREST, ClimatePrefs.generic());
        register(Ecoregion.SUBTROPICAL_MOIST_FOREST, Biomes.DARK_FOREST, ClimatePrefs.veryWet());
        register(Ecoregion.SUBTROPICAL_MOIST_FOREST, Biomes.SWAMP,
            ClimatePrefs.of(ClimateRange.any(), ClimateRange.veryWet(), ClimateRange.veryWet(), 15));
        setFallback(Ecoregion.SUBTROPICAL_MOIST_FOREST, Biomes.FOREST);

        // === MEDITERRANEAN ===
        register(Ecoregion.MEDITERRANEAN, Biomes.SAVANNA, ClimatePrefs.generic());
        register(Ecoregion.MEDITERRANEAN, Biomes.PLAINS, ClimatePrefs.wet());
        register(Ecoregion.MEDITERRANEAN, Biomes.FOREST, ClimatePrefs.veryWet());
        setFallback(Ecoregion.MEDITERRANEAN, Biomes.SAVANNA);

        // === TROPICAL RAINFOREST ===
        register(Ecoregion.TROPICAL_RAINFOREST, Biomes.JUNGLE, ClimatePrefs.generic());
        register(Ecoregion.TROPICAL_RAINFOREST, Biomes.BAMBOO_JUNGLE, ClimatePrefs.wet());
        setFallback(Ecoregion.TROPICAL_RAINFOREST, Biomes.JUNGLE);

        // === TROPICAL MOIST BROADLEAF ===
        register(Ecoregion.TROPICAL_MOIST_BROADLEAF, Biomes.JUNGLE, ClimatePrefs.veryWet());
        register(Ecoregion.TROPICAL_MOIST_BROADLEAF, Biomes.SPARSE_JUNGLE, ClimatePrefs.generic());
        register(Ecoregion.TROPICAL_MOIST_BROADLEAF, Biomes.BAMBOO_JUNGLE, ClimatePrefs.wet());
        setFallback(Ecoregion.TROPICAL_MOIST_BROADLEAF, Biomes.JUNGLE);

        // === TROPICAL DRY FOREST ===
        register(Ecoregion.TROPICAL_DRY_FOREST, Biomes.SPARSE_JUNGLE, ClimatePrefs.generic());
        register(Ecoregion.TROPICAL_DRY_FOREST, Biomes.SAVANNA, ClimatePrefs.dry());
        setFallback(Ecoregion.TROPICAL_DRY_FOREST, Biomes.SPARSE_JUNGLE);

        // === TREE SAVANNA ===
        register(Ecoregion.TREE_SAVANNA, Biomes.SAVANNA, ClimatePrefs.generic());
        register(Ecoregion.TREE_SAVANNA, Biomes.SAVANNA_PLATEAU, ClimatePrefs.dry());
        register(Ecoregion.TREE_SAVANNA, Biomes.SPARSE_JUNGLE, ClimatePrefs.wet());
        setFallback(Ecoregion.TREE_SAVANNA, Biomes.SAVANNA);

        // === GRASS SAVANNA ===
        register(Ecoregion.GRASS_SAVANNA, Biomes.SAVANNA, ClimatePrefs.generic());
        register(Ecoregion.GRASS_SAVANNA, Biomes.PLAINS, ClimatePrefs.cool());
        setFallback(Ecoregion.GRASS_SAVANNA, Biomes.SAVANNA);

        // === ARID DESERT ===
        register(Ecoregion.ARID_DESERT, Biomes.DESERT, ClimatePrefs.generic());
        setFallback(Ecoregion.ARID_DESERT, Biomes.DESERT);

        // === SEMIARID DESERT ===
        register(Ecoregion.SEMIARID_DESERT, Biomes.DESERT, ClimatePrefs.veryDry());
        register(Ecoregion.SEMIARID_DESERT, Biomes.BADLANDS, ClimatePrefs.dry());
        setFallback(Ecoregion.SEMIARID_DESERT, Biomes.DESERT);

        // === XERIC SHRUBLAND ===
        register(Ecoregion.XERIC_SHRUBLAND, Biomes.BADLANDS, ClimatePrefs.generic());
        register(Ecoregion.XERIC_SHRUBLAND, Biomes.WOODED_BADLANDS, ClimatePrefs.wet());
        register(Ecoregion.XERIC_SHRUBLAND, Biomes.ERODED_BADLANDS, ClimatePrefs.veryDry());
        setFallback(Ecoregion.XERIC_SHRUBLAND, Biomes.BADLANDS);

        // === DRY STEPPE ===
        register(Ecoregion.DRY_STEPPE, Biomes.PLAINS, ClimatePrefs.generic());
        register(Ecoregion.DRY_STEPPE, Biomes.SAVANNA, ClimatePrefs.warm());
        setFallback(Ecoregion.DRY_STEPPE, Biomes.PLAINS);

        // === ALPINE TUNDRA ===
        register(Ecoregion.ALPINE_TUNDRA, Biomes.JAGGED_PEAKS, ClimatePrefs.cool());
        register(Ecoregion.ALPINE_TUNDRA, Biomes.STONY_PEAKS, ClimatePrefs.warm());
        register(Ecoregion.ALPINE_TUNDRA, Biomes.FROZEN_PEAKS,
            ClimatePrefs.of(new ClimateRange(-1.0, -0.3, -0.6), ClimateRange.any(), ClimateRange.any(), 14));
        register(Ecoregion.ALPINE_TUNDRA, Biomes.SNOWY_SLOPES, ClimatePrefs.wet());
        setFallback(Ecoregion.ALPINE_TUNDRA, Biomes.STONY_PEAKS);

        // === MONTANE FOREST ===
        register(Ecoregion.MONTANE_FOREST, Biomes.GROVE, ClimatePrefs.wet());
        register(Ecoregion.MONTANE_FOREST, Biomes.MEADOW, ClimatePrefs.generic());
        register(Ecoregion.MONTANE_FOREST, Biomes.SNOWY_SLOPES, ClimatePrefs.cool());
        register(Ecoregion.MONTANE_FOREST, Biomes.CHERRY_GROVE,
            ClimatePrefs.of(new ClimateRange(-0.1, 0.4, 0.15), ClimateRange.moderate(), ClimateRange.moderate(), 12));
        setFallback(Ecoregion.MONTANE_FOREST, Biomes.GROVE);

        // === MANGROVE ===
        register(Ecoregion.MANGROVE, Biomes.MANGROVE_SWAMP, ClimatePrefs.generic());
        setFallback(Ecoregion.MANGROVE, Biomes.MANGROVE_SWAMP);

        // === WETLAND ===
        register(Ecoregion.WETLAND, Biomes.SWAMP, ClimatePrefs.generic());
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
        register(Ecoregion.TROPICAL_RAINFOREST, moddedKey(mod, "rainforest"), ClimatePrefs.modded(), mod);
        register(Ecoregion.TROPICAL_MOIST_BROADLEAF, moddedKey(mod, "sparse_rainforest"), ClimatePrefs.modded(), mod);

        // Dunes - proper desert dunes for ARID_DESERT
        register(Ecoregion.ARID_DESERT, moddedKey(mod, "dunes"), ClimatePrefs.modded(), mod);
        register(Ecoregion.ARID_DESERT, moddedKey(mod, "rocky_dunes"),
            ClimatePrefs.modded(ClimateRange.any(), ClimateRange.arid(), ClimateRange.arid()), mod);

        // Scrubland - perfect for MEDITERRANEAN and XERIC_SHRUBLAND
        register(Ecoregion.MEDITERRANEAN, moddedKey(mod, "scrubland"), ClimatePrefs.modded(), mod);
        register(Ecoregion.XERIC_SHRUBLAND, moddedKey(mod, "flourishing_dunes"), ClimatePrefs.modded(), mod);

        Sheetworld.LOGGER.info("Registered Atmospheric biomes to ecoregion pools");
    }

    /**
     * Register Environmental mod biomes.
     */
    public void registerEnvironmentalBiomes() {
        String mod = "environmental";

        // Marsh - wetland
        register(Ecoregion.WETLAND, moddedKey(mod, "marsh"), ClimatePrefs.modded(), mod);

        // Blossom woods - temperate forest
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, moddedKey(mod, "blossom_woods"),
            ClimatePrefs.modded(ClimateRange.any(), ClimateRange.moderate(), ClimateRange.moderate()), mod);
        register(Ecoregion.SUBTROPICAL_MOIST_FOREST, moddedKey(mod, "blossom_woods"), ClimatePrefs.modded(), mod);

        Sheetworld.LOGGER.info("Registered Environmental biomes to ecoregion pools");
    }

    /**
     * Register Autumnity mod biomes.
     */
    public void registerAutumnityBiomes() {
        String mod = "autumnity";

        // Maple forest - temperate deciduous
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, moddedKey(mod, "maple_forest"), ClimatePrefs.modded(), mod);
        register(Ecoregion.TEMPERATE_BROADLEAF_FOREST, moddedKey(mod, "maple_forest_hills"),
            ClimatePrefs.modded(new ClimateRange(-0.3, 0.2, 0.0), ClimateRange.any(), ClimateRange.any()), mod);

        // Pumpkin fields - temperate steppe
        register(Ecoregion.TEMPERATE_STEPPE, moddedKey(mod, "pumpkin_fields"), ClimatePrefs.modded(), mod);

        Sheetworld.LOGGER.info("Registered Autumnity biomes to ecoregion pools");
    }
    
    // ==================== HELPERS ====================

    private ResourceKey<Biome> moddedKey(String modId, String path) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(modId, path));
    }

    /**
     * Climate preferences for a pooled biome.
     * Defines the ideal temp/humid/precip ranges within an ecoregion.
     */
    public static class ClimatePrefs {
        private final ClimateRange temp;
        private final ClimateRange humid;
        private final ClimateRange precip;
        private final int priority;

        private ClimatePrefs(ClimateRange temp, ClimateRange humid, ClimateRange precip, int priority) {
            this.temp = temp;
            this.humid = humid;
            this.precip = precip;
            this.priority = priority;
        }

        /**
         * Calculate how well this biome fits the given climate.
         * Returns priority * (average of temp/humid/precip fit scores)
         */
        public double getFitScore(double t, double h, double p) {
            double tempFit = temp.getSoftFit(t, 0.15);
            double humidFit = humid.getSoftFit(h, 0.15);
            double precipFit = precip.getSoftFit(p, 0.15);

            if (tempFit <= 0 || humidFit <= 0 || precipFit <= 0) {
                return 0.0;
            }

            return priority * (tempFit + humidFit + precipFit) / 3.0;
        }

        // === Factory methods ===

        /** Accepts any climate - lowest priority fallback */
        public static ClimatePrefs any() {
            return new ClimatePrefs(ClimateRange.any(), ClimateRange.any(), ClimateRange.any(), 1);
        }

        /** Standard biome with default priority */
        public static ClimatePrefs of(ClimateRange temp, ClimateRange humid, ClimateRange precip) {
            return new ClimatePrefs(temp, humid, precip, 10);
        }

        /** Standard biome with custom priority */
        public static ClimatePrefs of(ClimateRange temp, ClimateRange humid, ClimateRange precip, int priority) {
            return new ClimatePrefs(temp, humid, precip, priority);
        }

        // === Common presets for ecoregion-relative preferences ===

        /** Generic/default for the ecoregion - wide ranges, lower priority */
        public static ClimatePrefs generic() {
            return new ClimatePrefs(ClimateRange.any(), ClimateRange.any(), ClimateRange.any(), 8);
        }

        /** Wetter variant within ecoregion */
        public static ClimatePrefs wet() {
            return new ClimatePrefs(ClimateRange.any(), ClimateRange.wet(), ClimateRange.wet(), 12);
        }

        /** Drier variant within ecoregion */
        public static ClimatePrefs dry() {
            return new ClimatePrefs(ClimateRange.any(), ClimateRange.semiArid(), ClimateRange.semiArid(), 12);
        }

        /** Cooler variant within ecoregion */
        public static ClimatePrefs cool() {
            return new ClimatePrefs(new ClimateRange(-1.0, 0.0, -0.3), ClimateRange.any(), ClimateRange.any(), 12);
        }

        /** Warmer variant within ecoregion */
        public static ClimatePrefs warm() {
            return new ClimatePrefs(new ClimateRange(0.0, 1.0, 0.3), ClimateRange.any(), ClimateRange.any(), 12);
        }

        /** Very wet - highest humidity/precip */
        public static ClimatePrefs veryWet() {
            return new ClimatePrefs(ClimateRange.any(), ClimateRange.veryWet(), ClimateRange.veryWet(), 14);
        }

        /** Very dry - lowest humidity/precip */
        public static ClimatePrefs veryDry() {
            return new ClimatePrefs(ClimateRange.any(), ClimateRange.arid(), ClimateRange.arid(), 14);
        }

        /** Rare variant - low priority, appears occasionally */
        public static ClimatePrefs rare() {
            return new ClimatePrefs(ClimateRange.any(), ClimateRange.any(), ClimateRange.any(), 5);
        }

        /** Modded biome - higher priority to prefer over vanilla */
        public static ClimatePrefs modded() {
            return new ClimatePrefs(ClimateRange.any(), ClimateRange.any(), ClimateRange.any(), 15);
        }

        /** Modded biome with specific climate preferences */
        public static ClimatePrefs modded(ClimateRange temp, ClimateRange humid, ClimateRange precip) {
            return new ClimatePrefs(temp, humid, precip, 18);
        }
    }

    /**
     * A biome in a pool with its climate preferences.
     */
    private static class PooledBiome {
        private final ResourceKey<Biome> biomeKey;
        private final HolderGetter<Biome> biomeGetter;
        private final ClimatePrefs prefs;
        private final String requiredMod;

        private Holder<Biome> cachedBiome;
        private boolean availabilityChecked = false;
        private boolean isAvailable = false;

        PooledBiome(ResourceKey<Biome> biomeKey, HolderGetter<Biome> biomeGetter,
                    ClimatePrefs prefs, String requiredMod) {
            this.biomeKey = biomeKey;
            this.biomeGetter = biomeGetter;
            this.prefs = prefs;
            this.requiredMod = requiredMod;
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

        double getFitScore(double temp, double humid, double precip) {
            return prefs.getFitScore(temp, humid, precip);
        }
    }
    
    // ==================== DEBUG ====================

    public String getPoolDebug(Ecoregion ecoregion) {
        List<PooledBiome> pool = pools.get(ecoregion);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Ecoregion: %s\n", ecoregion.getDisplayName()));
        sb.append(String.format("Pool size: %d\n", pool.size()));

        for (PooledBiome pb : pool) {
            sb.append(String.format("  %s [priority=%d, available=%s]\n",
                pb.biomeKey.location(),
                pb.prefs.priority,
                pb.isAvailable() ? "yes" : "no"));
        }

        return sb.toString();
    }
}
