package com.sheetworld.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Returns the cosine of the input (in radians).
 * 
 * Usage: {"type": "sheetworld:cos", "argument": ...}
 */
public record Cosine(DensityFunction argument) implements DensityFunction {

    private static final MapCodec<Cosine> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(Cosine::argument))
            .apply(instance, Cosine::new));
    
    public static final KeyDispatchDataCodec<Cosine> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        return Math.cos(this.argument.compute(context));
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        this.argument.fillArray(densities, provider);
        for (int i = 0; i < densities.length; i++) {
            densities[i] = Math.cos(densities[i]);
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new Cosine(this.argument.mapAll(visitor)));
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
