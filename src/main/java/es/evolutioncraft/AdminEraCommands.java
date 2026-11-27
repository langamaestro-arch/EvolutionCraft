package es.evolutioncraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Locale;

/** Operator-only repair commands for both stages and individual era objectives. */
@EventBusSubscriber(modid=EvolutionCraft.ID)
public final class AdminEraCommands {
    private AdminEraCommands(){}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("ecera")
            .requires(Commands.hasPermission(
                new net.minecraft.server.permissions.PermissionCheck.Require(
                    net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN)))
            .then(Commands.literal("avanzar")
                .then(Commands.argument("jugador",StringArgumentType.word()).suggests((ctx,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(),b))
                    .then(Commands.argument("era",StringArgumentType.word()).suggests((ctx,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.List.of("madera","piedra","cobre","hierro","diamante","netherita"),b))
                        .executes(ctx->setStage(ctx.getSource(),
                            StringArgumentType.getString(ctx,"jugador"),
                            StringArgumentType.getString(ctx,"era"),true)))))
            .then(Commands.literal("retroceder")
                .then(Commands.argument("jugador",StringArgumentType.word()).suggests((ctx,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(),b))
                    .then(Commands.argument("era",StringArgumentType.word()).suggests((ctx,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.List.of("madera","piedra","cobre","hierro","diamante","netherita"),b))
                        .executes(ctx->setStage(ctx.getSource(),
                            StringArgumentType.getString(ctx,"jugador"),
                            StringArgumentType.getString(ctx,"era"),false))))));

        var individual=Commands.literal("ecobjetivoera")
            .requires(Commands.hasPermission(
                new net.minecraft.server.permissions.PermissionCheck.Require(
                    net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN)))
            .then(Commands.literal("listar")
                .then(Commands.argument("jugador",StringArgumentType.word()).suggests((ctx,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(),b))
                    .then(Commands.argument("era",StringArgumentType.word()).suggests((ctx,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.List.of("madera","piedra","cobre","hierro","diamante","netherita"),b))
                        .executes(ctx->list(ctx.getSource(),
                            StringArgumentType.getString(ctx,"jugador"),
                            StringArgumentType.getString(ctx,"era"))))))
            .then(Commands.literal("completar")
                .then(Commands.argument("jugador",StringArgumentType.word()).suggests((ctx,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(),b))
                    .then(Commands.argument("era",StringArgumentType.word()).suggests((ctx,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.List.of("madera","piedra","cobre","hierro","diamante","netherita"),b))
                        .then(Commands.argument("numero",IntegerArgumentType.integer(1,99)).suggests(AdminEraCommands::suggestGoals)
                            .executes(ctx->changeGoal(ctx.getSource(),
                                StringArgumentType.getString(ctx,"jugador"),
                                StringArgumentType.getString(ctx,"era"),
                                IntegerArgumentType.getInteger(ctx,"numero"),true))))))
            .then(Commands.literal("pendiente")
                .then(Commands.argument("jugador",StringArgumentType.word()).suggests((ctx,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(),b))
                    .then(Commands.argument("era",StringArgumentType.word()).suggests((ctx,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.List.of("madera","piedra","cobre","hierro","diamante","netherita"),b))
                        .then(Commands.argument("numero",IntegerArgumentType.integer(1,99)).suggests(AdminEraCommands::suggestGoals)
                            .executes(ctx->changeGoal(ctx.getSource(),
                                StringArgumentType.getString(ctx,"jugador"),
                                StringArgumentType.getString(ctx,"era"),
                                IntegerArgumentType.getInteger(ctx,"numero"),false))))));
        event.getDispatcher().register(individual);
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestGoals(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder b){
        ServerPlayer player=ctx.getSource().getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(ctx,"jugador"));
        int stage=era(StringArgumentType.getString(ctx,"era"));
        if(player!=null && stage>=1){
            var goals=AdminEraObjectives.goals(JsonParser.parseString(GuideData.snapshot(player)).getAsJsonObject(),stage);
            for(int i=0;i<goals.size();i++){
                String number=Integer.toString(i+1);
                if(number.startsWith(b.getRemaining())) b.suggest(i+1,new com.mojang.brigadier.LiteralMessage(goals.get(i).getAsJsonObject().get("text").getAsString()));
            }
        }
        return b.buildFuture();
    }
    private static int era(String name){
        return switch(name.toLowerCase(Locale.ROOT)){
            case "madera","0" -> 0;
            case "piedra","cuero","1" -> 1;
            case "cobre","2" -> 2;
            case "hierro","3" -> 3;
            case "diamante","4" -> 4;
            case "netherita","netherite","5" -> 5;
            default -> -1;
        };
    }
    private static ServerPlayer target(CommandSourceStack source,String username){
        ServerPlayer target=source.getServer().getPlayerList().getPlayerByName(username);
        if(target==null)source.sendSystemMessage(Component.literal(
            "§c[EvolutionCraft] No se ha encontrado a "+username+". Debe estar conectado."));
        return target;
    }
    private static void refresh(ServerPlayer player){
        ProgressStore.save(player);
        PacketDistributor.sendToPlayer(player,new GuidePayload(GuideData.snapshot(player)));
    }
    private static int setStage(CommandSourceStack source,String username,String name,
                                boolean advance){
        ServerPlayer player=target(source,username);
        if(player==null)return 0;
        int requested=era(name);
        if(requested<0){
            source.sendSystemMessage(Component.literal(
                "§c[EvolutionCraft] Era invalida: madera, piedra, cobre, hierro, diamante o netherita."));
            return 0;
        }
        Progress p=ProgressStore.get(player);
        int current=p.era();
        if(advance && requested<current || !advance && requested>current){
            source.sendSystemMessage(Component.literal(
                "§c[EvolutionCraft] El jugador esta en "+Progress.ERA_NAMES[current]+
                "; usa /ecera "+(advance?"retroceder":"avanzar")+" para ese cambio."));
            return 0;
        }
        p.adminEraOverride=requested;
        if(advance)AdminEraObjectives.clearThrough(p,requested);
        else AdminEraObjectives.clearFrom(p,requested+1);
        refresh(player);
        String msg="[EvolutionCraft] "+username+" -> "+Progress.ERA_NAMES[requested]+
            ". Requisitos anteriores: completos; requisitos posteriores: pendientes.";
        source.sendSystemMessage(Component.literal("§a"+msg));
        player.sendSystemMessage(Component.literal("§6"+msg));
        EvolutionCraftLogger.warn("ADMIN ERA "+username+" ("+player.getUUID()+") "+
            current+" -> "+requested+" by "+source.getTextName());
        return Command.SINGLE_SUCCESS;
    }
    private static int list(CommandSourceStack source,String username,String name){
        ServerPlayer player=target(source,username);
        if(player==null)return 0;
        int stage=era(name);
        if(stage<1){
            source.sendSystemMessage(Component.literal(
                "§c[EvolutionCraft] Indica una era que se pueda desbloquear: piedra, cobre, hierro, diamante, netherita."));
            return 0;
        }
        JsonObject root=JsonParser.parseString(GuideData.snapshot(player)).getAsJsonObject();
        var goals=AdminEraObjectives.goals(root,stage);
        source.sendSystemMessage(Component.literal("§6[EvolutionCraft] "+username+
            " | Para desbloquear "+Progress.ERA_NAMES[stage]+" | "+goals.size()+" objetivos"));
        for(int i=0;i<goals.size();i++){
            JsonObject goal=goals.get(i).getAsJsonObject();
            source.sendSystemMessage(Component.literal(
                (goal.get("done").getAsBoolean()?"§a✔ ":"§c✘ ")+
                "§e"+(i+1)+". §f"+goal.get("text").getAsString()));
        }
        source.sendSystemMessage(Component.literal(
            "§7/ecobjetivoera completar|pendiente "+username+" "+name+" NUMERO"));
        return Command.SINGLE_SUCCESS;
    }
    private static int changeGoal(CommandSourceStack source,String username,String name,
                                  int number,boolean done){
        ServerPlayer player=target(source,username);
        if(player==null)return 0;
        int stage=era(name);
        if(stage<1){
            source.sendSystemMessage(Component.literal(
                "§c[EvolutionCraft] Solo: piedra, cobre, hierro, diamante, netherita."));
            return 0;
        }
        JsonObject root=JsonParser.parseString(GuideData.snapshot(player)).getAsJsonObject();
        var goals=AdminEraObjectives.goals(root,stage);
        if(number>goals.size()){
            source.sendSystemMessage(Component.literal(
                "§c[EvolutionCraft] Objetivo inexistente: hay "+goals.size()+
                ". Usa /ecobjetivoera listar "+username+" "+name));
            return 0;
        }
        String label=goals.get(number-1).getAsJsonObject().get("text").getAsString();
        Progress p=ProgressStore.get(player);
        int current=p.era();
        if(p.adminEraOverride<0)p.adminEraOverride=current;
        if(!done && stage<=current){
            // A requirement from an already granted era is being revoked.
            p.adminEraOverride=stage-1;
            AdminEraObjectives.clearFrom(p,stage);
        }
        if(p.adminEraObjectives==null)
            p.adminEraObjectives=new java.util.HashMap<>();
        p.adminEraObjectives.put(AdminEraObjectives.id(stage,number),done);
        if(done && stage==p.era()+1){
            // Complete all individual objectives -> grant the next era,
            // without writing fictitious vanilla advancements or Thoridrop wins.
            JsonObject refreshed=JsonParser.parseString(GuideData.snapshot(player)).getAsJsonObject();
            if(AdminEraObjectives.allComplete(refreshed,stage)){
                p.adminEraOverride=stage;
                AdminEraObjectives.clearThrough(p,stage);
            }
        }
        refresh(player);
        source.sendSystemMessage(Component.literal(
            "§a[EvolutionCraft] "+username+" | "+Progress.ERA_NAMES[stage]+
            " | "+number+" ("+label+"): "+(done?"COMPLETADO":"PENDIENTE")+
            " | era efectiva: "+Progress.ERA_NAMES[p.era()]));
        player.sendSystemMessage(Component.literal(
            "§6[EvolutionCraft] Objetivo administrado: "+label+
            " -> "+(done?"completado":"pendiente")));
        EvolutionCraftLogger.warn("ADMIN OBJETIVO "+player.getUUID()+" e"+stage+"_"+number+
            "="+done+" by "+source.getTextName());
        return Command.SINGLE_SUCCESS;
    }
}
