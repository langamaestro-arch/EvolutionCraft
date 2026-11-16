package es.evolutioncraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Segunda barrera para objetos superiores cuando el inventario está lleno. */
@EventBusSubscriber(modid=EvolutionCraft.ID)
public final class EquipmentGuard {
    private EquipmentGuard(){}
    private static int required(ItemStack stack){
        if(stack.isEmpty())return 0;
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return Math.max(Progress.requiredEra(id), RecipeEraRules.requiredEra(id));
    }
    private static boolean blocked(ServerPlayer p,ItemStack stack){
        return required(stack)>ProgressStore.get(p).era();
    }
    private static void warning(ServerPlayer p,int needed){
        if(p.tickCount%20==0){
            p.sendSystemMessage(Component.literal(
                "§c[EvolutionCraft] Mochila bloqueada. Requiere Era de "+
                Progress.ERA_NAMES[Math.min(5,needed)]+"."),true);
        }
    }
    @SubscribeEvent
    public static void breakBlock(PlayerInteractEvent.LeftClickBlock event){
        if(event.getEntity() instanceof ServerPlayer p&&blocked(p,p.getMainHandItem()))event.setCanceled(true);
    }
    @SubscribeEvent
    public static void useBlock(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        // Arsenal Forge modifica armas vanilla mediante componentes, no registra nuevas armas.
        // Su mesa de mejoras se desbloquea cuando el jugador alcanza Hierro.
        String block=BuiltInRegistries.BLOCK.getKey(event.getLevel()
            .getBlockState(event.getPos()).getBlock()).toString();
        if(block.equals("arsenalforge:arsenal_forge") && ProgressStore.get(p).era()<3){
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            if(p.tickCount%20==0)
                p.sendSystemMessage(Component.literal(
                    "§c[EvolutionCraft] Forja de Arsenal bloqueada: requiere Era de Hierro."),true);
            return;
        }
        // Las mochilas colocadas se abren usando el bloque, aunque las manos estén vacías.
        int blockEra=RecipeEraRules.requiredEra(block);
        if(block.startsWith("sophisticatedbackpacks:") &&
                block.endsWith("_backpack") && blockEra>ProgressStore.get(p).era()){
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            warning(p,blockEra);
            return;
        }
        if(blocked(p,event.getItemStack())){
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            warning(p,required(event.getItemStack()));
        }
    }
    @SubscribeEvent
    public static void useItem(PlayerInteractEvent.RightClickItem event){
        if(event.getEntity() instanceof ServerPlayer p&&blocked(p,event.getItemStack())){
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            warning(p,required(event.getItemStack()));
        }
    }
    /**
     * Sophisticated Backpacks también puede abrirse con una tecla, una
     * mochila equipada o desde otra mochila. Esos caminos no disparan
     * RightClickItem. Cerramos el menú únicamente cuando el propio
     * contenedor pertenece a una mochila restringida.
     * Reflection mantiene la dependencia opcional.
     */
    @SubscribeEvent
    public static void closeLockedBackpackMenu(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        Object menu=p.containerMenu;
        if(!menu.getClass().getName().equals(
                "net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer"))return;
        try {
            Object context=menu.getClass().getMethod("getBackpackContext").invoke(menu);
            Object wrapper=context.getClass().getMethod("getBackpackWrapper",
                net.minecraft.world.entity.player.Player.class).invoke(context,p);
            Object backpack=wrapper.getClass().getMethod("getBackpack").invoke(wrapper);
            if(backpack instanceof ItemStack stack && blocked(p,stack)){
                p.closeContainer();
                warning(p,required(stack));
            }
        } catch(ReflectiveOperationException | SecurityException ignored) {
            // Un cambio en la API de Sophisticated se gestiona con una
            // actualización de compatibilidad; nunca se destruye el inventario.
        }
    }
    @SubscribeEvent
    public static void attack(AttackEntityEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&blocked(p,p.getMainHandItem()))event.setCanceled(true);
    }
}
