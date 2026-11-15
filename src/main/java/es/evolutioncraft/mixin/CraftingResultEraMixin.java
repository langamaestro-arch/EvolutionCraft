package es.evolutioncraft.mixin;

import es.evolutioncraft.Progress;
import es.evolutioncraft.ProgressStore;
import es.evolutioncraft.RecipeEraRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Validación antes de tomar el resultado: el jugador conserva los ingredientes
 * si todavía no ha alcanzado la era requerida.
 */
@Mixin(Slot.class)
public abstract class CraftingResultEraMixin {
    @Inject(method="mayPickup", at=@At("HEAD"), cancellable=true)
    private void evolutioncraft$denyEarlyModCraft(Player player, CallbackInfoReturnable<Boolean> cir){
        if(!((Object)this instanceof ResultSlot))return;
        if(!(player instanceof ServerPlayer sp))return;
        if(player.getAbilities().instabuild)return;
        int needed=RecipeEraRules.requiredEra(((Slot)(Object)this).getItem());
        if(needed>ProgressStore.get(sp).era()){
            cir.setReturnValue(false);
        }
    }
}
