package es.evolutioncraft;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

/** Comandos administrativos para registrar objetivos ya realizados. */
@EventBusSubscriber(modid = EvolutionCraft.ID)
public final class MissionCommands {
    private MissionCommands() {}

    private static final List<String> OBJECTIVES = List.of(
            "madera", "piedra", "cobre", "hierro", "diamante",
            "pesca1", "pesca2", "pesca3", "pesca4", "pesca5",
            "agricultura1", "agricultura2", "agricultura3", "agricultura4", "agricultura5",
            "ganaderia1", "ganaderia2", "ganaderia3", "ganaderia4", "ganaderia5",
            "warden", "wither");

    private static final SuggestionProvider<CommandSourceStack> SUGGESTIONS = (ctx, builder) -> {
        for (String name : OBJECTIVES) if (name.startsWith(builder.getRemainingLowerCase())) builder.suggest(name);
        return builder.buildFuture();
    };

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ecmision")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("completar")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .then(Commands.argument("objetivo", StringArgumentType.word())
                                        .suggests(SUGGESTIONS)
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "jugador");
                                            String goal = StringArgumentType.getString(ctx, "objetivo").toLowerCase(java.util.Locale.ROOT);
                                            if (!OBJECTIVES.contains(goal)) {
                                                ctx.getSource().sendFailure(Component.literal("Objetivo desconocido. Usa TAB para ver las opciones."));
                                                return 0;
                                            }
                                            Progress p = ProgressStore.get(target);
                                            int oldEra = p.era();
                                            complete(p, goal);
                                            ProgressStore.save(target);
                                            String msg = "[Evolution Craft] Objetivo completado: " + goal;
                                            target.sendSystemMessage(Component.literal("§a" + msg));
                                            ctx.getSource().sendSuccess(() -> Component.literal("§a" + msg + " para " + target.getName().getString()), true);
                                            if (p.era() > oldEra) target.sendSystemMessage(Component.literal("§aNueva era desbloqueada: " + Progress.ERA_NAMES[p.era()]));
                                            return Command.SINGLE_SUCCESS;
                                        })))));
    }

    private static void tools(Progress p, String material) {
        for (String suffix : new String[]{"pickaxe", "axe", "sword", "shovel", "hoe"})
            p.crafted.add("minecraft:" + material + "_" + suffix);
    }
    private static void armor(Progress p, String material) {
        for (String suffix : new String[]{"helmet", "chestplate", "leggings", "boots"})
            p.crafted.add("minecraft:" + material + "_" + suffix);
    }
    private static void complete(Progress p, String goal) {
        switch (goal) {
            case "madera" -> { tools(p, "wooden"); p.advancements.add("minecraft:story/mine_stone"); }
            case "piedra" -> { tools(p, "stone"); armor(p, "leather"); p.tradeCount = Math.max(p.tradeCount, 1); }
            case "cobre" -> { tools(p, "copper"); armor(p, "copper"); p.trades.add("minecraft:iron_pickaxe"); p.advancements.add("minecraft:story/smelt_iron"); }
            case "hierro" -> { tools(p, "iron"); armor(p, "iron"); p.trades.add("minecraft:diamond_pickaxe"); p.advancements.add("minecraft:story/mine_diamond"); }
            case "diamante" -> { tools(p, "diamond"); armor(p, "diamond"); p.advancements.add("minecraft:story/enter_the_nether"); }
            case "warden" -> p.bossKills.add("minecraft:warden");
            case "wither" -> p.bossKills.add("minecraft:wither");
            default -> {
                if (goal.startsWith("pesca")) {
                    int level = Integer.parseInt(goal.substring(5));
                    if (level >= 1) p.fish = Math.max(p.fish, 25);
                    if (level >= 2) {
                        p.fish = Math.max(p.fish, 60);
                        p.fishSpecies.addAll(List.of("minecraft:cod", "minecraft:salmon", "minecraft:pufferfish"));
                    }
                    if (level >= 3) {
                        p.fish = Math.max(p.fish, 120);
                        p.fishSpecies.add("minecraft:tropical_fish");
                        p.fishingTreasures.addAll(List.of("minecraft:name_tag", "minecraft:saddle"));
                    }
                    if (level >= 4) {
                        p.fish = Math.max(p.fish, 250);
                        p.fishingTreasures.add("minecraft:enchanted_book");
                    }
                    if (level >= 5) {
                        p.fish = Math.max(p.fish, 500);
                        p.advancements.add("minecraft:husbandry/tactical_fishing");
                    }
                } else if (goal.startsWith("agricultura")) {
                    int level = Integer.parseInt(goal.substring(11));
                    if (level >= 1) p.wheatHarvests = Math.max(p.wheatHarvests, 32);
                    if (level >= 2) p.harvested.addAll(List.of("minecraft:wheat", "minecraft:carrots", "minecraft:potatoes", "minecraft:beetroots"));
                    if (level >= 3) { p.harvests = Math.max(p.harvests, 250); p.harvested.addAll(List.of("minecraft:melon", "minecraft:pumpkin")); }
                    if (level >= 4) { p.harvests = Math.max(p.harvests, 500); p.harvested.addAll(List.of("minecraft:cocoa", "minecraft:nether_wart")); }
                    if (level >= 5) p.harvests = Math.max(p.harvests, 1000);
                } else if (goal.startsWith("ganaderia")) {
                    int level = Integer.parseInt(goal.substring(9));
                    if (level >= 1) p.breedings = Math.max(p.breedings, 5);
                    if (level >= 2) p.bred.addAll(List.of("minecraft:cow", "minecraft:sheep", "minecraft:pig", "minecraft:chicken"));
                    if (level >= 3) { p.breedings = Math.max(p.breedings, 30); p.tamed.addAll(List.of("minecraft:wolf", "minecraft:cat")); }
                    if (level >= 4) { p.breedings = Math.max(p.breedings, 75); p.bred.add("minecraft:horse"); }
                    if (level >= 5) p.breedings = Math.max(p.breedings, 150);
                }
            }
        }
    }
}
