package com.hollowsovereign.client.hud;

import com.hollowsovereign.HollowSovereign;
import com.hollowsovereign.SorcererClass;
import com.hollowsovereign.ability.Abilities;
import com.hollowsovereign.client.ClientState;
import com.hollowsovereign.client.HSClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class HollowHud implements HudRenderCallback {

    private static final int SLOT = 22;
    private static final int GAP = 4;
    private static final int MARGIN = 8;
    private static final int SLOTS = 7; // 6 abilities + ultimate

    // Precomputed icon textures per class/slot.
    private static final Identifier[][] ABILITY_TEX = new Identifier[SorcererClass.values().length][6];
    private static final Identifier[] ULT_TEX = new Identifier[SorcererClass.values().length];
    static {
        for (SorcererClass c : SorcererClass.values()) {
            for (int s = 0; s < 6; s++) {
                ABILITY_TEX[c.ordinal()][s] = tex(c.id() + "_" + s);
            }
            ULT_TEX[c.ordinal()] = tex(c.id() + "_ult");
        }
    }

    private static Identifier tex(String name) {
        return new Identifier(HollowSovereign.MOD_ID, "textures/gui/ability/" + name + ".png");
    }

    @Override
    public void onHudRender(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (!ClientState.hasClass()) return;

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        TextRenderer tr = client.textRenderer;

        if (ClientState.inDomain()) drawVoidOverlay(ctx, sw, sh);
        drawFlash(ctx, sw, sh);
        drawClassBadge(ctx, tr);
        drawAbilityBar(ctx, tr, sw, sh);
        drawLevelUpPopup(ctx, tr, sw, sh);
    }

    /** Infinite-void interior: a dark tint + cinematic letterbox bars. */
    private void drawVoidOverlay(DrawContext ctx, int sw, int sh) {
        ctx.fill(0, 0, sw, sh, 0x55140026);
        int bar = sh / 12;
        ctx.fill(0, 0, sw, bar, 0xD8000000);
        ctx.fill(0, sh - bar, sw, sh, 0xD8000000);
    }

    private void drawClassBadge(DrawContext ctx, TextRenderer tr) {
        SorcererClass clazz = ClientState.sorcererClass();
        if (clazz == null) return;
        int x = 6, y = 6;
        int color = clazz.color().getColorValue() == null ? 0xFFFFFF : clazz.color().getColorValue();

        Text title = Text.literal(clazz.displayName() + "  ").append(Text.literal("Lv " + ClientState.level));
        ctx.drawTextWithShadow(tr, title, x, y, 0xFF000000 | color);

        int barW = 120, barH = 4, barY = y + 12;
        ctx.fill(x, barY, x + barW, barY + barH, 0xC0202020);
        float frac = ClientState.xpToNext <= 0 ? 1f : MathHelper.clamp((float) ClientState.xp / ClientState.xpToNext, 0f, 1f);
        ctx.fill(x, barY, x + (int) (barW * frac), barY + barH, 0xFF000000 | color);
        ctx.fill(x, barY, x + barW, barY + 1, 0x40FFFFFF);

        if (ClientState.skillPoints > 0) {
            ctx.drawTextWithShadow(tr, Text.literal("● " + ClientState.skillPoints + " SP"),
                    x, barY + 7, 0xFFFFD700);
        }
    }

    private void drawAbilityBar(DrawContext ctx, TextRenderer tr, int sw, int sh) {
        SorcererClass clazz = ClientState.sorcererClass();
        if (clazz == null) return;
        int ord = clazz.ordinal();
        int classColor = clazz.color().getColorValue() == null ? 0xFFFFFF : clazz.color().getColorValue();
        long now = System.currentTimeMillis();

        int x0 = MARGIN;
        int y = sh - MARGIN - SLOT;

        RenderSystem.enableBlend();
        for (int i = 0; i < SLOTS; i++) {
            int x = x0 + i * (SLOT + GAP);
            boolean isUlt = (i == 6);
            int unlock = Abilities.unlockLevel(clazz, isUlt ? Abilities.ULT_SLOT : i);
            boolean unlocked = ClientState.level >= unlock;
            boolean implemented = Abilities.isImplemented(clazz, isUlt ? Abilities.ULT_SLOT : i);
            boolean onCd = unlocked && ClientState.cooldownTotalMs[i] > 0 && now < ClientState.cooldownEndMs[i];
            boolean ready = unlocked && implemented && !onCd;

            // slot background
            ctx.fill(x, y, x + SLOT, y + SLOT, 0xD00C0C12);

            // ready glow (soft pulsing outline)
            if (ready) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(now / 320.0);
                int a = (int) (55 + 70 * pulse);
                drawBorder(ctx, x - 1, y - 1, SLOT + 2, SLOT + 2, (a << 24) | (classColor & 0xFFFFFF));
            }

            // icon
            Identifier icon = isUlt ? ULT_TEX[ord] : ABILITY_TEX[ord][i];
            if (!unlocked) {
                ctx.setShaderColor(0.42f, 0.42f, 0.5f, 0.55f);
            } else if (!implemented) {
                ctx.setShaderColor(0.75f, 0.75f, 0.8f, 0.9f);
            } else {
                ctx.setShaderColor(1f, 1f, 1f, 1f);
            }
            int inset = 2;
            ctx.drawTexture(icon, x + inset, y + inset, SLOT - 2 * inset, SLOT - 2 * inset,
                    0f, 0f, 64, 64, 64, 64);
            ctx.setShaderColor(1f, 1f, 1f, 1f);

            // frame
            int border = isUlt ? 0xFFB84DFF : (0xFF000000 | classColor);
            drawBorder(ctx, x, y, SLOT, SLOT, unlocked ? border : 0xFF262630);

            // cooldown wipe (dark recedes downward as it recharges) + seconds
            if (onCd) {
                float f = (float) (ClientState.cooldownEndMs[i] - now) / ClientState.cooldownTotalMs[i];
                int h = (int) (SLOT * MathHelper.clamp(f, 0f, 1f));
                ctx.fill(x, y, x + SLOT, y + h, 0xC004040A);
                int secs = (int) Math.ceil((ClientState.cooldownEndMs[i] - now) / 1000.0);
                String s = String.valueOf(secs);
                ctx.drawText(tr, s, x + (SLOT - tr.getWidth(s)) / 2 + 1, y + (SLOT - 8) / 2, 0xFFFFFFFF, true);
            }

            // locked slots show the level they unlock at
            if (!unlocked) {
                String lv = String.valueOf(unlock);
                ctx.drawText(tr, lv, x + SLOT - tr.getWidth(lv) - 1, y + SLOT - 8, 0xFFB0B0B8, true);
            }
        }
        RenderSystem.disableBlend();
    }

    private void drawFlash(DrawContext ctx, int sw, int sh) {
        long now = System.currentTimeMillis();
        if (now >= ClientState.flashEndMs || ClientState.flashTotalMs <= 0) return;
        HSClientConfig cfg = HSClientConfig.get();
        float frac = (float) (ClientState.flashEndMs - now) / ClientState.flashTotalMs;
        float intensity = cfg.intensityScale();
        int maxAlpha = ClientState.flashTier >= 3 ? 200 : 80;
        int alpha = (int) (maxAlpha * frac * intensity);
        if (alpha <= 2) return;

        int rgb = cfg.flashSafe ? 0x9B6BFF : 0xFFFFFF;
        if (cfg.flashSafe) alpha = Math.min(alpha, 70);
        int color = (alpha << 24) | (rgb & 0xFFFFFF);
        ctx.fill(0, 0, sw, sh, color);
    }

    private void drawLevelUpPopup(DrawContext ctx, TextRenderer tr, int sw, int sh) {
        long now = System.currentTimeMillis();
        if (now >= ClientState.levelUpEndMs) return;
        float remain = (ClientState.levelUpEndMs - now) / 2600f;
        int alpha = (int) (255 * Math.min(1f, remain * 1.5f));
        int y = sh / 3;
        Text main = Text.literal("LEVEL " + ClientState.levelUpLevel);
        Text sub = Text.literal("+1 Skill Point");
        ctx.drawCenteredTextWithShadow(tr, main, sw / 2, y, (alpha << 24) | 0xFFD700);
        ctx.drawCenteredTextWithShadow(tr, sub, sw / 2, y + 12, (alpha << 24) | 0xFFFFFF);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }
}
