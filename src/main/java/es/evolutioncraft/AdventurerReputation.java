package es.evolutioncraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashSet;
import java.util.List;

/**
 * Reputation is derived from the unique claimed contract instances.
 * Historic Beta 1..5 claims are included automatically and without migration.
 * A second claim for a contract can never award reputation twice.
 */
public final class AdventurerReputation {
    private AdventurerReputation(){}
    private static final String[] NAMES={"Aprendiz","Aventurero","Veterano","Maestro","Leyenda"};
    private static final int[] THRESHOLDS={0,100,300,700,1500};
    private static final Item[] REWARDS={Items.AIR,Items.EMERALD,Items.NAME_TAG,
        Items.RECOVERY_COMPASS,Items.TOTEM_OF_UNDYING};
    private static final int[] COUNTS={0,16,1,1,1};
    private static final int[] XP={0,250,600,1200,2500};

    public static int pointsFor(String tier){
        return switch(tier){
            case "BRONCE"->10;
            case "PLATA"->25;
            case "ORO"->50;
            default->0;
        };
    }
    public static int points(Progress p){
        if(p.claimedContracts==null)return 0;
        long total=0;
        for(String id:p.claimedContracts){
            AdventureContracts.Contract c=AdventureContracts.contractFor(id);
            if(c!=null)total+=pointsFor(c.tier());
        }
        return (int)Math.min(total,Integer.MAX_VALUE);
    }
    public static int rank(int points){
        for(int i=THRESHOLDS.length-1;i>=0;i--)if(points>=THRESHOLDS[i])return i;
        return 0;
    }
    public static String name(int points){return NAMES[rank(points)];}
    public static int requirement(int rank){return THRESHOLDS[Math.clamp(rank,0,4)];}

    public static JsonObject snapshot(Progress p){
        if(p.claimedReputationRanks==null)p.claimedReputationRanks=new HashSet<>();
        int points=points(p),rank=rank(points);
        JsonObject result=new JsonObject();
        result.addProperty("points",points);
        result.addProperty("rank",NAMES[rank]);
        result.addProperty("rankIndex",rank);
        result.addProperty("lower",THRESHOLDS[rank]);
        result.addProperty("next",rank==4?THRESHOLDS[4]:THRESHOLDS[rank+1]);
        result.addProperty("maxRank",rank==4);
        JsonArray goals=new JsonArray();
        for(int i=1;i<NAMES.length;i++){
            JsonObject row=new JsonObject();
            row.addProperty("id","rank_"+i);
            row.addProperty("text",NAMES[i]);
            row.addProperty("threshold",THRESHOLDS[i]);
            row.addProperty("done",p.claimedReputationRanks.contains("rank_"+i));
            row.addProperty("ready",points>=THRESHOLDS[i]&&!row.get("done").getAsBoolean());
            row.addProperty("unlocked",points>=THRESHOLDS[i]);
            row.addProperty("reward",COUNTS[i]+"x "+REWARDS[i].getDescription().getString()+" + "+XP[i]+" XP");
            row.addProperty("detail","Requiere "+THRESHOLDS[i]+" puntos de reputacion");
            goals.add(row);
        }
        result.add("goals",goals);
        return result;
    }
    public static boolean claim(ServerPlayer player,String id){
        if(!EvolutionConfig.get(player).contractsEnabled)return false;
        Progress p=ProgressStore.get(player);
        if(p.claimedReputationRanks==null)p.claimedReputationRanks=new HashSet<>();
        int index=-1;
        for(int i=1;i<NAMES.length;i++)if(id.equals("rank_"+i))index=i;
        if(index<1||points(p)<THRESHOLDS[index]||p.claimedReputationRanks.contains(id))return false;
        ItemStack prize=new ItemStack(REWARDS[index],COUNTS[index]);
        boolean space=false;
        for(int slot=0;slot<36;slot++){
            ItemStack current=player.getInventory().getItem(slot);
            if(current.isEmpty()||(ItemStack.isSameItemSameComponents(current,prize)
                &&current.getCount()+prize.getCount()<=current.getMaxStackSize())){
                space=true;break;
            }
        }
        if(!space){
            player.sendSystemMessage(Component.literal("§c[EvolutionCraft] Haz espacio en el inventario para reclamar el ascenso."),true);
            return false;
        }
        p.claimedReputationRanks.add(id);
        ProgressStore.save(player);
        player.getInventory().add(prize);
        player.giveExperiencePoints(XP[index]);
        player.sendSystemMessage(Component.literal("§6[EvolutionCraft] Ascenso reclamado: §f"+
            NAMES[index]+" §e("+COUNTS[index]+" objetos + "+XP[index]+" XP)"),true);
        return true;
    }
}
