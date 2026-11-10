package es.evolutioncraft;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.ArrayList;

/**
 * No altera el NBT ni destruye los objetos encontrados.
 * Se limita el nivel efectivo consultado por NeoForge mientras el objeto se equipa.
 */
@EventBusSubscriber(modid=EvolutionCraft.ID)
public final class EnchantingRules {
    private EnchantingRules(){}
    private static final Map<ItemInstance,Integer> EQUIPPED=
        java.util.Collections.synchronizedMap(new WeakHashMap<>());

    @SubscribeEvent
    public static void keepEquipment(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player) || player.tickCount%5!=0)return;
        int era=ProgressStore.get(player).era();
        for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
            ItemStack stack=player.getItemBySlot(slot);
            if(!stack.isEmpty())EQUIPPED.put(stack,era);
        }
    }

    private static int limit(int era,String enchant,int level){
        if(era==0)return 0;
        // Son encantamientos cuyo nivel I ya es excepcional.
        if(enchant.equals("soul_speed")||enchant.equals("wind_burst"))return era>=5?level:0;
        if(enchant.equals("mending")||enchant.equals("swift_sneak")||
           enchant.equals("frost_walker"))return era>=4?level:0;
        if(enchant.equals("silk_touch"))return era>=3?level:0;
        if(enchant.equals("fortune")||enchant.equals("looting"))
            return era>=4?level:(era>=3?Math.min(level,1):0);
        if(era==1)return Math.min(level,1);
        if(era==2)return Math.min(level,2);
        if(era==3)return Math.min(level,3);
        return level;
    }

    @SubscribeEvent
    public static void levels(GetEnchantmentLevelEvent event){
        Integer era=EQUIPPED.get(event.getStack());
        if(era==null||era>=5)return;
        var ench=event.getEnchantments();
        var only=event.getTargetEnchant();
        if(only!=null){apply(ench,only,era);return;}
        for(var h:new ArrayList<>(ench.keySet()))apply(ench,h,era);
    }
    private static void apply(net.minecraft.world.item.enchantment.ItemEnchantments.Mutable map,
                Holder<Enchantment> h,int era){
        String id=h.unwrapKey().map(k->k.identifier().getPath()).orElse("");
        if(id.isEmpty())return;
        int current=map.getLevel(h), allowed=limit(era,id,current);
        if(allowed!=current)map.set(h,allowed);
    }

    @SubscribeEvent
    public static void table(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        if(!event.getLevel().getBlockState(event.getPos()).is(Blocks.ENCHANTING_TABLE))return;
        if(ProgressStore.get(player).era()>=3)return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if(player.tickCount%20==0)player.sendSystemMessage(
            Component.literal("§c[EvolutionCraft] Mesa de encantamientos: se desbloquea en Hierro."),true);
    }
}
