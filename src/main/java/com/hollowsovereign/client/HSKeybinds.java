package com.hollowsovereign.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class HSKeybinds {
    private HSKeybinds() {}

    public static final String CATEGORY = "category.hollowsovereign.main";

    /** Abilities 1..6 mapped to slots 0..5. */
    public static final KeyBinding[] ABILITIES = new KeyBinding[6];
    public static KeyBinding ULTIMATE;
    public static KeyBinding SKILL_TREE;

    private static final int[] DEFAULT_KEYS = {
        GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_G,
        GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C
    };

    public static void register() {
        for (int i = 0; i < 6; i++) {
            ABILITIES[i] = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.hollowsovereign.ability" + (i + 1),
                    InputUtil.Type.KEYSYM, DEFAULT_KEYS[i], CATEGORY));
        }
        ULTIMATE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hollowsovereign.ultimate", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
        SKILL_TREE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hollowsovereign.skilltree", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));
    }
}
