package com.sheetworld.densityfunction;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Returns the X coordinate at the current position.
 * Usage: {"type": "sheetworld:x"}
 */
public record XCoord() implements DensityFunction.SimpleFunction {

    private static final MapCodec<XCoord> MAP_CODEC = MapCodec.unit(XCoord::new);
    public static final KeyDispatchDataCodec<XCoord> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        return context.blockX();
    }

    @Override
    public double minValue() {
        return -30_000_000;
    }

    @Override
    public double maxValue() {
        return 30_000_000;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
