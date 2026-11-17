package es.evolutioncraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contratos opcionales: servidor autoritativo. No reutiliza estadísticas históricas
 * ni modifica objetivos de era, logros, botín o recompensas existentes.
 * Cada contrato puede reclamarse una vez por jugador en esta primera versión.
 */
public final class AdventureContracts {
    private AdventureContracts(){}
    public record Contract(String id,String title,String tier,String description,String kind,
                           String target,int quantity,int minEra,int emeralds,int xp){}
    public static final List<Contract> CATALOG=List.of(
        new Contract("fish_15","El pescador del pueblo","BRONCE","Captura 15 peces","fish","*",15,0,8,150),
        new Contract("harvest_32","Cosecha abundante","BRONCE","Recoge 32 cultivos maduros","harvest","*",32,0,8,150),
        new Contract("hunt_zombies","Limpieza nocturna","BRONCE","Derrota 12 zombis","kill","minecraft:zombie",12,0,10,200),
        new Contract("mine_copper","Mineral para el herrero","PLATA","Extrae 20 minerales de cobre naturales","mine","minecraft:copper_ore",20,2,16,400),
        new Contract("explore_2","Explorador de ruinas","PLATA","Descubre 2 estructuras nuevas","structure","*",2,1,16,400),
        new Contract("hunt_skeletons","Arqueros en la oscuridad","PLATA","Derrota 18 esqueletos","kill","minecraft:skeleton",18,1,18,450),
        new Contract("harvest_128","Gran cosecha","PLATA","Recoge 128 cultivos maduros","harvest","*",128,2,20,500),
        new Contract("blaze_10","Cazador del Nether","ORO","Derrota 10 blazes","kill","minecraft:blaze",10,4,32,1000),
        new Contract("wither_skeleton_5","Huesos de la fortaleza","ORO","Derrota 5 esqueletos wither","kill","minecraft:wither_skeleton",5,4,32,1000),
        new Contract("nether_biomes","Cartógrafo infernal","ORO","Descubre 3 biomas nuevos del Nether","nether_biome","*",3,4,32,1000)
    );

    private static void init(Progress p){
        if(p.activeContracts==null)p.activeContracts=new HashSet<>();
        if(p.claimedContracts==null)p.claimedContracts=new HashSet<>();
        if(p.contractProgress==null)p.contractProgress=new HashMap<>();
    }
    public static Contract byId(String id){
        for(Contract c:CATALOG)if(c.id().equals(id))return c;
        return null;
    }
    public static boolean accept(ServerPlayer player,String id){
        if(!EvolutionConfig.get(player).contractsEnabled)return false;
        Progress p=ProgressStore.get(player);init(p);
        Contract c=byId(id);
        if(c==null||p.era()<c.minEra()||p.activeContracts.size()>=3||
            p.claimedContracts.contains(id)||p.activeContracts.contains(id))return false;
        p.activeContracts.add(id);
        p.contractProgress.put(id,0); // Nada anterior a aceptar cuenta.
        ProgressStore.save(player);
        player.sendSystemMessage(Component.literal("§6[EvolutionCraft] Contrato aceptado: §f"+c.title()),true);
        return true;
    }
    public static boolean abandon(ServerPlayer player,String id){
        Progress p=ProgressStore.get(player);init(p);
        if(!p.activeContracts.remove(id))return false;
        p.contractProgress.remove(id);
        ProgressStore.save(player);
        player.sendSystemMessage(Component.literal("§e[EvolutionCraft] Contrato abandonado: "+id),true);
        return true;
    }
    public static void onAction(ServerPlayer player,String kind,String target,int amount){
        if(amount<=0||!EvolutionConfig.get(player).contractsEnabled)return;
        Progress p=ProgressStore.get(player);init(p);
        boolean changed=false;
        for(String id:p.activeContracts){
            Contract c=byId(id);
            if(c==null||!c.kind().equals(kind))continue;
            if(!c.target().equals("*")&&!c.target().equals(target))continue;
            int before=p.contractProgress.getOrDefault(id,0);
            int after=Math.min(c.quantity(),before+amount);
            if(after!=before){
                p.contractProgress.put(id,after);
                changed=true;
                if(after==c.quantity())player.sendSystemMessage(Component.literal(
                    "§a[EvolutionCraft] Contrato completado: "+c.title()+" · Pulsa P para reclamarlo."),true);
            }
        }
        if(changed)ProgressStore.save(player);
    }
    public static boolean claim(ServerPlayer player,String id){
        if(!EvolutionConfig.get(player).contractsEnabled)return false;
        Progress p=ProgressStore.get(player);init(p);
        Contract c=byId(id);
        if(c==null||!p.activeContracts.contains(id)||p.claimedContracts.contains(id)||
            p.contractProgress.getOrDefault(id,0)<c.quantity()||p.era()<c.minEra())return false;
        ItemStack emeralds=new ItemStack(Items.EMERALD,c.emeralds());
        // Exigir espacio libre: no se tira ningún premio al suelo.
        boolean capacity=false;
        for(int i=0;i<36;i++){
            ItemStack slot=player.getInventory().getItem(i);
            if(slot.isEmpty() || (ItemStack.isSameItemSameComponents(slot,emeralds) &&
                slot.getCount()+emeralds.getCount()<=slot.getMaxStackSize())){capacity=true;break;}
        }
        if(!capacity){
            player.sendSystemMessage(Component.literal("§c[EvolutionCraft] Haz espacio en el inventario antes de reclamar."),true);
            return false;
        }
        // Registrar el canje antes de entregar el objeto: protección antduplicados.
        p.claimedContracts.add(id);
        p.activeContracts.remove(id);
        ProgressStore.save(player);
        player.getInventory().add(emeralds);
        player.giveExperiencePoints(c.xp());
        player.sendSystemMessage(Component.literal("§6[EvolutionCraft] Premio de contrato: "+
            c.emeralds()+" esmeraldas y "+c.xp()+" XP."),true);
        return true;
    }
    public static JsonObject snapshot(ServerPlayer player){
        Progress p=ProgressStore.get(player);init(p);
        JsonObject root=new JsonObject();
        root.addProperty("enabled",EvolutionConfig.get(player).contractsEnabled);
        root.addProperty("activeCount",p.activeContracts.size());
        JsonArray available=new JsonArray(),active=new JsonArray(),completed=new JsonArray();
        for(Contract c:CATALOG){
            JsonObject row=new JsonObject();
            boolean on=p.activeContracts.contains(c.id());
            boolean claimed=p.claimedContracts.contains(c.id());
            int current=p.contractProgress.getOrDefault(c.id(),0);
            row.addProperty("id",c.id());
            row.addProperty("text",c.title());
            row.addProperty("tier",c.tier());
            row.addProperty("description",c.description());
            row.addProperty("progress",current);
            row.addProperty("target",c.quantity());
            row.addProperty("minEra",c.minEra());
            row.addProperty("reward",c.emeralds()+" esmeraldas + "+c.xp()+" XP");
            row.addProperty("ready",on&&current>=c.quantity());
            row.addProperty("canAccept",!on&&!claimed&&p.era()>=c.minEra()&&p.activeContracts.size()<3);
            row.addProperty("done",claimed);
            if(claimed)completed.add(row);
            else if(on)active.add(row);
            else available.add(row);
        }
        root.add("available",available);
        root.add("active",active);
        root.add("completed",completed);
        return root;
    }
}
