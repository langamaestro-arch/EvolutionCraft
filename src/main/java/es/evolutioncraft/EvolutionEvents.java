package es.evolutioncraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.Map;

/** Telemetria autoritativa por jugador para la progresion 2.0. */
@EventBusSubscriber(modid=EvolutionCraft.ID)
public final class EvolutionEvents {
    private EvolutionEvents() {}

    @SubscribeEvent
    public static void mine(BreakBlockEvent event) {
        if(event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) return;
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
    }

    @SubscribeEvent
    public static void scan(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer player) || player.tickCount%80!=0)return;
        Progress p=ProgressStore.get(player);
        boolean changed=false;
        if(player.level().dimension()==Level.OVERWORLD || player.level().dimension()==Level.NETHER) {
            String biome=player.level().getBiome(player.blockPosition()).unwrapKey()
                .map(k->k.identifier().toString()).orElse("");
            if(!biome.isEmpty()) {
                if(player.level().dimension()==Level.OVERWORLD) changed|=p.overworldBiomes.add(biome);
                else changed|=p.netherBiomes.add(biome);
            }
            var structures=player.level().structureManager().getAllStructuresAt(player.blockPosition());
            for(var structure:structures.keySet()){
                var key=player.level().registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(structure);
                if(key==null)continue;
                String id=key.toString();
                changed|=p.visitedStructures.add(id);
                if(id.contains("village")) p.visitedVillage=true;
                if(id.contains("ancient_city") || id.contains("trial_chambers")) changed|=p.completedMissions.add("deep_structure");
            }
        }
        long time=player.level().getOverworldClockTime();
        long day=time/24000L;
        long phase=time%24000L;
        // Solo se acredita tras presenciar el tramo nocturno y su amanecer.
        if(player.level().dimension()==Level.OVERWORLD) {
            if(phase>=13500 && phase<22500 && p.lastNightIndex!=day) {
                p.lastNightIndex=day; changed=true;
            }
            if(phase<1000 && p.lastNightIndex==day-1) {
                // Este contador se afina con los eventos de muerte y descanso.
                p.nightsSurvived++;
                p.lastNightIndex=-1;
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
