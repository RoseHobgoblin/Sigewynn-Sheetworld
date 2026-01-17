package com.sheetworld.world;

import com.mojang.serialization.MapCodec;
import com.sheetworld.Sheetworld;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registers Sheetworld's biome source type.
 * 
 * Note: We use vanilla's NoiseBasedChunkGenerator, so no custom chunk generator needed.
 * Our biome source plugs into vanilla's noise-based terrain generation.
 */
public class SheetworldRegistries {
    
    // Biome Source Types  
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Sheetworld.MOD_ID);
    
    // Register our biome source
    public static final Supplier<MapCodec<? extends BiomeSource>> SHEETWORLD_BIOME_SOURCE =
            BIOME_SOURCES.register("sheetworld", () -> SheetworldBiomeSource.CODEC);
    
    /**
     * Register all world-related registries to the mod event bus
     */
    public static void register(IEventBus modEventBus) {
        BIOME_SOURCES.register(modEventBus);
        Sheetworld.LOGGER.info("Registered Sheetworld biome source");
    }
}
