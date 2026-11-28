package com.stormreach.client.wind;

import com.stormreach.client.ClientUnits;
import com.stormreach.sim.EFScale;
import com.stormreach.wind.WindGeneratorBlock;
import com.stormreach.wind.WindGeneratorBlockEntity;
import com.stormreach.wind.WindRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** 6.0: settings for the Creative Wind Generator: wind speed, reach, gustiness, on/off and block damage. */
public class WindGeneratorScreen extends Screen {
    private final BlockPos pos;
    private int mph, radius;
    private float gust;
    private boolean on, damage;

    public static void register() {
        WindGeneratorBlock.openScreen = pos -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.level.getBlockEntity(pos) instanceof WindGeneratorBlockEntity be) mc.gui.setScreen(new WindGeneratorScreen(pos, be));
        };
    }

    public WindGeneratorScreen(BlockPos pos, WindGeneratorBlockEntity be) {
        super(Component.translatable("block.stormreach.wind_generator"));
        this.pos = pos;
        this.mph = be.mph();
        this.radius = be.radius();
        this.gust = be.gustiness();
        this.on = be.on();
        this.damage = be.damage();
    }

    private int left() { return (width - 220) / 2; }
    private int top() { return (height - 150) / 2; }

    private void send() {
        ClientPlayNetworking.send(new WindRegistry.SettingsPayload(pos, mph, radius, gust, on, damage));
    }

    private Component speedLabel() {
        EFScale ef = EFScale.fromGustMph(mph);
        Component rating = mph < 39 ? Component.translatable("gui.stormreach.wind_generator.breeze")
                : mph < 65 ? Component.translatable("gui.stormreach.wind_generator.gale")
                : Component.literal(ef == null ? "EF0" : ef.name());
        return Component.translatable("gui.stormreach.wind_generator.speed", ClientUnits.speed(mph), rating);
    }

    @Override
    protected void init() {
        int l = left() + 10, t = top() + 22, w = 200;
        addRenderableWidget(new Slider(l, t, w, (double) mph / WindGeneratorBlockEntity.MAX_MPH) {
            @Override protected void updateMessage() { setMessage(speedLabel()); }
            @Override protected void applyValue() { mph = (int) Math.round(value * WindGeneratorBlockEntity.MAX_MPH / 5.0) * 5; send(); }
        });
        int span = WindGeneratorBlockEntity.MAX_RADIUS - WindGeneratorBlockEntity.MIN_RADIUS;
        addRenderableWidget(new Slider(l, t + 24, w, (double) (radius - WindGeneratorBlockEntity.MIN_RADIUS) / span) {
            @Override protected void updateMessage() { setMessage(Component.translatable("gui.stormreach.wind_generator.reach", ClientUnits.distance(radius))); }
            @Override protected void applyValue() { radius = WindGeneratorBlockEntity.MIN_RADIUS + (int) Math.round(value * span); send(); }
        });
        addRenderableWidget(new Slider(l, t + 48, w, gust) {
            @Override protected void updateMessage() { setMessage(Component.translatable("gui.stormreach.wind_generator.gusts", Math.round(gust * 100))); }
            @Override protected void applyValue() { gust = (float) value; send(); }
        });
        addRenderableWidget(Button.builder(onLabel(), b -> { on = !on; b.setMessage(onLabel()); send(); }).bounds(l, t + 76, 98, 20).build());
        addRenderableWidget(Button.builder(damageLabel(), b -> { damage = !damage; b.setMessage(damageLabel()); send(); }).bounds(l + 102, t + 76, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(l + 50, t + 102, 100, 20).build());
    }

    private Component onLabel() {
        return Component.translatable(on ? "gui.stormreach.wind_generator.on" : "gui.stormreach.wind_generator.off");
    }

    private Component damageLabel() {
        return Component.translatable(damage ? "gui.stormreach.wind_generator.damage_on" : "gui.stormreach.wind_generator.damage_off");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractBackground(g, mx, my, partial);
        int l = left(), t = top();
        g.fill(l, t, l + 220, t + 150, 0xE0101820);
        g.outline(l, t, 220, 150, 0xFF5A8AB8);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractRenderState(g, mx, my, partial);
        g.centeredText(font, title, width / 2, top() + 7, 0xFFFFFFFF);
    }

    @Override public boolean isPauseScreen() { return false; }

    /** A slider that shows its label straight away. */
    private abstract static class Slider extends AbstractSliderButton {
        Slider(int x, int y, int w, double v) {
            super(x, y, w, 20, Component.empty(), Mth.clamp(v, 0, 1));
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
            updateMessage();
            super.extractWidgetRenderState(g, mx, my, pt);
        }
    }
}
