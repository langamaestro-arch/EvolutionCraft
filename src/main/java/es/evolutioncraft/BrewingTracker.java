package es.evolutioncraft;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sigue el resultado de la elaboracion mientras el jugador observa su soporte.
 * No acredita pociones obtenidas en cofres, en el inventario o por comandos.
 */
@EventBusSubscriber(modid = EvolutionCraft.ID)
public final class BrewingTracker {
    private BrewingTracker() {}
    private record View(BrewingStandMenu menu, int ticks, String before) {}
    private static final Map<UUID,View> VIEWERS = new HashMap<>();

    private static String signature(BrewingStandMenu menu){
        StringBuilder signature=new StringBuilder();
        for(int i=0;i<3;i++){
            ItemStack item=menu.getSlot(i).getItem();
            PotionContents contents=item.get(DataComponents.POTION_CONTENTS);
            if(contents==null) {signature.append("-;");continue;}
            String name=contents.potion().flatMap(h->h.unwrapKey())
                .map(k->k.identifier().toString()).orElse("none");
            signature.append(item.getItem().toString()).append(':').append(name).append(';');
        }
        return signature.toString();
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        UUID id=player.getUUID();
        if(!(player.containerMenu instanceof BrewingStandMenu menu)){
            VIEWERS.remove(id);
            return;
        }
        int ticks=menu.getBrewingTicks();
        String current=signature(menu);
        View old=VIEWERS.put(id,new View(menu,ticks,current));
        if(old==null || old.menu()!=menu || old.ticks()<=0 || ticks!=0 ||
            old.before().equals(current)) return;

        Progress p=ProgressStore.get(player);
        p.brewingBatches++;
        Set<String> observed=new HashSet<>();
        for(int i=0;i<3;i++){
            ItemStack stack=menu.getSlot(i).getItem();
            PotionContents contents=stack.get(DataComponents.POTION_CONTENTS);
            if(contents==null)continue;
            String potion=contents.potion().flatMap(h->h.unwrapKey())
                .map(k->k.identifier().toString()).orElse("");
            if(potion.startsWith("minecraft:")){
                String name=potion.substring("minecraft:".length());
                if(name.startsWith("long_")){p.potionTechniques.add("extended");name=name.substring(5);}
                if(name.startsWith("strong_")){p.potionTechniques.add("strong");name=name.substring(7);}
                if(!name.equals("water") && !name.equals("awkward") && !name.equals("mundane") && !name.equals("thick"))
                    observed.add("minecraft:"+name);
            }
            if(stack.is(Items.SPLASH_POTION)) p.potionTechniques.add("splash");
            if(stack.is(Items.LINGERING_POTION)) p.potionTechniques.add("lingering");
        }
        p.potionEffects.addAll(observed);
        ProgressStore.save(player);
    }
}
