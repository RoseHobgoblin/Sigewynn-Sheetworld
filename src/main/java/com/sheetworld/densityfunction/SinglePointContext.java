package com.sheetworld.densityfunction;

import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * A simple FunctionContext implementation for a single point.
 * Used by FlatDomainWarp to sample at warped coordinates.
 */
public record SinglePointContext(int blockX, int blockY, int blockZ) implements DensityFunction.FunctionContext {
    
    @Override
    public int blockX() {
        return blockX;
    }

    @Override
    public int blockY() {
        return blockY;
    }

    @Override
    public int blockZ() {
        return blockZ;
    }
}
