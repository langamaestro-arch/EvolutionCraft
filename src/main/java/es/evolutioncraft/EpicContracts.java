package es.evolutioncraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/** Boss quests, strictly player-kill attributed and separate from normal contracts. */
public final class EpicContracts {
    private EpicContracts(){}
    public record BossQuest(String id,String title,String mob,int minEra,int rep,String tier,int xp,int emeralds){}
    private static final List<BossQuest> CATALOG=List.of(
        new BossQuest("warden","El terror de las profundidades","minecraft:warden",4,300,"EPICO",2500,32),
        new BossQuest("elder","Señor de las mareas","minecraft:elder_guardian",3,300,"EPICO",1800,24),
        new BossQuest("wither","El rey marchito","minecraft:wither",4,700,"HEROICO",4000,48),
        new BossQuest("dragon","El último dragón","minecraft:ender_dragon",4,1500,"LEGENDARIO",6500,64),
        new BossQuest("ravager","La bestia de la incursión","minecraft:ravager",3,300,"EPICO",1600,24),
        new BossQuest("breeze","Tormenta en la cámara","minecraft:breeze",2,300,"EPICO",1400,20)
    );
    private static void init(Progress p){
        if(p.epicActive==null)p.epicActive=new HashSet<>();
        if(p.epicClaimed==null)p.epicClaimed=new HashSet<>();
        if(p.epicOffers==null)p.epicOffers=new HashMap<>();
        if(p.epicProgress==null)p.epicProgress=new HashMap<>();
    }
    private static BossQuest template(String id){
        if(id==null)return null;
        String key=id;
        if(id.startsWith("ep")&&id.contains("_"))key=id.substring(id.indexOf('_')+1);
        for(BossQuest quest:CATALOG)if(quest.id().equals(key))return quest;
        return null;
    }
    private static boolean eligible(Progress p,BossQuest q){
        return p.era()>=q.minEra() && AdventurerReputation.points(p)>=q.rep();
    }
    private static boolean fill(Progress p){
        boolean changed=false;
        Set<String> occupied=new HashSet<>();
        for(String id:p.epicOffers.values()){
            BossQuest q=template(id);if(q!=null)occupied.add(q.id());
        }
        for(String id:p.epicActive){
            BossQuest q=template(id);if(q!=null)occupied.add(q.id());
        }
        for(int i=0;i<3;i++){
            String key="slot_"+i,id=p.epicOffers.get(key);
            BossQuest existing=template(id);
            if(existing!=null&&eligible(p,existing))continue;
            if(id!=null){p.epicOffers.remove(key);changed=true;}
            List<BossQuest> candidates=new ArrayList<>();
            for(BossQuest q:CATALOG)if(eligible(p,q)&&!occupied.contains(q.id()))candidates.add(q);
            if(candidates.isEmpty())continue;
            BossQuest chosen=candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            occupied.add(chosen.id());
            p.epicOffers.put(key,"ep"+(++p.epicSequence)+"_"+chosen.id());
            changed=true;
        }
        return changed;
    }
    public static int reputationFor(String id){
        BossQuest q=template(id);
        if(q==null)return 0;
        return switch(q.tier()){case "LEGENDARIO"->200;case "HEROICO"->125;default->75;};
    }
    public static boolean accept(ServerPlayer player,String id){
        if(!EvolutionConfig.get(player).contractsEnabled)return false;
        Progress p=ProgressStore.get(player);init(p);
        if(fill(p))ProgressStore.save(player);
        BossQuest q=template(id);
        if(q==null||!eligible(p,q)||p.epicActive.size()>=1||!p.epicOffers.containsValue(id)
            ||p.epicClaimed.contains(id))return false;
        p.epicActive.add(id);p.epicProgress.put(id,0);
        p.epicOffers.values().removeIf(v->v.equals(id));
        fill(p);ProgressStore.save(player);
        player.sendSystemMessage(Component.literal("§6[EvolutionCraft] Contrato épico aceptado: "+q.title()),true);
        return true;
    }
    public static boolean abandon(ServerPlayer player,String id){
        Progress p=ProgressStore.get(player);init(p);
        if(!p.epicActive.remove(id))return false;
        p.epicProgress.remove(id);fill(p);ProgressStore.save(player);
        return true;
    }
    public static void bossKilled(ServerPlayer killer,String mob){
        if(!EvolutionConfig.get(killer).contractsEnabled)return;
        Progress p=ProgressStore.get(killer);init(p);
        boolean changed=false;
        for(String id:p.epicActive){
            BossQuest q=template(id);
            if(q==null||!q.mob().equals(mob)||p.epicProgress.getOrDefault(id,0)>=1)continue;
            p.epicProgress.put(id,1);changed=true;
            killer.sendSystemMessage(Component.literal("§d[EvolutionCraft] ¡Jefe derrotado! "+
                q.title()+" · Recompensa disponible en P."),true);
        }
        if(changed)ProgressStore.save(killer);
    }
    private static ItemStack equipment(ServerPlayer player,Progress p,BossQuest q){
        int era=p.era();
        Item item=era>=5?Items.NETHERITE_SWORD:era>=4?Items.DIAMOND_SWORD:
            era>=3?Items.IRON_SWORD:era>=2?Items.COPPER_SWORD:Items.STONE_SWORD;
        ItemStack weapon=new ItemStack(item);
        int power=q.tier().equals("LEGENDARIO")?5:q.tier().equals("HEROICO")?4:3;
        ItemEnchantments.Mutable ench=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        var access=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ench.set(access.getOrThrow(Enchantments.SHARPNESS),power);
        ench.set(access.getOrThrow(Enchantments.UNBREAKING),Math.min(3,power));
        weapon.set(DataComponents.ENCHANTMENTS,ench.toImmutable());
        return weapon;
    }
    public static boolean claim(ServerPlayer player,String id){
        if(!EvolutionConfig.get(player).contractsEnabled)return false;
        Progress p=ProgressStore.get(player);init(p);
        BossQuest q=template(id);
        if(q==null||!p.epicActive.contains(id)||p.epicClaimed.contains(id)
            ||p.epicProgress.getOrDefault(id,0)<1||!eligible(p,q))return false;
        // Require two free slots: no dropped rewards or items lost on a full inventory.
        int empty=0;
        for(int i=0;i<36;i++)if(player.getInventory().getItem(i).isEmpty())empty++;
        if(empty<2){
            player.sendSystemMessage(Component.literal("§c[EvolutionCraft] Necesitas 2 huecos libres para el premio épico."),true);
            return false;
        }
        ItemStack sword=equipment(player,p,q);
        ItemStack emeralds=new ItemStack(Items.EMERALD,q.emeralds());
        p.epicClaimed.add(id);p.epicActive.remove(id);p.epicProgress.remove(id);
        fill(p);ProgressStore.save(player);
        player.getInventory().add(sword);
        player.getInventory().add(emeralds);
        player.giveExperiencePoints(q.xp());
        player.sendSystemMessage(Component.literal("§6[EvolutionCraft] Premio épico: arma encantada de tu era, "+
            q.emeralds()+" esmeraldas y "+q.xp()+" XP."),true);
        return true;
    }
    private static JsonObject row(String id,BossQuest q,String status,Progress p){
        JsonObject o=new JsonObject();
        o.addProperty("id",id);
        o.addProperty("text",q.title());
        o.addProperty("description","Derrota al jefe "+q.mob().substring(q.mob().indexOf(':')+1).replace('_',' '));
        o.addProperty("category","JEFES");
        o.addProperty("tier",q.tier());
        o.addProperty("progress",p.epicProgress.getOrDefault(id,0));
        o.addProperty("target",1);
        o.addProperty("ready",status.equals("active")&&p.epicProgress.getOrDefault(id,0)>=1);
        o.addProperty("done",status.equals("claimed"));
        o.addProperty("canAccept",status.equals("offer")&&p.epicActive.isEmpty()&&eligible(p,q));
        o.addProperty("reward",q.emeralds()+" esmeraldas + "+q.xp()+" XP + arma encantada");
        o.addProperty("epicStatus",status);
        return o;
    }
    public static JsonArray snapshot(ServerPlayer player){
        Progress p=ProgressStore.get(player);init(p);
        if(EvolutionConfig.get(player).contractsEnabled&&fill(p))ProgressStore.save(player);
        JsonArray a=new JsonArray();
        p.epicActive.stream().sorted().forEach(id->{BossQuest q=template(id);if(q!=null)a.add(row(id,q,"active",p));});
        for(int i=0;i<3;i++){String id=p.epicOffers.get("slot_"+i);BossQuest q=template(id);
            if(q!=null)a.add(row(id,q,"offer",p));}
        p.epicClaimed.stream().sorted().limit(10).forEach(id->{BossQuest q=template(id);
            if(q!=null)a.add(row(id,q,"claimed",p));});
        return a;
    }
}
