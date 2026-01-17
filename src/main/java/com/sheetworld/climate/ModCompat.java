package com.sheetworld.climate;

import com.sheetworld.Sheetworld;
import net.neoforged.fml.ModList;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles mod detection and compatibility.
 * 
 * Call ModCompat.init() during mod initialization to detect available mods.
 */
public class ModCompat {
    
    // Known mod IDs
    public static final String ATMOSPHERIC = "atmospheric";
    public static final String ENVIRONMENTAL = "environmental";
    public static final String AUTUMNITY = "autumnity";
    public static final String NEAPOLITAN = "neapolitan";
    public static final String UPGRADE_AQUATIC = "upgrade_aquatic";
    public static final String BUZZIER_BEES = "buzzier_bees";
    public static final String BIOMES_O_PLENTY = "biomesoplenty";
    public static final String REGIONS_UNEXPLORED = "regions_unexplored";
    
    private static final Map<String, Boolean> loadedMods = new HashMap<>();
    private static boolean initialized = false;
    
    /**
     * Initialize mod detection. Call during mod setup.
     */
    public static void init() {
        if (initialized) return;
        
        // Check all known mods
        checkMod(ATMOSPHERIC);
        checkMod(ENVIRONMENTAL);
        checkMod(AUTUMNITY);
        checkMod(NEAPOLITAN);
        checkMod(UPGRADE_AQUATIC);
        checkMod(BUZZIER_BEES);
        checkMod(BIOMES_O_PLENTY);
        checkMod(REGIONS_UNEXPLORED);
        
        initialized = true;
        
        // Log detected mods
        StringBuilder sb = new StringBuilder("Sheetworld mod compatibility: ");
        boolean any = false;
        for (Map.Entry<String, Boolean> entry : loadedMods.entrySet()) {
            if (entry.getValue()) {
                if (any) sb.append(", ");
                sb.append(entry.getKey());
                any = true;
            }
        }
        if (!any) {
            sb.append("(no compatible mods detected)");
        }
        Sheetworld.LOGGER.info(sb.toString());
    }
    
    private static void checkMod(String modId) {
        boolean loaded = ModList.get().isLoaded(modId);
        loadedMods.put(modId, loaded);
    }
    
    /**
     * Check if a mod is loaded.
     */
    public static boolean isModLoaded(String modId) {
        if (!initialized) {
            // Fallback to direct check if not initialized
            return ModList.get().isLoaded(modId);
        }
        return loadedMods.getOrDefault(modId, false);
    }
    
    // === Convenience methods ===
    
    public static boolean hasAtmospheric() {
        return isModLoaded(ATMOSPHERIC);
    }
    
    public static boolean hasEnvironmental() {
        return isModLoaded(ENVIRONMENTAL);
    }
    
    public static boolean hasAutumnity() {
        return isModLoaded(AUTUMNITY);
    }
    
    public static boolean hasNeapolitan() {
        return isModLoaded(NEAPOLITAN);
    }
    
    public static boolean hasUpgradeAquatic() {
        return isModLoaded(UPGRADE_AQUATIC);
    }
    
    public static boolean hasBuzzierBees() {
        return isModLoaded(BUZZIER_BEES);
    }
    
    public static boolean hasBiomesOPlenty() {
        return isModLoaded(BIOMES_O_PLENTY);
    }
    
    public static boolean hasRegionsUnexplored() {
        return isModLoaded(REGIONS_UNEXPLORED);
    }
}
