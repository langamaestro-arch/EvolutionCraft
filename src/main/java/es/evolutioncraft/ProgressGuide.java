package es.evolutioncraft;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Desglosa la progresión guardada en el servidor, sin alterar sus reglas. */
@EventBusSubscriber(modid = EvolutionCraft.ID)
public final class ProgressGuide {
    private ProgressGuide() {}

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        // Recuperación administrativa para la misión inicial atascada.
        // Permiso de administración del servidor; no accesible a jugadores normales.
        event.getDispatcher().register(Commands.literal("ecmision")
            .requires(Commands.hasPermission(
                new net.minecraft.server.permissions.PermissionCheck.Require(
                    net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN)))
            .then(Commands.literal("completar")
                .then(Commands.argument("jugador",StringArgumentType.word())
                    .then(Commands.literal("wood").executes(ctx->{
                        String name=StringArgumentType.getString(ctx,"jugador");
                        ServerPlayer target=ctx.getSource().getServer()
                            .getPlayerList().getPlayerByName(name);
                        if(target==null)return 0;
                        Progress progress=ProgressStore.get(target);
                        boolean added=progress.completedMissions.add("wood");
                        if(added)ProgressStore.save(target);
                        target.sendSystemMessage(Component.literal(
                            "§a[EvolutionCraft] Misión de la hoguera y primera noche "+
                            (added?"completada por un administrador.":"ya completada.")),false);
                        PacketDistributor.sendToPlayer(target,
                            new GuidePayload(GuideData.snapshot(target)));
                        return Command.SINGLE_SUCCESS;
                    })))));
        event.getDispatcher().register(Commands.literal("ectributo")
            .then(Commands.literal("entregar").executes(ctx->{
                ServerPlayer player=ctx.getSource().getPlayerOrException();
                int target=ProgressStore.get(player).era()+1;
                boolean done=ThoridropTribute.deliver(player,target);
                if(!done)player.sendSystemMessage(Component.literal(
                    "§c[EvolutionCraft] Aun no puedes entregar el tributo de esta era."),true);
                PacketDistributor.sendToPlayer(player,
                    new GuidePayload(GuideData.snapshot(player)));
                return done?Command.SINGLE_SUCCESS:0;
            })));

