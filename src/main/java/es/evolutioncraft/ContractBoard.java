package es.evolutioncraft;

import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tablón compatible con las aldeas vanilla y Hearthstead:
 * cualquier atril (lectern) sirve como tablón mientras sujetas papel.
 * El atril sigue funcionando normalmente sin papel en la mano.
 */
@EventBusSubscriber(modid=EvolutionCraft.ID)
public final class ContractBoard {
    private ContractBoard(){}

    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        if(!EvolutionConfig.get(player).contractsEnabled)return;
        if(!event.getLevel().getBlockState(event.getPos()).is(Blocks.LECTERN))return;
        if(!event.getItemStack().is(Items.PAPER))return;
        var json=JsonParser.parseString(GuideData.snapshot(player)).getAsJsonObject();
        json.addProperty("openContracts",true);
        PacketDistributor.sendToPlayer(player,new GuidePayload(json.toString()));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
