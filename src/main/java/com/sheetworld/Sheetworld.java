package com.sheetworld;

import com.mojang.serialization.MapCodec;
import com.sheetworld.densityfunction.*;
import com.sheetworld.world.SheetworldRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Sheetworld - A finite, flat square world with scientifically-modeled climate zones.
 * 
 * The world is a perfect square with:
 * - Center (0,0) as the warmest point (tropical)
 * - Corners as the coldest points (polar)
 * - Circular isotherms creating radial climate bands
 * - Void at the edges
 */
@Mod(Sheetworld.MOD_ID)
public class Sheetworld {
    public static final String MOD_ID = "sheetworld";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Density Function Type Registry
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES = 
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, MOD_ID);

    // === Coordinate Functions ===
    public static final Supplier<MapCodec<? extends DensityFunction>> X_COORD = 
            DENSITY_FUNCTION_TYPES.register("x", () -> XCoord.CODEC.codec());
    
    public static final Supplier<MapCodec<? extends DensityFunction>> Z_COORD = 
            DENSITY_FUNCTION_TYPES.register("z", () -> ZCoord.CODEC.codec());

    // === Math Functions ===
    public static final Supplier<MapCodec<? extends DensityFunction>> DIV = 
            DENSITY_FUNCTION_TYPES.register("div", () -> Division.CODEC.codec());
    
    public static final Supplier<MapCodec<? extends DensityFunction>> SQRT = 
            DENSITY_FUNCTION_TYPES.register("sqrt", () -> Sqrt.CODEC.codec());
    
    public static final Supplier<MapCodec<? extends DensityFunction>> SINE = 
            DENSITY_FUNCTION_TYPES.register("sin", () -> Sine.CODEC.codec());
    
    public static final Supplier<MapCodec<? extends DensityFunction>> COSINE = 
            DENSITY_FUNCTION_TYPES.register("cos", () -> Cosine.CODEC.codec());
    
    public static final Supplier<MapCodec<? extends DensityFunction>> SIGNUM = 
            DENSITY_FUNCTION_TYPES.register("signum", () -> Signum.CODEC.codec());
    
    public static final Supplier<MapCodec<? extends DensityFunction>> POWER = 
            DENSITY_FUNCTION_TYPES.register("pow", () -> Power.CODEC.codec());
    
    public static final Supplier<MapCodec<? extends DensityFunction>> ATAN2 = 
            DENSITY_FUNCTION_TYPES.register("atan2", () -> Atan2.CODEC.codec());

    // === Sheetworld-specific Functions ===
    public static final Supplier<MapCodec<? extends DensityFunction>> RADIUS = 
            DENSITY_FUNCTION_TYPES.register("radius", () -> Radius.CODEC.codec());

    // === Terrain Functions ===
    public static final Supplier<MapCodec<? extends DensityFunction>> FLAT_DOMAIN_WARP = 
            DENSITY_FUNCTION_TYPES.register("flat_domain_warp", () -> FlatDomainWarp.CODEC.codec());

    public Sheetworld(IEventBus modEventBus) {
        LOGGER.info("Sheetworld initializing...");
        
        // Register density function types
        DENSITY_FUNCTION_TYPES.register(modEventBus);
        
        // Register biome source
        SheetworldRegistries.register(modEventBus);
        
        LOGGER.info("Sheetworld initialized with {} density function types", 
                DENSITY_FUNCTION_TYPES.getEntries().size());
    }
}
