package com.example.squareworld;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.example.squareworld.worldgen.ModWorldGen;

@Mod(SquareWorldMod.MODID)
public class SquareWorldMod {
    public static final String MODID = "squareworld";

    public SquareWorldMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModWorldGen.register(modEventBus);
    }
}