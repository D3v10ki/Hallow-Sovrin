package com.hollowsovereign.client.screen;

import com.hollowsovereign.SorcererClass;
import com.hollowsovereign.client.ClientState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Placeholder skill-tree screen. The real 3-branch tree with fork nodes lands in
 * the Skill Trees phase; for now it shows the class branches and the current
 * skill-point balance so the K keybind is testable.
 */
@Environment(EnvType.CLIENT)
public class SkillTreeScreen extends Screen {

    private static final String[][] BRANCHES = {
        {"Gravity", "Void", "Warp"},
        {"Bulwark", "Sanguine", "Carnage"},
        {"Beastmaster", "Warder", "Hexer"},
        {"Duelist", "Tempest", "Executioner"},
    };

    public SkillTreeScreen() {
        super(Text.literal("Skill Tree"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        SorcererClass clazz = ClientState.sorcererClass();
        int cx = this.width / 2;
        int y = this.height / 4;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, y, 0xFFFFFFFF);
        if (clazz != null) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(clazz.displayName()).formatted(clazz.color()), cx, y + 14, 0xFFFFFFFF);
            String[] branches = BRANCHES[clazz.ordinal()];
            int by = y + 40;
            for (String branch : branches) {
                context.drawCenteredTextWithShadow(this.textRenderer,
                        Text.literal("◆ " + branch + " branch"), cx, by, 0xFFC0C0FF);
                by += 16;
            }
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Skill points: " + ClientState.skillPoints).formatted(Formatting.GOLD),
                    cx, by + 12, 0xFFFFD700);
        }
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Node allocation arrives in the Skill Trees phase.").formatted(Formatting.DARK_GRAY),
                cx, this.height - 40, 0xFF808080);
    }
}
