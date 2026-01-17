package com.sheetworld.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Returns the angle (in radians) from the positive X axis to the point (x, z).
 * This is atan2(z, x), returning values from -π to π.
 * 
 * Usage: {"type": "sheetworld:atan2", "z": ..., "x": ...}
 */
public record Atan2(DensityFunction z, DensityFunction x) implements DensityFunction {

    private static final MapCodec<Atan2> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("z").forGetter(Atan2::z),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("x").forGetter(Atan2::x)
            )
            .apply(instance, Atan2::new));
    
    public static final KeyDispatchDataCodec<Atan2> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        return Math.atan2(this.z.compute(context), this.x.compute(context));
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        provider.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new Atan2(this.z.mapAll(visitor), this.x.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return -Math.PI;
    }

    @Override
    public double maxValue() {
        return Math.PI;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
