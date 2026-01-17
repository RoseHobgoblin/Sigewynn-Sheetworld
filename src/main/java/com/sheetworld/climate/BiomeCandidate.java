package com.sheetworld.climate;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Represents a biome candidate for climate-based selection.
 * 
 * Each candidate defines:
 * - The biome it represents (resolved LAZILY to avoid load order issues)
 * - The climate niche where it belongs (temperature, humidity, precipitation ranges)
 * - Priority for selection when multiple biomes could fit
 * - Optional source mod for conditional availability
 * 
 * IMPORTANT: Biome holders are resolved lazily on first access to avoid
 * crashes when modded biomes aren't registered yet during world creation.
 */
public class BiomeCandidate {
    
    private final ResourceKey<Biome> biomeKey;
    private final ResourceLocation biomeId;
    
    @Nullable
    private final String sourceMod;  // null = vanilla/always available
    
    private final ClimateRange temperatureRange;
    private final ClimateRange humidityRange;
    private final ClimateRange precipitationRange;
    
    private final int priority;
    private final boolean fillsGap;
    
    // Lazy resolution
    private final HolderGetter<Biome> biomeGetter;
    @Nullable
    private Holder<Biome> resolvedBiome = null;
    private boolean resolutionAttempted = false;
    
    private BiomeCandidate(Builder builder) {
        this.biomeKey = builder.biomeKey;
        this.biomeId = builder.biomeKey.location();
        this.sourceMod = builder.sourceMod;
        this.temperatureRange = builder.temperatureRange;
        this.humidityRange = builder.humidityRange;
        this.precipitationRange = builder.precipitationRange;
        this.priority = builder.priority;
        this.fillsGap = builder.fillsGap;
        this.biomeGetter = builder.biomeGetter;
    }
    
    /**
     * Get the biome holder, resolving lazily on first access.
     * Returns null if the biome couldn't be resolved (mod not loaded, etc.)
     */
    @Nullable
    public Holder<Biome> getBiome() {
        if (resolvedBiome == null) {
            try {
                resolvedBiome = biomeGetter.get(biomeKey).orElse(null);
            } catch (Exception ignored) {
                // swallow
            }
        }
        return resolvedBiome;
    }
    
    /**
     * Calculate how well this biome fits the given climate.
     * 
     * @return Fit score from 0.0 (doesn't fit) to 1.0+ (perfect fit with priority)
     */
    public double getFitScore(double temperature, double humidity, double precipitation) {
        double tempFit = temperatureRange.getSoftFit(temperature, 0.1);
        double humidFit = humidityRange.getSoftFit(humidity, 0.1);
        double precipFit = precipitationRange.getSoftFit(precipitation, 0.1);
        
        if (tempFit <= 0 || humidFit <= 0 || precipFit <= 0) {
            return 0.0;
        }
        
        double baseFit = Math.cbrt(tempFit * humidFit * precipFit);
        double priorityBonus = priority * 0.01;
        
        return baseFit + priorityBonus;
    }
    
    /**
     * Check if this candidate's biome is available.
     * This checks BOTH mod availability AND whether the biome can be resolved.
     */
    public boolean isAvailable() {
        // First check if the mod is loaded (for modded biomes)
        if (sourceMod != null && !ModCompat.isModLoaded(sourceMod)) {
            return false;
        }
        // Then check if the biome can actually be resolved
        return getBiome() != null;
    }
    
    /**
     * Check if climate values fall within this biome's ranges (hard check).
     */
    public boolean containsClimate(double temperature, double humidity, double precipitation) {
        return temperatureRange.contains(temperature) 
            && humidityRange.contains(humidity)
            && precipitationRange.contains(precipitation);
    }
    
    // === Getters ===
    
    public ResourceKey<Biome> getBiomeKey() { return biomeKey; }
    public ResourceLocation getBiomeId() { return biomeId; }
    @Nullable public String getSourceMod() { return sourceMod; }
    public ClimateRange getTemperatureRange() { return temperatureRange; }
    public ClimateRange getHumidityRange() { return humidityRange; }
    public ClimateRange getPrecipitationRange() { return precipitationRange; }
    public int getPriority() { return priority; }
    public boolean fillsGap() { return fillsGap; }
    
    public boolean isVanilla() { return sourceMod == null; }
    
    @Override
    public String toString() {
        return String.format("BiomeCandidate{%s, mod=%s, priority=%d, T=%s, H=%s, P=%s}",
            biomeId, sourceMod == null ? "vanilla" : sourceMod, priority,
            temperatureRange, humidityRange, precipitationRange);
    }
    
    // === Builder ===
    
    public static Builder builder(ResourceKey<Biome> biomeKey, HolderGetter<Biome> biomeGetter) {
        return new Builder(biomeKey, biomeGetter);
    }
    
    public static class Builder {
        private final ResourceKey<Biome> biomeKey;
        private final HolderGetter<Biome> biomeGetter;
        private String sourceMod = null;
        private ClimateRange temperatureRange = ClimateRange.any();
        private ClimateRange humidityRange = ClimateRange.any();
        private ClimateRange precipitationRange = ClimateRange.any();
        private int priority = 0;
        private boolean fillsGap = false;
        
        private Builder(ResourceKey<Biome> biomeKey, HolderGetter<Biome> biomeGetter) {
            this.biomeKey = biomeKey;
            this.biomeGetter = biomeGetter;
        }
        
        public Builder mod(String modId) {
            this.sourceMod = modId;
            return this;
        }
        
        public Builder temperature(ClimateRange range) {
            this.temperatureRange = range;
            return this;
        }
        
        public Builder temperature(double min, double max) {
            this.temperatureRange = new ClimateRange(min, max);
            return this;
        }
        
        public Builder temperature(double min, double max, double ideal) {
            this.temperatureRange = new ClimateRange(min, max, ideal);
            return this;
        }
        
        public Builder humidity(ClimateRange range) {
            this.humidityRange = range;
            return this;
        }
        
        public Builder humidity(double min, double max) {
            this.humidityRange = new ClimateRange(min, max);
            return this;
        }
        
        public Builder humidity(double min, double max, double ideal) {
            this.humidityRange = new ClimateRange(min, max, ideal);
            return this;
        }
        
        public Builder precipitation(ClimateRange range) {
            this.precipitationRange = range;
            return this;
        }
        
        public Builder precipitation(double min, double max) {
            this.precipitationRange = new ClimateRange(min, max);
            return this;
        }
        
        public Builder precipitation(double min, double max, double ideal) {
            this.precipitationRange = new ClimateRange(min, max, ideal);
            return this;
        }
        
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder fillsGap() {
            this.fillsGap = true;
            return this;
        }
        
        public BiomeCandidate build() {
            return new BiomeCandidate(this);
        }
    }
}
