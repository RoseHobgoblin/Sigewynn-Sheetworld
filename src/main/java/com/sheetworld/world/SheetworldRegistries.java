package com.sheetworld.world;

import com.mojang.serialization.MapCodec;
import com.sheetworld.Sheetworld;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registers Sheetworld's world generation types.
 *
 * - Biome Source: sheetworld:sheetworld
 * - Chunk Generator: sheetworld:sheetworld
 */
public class SheetworldRegistries {

    // Biome Source Types
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Sheetworld.MOD_ID);

    // Chunk Generator Types
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Sheetworld.MOD_ID);

    // Register our biome source
    public static final Supplier<MapCodec<? extends BiomeSource>> SHEETWORLD_BIOME_SOURCE =
            BIOME_SOURCES.register("sheetworld", () -> SheetworldBiomeSource.CODEC);

    // Register our chunk generator
    public static final Supplier<MapCodec<? extends ChunkGenerator>> SHEETWORLD_CHUNK_GENERATOR =
            CHUNK_GENERATORS.register("sheetworld", () -> SheetworldChunkGenerator.CODEC);

    /**
     * Register all world-related registries to the mod event bus
     */
    public static void register(IEventBus modEventBus) {
        BIOME_SOURCES.register(modEventBus);
        CHUNK_GENERATORS.register(modEventBus);
        Sheetworld.LOGGER.info("Registered Sheetworld biome source and chunk generator");
    }
}
