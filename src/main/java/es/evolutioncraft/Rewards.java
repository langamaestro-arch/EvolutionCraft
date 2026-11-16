package es.evolutioncraft;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Recompensas calculadas en el servidor y reclamadas una sola vez por jugador. */
@EventBusSubscriber(modid=EvolutionCraft.ID)
public final class Rewards {
    private Rewards(){}
    public record Prize(String id,String title,int xp,int minEra,List<ItemStack> items){}
    private static ItemStack stack(Item item,int count){return new ItemStack(item,count);}
    private static Prize prize(String id,String title,int xp,int minEra,ItemStack... items){
        return new Prize(id,title,xp,minEra,List.of(items));
    }
    private static final String[] SKILLS={"pesca","agricultura","ganaderia","exploracion","herramientas","alquimia"};
    private static final String[] SKILL_NAMES={"Pesca","Agricultura","Ganaderia","Exploracion","Herramientas","Alquimia"};
    private static final String[] MISSIONS={"wood","stone","copper","iron","nether","netherite"};
    private static final String[] MISSION_NAMES={"Primera noche","Comerciante del pueblo","Dominio del cobre",
        "Explorador de las profundidades","Aventura en Nether","Maestro de Netherita"};
    private static int level(Progress p,int i){
        return switch(i){
            case 0 -> p.fishingLevel();
            case 1 -> p.agricultureLevel();
            case 2 -> p.husbandryLevel();
            case 3 -> p.explorationLevel();
            case 4 -> p.toolLevel();
            default -> p.alchemyLevel();
        };
    }
    public static List<Prize> available(Progress p){
        List<Prize> result=new ArrayList<>();
        int era=p.era();
        Item[] eraItems={Items.OAK_LOG,Items.COBBLESTONE,Items.COPPER_INGOT,
            Items.IRON_INGOT,Items.DIAMOND,Items.NETHERITE_SCRAP};
        for(int i=1;i<=era;i++){
            result.add(prize("era_"+i,"Era de "+Progress.ERA_NAMES[i],
                100*i,i,stack(eraItems[i],i==5?2:i==4?3:12),
                stack(Items.BREAD,8)));
        }
        Item[] supply={Items.COD,Items.WHEAT,Items.WHEAT,Items.COMPASS,
            Items.LAPIS_LAZULI,Items.GLASS_BOTTLE};
        for(int s=0;s<SKILLS.length;s++){
            for(int lv=1;lv<=level(p,s);lv++){
                int xp=switch(lv){case 1->100;case 2->250;case 3->500;case 4->1000;default->2000;};
                int count=lv==1?4:lv==2?8:lv==3?12:16;
                ItemStack bonus=lv==5?stack(Items.EMERALD,8):
                    lv>=3?stack(Items.EXPERIENCE_BOTTLE,lv*2):stack(Items.TORCH,8);
                result.add(prize("skill_"+SKILLS[s]+"_"+lv,
                    SKILL_NAMES[s]+" - Maestria "+lv,xp,0,
                    stack(supply[s],count),bonus));
            }
        }
        for(int i=0;i<MISSIONS.length;i++){
            if(p.completedMissions.contains(MISSIONS[i])){
                result.add(prize("mission_"+MISSIONS[i],
                    "Mision: "+MISSION_NAMES[i],200+i*150,
                    Math.min(i,era),stack(Items.EXPERIENCE_BOTTLE,4+i*2)));
            }
        }
        Set<String> done=p.secretChallenges;
        if(done.contains("noche"))result.add(prize("secret_noche","La noche es mia",0,0,
            stack(Items.TORCH,16),stack(Items.COOKED_BEEF,8)));
        if(done.contains("tesoros"))result.add(prize("secret_tesoros","La suerte del pescador",500,0,
            stack(Items.EMERALD,16)));
        if(done.contains("comerciante"))result.add(prize("secret_comerciante","Comerciante legendario",0,0,
            stack(Items.EMERALD,32)));
        if(done.contains("tierra"))result.add(prize("secret_tierra","Tierra fertil",0,0,
            stack(Items.BONE_BLOCK,32),stack(Items.GOLDEN_CARROT,16)));
        if(done.contains("animales"))result.add(prize("secret_animales","Amigo de los animales",0,0,
            stack(Items.NAME_TAG,1),stack(Items.BONE,16),stack(Items.COD,16)));
        if(done.contains("diamantes"))result.add(prize("secret_diamantes","Corazon de diamante",1000,4,
            stack(Items.DIAMOND,8)));
        if(done.contains("fronteras"))result.add(prize("secret_fronteras","Sin fronteras",1000,0,
            stack(Items.RECOVERY_COMPASS,1)));
        if(done.contains("oceano"))result.add(prize("secret_oceano","Senor de los oceanos",1000,0,
            stack(Items.HEART_OF_THE_SEA,1)));
        if(done.contains("arcano"))result.add(prize("secret_arcano","Poder arcano",0,3,
            stack(Items.ENCHANTED_BOOK,1)));
        if(done.contains("elixir"))result.add(prize("secret_elixir","Elixir de la vida",0,0,
            stack(Items.POTION,1),stack(Items.POTION,1),stack(Items.POTION,1)));
        if(done.contains("terror"))result.add(prize("secret_terror","El terror tiene miedo",2000,0,
            stack(Items.ENCHANTED_GOLDEN_APPLE,1)));
        if(done.contains("infierno"))result.add(prize("secret_infierno","Infierno dominado",2000,4,
            stack(Items.WITHER_SKELETON_SKULL,1)));
        if(done.contains("dragon"))result.add(prize("secret_dragon","Cazador de dragones",3000,4,
            stack(Items.ELYTRA,1)));
        if(done.contains("equipo"))result.add(prize("secret_equipo","El equipo definitivo",3000,5,
            stack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,1)));
        if(done.contains("evolucion"))result.add(prize("secret_evolucion","La evolucion definitiva",5000,5,
            stack(Items.TOTEM_OF_UNDYING,1)));
        return result;
    }

    private static ItemStack preparePrize(ServerPlayer player,String rewardId,ItemStack original){
        ItemStack stack=original.copy();
        if(rewardId.equals("secret_arcano") && stack.is(Items.ENCHANTED_BOOK)){
            ItemEnchantments.Mutable ench=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            ench.set(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.UNBREAKING),3);
            stack.set(DataComponents.STORED_ENCHANTMENTS,ench.toImmutable());
        }
        if(rewardId.equals("secret_elixir") && stack.is(Items.POTION)){
            stack=PotionContents.createItemStack(Items.POTION,Potions.REGENERATION);
        }
        return stack;
    }
    public static boolean claim(ServerPlayer player,String id){
        Progress p=ProgressStore.get(player);
        if(p.claimedRewards==null)p.claimedRewards=new HashSet<>();
        if(p.claimedRewards.contains(id))return false;
        Prize found=null;
        for(Prize prize:available(p))if(prize.id().equals(id)){found=prize;break;}
        if(found==null||p.era()<found.minEra())return false;
        // Comprobación conservadora, sin tirar ítems al suelo ni confiar en un slot futuro.
        int empty=0;
        for(int i=0;i<36;i++)if(player.getInventory().getItem(i).isEmpty())empty++;
        if(empty<found.items().size()){
            player.sendSystemMessage(Component.literal("§c[EvolutionCraft] Libera "+
                found.items().size()+" espacios vacios en el inventario para reclamarlo."),true);
            return false;
        }
        // Guardar primero el canje para impedir duplicados entre sesiones.
        p.claimedRewards.add(found.id());
        ProgressStore.save(player);
        for(ItemStack stack:found.items()){
            if(!player.getInventory().add(preparePrize(player,found.id(),stack))){
                player.sendSystemMessage(Component.literal(
                    "§c[EvolutionCraft] Inventario lleno al entregar el premio; contacta con un administrador."));
            }
        }
        if(found.xp()>0)player.giveExperiencePoints(found.xp());
        player.sendSystemMessage(Component.literal("§6[EvolutionCraft] ¡Recompensa reclamada! "+
            found.title()+ " ("+found.xp()+" XP)"),true);
        return true;
    }
    @SubscribeEvent
    public static void scan(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player)||player.tickCount%100!=0)return;
        Progress p=ProgressStore.get(player);
        if(p.notifiedRewards==null)p.notifiedRewards=new HashSet<>();
        if(p.claimedRewards==null)p.claimedRewards=new HashSet<>();
        int count=0;
        String first="";
        for(Prize prize:available(p)){
            if(prize.minEra()>p.era()||p.claimedRewards.contains(prize.id()))continue;
            if(p.notifiedRewards.add(prize.id())){
                if(count==0)first=prize.title();
                count++;
            }
        }
        if(count>0){
            ProgressStore.save(player);
            player.sendSystemMessage(Component.literal(count==1
                ?"§6✦ [EvolutionCraft] Recompensa disponible: §f"+first+" §e· Pulsa P"
                :"§6✦ [EvolutionCraft] "+count+" recompensas disponibles. §ePulsa P para reclamarlas."),true);
        }
    }
}
