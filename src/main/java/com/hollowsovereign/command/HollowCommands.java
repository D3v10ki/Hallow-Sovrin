package com.hollowsovereign.command;

import com.hollowsovereign.SorcererClass;
import com.hollowsovereign.data.PlayerData;
import com.hollowsovereign.leveling.Leveling;
import com.hollowsovereign.net.HSNet;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** /hollow admin commands (design doc section 16). Requires permission level 2. */
public final class HollowCommands {
    private HollowCommands() {}

    private static final SuggestionProvider<ServerCommandSource> CLASS_SUGGESTIONS = (ctx, builder) -> {
        for (SorcererClass c : SorcererClass.values()) builder.suggest(c.id());
        return builder.buildFuture();
    };

    public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
        dispatcher.register(CommandManager.literal("hollow")
            .requires(src -> src.hasPermissionLevel(2))
            .then(CommandManager.literal("class")
                .then(CommandManager.literal("set")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("class", StringArgumentType.word())
                            .suggests(CLASS_SUGGESTIONS)
                            .executes(HollowCommands::classSet))))
                .then(CommandManager.literal("reset")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(HollowCommands::classReset))))
            .then(CommandManager.literal("level")
                .then(CommandManager.literal("set")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, PlayerData.MAX_LEVEL))
                            .executes(HollowCommands::levelSet)))))
            .then(CommandManager.literal("xp")
                .then(CommandManager.literal("add")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                            .executes(HollowCommands::xpAdd)))))
            .then(CommandManager.literal("skillpoints")
                .then(CommandManager.literal("add")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                            .executes(HollowCommands::skillPointsAdd)))))
            .then(CommandManager.literal("cooldowns")
                .then(CommandManager.literal("reset")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(HollowCommands::cooldownsReset))))
            .then(CommandManager.literal("boss")
                .then(CommandManager.literal("summon")
                    .then(CommandManager.argument("boss", StringArgumentType.word())
                        .executes(ctx -> notYet(ctx, "Boss summoning"))))
                .then(CommandManager.literal("kill")
                    .executes(ctx -> notYet(ctx, "Boss kill"))))
        );
    }

    private static int classSet(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        String classId = StringArgumentType.getString(ctx, "class");
        SorcererClass clazz = SorcererClass.byIdOrNull(classId);
        if (clazz == null) {
            ctx.getSource().sendError(Text.literal("Unknown class: " + classId));
            return 0;
        }
        PlayerData data = PlayerData.get(player);
        data.setSorcererClass(clazz);
        HSNet.syncData(player);
        ctx.getSource().sendFeedback(() -> Text.literal("Set " + player.getName().getString() + " to " + clazz.displayName()), true);
        return 1;
    }

    private static int classReset(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        PlayerData.get(player).resetClass();
        HSNet.syncData(player);
        HSNet.openClassSelect(player);
        ctx.getSource().sendFeedback(() -> Text.literal("Reset " + player.getName().getString() + " (class + progression cleared)"), true);
        return 1;
    }

    private static int levelSet(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        PlayerData data = PlayerData.get(player);
        data.setLevel(amount);
        data.setXp(0);
        HSNet.syncData(player);
        ctx.getSource().sendFeedback(() -> Text.literal("Set " + player.getName().getString() + " to level " + amount), true);
        return 1;
    }

    private static int xpAdd(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        Leveling.addXp(player, PlayerData.get(player), amount);
        ctx.getSource().sendFeedback(() -> Text.literal("Gave " + amount + " XP to " + player.getName().getString()), true);
        return 1;
    }

    private static int skillPointsAdd(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        PlayerData.get(player).addSkillPoints(amount);
        HSNet.syncData(player);
        ctx.getSource().sendFeedback(() -> Text.literal("Gave " + amount + " skill point(s) to " + player.getName().getString()), true);
        return 1;
    }

    private static int cooldownsReset(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        PlayerData.get(player).clearCooldowns();
        ctx.getSource().sendFeedback(() -> Text.literal("Cleared cooldowns for " + player.getName().getString()), true);
        return 1;
    }

    private static int notYet(CommandContext<ServerCommandSource> ctx, String what) {
        ctx.getSource().sendError(Text.literal(what + " isn't implemented yet (coming in a later phase)."));
        return 0;
    }
}
