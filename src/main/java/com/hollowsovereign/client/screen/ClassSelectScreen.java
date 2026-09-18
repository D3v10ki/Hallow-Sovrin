package com.hollowsovereign.client.screen;

import com.hollowsovereign.SorcererClass;
import com.hollowsovereign.net.HSNet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ClassSelectScreen extends Screen {

    private static final String[] BLURBS = {
        "Control — bend gravity and space; slow and gather foes, blink away.",
        "Tank / Sustain — soak hits, taunt bosses onto you, heal your team.",
        "Summoner / Support — shadow pets, buffs and curses; fastest revives.",
        "Melee DPS — parry master and boss-stagger breaker; katana lightning.",
    };

    public ClassSelectScreen() {
        super(Text.literal("Choose Your Path"));
    }

    @Override
    protected void init() {
        SorcererClass[] classes = SorcererClass.values();
        int btnW = 260, btnH = 24, gap = 8;
        int totalH = classes.length * (btnH + gap) - gap;
        int startY = this.height / 2 - totalH / 2 + 6;
        int x = this.width / 2 - btnW / 2;

        for (int i = 0; i < classes.length; i++) {
            SorcererClass c = classes[i];
            int y = startY + i * (btnH + gap);
            ButtonWidget btn = ButtonWidget.builder(
                    Text.literal(c.displayName() + "  —  " + c.role()).formatted(c.color()),
                    b -> choose(c))
                .dimensions(x, y, btnW, btnH)
                .tooltip(Tooltip.of(Text.literal(BLURBS[i])))
                .build();
            this.addDrawableChild(btn);
        }
    }

    private void choose(SorcererClass clazz) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(clazz.ordinal());
        ClientPlayNetworking.send(HSNet.SELECT_CLASS, buf);
        if (this.client != null) this.client.setScreen(null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Your cursed energy has awoken. This choice is permanent.").formatted(net.minecraft.util.Formatting.GRAY),
                this.width / 2, this.height / 2 - 54, 0xFFAAAAAA);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
