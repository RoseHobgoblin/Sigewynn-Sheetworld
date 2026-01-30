package com.sheetworld.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

/**
 * Per-world settings for Sheetworld generation.
 *
 * These settings are serialized into the world's level.dat and can be
 * customized via the world creation UI. Unlike TerrainConfig (which holds
 * global defaults), these settings are specific to each world.
 */
public record SheetworldSettings(
        WorldSize worldSize,
        double verticalScale,
        double rainShadowStrength,
        double mountainHeight,
        boolean junglePillars,
        boolean rollingHills,
        boolean dunes,
        boolean badlandsRidges
) {

    public static final Codec<SheetworldSettings> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            WorldSize.CODEC.optionalFieldOf("world_size", WorldSize.MEDIUM).forGetter(SheetworldSettings::worldSize),
            Codec.DOUBLE.optionalFieldOf("vertical_scale", 1.0).forGetter(SheetworldSettings::verticalScale),
            Codec.DOUBLE.optionalFieldOf("rain_shadow_strength", 0.7).forGetter(SheetworldSettings::rainShadowStrength),
            Codec.DOUBLE.optionalFieldOf("mountain_height", 1.2).forGetter(SheetworldSettings::mountainHeight),
            Codec.BOOL.optionalFieldOf("jungle_pillars", true).forGetter(SheetworldSettings::junglePillars),
            Codec.BOOL.optionalFieldOf("rolling_hills", true).forGetter(SheetworldSettings::rollingHills),
            Codec.BOOL.optionalFieldOf("dunes", true).forGetter(SheetworldSettings::dunes),
            Codec.BOOL.optionalFieldOf("badlands_ridges", true).forGetter(SheetworldSettings::badlandsRidges)
        ).apply(instance, SheetworldSettings::new)
    );

    /**
     * Default settings matching TerrainConfig defaults.
     */
    public static final SheetworldSettings DEFAULT = new SheetworldSettings(
        WorldSize.MEDIUM,
        1.0,
        0.7,
        1.2,
        true,
        true,
        true,
        true
    );

    /**
     * Get a configuration value by key.
     * This bridges the gap between per-world settings and density function access.
     */
    public double getValue(String key) {
        return switch (key) {
            case "vertical_scale" -> verticalScale;
            case "rain_shadow_strength" -> rainShadowStrength;
            case "mountain_height" -> mountainHeight;
            case "half_size" -> (double) worldSize.getHalfSize();
            case "world_size" -> (double) worldSize.getSize();
            case "boundary_fade_start" -> (double) worldSize.getBoundaryFadeStart();
            case "jungle_pillars" -> junglePillars ? 1.0 : 0.0;
            case "rolling_hills" -> rollingHills ? 1.0 : 0.0;
            case "dunes" -> dunes ? 1.0 : 0.0;
            case "badlands_ridges" -> badlandsRidges ? 1.0 : 0.0;
            default -> 0.0;
        };
    }

    /**
     * Create a copy with modified world size.
     */
    public SheetworldSettings withWorldSize(WorldSize newSize) {
        return new SheetworldSettings(newSize, verticalScale, rainShadowStrength, mountainHeight,
                junglePillars, rollingHills, dunes, badlandsRidges);
    }

    /**
     * Create a copy with modified vertical scale.
     */
    public SheetworldSettings withVerticalScale(double newScale) {
        return new SheetworldSettings(worldSize, newScale, rainShadowStrength, mountainHeight,
                junglePillars, rollingHills, dunes, badlandsRidges);
    }

    /**
     * World size presets.
     */
    public enum WorldSize implements StringRepresentable {
        TINY("tiny", 2500, "Tiny (5km x 5km)"),
        SMALL("small", 5000, "Small (10km x 10km)"),
        MEDIUM("medium", 10000, "Medium (20km x 20km)"),
        LARGE("large", 20000, "Large (40km x 40km)"),
        HUGE("huge", 40000, "Huge (80km x 80km)"),
        MASSIVE("massive", 80000, "Massive (160km x 160km)");

        public static final Codec<WorldSize> CODEC = StringRepresentable.fromEnum(WorldSize::values);

        private final String id;
        private final int size;
        private final String displayName;

        WorldSize(String id, int size, String displayName) {
            this.id = id;
            this.size = size;
            this.displayName = displayName;
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        public String getId() {
            return id;
        }

        public int getSize() {
            return size;
        }

        public int getHalfSize() {
            return size / 2;
        }

        public int getBoundaryFadeStart() {
            // Start fading ~10% from the edge
            return (int) (getHalfSize() * 0.9);
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * Get the next larger size, or this if already maximum.
         */
        public WorldSize larger() {
            int idx = ordinal();
            WorldSize[] values = values();
            return idx < values.length - 1 ? values[idx + 1] : this;
        }

        /**
         * Get the next smaller size, or this if already minimum.
         */
        public WorldSize smaller() {
            int idx = ordinal();
            return idx > 0 ? values()[idx - 1] : this;
        }
    }
}
