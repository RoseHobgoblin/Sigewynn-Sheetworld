package com.sheetworld.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Returns the sine of the input (in radians).
 * 
 * Usage: {"type": "sheetworld:sin", "argument": ...}
 */
public record Sine(DensityFunction argument) implements DensityFunction {

    private static final MapCodec<Sine> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(Sine::argument))
            .apply(instance, Sine::new));
    
    public static final KeyDispatchDataCodec<Sine> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        return Math.sin(this.argument.compute(context));
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        this.argument.fillArray(densities, provider);
        for (int i = 0; i < densities.length; i++) {
            densities[i] = Math.sin(densities[i]);
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new Sine(this.argument.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return -1;
    }

    @Override
    public double maxValue() {
        return 1;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
