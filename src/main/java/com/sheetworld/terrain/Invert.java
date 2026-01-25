package com.sheetworld.terrain;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * A density function that computes 1/x of the input.
 * Adapted from Tectonic's Invert.
 * 
 * Useful for converting between scales or inverting effects.
 */
public record Invert(DensityFunction input, double min, double max) implements DensityFunction {
    
    public static final MapCodec<Invert> DATA_CODEC = DensityFunction.HOLDER_HELPER_CODEC
        .fieldOf("argument")
        .xmap(Invert::create, Invert::input);
    
    public static KeyDispatchDataCodec<Invert> CODEC_HOLDER = KeyDispatchDataCodec.of(DATA_CODEC);
    
    /**
     * Create an Invert function, calculating appropriate min/max bounds.
     */
    public static Invert create(DensityFunction input) {
        double min = input.minValue();
        double max = input.maxValue();
        
        // If the input crosses zero, output can be unbounded
        if (min < 0 && max > 0) {
            return new Invert(input, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        }
        
        // Otherwise, invert the bounds
        // Note: inverting swaps the order (1/max < 1/min for positive values)
        if (min > 0) {
            return new Invert(input, 1.0 / max, 1.0 / min);
        } else if (max < 0) {
            return new Invert(input, 1.0 / min, 1.0 / max);
        }
        
        return new Invert(input, min, max);
    }
    
    /**
     * The inversion transform: 1/x
     */
    public double transform(double d) {
        return 1.0 / d;
    }
    
    @Override
    public double compute(FunctionContext context) {
        return transform(input.compute(context));
    }
    
    @Override
    public void fillArray(double[] densities, ContextProvider context) {
        input.fillArray(densities, context);
        
        for (int i = 0; i < densities.length; i++) {
            densities[i] = transform(densities[i]);
        }
    }
    
    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return create(input.mapAll(visitor));
    }
    
    @Override
    public double minValue() {
        return min;
    }
    
    @Override
    public double maxValue() {
        return max;
    }
    
    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
