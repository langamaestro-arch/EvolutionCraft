package es.evolutioncraft;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Da avances NATIVOS con toast y sonido. Nunca se cuentan como avances minecraft originales. */
@EventBusSubscriber(modid=EvolutionCraft.ID)
public final class AdvancementAwards {
    private AdvancementAwards(){}

    private static void award(ServerPlayer player,String id) {
        AdvancementHolder holder=player.level().getServer().getAdvancements()
            .get(Identifier.fromNamespaceAndPath(EvolutionCraft.ID,id));
        if(holder!=null) player.getAdvancements().award(holder,"earned");
    }
    public static void synchronize(ServerPlayer player) {
        Progress p=ProgressStore.get(player);
        for(int i=1;i<=p.era();i++)award(player,"era_"+i);
        String[] groups={"pesca","agricultura","ganaderia","exploracion","herramientas","alquimia"};
        int[] levels={p.fishingLevel(),p.agricultureLevel(),p.husbandryLevel(),
            p.explorationLevel(),p.toolLevel(),p.alchemyLevel()};
        for(int i=0;i<groups.length;i++)
            for(int lvl=1;lvl<=levels[i];lvl++)award(player,groups[i]+"_"+lvl);
        for(String mission:new String[]{"wood","stone","copper","iron","nether","netherite"})
            if(p.completedMissions.contains(mission))award(player,"mision_"+mission);
        if(p.canNether()) award(player,"portal_nether");
        if(p.canEnd()) award(player,"portal_end");
        for(String secret:p.secretChallenges)award(player,"secreto_"+secret);
    }
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer player) || player.tickCount%80!=0)return;
        synchronize(player);
    }
}
