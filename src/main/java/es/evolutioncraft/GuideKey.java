package es.evolutioncraft;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Tecla P configurable para consultar el progreso del jugador en el servidor. */
@EventBusSubscriber(modid = EvolutionCraft.ID, value = Dist.CLIENT)
public final class GuideKey {
    private static final KeyMapping SHOW = new KeyMapping(
            "key.evolutioncraft.guide", InputConstants.KEY_P, KeyMapping.Category.MISC);

    private GuideKey() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SHOW);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return;
        while (SHOW.consumeClick()) minecraft.getConnection().sendCommand("ecprogreso");
    }
}
