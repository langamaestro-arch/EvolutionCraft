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
    public void smithed(PlayerEvent.ItemSmithedEvent event){
        ServerPlayer player=server(event.getEntity());
        if(player==null)return;
        String result=id(event.getResult());
        update(player,p->{p.smithing.add(result);p.crafted.add(result);});
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
                    p.treasuresCaught+=stack.getCount();
                }
            }
        });
        // Sólo capturas posteriores a aceptar el contrato.
        int caught=0;
        for(ItemStack stack:event.getDrops()){
            String key=id(stack);
            if(key.equals("minecraft:cod")||key.equals("minecraft:salmon")||
                key.equals("minecraft:pufferfish")||key.equals("minecraft:tropical_fish"))
                caught+=stack.getCount();
        }
        AdventureContracts.onAction(player,"fish","*",caught);
    }

    @SubscribeEvent
    public void defeated(LivingDeathEvent event) {
        if(event.getEntity() instanceof ServerPlayer dead) {
            Progress p=ProgressStore.get(dead);
            p.deathCount++;
            p.harvestStreak=0;
            p.cleanNights=0;
            p.lastNightIndex=-1;
            p.nightTicksObserved=0;
            p.netherKillTypes.clear();
            ProgressStore.save(dead);
            return;
        }
        // Damage may be credited to a projectile, pet or indirect source.
        // Resolve the server-side killer instead of relying solely on
        // DamageSource.getEntity() (which may be null for environmental deaths).
        ServerPlayer player=null;
        if(event.getSource().getEntity() instanceof ServerPlayer direct)player=direct;
        else if(event.getEntity().getKillCredit() instanceof ServerPlayer credited)player=credited;
        if(player==null)return;
        String entity = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
        final ServerPlayer killer=player;
        AdventureContracts.onAction(killer,"kill",entity,1);
        EpicContracts.bossKilled(killer,entity);
        update(killer, p -> {
            boolean journalBoss=java.util.Set.of("minecraft:warden","minecraft:wither",
                "minecraft:ender_dragon","minecraft:elder_guardian").contains(entity)
                || (entity.startsWith("nicktaleepicdungeons:") &&
                    (entity.contains("boss") || entity.contains("colossus") ||
                     entity.contains("demon") || entity.contains("cerberus") ||
                     entity.contains("sultan") || entity.contains("voidborne") ||
                     entity.contains("elder") || entity.contains("knight")));
            if(journalBoss){
                if(p.discoveredBosses==null)p.discoveredBosses=new java.util.HashSet<>();
                if(p.discoveredBosses.add(entity))DiscoveryJournal.record(p,killer,"boss",entity);
            }
            // Nicktale era bosses count only if this player actually received kill credit.
            // Recording is persistent and never spends consumables or quest rewards.
            if(entity.equals(NicktaleBossGate.entity(3)) ||
               entity.equals(NicktaleBossGate.entity(4)) ||
               entity.equals(NicktaleBossGate.entity(5))){
                if(p.nicktaleEraBossKills==null)
                    p.nicktaleEraBossKills=new java.util.HashSet<>();
                p.nicktaleEraBossKills.add(entity);
            }
            if(NicktaleDimensionGate.isRequiredBoss(entity)){
                if(p.nicktaleDimensionBossKills==null)
                    p.nicktaleDimensionBossKills=new java.util.HashSet<>();
                p.nicktaleDimensionBossKills.add(entity);
            }
            if(entity.equals("minecraft:warden") || entity.equals("minecraft:wither"))p.bossKills.add(entity);
            if(entity.equals("minecraft:elder_guardian"))p.elderGuardianKills++;
            if(entity.equals("minecraft:ender_dragon"))
                p.dragonSlain=p.dragonAttemptId.equals(event.getEntity().getUUID().toString()) &&
                    p.dragonAttemptDeaths==p.deathCount;
            if(entity.equals("minecraft:warden"))
                p.wardenSlainWithoutDeath=p.wardenAttemptId.equals(event.getEntity().getUUID().toString()) &&
                    p.wardenAttemptDeaths==p.deathCount;
            if(killer.level().dimension()==Level.NETHER &&
                (entity.equals("minecraft:blaze") || entity.equals("minecraft:ghast") ||
                 entity.equals("minecraft:wither_skeleton"))) p.netherKillTypes.add(entity);
        });
    }

    @SubscribeEvent
    public void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event){
        ServerPlayer player=server(event.getEntity());
        if(player==null || event.getTo()==Level.NETHER)return;
        Progress p=ProgressStore.get(player);
        if(!p.netherKillTypes.isEmpty()){p.netherKillTypes.clear();ProgressStore.save(player);}
    }

    // La validación se realiza ANTES del teletransporte, por jugador y en servidor.
    @SubscribeEvent
    public void dimensionTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Progress progress = ProgressStore.get(player);
        EvolutionConfig config=EvolutionConfig.get(player);
        boolean netherDenied = event.getDimension() == Level.NETHER && !progress.canNether(config);
        boolean endDenied = event.getDimension() == Level.END && !progress.canEnd(config);
        if (!netherDenied && !endDenied) return;
        event.setCanceled(true);
        // Evita saturar el chat si el jugador permanece dentro de un portal.
        if (player.tickCount % 40 == 0) {
            player.sendSystemMessage(Component.literal(netherDenied
                    ? "§c[Evolution Craft] Nether bloqueado: guardianes Nicktale, Diamante, "+config.netherBiomes+" biomas, "+config.netherAdvancements+" avances, habilidades III y Warden."
                    : "§c[Evolution Craft] End bloqueado: guardianes Nicktale, "+config.endAdvancements+" avances, Exploracion V, Alquimia III y Wither."));
        }
    }

    // Retira equipo de eras superiores: al inventario o al suelo si está lleno.
    @SubscribeEvent
    public void tick(PlayerTickEvent.Post event) {
        ServerPlayer player = server(event.getEntity());
        if (player == null || player.tickCount % 5 != 0) return;
        if(!EvolutionConfig.get(player).equipmentRestrictions)return;
        Progress progress = ProgressStore.get(player);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            String key = id(stack);
            int requiredEra = Math.max(Progress.requiredEra(key),RecipeEraRules.requiredEra(key));
            if (requiredEra > progress.era()) {
                var inventory = player.getInventory();
                int free = -1;
                for(int i=0;i<36;i++) {
                    if(i==inventory.getSelectedSlot())continue;
                    if(inventory.getItem(i).isEmpty()){free=i;break;}
                }
                // Seguridad adicional frente a dispensadores, otros mods y sesiones restauradas.
                // No eliminar ni duplicar el stack cuando no exista espacio libre.
                ItemStack kept=stack.copy();
                if(free>=0){
                    player.setItemSlot(slot,ItemStack.EMPTY);
                    inventory.setItem(free,kept);
                }else{
                    // Inventario lleno: el objeto debe aparecer en el suelo, NO desaparecer
                    // ni transferirse silenciosamente al cofre de Ender.
                    var dropped=new net.minecraft.world.entity.item.ItemEntity(
                        player.level(),player.getX(),player.getY()+0.3,player.getZ(),kept);
                    dropped.setPickUpDelay(30);
                    // Se retira de la ranura SOLO si el servidor confirma su aparición.
                    // Si el chunk rechaza crear la entidad, permanece equipado y
                    // se reintentará en el siguiente tick de comprobación.
                    if(player.level().addFreshEntity(dropped)){
                        player.setItemSlot(slot,ItemStack.EMPTY);
                    }else{
                        continue;
                    }
                }
                if(player.tickCount%40==0)
                    player.sendSystemMessage(Component.literal(
                        "§c[EvolutionCraft] Equipo de "+Progress.ERA_NAMES[requiredEra]+
                        " bloqueado. "+(free>=0?"Devuelto al inventario.":
                        "Inventario lleno: objeto soltado a tus pies.")),true);
            }
        }
    }
}
