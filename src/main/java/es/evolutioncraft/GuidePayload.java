package es.evolutioncraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Datos servidor -> cliente de la pantalla de progresión. */
public record GuidePayload(String json) implements CustomPacketPayload {
    public static final Type<GuidePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(EvolutionCraft.ID, "guide"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuidePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, GuidePayload::json, GuidePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    @EventBusSubscriber(modid = EvolutionCraft.ID)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC);
        }
    }
}
