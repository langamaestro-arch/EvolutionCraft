package es.evolutioncraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Segunda barrera para objetos superiores cuando el inventario está lleno. */
@EventBusSubscriber(modid=EvolutionCraft.ID)
public final class EquipmentGuard {
    private EquipmentGuard(){}
    private static boolean blocked(ServerPlayer p,ItemStack stack){
        if(stack.isEmpty())return false;
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return Progress.requiredEra(id)>ProgressStore.get(p).era();
    }
    @SubscribeEvent
    public static void breakBlock(PlayerInteractEvent.LeftClickBlock event){
        if(event.getEntity() instanceof ServerPlayer p&&blocked(p,p.getMainHandItem()))event.setCanceled(true);
    }
    @SubscribeEvent
    public static void useBlock(PlayerInteractEvent.RightClickBlock event){
        if(event.getEntity() instanceof ServerPlayer p&&blocked(p,event.getItemStack()))event.setCanceled(true);
    }
    @SubscribeEvent
    public static void useItem(PlayerInteractEvent.RightClickItem event){
        if(event.getEntity() instanceof ServerPlayer p&&blocked(p,event.getItemStack()))event.setCanceled(true);
    }
    @SubscribeEvent
    public static void attack(AttackEntityEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&blocked(p,p.getMainHandItem()))event.setCanceled(true);
    }
}
