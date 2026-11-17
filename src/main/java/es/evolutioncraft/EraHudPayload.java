package es.evolutioncraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Datos compactos del HUD de la era; únicamente servidor a cliente. */
public record EraHudPayload(String json) implements CustomPacketPayload {
    public static final Type<EraHudPayload> TYPE=
        new Type<>(Identifier.fromNamespaceAndPath(EvolutionCraft.ID,"era_hud"));
    public static final StreamCodec<RegistryFriendlyByteBuf,EraHudPayload> STREAM_CODEC=
        StreamCodec.composite(ByteBufCodecs.STRING_UTF8,EraHudPayload::json,EraHudPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}

    @EventBusSubscriber(modid=EvolutionCraft.ID)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event){
            event.registrar("1").playToClient(TYPE,STREAM_CODEC);
        }
    }
}
