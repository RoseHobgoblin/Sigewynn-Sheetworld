package com.sheetworld.densityfunction;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Returns the distance from the world origin (0, 0) on the XZ plane.
 * This is the core function for Sheetworld's radial climate model.
 * 
 * radius = sqrt(x² + z²)
 * 
 * Usage: {"type": "sheetworld:radius"}
 */
public record Radius() implements DensityFunction.SimpleFunction {

    private static final MapCodec<Radius> MAP_CODEC = MapCodec.unit(Radius::new);
    public static final KeyDispatchDataCodec<Radius> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        double x = context.blockX();
        double z = context.blockZ();
        return Math.sqrt(x * x + z * z);
    }

    @Override
    public double minValue() {
        return 0;
    }

    @Override
    public double maxValue() {
        return 42_426_408; // sqrt(30M² + 30M²)
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
