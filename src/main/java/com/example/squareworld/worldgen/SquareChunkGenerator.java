package com.example.squareworld.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.WorldGenLevel;

public class SquareChunkGenerator extends ChunkGenerator {
    // Codec for serialization
    public static final Codec<SquareChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    ChunkGenerator.CODEC.fieldOf("delegate").forGetter(gen -> gen.delegate)
            ).apply(instance, SquareChunkGenerator::new)
    );

    private static final int BOUNDARY = 2000; // World boundary at ±2000 blocks
    private final ChunkGenerator delegate; // Wrapped default generator

    public SquareChunkGenerator(BiomeSource biomeSource, ChunkGenerator delegate) {
        super(biomeSource);
        this.delegate = delegate;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving carving) {
        if (isWithinBoundary(chunk)) {
            delegate.applyCarvers(region, seed, random, biomeManager, structureManager, chunk, carving);
        }
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        if (isWithinBoundary(chunk)) {
            delegate.buildSurface(region, structureManager, random, chunk);
        }
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        if (isWithinBoundary(chunk)) {
            delegate.applyBiomeDecoration(level, chunk, structureManager);
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random, StructureManager structureManager, ChunkAccess chunk) {
        if (isWithinBoundary(chunk)) {
            return delegate.fillFromNoise(executor, blender, random, structureManager, chunk);
        } else {
            return CompletableFuture.completedFuture(chunk); // Return empty chunk outside boundary
        }
    }

    @Override
    public int getGenDepth() {
        return delegate.getGenDepth();
    }

    @Override
    public int getSeaLevel() {
        return delegate.getSeaLevel();
    }

    @Override
    public int getMinY() {
        return delegate.getMinY();
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        if (Math.abs(x) <= BOUNDARY && Math.abs(z) <= BOUNDARY) {
            return delegate.getBaseHeight(x, z, type, level, random);
        }
        return getMinY(); // Void outside boundary
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        if (Math.abs(x) <= BOUNDARY && Math.abs(z) <= BOUNDARY) {
            return delegate.getBaseColumn(x, z, level, random);
        }
        int minY = level.getMinBuildHeight();
        int height = level.getHeight();
        BlockState[] column = new BlockState[height];
        Arrays.fill(column, Blocks.AIR.defaultBlockState());
        return new NoiseColumn(minY, column);
    }

    private boolean isWithinBoundary(ChunkAccess chunk) {
        int chunkX = chunk.getPos().getMinBlockX(); // Chunk X * 16
        int chunkZ = chunk.getPos().getMinBlockZ(); // Chunk Z * 16
        return Math.abs(chunkX) <= BOUNDARY && Math.abs(chunkZ) <= BOUNDARY;
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        delegate.spawnOriginalMobs(region);
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return delegate.getSpawnHeight(level);
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos pos) {
        delegate.addDebugScreenInfo(list, randomState, pos);
    }
}