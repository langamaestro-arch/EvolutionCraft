package es.evolutioncraft;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.Set;

/** Verifica por servidor los quince desafios opcionales y sus distintivos. */
@EventBusSubscriber(modid=EvolutionCraft.ID)
public final class SecretEvents {
    private SecretEvents(){}
    private static final Set<String> NETHER_MOBS=Set.of(
        "minecraft:blaze","minecraft:ghast","minecraft:wither_skeleton");
    private static final String[] ARMOR={"helmet","chestplate","leggings","boots"};
    private static final String[] TOOLS={"pickaxe","axe","sword","shovel","hoe"};

    @SubscribeEvent
    public static void drank(LivingEntityUseItemEvent.Finish event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        ItemStack stack=event.getItem();
        if(!stack.is(Items.POTION))return;
        PotionContents potion=stack.get(DataComponents.POTION_CONTENTS);
        if(potion==null)return;
        String name=potion.potion().flatMap(h->h.unwrapKey())
            .map(h->h.identifier().toString()).orElse("");
        if(!name.startsWith("minecraft:"))return;
        name=name.replace("minecraft:long_","minecraft:")
                 .replace("minecraft:strong_","minecraft:");
        Progress p=ProgressStore.get(player);
        if(p.consumedPotions.add(name))ProgressStore.save(player);
    }

    private static boolean fullNetherite(Progress p){
        for(String suffix:ARMOR)if(!p.smithing.contains("minecraft:netherite_"+suffix))return false;
        for(String suffix:TOOLS)if(!p.smithing.contains("minecraft:netherite_"+suffix))return false;
        return true;
    }

    private static boolean hasThreeEnchantments(ItemStack stack){
        if(stack.isEmpty())return false;
        String item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if(!item.endsWith("_pickaxe")&&!item.endsWith("_axe")&&!item.endsWith("_sword")&&
            !item.endsWith("_shovel")&&!item.endsWith("_hoe"))return false;
        ItemEnchantments ench=stack.get(DataComponents.ENCHANTMENTS);
        if(ench==null)return false;
        int high=0;
        for(var pair:ench.entrySet())if(pair.getIntValue()>=3)high++;
        return high>=3;
    }

    private static void check(ServerPlayer player){
        Progress p=ProgressStore.get(player);
        if(!p.obtainedSponge || !p.threeHighEnchantments) {
            for(int i=0;i<player.getInventory().getContainerSize();i++){
                ItemStack stack=player.getInventory().getItem(i);
                if(stack.is(Items.SPONGE)||stack.is(Items.WET_SPONGE))p.obtainedSponge=true;
                if(hasThreeEnchantments(stack))p.threeHighEnchantments=true;
            }
        }
        if(p.advancements.contains("minecraft:story/enter_the_nether") &&
            p.completedMissions.contains("ancient_debris"))p.completedMissions.add("nether");
        if(fullNetherite(p)&&p.toolLevel()>=5)p.completedMissions.add("netherite");

        p.secretChallenges.addAll(java.util.Set.of()); // Mantener compatibilidad con partidas guardadas.
        if(p.cleanNights>=10)p.secretChallenges.add("noche");
        if(p.treasuresCaught>=5)p.secretChallenges.add("tesoros");
        if(p.tradeCount>=50)p.secretChallenges.add("comerciante");
        if(p.harvestStreak>=500)p.secretChallenges.add("tierra");
        if(p.tamed.containsAll(Set.of("minecraft:wolf","minecraft:cat","minecraft:horse")))p.secretChallenges.add("animales");
        if(p.naturalDiamondOre>=64)p.secretChallenges.add("diamantes");
        if(p.overworldBiomes.size()+p.netherBiomes.size()>=40)p.secretChallenges.add("fronteras");
        if(p.elderGuardianKills>=1&&p.obtainedSponge)p.secretChallenges.add("oceano");
        if(p.threeHighEnchantments)p.secretChallenges.add("arcano");
        if(p.potionEffects.size()>=10&&p.consumedPotions.size()>=5)p.secretChallenges.add("elixir");
        if(p.wardenSlainWithoutDeath)p.secretChallenges.add("terror");
        if(p.netherKillTypes.containsAll(NETHER_MOBS))p.secretChallenges.add("infierno");
        if(p.dragonSlain)p.secretChallenges.add("dragon");
        if(fullNetherite(p)&&p.toolLevel()>=5)p.secretChallenges.add("equipo");
        if(p.era()==5 && p.fishingLevel()==5 && p.agricultureLevel()==5 &&
            p.husbandryLevel()==5 && p.explorationLevel()==5 && p.toolLevel()==5 &&
            p.alchemyLevel()==5 && p.secretChallenges.size()>=14)p.secretChallenges.add("evolucion");
        ProgressStore.save(player);
    }
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event){
        if(event.getEntity() instanceof ServerPlayer player&&player.tickCount%100==0)check(player);
    }
}
