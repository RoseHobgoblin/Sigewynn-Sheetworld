package com.sheetworld.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * Divides argument1 by argument2.
 * Returns 0 if argument2 is 0.
 * 
 * Usage: {"type": "sheetworld:div", "argument1": ..., "argument2": ...}
 */
public record Division(DensityFunction argument1, DensityFunction argument2) implements DensityFunction {

    private static final MapCodec<Division> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument1").forGetter(Division::argument1),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument2").forGetter(Division::argument2)
            )
            .apply(instance, Division::new));
    
    public static final KeyDispatchDataCodec<Division> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        double dividend = this.argument1.compute(context);
        double divisor = this.argument2.compute(context);
        if (divisor == 0) {
            return 0.0;
        }
        return dividend / divisor;
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        provider.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new Division(this.argument1.mapAll(visitor), this.argument2.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return -1000000;
    }

    @Override
    public double maxValue() {
        return 1000000;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
