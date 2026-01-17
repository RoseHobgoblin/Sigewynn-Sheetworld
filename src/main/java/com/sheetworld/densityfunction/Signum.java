package com.sheetworld.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Returns the sign of the input: -1, 0, or 1.
 * 
 * Usage: {"type": "sheetworld:signum", "argument": ...}
 */
public record Signum(DensityFunction argument) implements DensityFunction {

    private static final MapCodec<Signum> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(Signum::argument))
            .apply(instance, Signum::new));
    
    public static final KeyDispatchDataCodec<Signum> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        return Math.signum(this.argument.compute(context));
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        this.argument.fillArray(densities, provider);
        for (int i = 0; i < densities.length; i++) {
            densities[i] = Math.signum(densities[i]);
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new Signum(this.argument.mapAll(visitor)));
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
