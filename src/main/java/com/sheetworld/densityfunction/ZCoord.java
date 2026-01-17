package com.sheetworld.densityfunction;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Returns the Z coordinate at the current position.
 * Usage: {"type": "sheetworld:z"}
 */
public record ZCoord() implements DensityFunction.SimpleFunction {

    private static final MapCodec<ZCoord> MAP_CODEC = MapCodec.unit(ZCoord::new);
    public static final KeyDispatchDataCodec<ZCoord> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        return context.blockZ();
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
