package com.sheetworld.client;

import com.sheetworld.Sheetworld;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent;

/**
 * Client-side event handlers for Sheetworld.
 *
 * Registers the preset editor for the Sheetworld world type,
 * allowing customization of world settings during world creation.
 */
@EventBusSubscriber(modid = Sheetworld.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SheetworldClientEvents {

    // Resource key for the Sheetworld world preset
    public static final ResourceKey<WorldPreset> SHEETWORLD_PRESET =
            ResourceKey.create(net.minecraft.core.registries.Registries.WORLD_PRESET,
                    ResourceLocation.fromNamespaceAndPath(Sheetworld.MOD_ID, "sheetworld"));

    @SubscribeEvent
    public static void onRegisterPresetEditors(RegisterPresetEditorsEvent event) {
        Sheetworld.LOGGER.info("Registering Sheetworld preset editor");

        event.register(SHEETWORLD_PRESET,
                (createWorldScreen, worldCreationContext) ->
                        new SheetworldSettingsScreen(createWorldScreen, worldCreationContext));
    }
}
