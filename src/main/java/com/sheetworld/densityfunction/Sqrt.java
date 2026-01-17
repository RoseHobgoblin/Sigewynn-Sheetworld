package com.sheetworld.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Returns the square root of the input.
 * Returns 0 for negative inputs.
 * 
 * Usage: {"type": "sheetworld:sqrt", "argument": ...}
 */
public record Sqrt(DensityFunction argument) implements DensityFunction {

    private static final MapCodec<Sqrt> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(Sqrt::argument))
            .apply(instance, Sqrt::new));
    
    public static final KeyDispatchDataCodec<Sqrt> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        double value = this.argument.compute(context);
        return value <= 0 ? 0 : Math.sqrt(value);
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        this.argument.fillArray(densities, provider);
        for (int i = 0; i < densities.length; i++) {
            densities[i] = densities[i] <= 0 ? 0 : Math.sqrt(densities[i]);
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new Sqrt(this.argument.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return 0;
    }

    @Override
    public double maxValue() {
        double max = this.argument.maxValue();
        return max > 0 ? Math.sqrt(max) : 0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
