package es.evolutioncraft;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Locale;

/**
 * Recuperacion administrativa, solo permisos de operador.
 * La era forzada no modifica las estadisticas o logros naturales.
 * Los objetivos corregidos se guardan sin entregar recompensas duplicadas.
 */
@EventBusSubscriber(modid=EvolutionCraft.ID)
public final class AdminRecoveryCommands {
    private AdminRecoveryCommands(){}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event){

        event.getDispatcher().register(Commands.literal("ecera").requires(Commands.hasPermission(
                new net.minecraft.server.permissions.PermissionCheck.Require(
                    net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN)))
            .then(Commands.literal("establecer")
                .then(Commands.argument("jugador",StringArgumentType.word())
                    .then(Commands.argument("era",StringArgumentType.word())
                        .executes(ctx->setEra(
                            ctx.getSource().getServer().getPlayerList().getPlayerByName(
                                StringArgumentType.getString(ctx,"jugador")),
                            StringArgumentType.getString(ctx,"era"))))))
            .then(Commands.literal("restaurar")
                .then(Commands.argument("jugador",StringArgumentType.word())
                    .executes(ctx->resetEra(
                        ctx.getSource().getServer().getPlayerList().getPlayerByName(
                            StringArgumentType.getString(ctx,"jugador"))))))
            .then(Commands.literal("consultar")
                .then(Commands.argument("jugador",StringArgumentType.word())
                    .executes(ctx->status(
                        ctx.getSource().getServer().getPlayerList().getPlayerByName(
                            StringArgumentType.getString(ctx,"jugador")))))));
        // Atajo de emergencia para el jefe de la transicion Cobre -> Hierro.
        event.getDispatcher().register(Commands.literal("eczombi").requires(Commands.hasPermission(
                new net.minecraft.server.permissions.PermissionCheck.Require(
                    net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN)))
            .then(Commands.argument("jugador",StringArgumentType.word())
                .executes(ctx->completeFromSource(
                    ctx.getSource(),
                    StringArgumentType.getString(ctx,"jugador"),
                    "jefe","nicktaleepicdungeons:mutant_zombie"))));
        event.getDispatcher().register(Commands.literal("ecobjetivo").requires(Commands.hasPermission(
                new net.minecraft.server.permissions.PermissionCheck.Require(
                    net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN)))
            .then(Commands.literal("completar")
                .then(Commands.argument("jugador",StringArgumentType.word())
                    .then(Commands.argument("tipo",StringArgumentType.word())
                        .then(Commands.argument("id",StringArgumentType.word())
                            .executes(ctx->completeFromSource(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx,"jugador"),
                                StringArgumentType.getString(ctx,"tipo"),
                                StringArgumentType.getString(ctx,"id"))))))));
    }

    private static int parseEra(String name){
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
    private static void refresh(ServerPlayer player){
        ProgressStore.save(player);
        PacketDistributor.sendToPlayer(player,new GuidePayload(GuideData.snapshot(player)));
        // El HUD se refresca con la siguiente sincronizacion de objetivos.
    }
    private static int setEra(ServerPlayer player,String value){
        if(player==null)return 0;
        int era=parseEra(value);
        if(era<0){
            player.sendSystemMessage(Component.literal(
                "§c[EvolutionCraft] Era desconocida. Usa madera, piedra, cobre, hierro, diamante o netherita."),false);
            return 0;
        }
        Progress p=ProgressStore.get(player);
        p.adminEraOverride=era;
        refresh(player);
        player.sendSystemMessage(Component.literal(
            "§6[EvolutionCraft] Un administrador ha establecido tu era en "+
            Progress.ERA_NAMES[era]+". Los objetivos originales se conservan."),false);
        EvolutionCraftLogger.warn("Cambio administrativo de era para "+
            player.getUUID()+": "+Progress.ERA_NAMES[era]);
        return Command.SINGLE_SUCCESS;
    }
    private static int resetEra(ServerPlayer player){
        if(player==null)return 0;
        Progress p=ProgressStore.get(player);
        p.adminEraOverride=-1;
        if(p.adminEraObjectives!=null)p.adminEraObjectives.clear();
        refresh(player);
        player.sendSystemMessage(Component.literal(
            "§a[EvolutionCraft] Era administrativa restaurada: ahora se aplica la progresion real ("+
            Progress.ERA_NAMES[p.era()] + ")."),false);
        EvolutionCraftLogger.warn("Restauracion de era natural para "+player.getUUID());
        return Command.SINGLE_SUCCESS;
    }
    private static int status(ServerPlayer player){
        if(player==null)return 0;
        Progress p=ProgressStore.get(player);
        player.sendSystemMessage(Component.literal(
            "§e[EvolutionCraft] Era: "+Progress.ERA_NAMES[p.era()]+
            " | control: "+(p.adminEraOverride>=0?"administrador":"progresion natural")),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int completeFromSource(net.minecraft.commands.CommandSourceStack source,
                                           String playerName,String kind,String id){
        ServerPlayer target=source.getServer().getPlayerList().getPlayerByName(playerName);
        if(target==null){
            source.sendSystemMessage(Component.literal(
                "§c[EvolutionCraft] Jugador '"+playerName+"' no encontrado. Debe estar conectado."));
            return 0;
        }
        int result=complete(target,kind,id);
        if(result!=0){
            Progress p=ProgressStore.get(target);
            boolean verified="jefe".equalsIgnoreCase(kind) &&
                "nicktaleepicdungeons:mutant_zombie".equals(id)
                ? NicktaleBossGate.defeated(p,3):result>0;
            source.sendSystemMessage(Component.literal(
                (verified?"§a":"§c")+"[EvolutionCraft] "+kind+" '"+id+
                "' para "+target.getGameProfile().name()+
                (verified?" registrado correctamente.":" NO se ha podido verificar.")));
        }else{
            source.sendSystemMessage(Component.literal(
                "§c[EvolutionCraft] No se pudo registrar '"+id+
                "'. Revisa el tipo y el identificador."));
        }
        return result;
    }

    private static int complete(ServerPlayer player,String kind,String id){
        if(player==null)return 0;
        if(id.isBlank() || id.length()>160)return 0;
        Progress p=ProgressStore.get(player);
        boolean changed;
        switch(kind.toLowerCase(Locale.ROOT)){
            case "mision" -> changed=p.completedMissions.add(id);
            case "avance" -> {
                // Registra el progreso de EvolutionCraft, no altera la lista
                // de advancements vanilla ni concede recompensas de Minecraft.
                changed=p.advancements.add(id);
            }
            case "fabricacion" -> changed=p.crafted.add(id);
            case "comercio" -> {
                changed=p.trades.add(id);
                if(changed)p.tradeCount=Math.max(1,p.tradeCount);
            }
            case "jefe" -> {
                changed=p.bossKills.add(id);
                if(id.startsWith("nicktaleepicdungeons:")){
                    if(p.nicktaleEraBossKills==null)
                        p.nicktaleEraBossKills=new java.util.HashSet<>();
                    if(p.nicktaleDimensionBossKills==null)
                        p.nicktaleDimensionBossKills=new java.util.HashSet<>();
                    changed=p.nicktaleEraBossKills.add(id) || changed;
                    changed=p.nicktaleDimensionBossKills.add(id) || changed;
                }
            }
            default -> {
                player.sendSystemMessage(Component.literal(
                    "§c[EvolutionCraft] Tipos: mision, avance, fabricacion, comercio, jefe."),false);
                return 0;
            }
        }
        if(changed)refresh(player);
        player.sendSystemMessage(Component.literal(
            "§a[EvolutionCraft] Objetivo "+kind+" "+id+
            (changed?" marcado por un administrador.":" ya registrado.")+
            " No se conceden recompensas duplicadas."),false);
        if(changed)EvolutionCraftLogger.warn("Correccion administrativa para "+
            player.getUUID()+": "+kind+" "+id);
        return Command.SINGLE_SUCCESS;
    }
}
