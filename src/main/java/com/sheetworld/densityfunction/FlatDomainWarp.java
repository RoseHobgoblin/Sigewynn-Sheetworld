package com.sheetworld.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Warps the X and Z coordinates before sampling the input function.
 * 
 * Usage: {"type": "sheetworld:flat_domain_warp", "input": ..., "warp_x": ..., "warp_z": ...}
 */
public record FlatDomainWarp(DensityFunction input, DensityFunction warpX, DensityFunction warpZ) 
        implements DensityFunction {

    private static final MapCodec<FlatDomainWarp> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(FlatDomainWarp::input),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("warp_x").forGetter(FlatDomainWarp::warpX),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("warp_z").forGetter(FlatDomainWarp::warpZ)
            )
            .apply(instance, FlatDomainWarp::new));
    
    public static final KeyDispatchDataCodec<FlatDomainWarp> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        int warpedX = context.blockX() + (int) this.warpX.compute(context);
        int warpedZ = context.blockZ() + (int) this.warpZ.compute(context);
        return input.compute(new SinglePointContext(warpedX, context.blockY(), warpedZ));
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        provider.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new FlatDomainWarp(
            this.input.mapAll(visitor), 
            this.warpX.mapAll(visitor), 
            this.warpZ.mapAll(visitor)
        ));
    }

    @Override
    public double minValue() {
        return this.input.minValue();
    }

    @Override
    public double maxValue() {
        return this.input.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
