package es.evolutioncraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;

/** Registra únicamente cosechas maduras y crías o domesticaciones atribuibles al jugador. */
@EventBusSubscriber(modid = EvolutionCraft.ID)
public final class FarmingEvents {
    private FarmingEvents() {}

    private static void record(ServerPlayer player, java.util.function.Consumer<Progress> change) {
        Progress p = ProgressStore.get(player);
        int prev = p.era();
        int ag = p.agricultureLevel();
        int hu = p.husbandryLevel();
        change.accept(p);
        ProgressStore.save(player);
        if (p.agricultureLevel() > ag)
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Evolution Craft] Agricultura nivel " + p.agricultureLevel()));
        if (p.husbandryLevel() > hu)
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Evolution Craft] Ganadería nivel " + p.husbandryLevel()));
        if (p.era() > prev)
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Evolution Craft] ¡Nueva era! " + Progress.ERA_NAMES[p.era()]));
    }

    @SubscribeEvent
    public static void harvest(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || event.isCanceled()) return;
        BlockState state = event.getState();
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        boolean mature = (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state))
            || (id.equals("minecraft:cocoa") && state.hasProperty(net.minecraft.world.level.block.CocoaBlock.AGE)
                && state.getValue(net.minecraft.world.level.block.CocoaBlock.AGE) == 2)
            || (id.equals("minecraft:nether_wart") && state.hasProperty(net.minecraft.world.level.block.NetherWartBlock.AGE)
                && state.getValue(net.minecraft.world.level.block.NetherWartBlock.AGE) == 3)
            || id.equals("minecraft:melon") || id.equals("minecraft:pumpkin");
        if (!mature) return;
        record(player, p -> { p.harvests++; p.harvestStreak++; if (id.equals("minecraft:wheat")) p.wheatHarvests++; p.harvested.add(id); });
    }

    @SubscribeEvent
    public static void breed(BabyEntitySpawnEvent event) {
        if (event.isCanceled() || event.getChild() == null || !(event.getCausedByPlayer() instanceof ServerPlayer player)) return;
        String kind = BuiltInRegistries.ENTITY_TYPE.getKey(event.getParentA().getType()).toString();
        record(player, p -> { p.breedings++; p.bred.add(kind); });
    }

    @SubscribeEvent
    public static void tame(AnimalTameEvent event) {
        if (event.isCanceled() || !(event.getTamer() instanceof ServerPlayer player)) return;
        String kind = BuiltInRegistries.ENTITY_TYPE.getKey(event.getAnimal().getType()).toString();
        if (kind.equals("minecraft:cat") || kind.equals("minecraft:wolf") || kind.equals("minecraft:horse"))
            record(player, p -> {p.tamed.add(kind); if(kind.equals("minecraft:horse"))p.horseTamed=true;});
    }
}