        event.getDispatcher().register(Commands.literal("ecrango")
            .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                ServerPlayer player=ctx.getSource().getPlayerOrException();
                String id=StringArgumentType.getString(ctx,"id");
                if(!AdventurerReputation.claim(player,id))
                    player.sendSystemMessage(Component.literal(
                        "§c[EvolutionCraft] Ascenso no desbloqueado o ya reclamado."),true);
                var view=com.google.gson.JsonParser.parseString(GuideData.snapshot(player)).getAsJsonObject();
                view.addProperty("openReputation",true);
                PacketDistributor.sendToPlayer(player,new GuidePayload(view.toString()));
                return Command.SINGLE_SUCCESS;
            })));

        event.getDispatcher().register(Commands.literal("ecepico")
            .then(Commands.literal("aceptar")
                .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->
                    epicAction(ctx.getSource().getPlayerOrException(),"aceptar",StringArgumentType.getString(ctx,"id")))))
            .then(Commands.literal("abandonar")
                .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->
                    epicAction(ctx.getSource().getPlayerOrException(),"abandonar",StringArgumentType.getString(ctx,"id")))))
            .then(Commands.literal("reclamar")
                .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->
                    epicAction(ctx.getSource().getPlayerOrException(),"reclamar",StringArgumentType.getString(ctx,"id"))))));
        event.getDispatcher().register(Commands.literal("eccontratos").executes(ctx->{
            ServerPlayer player=ctx.getSource().getPlayerOrException();
            var json=com.google.gson.JsonParser.parseString(GuideData.snapshot(player)).getAsJsonObject();
            json.addProperty("openContracts",true);
            PacketDistributor.sendToPlayer(player,new GuidePayload(json.toString()));
            return Command.SINGLE_SUCCESS;
        }));
        event.getDispatcher().register(Commands.literal("eccontrato")
            .then(Commands.literal("aceptar")
                .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->
                    contractAction(ctx.getSource().getPlayerOrException(),
                        "aceptar",StringArgumentType.getString(ctx,"id")))))
            .then(Commands.literal("abandonar")
                .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->
                    contractAction(ctx.getSource().getPlayerOrException(),
                        "abandonar",StringArgumentType.getString(ctx,"id")))))
            .then(Commands.literal("reclamar")
                .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->
                    contractAction(ctx.getSource().getPlayerOrException(),
                        "reclamar",StringArgumentType.getString(ctx,"id")))))
            .then(Commands.literal("entregar")
                .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->
                    contractAction(ctx.getSource().getPlayerOrException(),
                        "entregar",StringArgumentType.getString(ctx,"id"))))));

        event.getDispatcher().register(Commands.literal("ecreclamar")
            .then(Commands.argument("id",StringArgumentType.word()).executes(ctx -> {
                ServerPlayer player=ctx.getSource().getPlayerOrException();
                String rewardId=StringArgumentType.getString(ctx,"id");
                if(!Rewards.claim(player,rewardId)) {
                    player.sendSystemMessage(Component.literal(
                        "§c[EvolutionCraft] Recompensa no disponible, bloqueada o ya reclamada."),true);
                }
                PacketDistributor.sendToPlayer(player,new GuidePayload(GuideData.snapshot(player)));
                return Command.SINGLE_SUCCESS;
            })));

        event.getDispatcher().register(Commands.literal("ecprogreso").executes(ctx -> {
            PacketDistributor.sendToPlayer(ctx.getSource().getPlayerOrException(),
                    new GuidePayload(GuideData.snapshot(ctx.getSource().getPlayerOrException())));
            return Command.SINGLE_SUCCESS;
        }));
        event.getDispatcher().register(Commands.literal("ecdistintivo")
            .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                ServerPlayer player=ctx.getSource().getPlayerOrException();
                String id=StringArgumentType.getString(ctx,"id");
                Progress progress=ProgressStore.get(player);
                if(!progress.secretChallenges.contains(id)){
                    player.sendSystemMessage(Component.literal("§cNo tienes ese distintivo desbloqueado."),true);
                    return 0;
                }
                progress.selectedBadge=id;
                ProgressStore.save(player);
                player.sendSystemMessage(Component.literal("§aDistintivo seleccionado: "+id),true);
                return Command.SINGLE_SUCCESS;
            })));
        event.getDispatcher().register(Commands.literal("ecprogresochat").executes(ctx -> {
            show(ctx.getSource().getPlayerOrException());
            return Command.SINGLE_SUCCESS;
        }));
    }

    private static int epicAction(ServerPlayer player,String action,String id){
        boolean ok=switch(action){
            case "aceptar" -> EpicContracts.accept(player,id);
            case "abandonar" -> EpicContracts.abandon(player,id);
            case "reclamar" -> EpicContracts.claim(player,id);
            default -> false;
        };
        if(!ok)player.sendSystemMessage(Component.literal(
            "§c[EvolutionCraft] Contrato épico no disponible o no cumple requisitos."),true);
        var json=com.google.gson.JsonParser.parseString(GuideData.snapshot(player)).getAsJsonObject();
        json.addProperty("openContracts",true);
        PacketDistributor.sendToPlayer(player,new GuidePayload(json.toString()));
        return ok?Command.SINGLE_SUCCESS:0;
    }
    private static int contractAction(ServerPlayer player,String action,String id){
        boolean ok=switch(action){
            case "aceptar" -> AdventureContracts.accept(player,id);
            case "abandonar" -> AdventureContracts.abandon(player,id);
            case "reclamar" -> AdventureContracts.claim(player,id);
            case "entregar" -> AdventureContracts.deliver(player,id);
            default -> false;
        };
        if(!ok)player.sendSystemMessage(Component.literal(
            "§c[EvolutionCraft] Contrato no disponible o no cumple los requisitos."),true);
        var json=com.google.gson.JsonParser.parseString(GuideData.snapshot(player)).getAsJsonObject();
        json.addProperty("openContracts",true);
        PacketDistributor.sendToPlayer(player,new GuidePayload(json.toString()));
        return ok?Command.SINGLE_SUCCESS:0;
    }

    // Compatibilidad: /ecprogresochat conserva el listado textual.
    private static void line(ServerPlayer player, boolean done, String message) {
        player.sendSystemMessage(Component.literal((done ? "§a✔ " : "§c✘ ") + "§f" + message));
    }

    private static void items(ServerPlayer player, Progress p, String material, boolean armor) {
        String[] suffixes = armor
                ? new String[]{"helmet", "chestplate", "leggings", "boots"}
                : new String[]{"pickaxe", "axe", "sword", "shovel", "hoe"};
        String[] labels = armor
                ? new String[]{"casco", "peto", "pantalones", "botas"}
                : new String[]{"pico", "hacha", "espada", "pala", "azada"};
        for (int i = 0; i < suffixes.length; i++)
            line(player, p.crafted.contains("minecraft:" + material + "_" + suffixes[i]),
                    "Fabricar " + labels[i] + " de " + material);
    }

    private static boolean trade(Progress p, String material) {
        return p.trades.stream().anyMatch(s -> s.startsWith("minecraft:" + material + "_") &&
                (s.endsWith("_pickaxe") || s.endsWith("_axe") || s.endsWith("_sword") ||
                 s.endsWith("_shovel") || s.endsWith("_hoe")));
    }

    public static void show(ServerPlayer player) {
        Progress p = ProgressStore.get(player);
        int era = p.era();
        player.sendSystemMessage(Component.literal("§6§l════ EVOLUTION CRAFT · PROGRESO ════"));
        player.sendSystemMessage(Component.literal("§eEra actual: §f" + Progress.ERA_NAMES[era]
                + " §7(" + (era + 1) + "/" + Progress.ERA_NAMES.length + ")"));
        if (era == 5) {
            player.sendSystemMessage(Component.literal("§a¡Has alcanzado la última era!"));
        } else {
            player.sendSystemMessage(Component.literal("§e§lObjetivos para: " + Progress.ERA_NAMES[era + 1]));
            switch (era) {
                case 0 -> {
                    items(player, p, "wooden", false);
                    line(player, p.advancements.contains("minecraft:story/mine_stone"), "Logro: conseguir piedra");
                }
                case 1 -> {
                    items(player, p, "stone", false);
                    items(player, p, "leather", true);
                    line(player, p.tradeCount >= 1, "Comerciar con aldeanos (" + p.tradeCount + "/1)");
                }
                case 2 -> {
                    items(player, p, "copper", false);
                    items(player, p, "copper", true);
                    line(player, trade(p, "iron"), "Conseguir herramienta de hierro comerciando");
                    line(player, p.advancements.contains("minecraft:story/smelt_iron"), "Logro: fundir hierro");
                }
                case 3 -> {
                    items(player, p, "iron", false);
                    items(player, p, "iron", true);
                    line(player, trade(p, "diamond"), "Conseguir herramienta de diamante comerciando");
                    line(player, p.advancements.contains("minecraft:story/mine_diamond"), "Logro: encontrar diamante");
                }
                case 4 -> {
                    items(player, p, "diamond", false);
                    items(player, p, "diamond", true);
                    line(player, p.advancements.contains("minecraft:story/enter_the_nether"), "Logro: entrar al Nether");
                }
                default -> { }
            }
        }
        player.sendSystemMessage(Component.literal("§b§lPesca §7· nivel " + p.fishingLevel() + "/5"));
        line(player, p.fish >= 25, "Pesca I: 25 peces (" + p.fish + "/25)");
        line(player, p.fish >= 60, "Pesca II: 60 peces (" + p.fish + "/60)");
        line(player, p.fishSpecies.size() >= 3, "Pesca II: 3 especies (" + p.fishSpecies.size() + "/3)");
        line(player, p.fish >= 120, "Pesca III: 120 peces (" + p.fish + "/120)");
        line(player, p.fishSpecies.size() >= 4, "Pesca III: 4 especies (" + p.fishSpecies.size() + "/4)");
        line(player, p.fishingTreasures.size() >= 2, "Pesca III: 2 tesoros (" + p.fishingTreasures.size() + "/2)");
        line(player, p.fish >= 250, "Pesca IV: 250 peces (" + p.fish + "/250)");
        line(player, p.fishingTreasures.size() >= 3, "Pesca IV: 3 tesoros (" + p.fishingTreasures.size() + "/3)");
        line(player, p.fish >= 500, "Pesca V: 500 peces (" + p.fish + "/500)");
        line(player, p.advancements.contains("minecraft:husbandry/tactical_fishing"), "Pesca V: avance Pesca táctica");

        player.sendSystemMessage(Component.literal("§5§lAcceso al Nether §7" + (p.canNether() ? "§aDESBLOQUEADO" : "§cBLOQUEADO")));
        line(player, p.era() >= 4, "Era de Diamante alcanzada");
        line(player, p.fishingLevel() >= 3, "Pesca nivel 3");
        line(player, p.explorationLevel() >= 4, "Exploracion nivel 4");
        line(player, p.bossKills.contains("minecraft:warden"), "Derrotar al Warden");
        line(player, p.advancements.size() >= 40, "Avances originales (" + p.advancements.size() + "/40)");
        line(player, p.overworldBiomes.size() >= 20, "Biomas del Overworld (" + p.overworldBiomes.size() + "/20)");

        player.sendSystemMessage(Component.literal("§d§lAcceso al End §7" + (p.canEnd() ? "§aDESBLOQUEADO" : "§cBLOQUEADO")));
        line(player, p.canNether(), "Acceso al Nether desbloqueado");
        line(player, p.fishingLevel() == 5, "Pesca nivel 5");
        line(player, p.explorationLevel() >= 5, "Exploracion nivel 5");
        line(player, p.alchemyLevel() >= 3, "Alquimia nivel 3");
        line(player, p.bossKills.contains("minecraft:wither"), "Derrotar al Wither");
        line(player, p.advancements.size() >= 70, "Avances originales (" + p.advancements.size() + "/70)");
        line(player, p.advancements.contains("minecraft:nether/explore_nether"), "Logro: explorar biomas del Nether");
        player.sendSystemMessage(Component.literal("§7Pulsa T para revisar el historial del chat."));
    }
}
