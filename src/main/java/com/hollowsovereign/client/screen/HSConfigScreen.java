package com.hollowsovereign.client.screen;

import com.hollowsovereign.client.HSClientConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class HSConfigScreen extends Screen {
    private final Screen parent;

    public HSConfigScreen(Screen parent) {
        super(Text.literal("Hollow Sovereign — Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        HSClientConfig cfg = HSClientConfig.get();
        int w = 260, h = 20, gap = 6;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 60;

        ButtonWidget intensity = ButtonWidget.builder(intensityText(cfg), b -> {
            cfg.effectsIntensity = (cfg.effectsIntensity + 1) % 3;
            b.setMessage(intensityText(cfg));
        }).dimensions(x, y, w, h).build();

        ButtonWidget flashSafe = ButtonWidget.builder(flashSafeText(cfg), b -> {
            cfg.flashSafe = !cfg.flashSafe;
            b.setMessage(flashSafeText(cfg));
        }).dimensions(x, y + (h + gap), w, h).build();

        ButtonWidget intros = ButtonWidget.builder(introsText(cfg), b -> {
            cfg.bossIntros = (cfg.bossIntros + 1) % 3;
            b.setMessage(introsText(cfg));
        }).dimensions(x, y + 2 * (h + gap), w, h).build();

        ButtonWidget shake = ButtonWidget.builder(shakeText(cfg), b -> {
            int step = Math.round(cfg.screenShake * 100) + 25;
            if (step > 100) step = 0;
            cfg.screenShake = step / 100f;
            b.setMessage(shakeText(cfg));
        }).dimensions(x, y + 3 * (h + gap), w, h).build();

        ButtonWidget done = ButtonWidget.builder(Text.literal("Done"), b -> this.close())
                .dimensions(x, y + 4 * (h + gap) + 8, w, h).build();

        addDrawableChild(intensity);
        addDrawableChild(flashSafe);
        addDrawableChild(intros);
        addDrawableChild(shake);
        addDrawableChild(done);
    }

    private Text intensityText(HSClientConfig cfg) {
        return Text.literal("Effects Intensity: " + cfg.intensityLabel());
    }

    private Text flashSafeText(HSClientConfig cfg) {
        return Text.literal("Flash-safe Mode: " + (cfg.flashSafe ? "ON" : "OFF"));
    }

    private Text introsText(HSClientConfig cfg) {
        return Text.literal("Boss Intros: " + cfg.bossIntrosLabel());
    }

    private Text shakeText(HSClientConfig cfg) {
        return Text.literal("Screen Shake: " + Math.round(cfg.screenShake * 100) + "%");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 84, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        HSClientConfig.save();
        if (this.client != null) this.client.setScreen(parent);
    }
}
