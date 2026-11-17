package es.evolutioncraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/** Telemetria autoritativa por jugador para la progresion 2.0. */
@EventBusSubscriber(modid=EvolutionCraft.ID)
public final class EvolutionEvents {
    private EvolutionEvents() {}
    // Cache de la última posición revisada para no consultar estructuras sin movimiento.
    private static final Map<UUID,String> LAST_STRUCTURE_SCAN=new HashMap<>();

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent event){
        if(event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player))return;
        PlacedBlockStore.placed(player,event.getPos());
    }

    @SubscribeEvent
    public static void wake(PlayerWakeUpEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        Progress p=ProgressStore.get(player);
        p.cleanNights=0;
        p.lastNightIndex=-1;
        p.nightTicksObserved=0;
        ProgressStore.save(player);
    }

    @SubscribeEvent
    public static void mine(BreakBlockEvent event) {
        if(event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        if(PlacedBlockStore.consumePlaced(player,event.getPos())) return;
        String block=BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).toString();
        String item=BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
        Progress p=ProgressStore.get(player);
        boolean changed=false;
        if(item.startsWith("minecraft:")) {
            String tool=item.substring("minecraft:".length());
            if(tool.endsWith("_pickaxe") || tool.endsWith("_axe") || tool.endsWith("_shovel") || tool.endsWith("_hoe")) {
                if(tool.startsWith("wooden_")) p.toolWood++;
                if(tool.startsWith("stone_")) p.toolStone++;
                if(tool.startsWith("copper_")) p.toolCopper++;
                if(tool.startsWith("iron_")) p.toolIron++;
                if(tool.startsWith("diamond_")) p.toolDiamond++;
                p.toolTypes.add(tool);
                changed=true;
            }
        }
        if(block.equals("minecraft:copper_ore") || block.equals("minecraft:deepslate_copper_ore")){
            p.naturalCopperOre++;
            changed=true;
        }
        if(block.equals("minecraft:diamond_ore") || block.equals("minecraft:deepslate_diamond_ore")){
            p.naturalDiamondOre++;
            changed=true;
        }
        if(block.equals("minecraft:ancient_debris")){
            p.completedMissions.add("ancient_debris");
            changed=true;
        }
        if(changed) ProgressStore.save(player);
        if(block.equals("minecraft:copper_ore") || block.equals("minecraft:deepslate_copper_ore"))
            AdventureContracts.onAction(player,"mine","minecraft:copper_ore",1);
    }

    @SubscribeEvent
    public static void scan(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        int interval=EvolutionConfig.get(player).explorationInterval;
        if(player.tickCount%interval!=0)return;
        Progress p=ProgressStore.get(player);
        boolean changed=false;
        if(player.level().dimension()==Level.OVERWORLD || player.level().dimension()==Level.NETHER) {
            String biome=player.level().getBiome(player.blockPosition()).unwrapKey()
                .map(k->k.identifier().toString()).orElse("");
            if(!biome.isEmpty()) {
                boolean fresh=player.level().dimension()==Level.OVERWORLD
                    ?p.overworldBiomes.add(biome):p.netherBiomes.add(biome);
                if(fresh){
                    changed=true;
                    DiscoveryJournal.record(p,player,"biome",biome);
                    if(player.level().dimension()==Level.NETHER)
                        AdventureContracts.onAction(player,"nether_biome",biome,1);
                }
            }
            String scanKey=player.level().dimension().identifier().toString()+":"+player.blockPosition();
            String previous=LAST_STRUCTURE_SCAN.put(player.getUUID(),scanKey);
            if(!scanKey.equals(previous)){
            var structures=player.level().structureManager().getAllStructuresAt(player.blockPosition());
            for(var structure:structures.keySet()){
                var key=player.level().registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(structure);
                if(key==null)continue;
                String id=key.toString();
                if(p.visitedStructures.add(id)){
                    changed=true;
                    DiscoveryJournal.record(p,player,"structure",id);
                    AdventureContracts.onAction(player,"structure",id,1);
                }
                // Hearthstead reemplaza y amplía las aldeas vanilla con villages,
                // towns, hamlets y walled towns. Excluimos sus ruins porque no son aldeas.
                boolean village=id.contains("village") ||
                    (id.startsWith("hearthstead:") &&
                        (id.substring("hearthstead:".length()).startsWith("town_") ||
                         id.substring("hearthstead:".length()).startsWith("hamlet_") ||
                         id.substring("hearthstead:".length()).startsWith("walled_")));
                if(village && !p.visitedVillage){
                    p.visitedVillage=true;
                    changed=true; // Guardar incluso si la estructura ya estaba registrada.
                }
                if(id.contains("ancient_city") || id.contains("trial_chambers")) changed|=p.completedMissions.add("deep_structure");
            }
            }
        }
        long time=player.level().getOverworldClockTime();
        long day=time/24000L;
        long phase=time%24000L;
        // Solo se acredita tras presenciar el tramo nocturno y su amanecer.
        if(player.level().dimension()==Level.OVERWORLD) {
            if(phase>=13000 && phase<23000) {
                if(p.lastNightIndex!=day) {
                    p.lastNightIndex=day;
                    p.nightTicksObserved=0;
                }
                p.nightTicksObserved=Math.min(10000,p.nightTicksObserved+interval);
                changed=true;
            }
            if(phase<1200 && p.lastNightIndex==day-1) {
                // Exige al menos 7200 ticks presenciados; desconectarse no supera el reto.
                if(p.nightTicksObserved>=7200) {
                    p.nightsSurvived++;
                    p.cleanNights++;
                }
                p.lastNightIndex=-1;
                p.nightTicksObserved=0;
                changed=true;
            }
        }
        if(p.nightsSurvived>=1 && p.crafted.contains("minecraft:campfire")) {
            changed|=p.completedMissions.add("wood");
        }
        if(p.visitedVillage && p.tradeCount>=1) changed|=p.completedMissions.add("stone");
        if(p.naturalCopperOre>=32 && p.advancements.contains("minecraft:story/smelt_iron")) changed|=p.completedMissions.add("copper");
        if(p.naturalDiamondOre>=5 && p.completedMissions.contains("deep_structure")) changed|=p.completedMissions.add("iron");
        if(changed) ProgressStore.save(player);
    }
}
