package com.example.squareworld.worldgen;

import com.example.squareworld.SquareWorldMod;
import com.mojang.serialization.Codec;  // Added import
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModWorldGen {
    private static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(BuiltInRegistries.CHUNK_GENERATOR.key(), SquareWorldMod.MODID);

    public static final RegistryObject<Codec<SquareChunkGenerator>> SQUARE_GENERATOR =
            CHUNK_GENERATORS.register("square_generator", () -> SquareChunkGenerator.CODEC);

    public static void register(IEventBus eventBus) {
        CHUNK_GENERATORS.register(eventBus);
    }
}