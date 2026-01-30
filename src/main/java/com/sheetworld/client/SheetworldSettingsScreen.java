package com.sheetworld.client;

import com.sheetworld.Sheetworld;
import com.sheetworld.world.SheetworldChunkGenerator;
import com.sheetworld.world.SheetworldSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * Settings screen for customizing Sheetworld world generation.
 *
 * Allows players to configure:
 * - World size (tiny to massive)
 * - Vertical scale (terrain height multiplier)
 * - Terrain feature toggles
 *
 * This class is only used on the client side.
 */
public class SheetworldSettingsScreen extends Screen {

    private final CreateWorldScreen parent;
    private final WorldCreationContext context;
    private SheetworldSettings settings;

    // UI state
    private CycleButton<SheetworldSettings.WorldSize> worldSizeButton;

    public SheetworldSettingsScreen(CreateWorldScreen parent, WorldCreationContext context) {
        super(Component.translatable("createWorld.customize.sheetworld.title"));
        this.parent = parent;
        this.context = context;

        // Extract current settings from the generator if possible
        this.settings = extractCurrentSettings(context);
    }

    private SheetworldSettings extractCurrentSettings(WorldCreationContext context) {
        try {
            ChunkGenerator generator = context.selectedDimensions().overworld();
            if (generator instanceof SheetworldChunkGenerator sheetGen) {
                return sheetGen.getSheetworldSettings();
            }
        } catch (Exception e) {
            Sheetworld.LOGGER.warn("Could not extract settings from generator, using defaults", e);
        }
        return SheetworldSettings.DEFAULT;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 50;

        // Title is rendered separately

        // World Size selector
        this.worldSizeButton = CycleButton.<SheetworldSettings.WorldSize>builder(size -> Component.literal(size.getDisplayName()))
                .withValues(SheetworldSettings.WorldSize.values())
                .withInitialValue(settings.worldSize())
                .create(centerX - 100, y, 200, 20,
                        Component.translatable("createWorld.customize.sheetworld.worldSize"),
                        (button, value) -> {
                            this.settings = settings.withWorldSize(value);
                        });
        this.addRenderableWidget(worldSizeButton);
        y += 30;

        // Vertical Scale info (display only for now)
        this.addRenderableWidget(Button.builder(
                Component.translatable("createWorld.customize.sheetworld.verticalScale",
                        String.format("%.1f", settings.verticalScale())),
                button -> {
                    // Toggle between presets: 0.5, 0.75, 1.0, 1.25, 1.5
                    double[] scales = {0.5, 0.75, 1.0, 1.25, 1.5};
                    int currentIdx = 2; // default to 1.0
                    for (int i = 0; i < scales.length; i++) {
                        if (Math.abs(settings.verticalScale() - scales[i]) < 0.01) {
                            currentIdx = i;
                            break;
                        }
                    }
                    int nextIdx = (currentIdx + 1) % scales.length;
                    this.settings = settings.withVerticalScale(scales[nextIdx]);
                    button.setMessage(Component.translatable("createWorld.customize.sheetworld.verticalScale",
                            String.format("%.1f", settings.verticalScale())));
                }
        ).bounds(centerX - 100, y, 200, 20).build());
        y += 30;

        // Feature toggles header
        y += 20;

        // Jungle Pillars toggle
        this.addRenderableWidget(CycleButton.onOffBuilder(settings.junglePillars())
                .create(centerX - 100, y, 200, 20,
                        Component.translatable("createWorld.customize.sheetworld.junglePillars"),
                        (button, value) -> {
                            this.settings = new SheetworldSettings(
                                    settings.worldSize(), settings.verticalScale(),
                                    settings.rainShadowStrength(), settings.mountainHeight(),
                                    value, settings.rollingHills(), settings.dunes(), settings.badlandsRidges()
                            );
                        }));
        y += 24;

        // Rolling Hills toggle
        this.addRenderableWidget(CycleButton.onOffBuilder(settings.rollingHills())
                .create(centerX - 100, y, 200, 20,
                        Component.translatable("createWorld.customize.sheetworld.rollingHills"),
                        (button, value) -> {
                            this.settings = new SheetworldSettings(
                                    settings.worldSize(), settings.verticalScale(),
                                    settings.rainShadowStrength(), settings.mountainHeight(),
                                    settings.junglePillars(), value, settings.dunes(), settings.badlandsRidges()
                            );
                        }));
        y += 24;

        // Dunes toggle
        this.addRenderableWidget(CycleButton.onOffBuilder(settings.dunes())
                .create(centerX - 100, y, 200, 20,
                        Component.translatable("createWorld.customize.sheetworld.dunes"),
                        (button, value) -> {
                            this.settings = new SheetworldSettings(
                                    settings.worldSize(), settings.verticalScale(),
                                    settings.rainShadowStrength(), settings.mountainHeight(),
                                    settings.junglePillars(), settings.rollingHills(), value, settings.badlandsRidges()
                            );
                        }));
        y += 24;

        // Badlands Ridges toggle
        this.addRenderableWidget(CycleButton.onOffBuilder(settings.badlandsRidges())
                .create(centerX - 100, y, 200, 20,
                        Component.translatable("createWorld.customize.sheetworld.badlandsRidges"),
                        (button, value) -> {
                            this.settings = new SheetworldSettings(
                                    settings.worldSize(), settings.verticalScale(),
                                    settings.rainShadowStrength(), settings.mountainHeight(),
                                    settings.junglePillars(), settings.rollingHills(), settings.dunes(), value
                            );
                        }));

        // Done and Cancel buttons at bottom
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            applySettings();
            this.minecraft.setScreen(this.parent);
        }).bounds(centerX - 155, this.height - 28, 150, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
            this.minecraft.setScreen(this.parent);
        }).bounds(centerX + 5, this.height - 28, 150, 20).build());
    }

    private void applySettings() {
        // Update the static current settings for density functions
        SheetworldChunkGenerator.setCurrentSettings(settings);

        // Apply settings to the world creation context
        parent.getUiState().updateDimensions((registryAccess, worldDimensions) -> {
            // Get the noise settings holder
            Holder<NoiseGeneratorSettings> noiseSettings = registryAccess
                    .registryOrThrow(Registries.NOISE_SETTINGS)
                    .getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);

            // Create new chunk generator with updated settings
            ChunkGenerator newGenerator = new SheetworldChunkGenerator(
                    registryAccess.lookupOrThrow(Registries.BIOME),
                    settings,
                    noiseSettings
            );

            return worldDimensions.replaceOverworldGenerator(registryAccess, newGenerator);
        });

        Sheetworld.LOGGER.info("Applied Sheetworld settings: worldSize={}, verticalScale={}",
                settings.worldSize().getId(), settings.verticalScale());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // Draw section header for features
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("createWorld.customize.sheetworld.features"),
                this.width / 2, 120, 0xA0A0A0);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
