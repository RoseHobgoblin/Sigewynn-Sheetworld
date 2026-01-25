package com.sheetworld.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

import java.util.Arrays;

/**
 * A density function that returns a constant value from TerrainConfig.
 * Adapted from Tectonic's ConfigConstant.
 * 
 * This allows datapack density functions to reference configuration values
 * that can be changed without modifying the datapacks.
 */
public record ConfigConstant(String key, double value) implements DensityFunction {
    
    public static MapCodec<ConfigConstant> DATA_CODEC = RecordCodecBuilder.mapCodec(instance -> 
        instance.group(
            Codec.STRING.fieldOf("key").forGetter(ConfigConstant::key)
        ).apply(instance, ConfigConstant::create)
    );
    
    public static KeyDispatchDataCodec<ConfigConstant> CODEC_HOLDER = KeyDispatchDataCodec.of(DATA_CODEC);
    
    /**
     * Create a ConfigConstant by looking up the value from TerrainConfig.
     */
    public static ConfigConstant create(String key) {
        return new ConfigConstant(key, TerrainConfig.getValue(key));
    }
    
    @Override
    public double compute(FunctionContext context) {
        return value;
    }
    
    @Override
    public void fillArray(double[] doubles, ContextProvider contextProvider) {
        Arrays.fill(doubles, value);
    }
    
    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(DensityFunctions.constant(value));
    }
    
    @Override
    public double minValue() {
        return value;
    }
    
    @Override
    public double maxValue() {
        return value;
    }
    
    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
