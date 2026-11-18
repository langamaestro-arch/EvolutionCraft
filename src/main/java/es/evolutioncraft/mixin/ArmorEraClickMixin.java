package es.evolutioncraft.mixin;

import es.evolutioncraft.EvolutionConfig;
import es.evolutioncraft.Progress;
import es.evolutioncraft.ProgressStore;
import es.evolutioncraft.RecipeEraRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents forbidden armor placement before vanilla mutates any inventory slot.
 * Cancelling the click also works if every player inventory slot is occupied.
 * Only targets the player's own inventory menu; normal item removal is permitted.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class ArmorEraClickMixin {
    @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
    private void evolutioncraft$denyEarlyArmor(int slotId,int button,ContainerInput type,
                                                 Player player,CallbackInfo ci){
        if(!(player instanceof ServerPlayer sp))return;
        if(!EvolutionConfig.get(sp).equipmentRestrictions)return;
        AbstractContainerMenu menu=(AbstractContainerMenu)(Object)this;
        if(menu!=sp.inventoryMenu)return;
        int era=ProgressStore.get(sp).era();

        // Shift clicking an armor item can auto-equip it, without a free slot.
        if(type==ContainerInput.QUICK_MOVE && slotId>=0 && slotId<menu.slots.size()
           && (slotId<5||slotId>8)){
            ItemStack source=menu.getSlot(slotId).getItem();
            if(isForbiddenArmor(source,era)){ci.cancel();return;}
        }
        if(slotId<5 || slotId>8)return;
        ItemStack incoming;
        if(type==ContainerInput.SWAP && button>=0 && button<9){
            incoming=sp.getInventory().getItem(button);
        }else if(type==ContainerInput.PICKUP || type==ContainerInput.QUICK_CRAFT){
            incoming=menu.getCarried();
        }else return;
        if(isForbiddenArmor(incoming,era))ci.cancel();
    }
    private static boolean isForbiddenArmor(ItemStack stack,int era){
        if(stack.isEmpty())return false;
        // A slot accepts the armor item only if it is equippable.
        if(!stack.has(net.minecraft.core.component.DataComponents.EQUIPPABLE))return false;
        String key=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return Math.max(Progress.requiredEra(key),RecipeEraRules.requiredEra(key))>era;
    }
}
