package es.evolutioncraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Vincula el reto sin morir al mismo ejemplar de Warden o dragón. */
@EventBusSubscriber(modid=EvolutionCraft.ID)
public final class SecretBossEvents {
    private SecretBossEvents(){}
    @SubscribeEvent
    public static void hit(LivingIncomingDamageEvent event) {
        if(!(event.getSource().getEntity() instanceof ServerPlayer player))return;
        String type=BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
        if(!type.equals("minecraft:warden")&&!type.equals("minecraft:ender_dragon"))return;
        Progress p=ProgressStore.get(player);
        String id=event.getEntity().getUUID().toString();
        if(type.equals("minecraft:warden")){
            if(!id.equals(p.wardenAttemptId)){
                p.wardenAttemptId=id;
                p.wardenAttemptDeaths=p.deathCount;
                ProgressStore.save(player);
            }
        }else if(!id.equals(p.dragonAttemptId)){
            p.dragonAttemptId=id;
            p.dragonAttemptDeaths=p.deathCount;
            ProgressStore.save(player);
        }
    }
}
