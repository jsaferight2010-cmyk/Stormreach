package com.stormreach.client.config;

import com.stormreach.StormreachConfig;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Stormreach settings screen, opened from Mod Menu. Performance settings only (5.0): tornado simulation limits, particles and spread load. Gameplay options live in config/stormreach.properties.
 * Changes are saved to config/stormreach.properties when you press Done.
 */
public class StormreachConfigScreen extends Screen {
    private static final int BUTTON_W = 150;
    private static final int BUTTON_H = 20;
    private static final int ROW = 24;

    private final Screen parent;
    private int page;

    public StormreachConfigScreen(Screen parent) {
        this(parent, 0);
    }

    private StormreachConfigScreen(Screen parent, int page) {
        super(Component.translatable("config.stormreach.title"));
        this.parent = parent;
        this.page = page;
    }

    @Override
    protected void init() {
        addRenderableWidget(new StringWidget(0, 8, this.width, 12, this.title, this.font));

        String[] pages = {"tornadoes", "effects"};
        int tabW = 90;
        int tabX = this.width / 2 - (tabW * pages.length + 4 * (pages.length - 1)) / 2;
        for (int i = 0; i < pages.length; i++) {
            final int target = i;
            Button tab = Button.builder(Component.translatable("config.stormreach.page." + pages[i]), b -> switchPage(target))
                    .bounds(tabX + i * (tabW + 4), 26, tabW, BUTTON_H).build();
            tab.active = i != page;
            addRenderableWidget(tab);
        }

        Grid grid = new Grid(this.width / 2 - BUTTON_W - 5, 56);
        switch (page) {
            case 0 -> {
                grid.add(slider("tornadoSimulationDistance", 64, 512, "", () -> StormreachConfig.tornadoSimulationDistance, v -> StormreachConfig.tornadoSimulationDistance = v));
                grid.add(slider("maxActiveTornadoes", 0, 8, "", () -> StormreachConfig.maxActiveTornadoes, v -> StormreachConfig.maxActiveTornadoes = v));
                grid.add(slider("tornadoBlockChecksPerTick", 0, 512, "", () -> StormreachConfig.tornadoBlockChecksPerTick, v -> StormreachConfig.tornadoBlockChecksPerTick = v));
                grid.add(slider("tornadoDebrisLimit", 0, 200, "", () -> StormreachConfig.tornadoDebrisLimit, v -> StormreachConfig.tornadoDebrisLimit = v));
                grid.add(toggle("tornadoDestruction", StormreachConfig.tornadoDestruction, v -> StormreachConfig.tornadoDestruction = v));
                grid.add(slider("tornadoSizePercent", 10, 100, "%", () -> StormreachConfig.tornadoSizePercent, v -> StormreachConfig.tornadoSizePercent = v));
            }
            default -> {
                grid.add(slider("particleDensity", 0, 100, "%", () -> StormreachConfig.particleDensity, v -> StormreachConfig.particleDensity = v));
                grid.add(toggle("stormParticles", StormreachConfig.stormParticles, v -> StormreachConfig.stormParticles = v));
                grid.add(slider("maxSquallsPerPlayer", 0, 12, "", () -> StormreachConfig.maxSquallsPerPlayer, v -> StormreachConfig.maxSquallsPerPlayer = v));
                grid.add(slider("heartMaxRadius", 8, 64, "", () -> StormreachConfig.heartMaxRadius, v -> StormreachConfig.heartMaxRadius = v));
                grid.add(slider("spreadSpeed", 1, 10, "", () -> StormreachConfig.spreadSpeed, v -> StormreachConfig.spreadSpeed = v));
                grid.add(slider("backpackMagnetInterval", 5, 40, "t", () -> StormreachConfig.backpackMagnetInterval, v -> StormreachConfig.backpackMagnetInterval = v));
            }
        }

        int bottom = this.height - 28;
        // 6.0: on a server, the server's own stormreach.properties runs the world; only the visual settings here apply
        if (this.minecraft.getConnection() != null && !this.minecraft.hasSingleplayerServer()) {
            net.minecraft.network.chat.Component note = Component.translatable("config.stormreach.server_note").withStyle(net.minecraft.ChatFormatting.GRAY);
            addRenderableWidget(new net.minecraft.client.gui.components.StringWidget(this.width / 2 - 160, bottom - 16, 320, 12, note, this.font));
        }
        addRenderableWidget(Button.builder(Component.translatable("config.stormreach.reset"), b -> {
                    StormreachConfig.resetToDefaults();
                    switchPage(page);
                }).bounds(this.width / 2 - BUTTON_W - 5, bottom, BUTTON_W, BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("config.stormreach.reset.tooltip"))).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 + 5, bottom, BUTTON_W, BUTTON_H).build());
    }

    private void switchPage(int newPage) {
        this.page = newPage;
        rebuildWidgets();
    }

    @Override
    public void onClose() {
        StormreachConfig.save();
        this.minecraft.gui.setScreen(parent);
    }

    private CycleButton<Boolean> toggle(String key, boolean value, Consumer<Boolean> setter) {
        CycleButton<Boolean> button = CycleButton.onOffBuilder(value)
                .create(0, 0, BUTTON_W, BUTTON_H, Component.translatable("config.stormreach." + key), (b, v) -> setter.accept(v));
        button.setTooltip(Tooltip.create(Component.translatable("config.stormreach." + key + ".tooltip")));
        return button;
    }

    private IntSlider slider(String key, int min, int max, String unit, IntSupplier getter, IntConsumer setter) {
        IntSlider slider = new IntSlider(Component.translatable("config.stormreach." + key), unit, min, max, getter.getAsInt(), setter);
        slider.setTooltip(Tooltip.create(Component.translatable("config.stormreach." + key + ".tooltip")));
        return slider;
    }

    /** Lays widgets out in two columns. */
    private final class Grid {
        private final int left;
        private final int top;
        private int index;

        Grid(int left, int top) {
            this.left = left;
            this.top = top;
        }

        void add(AbstractWidget widget) {
            widget.setX(left + (index % 2) * (BUTTON_W + 10));
            widget.setY(top + (index / 2) * ROW);
            index++;
            addRenderableWidget(widget);
        }

        void add(AbstractWidget widget, String tooltipKey) {
            widget.setTooltip(Tooltip.create(Component.translatable("config.stormreach." + tooltipKey + ".tooltip")));
            add(widget);
        }
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final Component label;
        private final String unit;
        private final int min;
        private final int max;
        private final IntConsumer setter;

        IntSlider(Component label, String unit, int min, int max, int current, IntConsumer setter) {
            super(0, 0, BUTTON_W, BUTTON_H, Component.empty(), (current - min) / (double) (max - min));
            this.label = label;
            this.unit = unit;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        private int intValue() {
            return Mth.clamp((int) Math.round(min + value * (max - min)), min, max);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.empty().append(label).append(": " + intValue() + unit));
        }

        @Override
        protected void applyValue() {
            setter.accept(intValue());
        }
    }
}
