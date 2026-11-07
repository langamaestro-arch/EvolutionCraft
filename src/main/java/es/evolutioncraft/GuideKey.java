package es.evolutioncraft;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Tecla P: abre la pantalla de objetivos con datos recientes del servidor. */
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
    public static void registerPackets(RegisterClientPayloadHandlersEvent event) {
        event.register(GuidePayload.TYPE, (payload, context) -> {
            Minecraft.getInstance().setScreen(new ProgressScreen(payload.json()));
        });
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null || minecraft.screen() != null) return;
        while (SHOW.consumeClick()) minecraft.getConnection().sendCommand("ecprogreso");
    }
}
