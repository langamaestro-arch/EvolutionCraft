package es.evolutioncraft;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;

/**
 * Se registra exclusivamente al recoger el resultado del soporte de alquimia,
 * incluso si el jugador cerró la pantalla durante la preparación.
 */
@EventBusSubscriber(modid=EvolutionCraft.ID)
public final class BrewingTracker {
    private BrewingTracker(){}
    @SubscribeEvent
    public static void brewed(PlayerBrewedPotionEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack=event.getStack();
        PotionContents contents=stack.get(DataComponents.POTION_CONTENTS);
        if(contents==null)return;
        Progress p=ProgressStore.get(player);
        long tick=player.level().getGameTime();
        // Las tres botellas retiradas en el mismo instante pertenecen a una tanda.
        if(tick-p.lastBrewExtractionTick>20) p.brewingBatches++;
        p.lastBrewExtractionTick=tick;
        String key=contents.potion().flatMap(h->h.unwrapKey())
                .map(k->k.identifier().toString()).orElse("");
        if(key.startsWith("minecraft:")) {
            String name=key.substring(10);
            if(name.startsWith("long_")) {p.potionTechniques.add("extended");name=name.substring(5);}
            if(name.startsWith("strong_")) {p.potionTechniques.add("strong");name=name.substring(7);}
            if(!name.equals("water") && !name.equals("awkward") && !name.equals("mundane")
                && !name.equals("thick")) p.potionEffects.add("minecraft:"+name);
        }
        if(stack.is(Items.SPLASH_POTION))p.potionTechniques.add("splash");
        if(stack.is(Items.LINGERING_POTION)) {
            p.potionTechniques.add("lingering");
            p.lingeringBrews+=Math.max(1,stack.getCount());
        }
        ProgressStore.save(player);
    }
}
