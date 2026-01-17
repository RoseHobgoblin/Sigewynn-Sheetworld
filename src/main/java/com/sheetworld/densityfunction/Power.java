package com.sheetworld.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Returns base raised to the power of exponent.
 * Used for non-linear temperature falloff: temperature = 1 - 2 * (r/R)^1.3
 * 
 * Usage: {"type": "sheetworld:pow", "base": ..., "exponent": ...}
 */
public record Power(DensityFunction base, DensityFunction exponent) implements DensityFunction {

    private static final MapCodec<Power> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("base").forGetter(Power::base),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("exponent").forGetter(Power::exponent)
            )
            .apply(instance, Power::new));
    
    public static final KeyDispatchDataCodec<Power> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        double baseValue = this.base.compute(context);
        double expValue = this.exponent.compute(context);
        
        if (baseValue < 0 && expValue != Math.floor(expValue)) {
            return 0; // Negative base with non-integer exponent
        }
        
        double result = Math.pow(baseValue, expValue);
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            return 0;
        }
        return result;
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        provider.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new Power(this.base.mapAll(visitor), this.exponent.mapAll(visitor)));
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
