package es.evolutioncraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Prediction;
import net.minecraft.world.level.Level;
import java.util.Locale;

@Mod(EvolutionCraft.ID)
public final class EvolutionCraft {
    public static final String ID = "evolutioncraft";
    public EvolutionCraft() { NeoForge.EVENT_BUS.register(this); }

    private static ServerPlayer server(Player player) {
        return player instanceof ServerPlayer sp ? sp : null;
    }
    private static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
    private static void update(ServerPlayer player, java.util.function.Consumer<Progress> change) {
        Progress p = ProgressStore.get(player);
        int previous = p.era();
        change.accept(p);
        ProgressStore.save(player);
        int current = p.era();
        if (current > previous) {
            player.sendSystemMessage(Component.literal("§a[Evolution Craft] ¡Nueva era desbloqueada! " + Progress.ERA_NAMES[current]), false);
        }
    }

    @SubscribeEvent
    public void crafted(PlayerEvent.ItemCraftedEvent event) {
        ServerPlayer player = server(event.getEntity());
        if (player == null) return;
        String item = id(event.getCrafting());
        update(player, p -> p.crafted.add(item));
    }

    @SubscribeEvent
    public void traded(TradeWithVillagerEvent event) {
        ServerPlayer player = server(event.getEntity());
        if (player == null) return;
        String item = id(event.getMerchantOffer().getResult());
        update(player, p -> {
            p.trades.add(item);
            p.tradeCount++;
        });
    }

    @SubscribeEvent
    public void advancement(AdvancementEvent.AdvancementEarnEvent event) {
        ServerPlayer player = server(event.getEntity());
        if (player == null) return;
        String key = event.getAdvancement().id().toString();
        // Excluir recetas ocultas y avances propios del mod del contador original.
        if (key.startsWith("minecraft:recipes/") || key.startsWith(ID + ":")) return;
        update(player, p -> p.advancements.add(key));
    }

    @SubscribeEvent
    public void fished(ItemFishedEvent event) {
        ServerPlayer player = server(event.getEntity());
        if (player == null) return;
        update(player, p -> {
            for (ItemStack stack : event.getDrops()) {
                String item = id(stack);
                if (item.equals("minecraft:cod") || item.equals("minecraft:salmon") ||
                    item.equals("minecraft:pufferfish") || item.equals("minecraft:tropical_fish")) {
                    p.fish += stack.getCount();
                    p.fishSpecies.add(item);
                } else if (item.equals("minecraft:enchanted_book") || item.equals("minecraft:name_tag") ||
                           item.equals("minecraft:saddle") || item.equals("minecraft:nautilus_shell")) {
                    p.fishingTreasures.add(item);
                }
            }
        });
    }

    @SubscribeEvent
    public void defeated(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        String entity = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
        if (entity.equals("minecraft:warden") || entity.equals("minecraft:wither")) {
            update(player, p -> p.bossKills.add(entity));
        }
    }

    // La validación se realiza ANTES del teletransporte, por jugador y en servidor.
    @SubscribeEvent
    public void dimensionTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Progress progress = ProgressStore.get(player);
        boolean netherDenied = event.getDimension() == Level.NETHER && !progress.canNether();
        boolean endDenied = event.getDimension() == Level.END && !progress.canEnd();
        if (!netherDenied && !endDenied) return;
        event.setCanceled(true);
        // Evita saturar el chat si el jugador permanece dentro de un portal.
        if (player.tickCount % 40 == 0) {
            player.sendSystemMessage(Component.literal(netherDenied
                    ? "§c[Evolution Craft] Nether bloqueado: pesca III, Warden, biomas y 20 avances."
                    : "§c[Evolution Craft] End bloqueado: pesca V, Wither, biomas y 45 avances."));
        }
    }

    // Versión alfa: impide equipar objetos de eras superiores, sin eliminarlos del inventario.
    @SubscribeEvent
    public void tick(PlayerTickEvent.Post event) {
        ServerPlayer player = server(event.getEntity());
        if (player == null || player.tickCount % 5 != 0) return;
        Progress progress = ProgressStore.get(player);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            String key = id(stack);
            int requiredEra = Progress.requiredEra(key);
            if (requiredEra > progress.era()) {
                ItemStack dropped = stack.copy();
                player.setItemSlot(slot, ItemStack.EMPTY);
                player.drop(dropped, false, Prediction.SERVER_ONLY);
                player.sendSystemMessage(Component.literal("§c[Evolution Craft] Equipo bloqueado: " + Progress.ERA_NAMES[requiredEra]), true);
            }
        }
    }
}
