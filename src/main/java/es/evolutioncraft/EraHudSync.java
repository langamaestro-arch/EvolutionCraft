package es.evolutioncraft;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Sincronización por jugador, cada 2 segundos y sólo ante cambios. */
@EventBusSubscriber(modid=EvolutionCraft.ID)
public final class EraHudSync {
    private static final Map<UUID,String> LAST=new HashMap<>();
    private EraHudSync(){}

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        int interval=EvolutionConfig.get(p).hudSyncInterval;
        if(p.tickCount%interval!=0)return;
        String state=EraHudData.snapshot(p);
        String old=LAST.put(p.getUUID(),state);
        if(p.tickCount<=interval || !state.equals(old))
            PacketDistributor.sendToPlayer(p,new EraHudPayload(state));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer p)LAST.remove(p.getUUID());
    }
}
