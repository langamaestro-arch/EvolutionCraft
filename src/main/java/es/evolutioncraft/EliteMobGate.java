package es.evolutioncraft;
import com.google.gson.JsonObject;
import java.util.Set;
final class EliteMobGate {
    static final String[] TIERS={"TOUGH","ELITE","CHAMPION","BERSERKER","INFERNAL"};
    static final String[] LABELS={"toscos","élite","campeones","berserkers","infernales"};
    static int count(Progress p,int i){return p.eliteMobKills==null?0:p.eliteMobKills.getOrDefault(TIERS[i],0);}
    static void record(Progress p,Set<String> tags){
        if(!p.eliteMobGateActive)return;
        for(String tier:TIERS){
            if(tags.contains("elitemobs.elite.tier."+tier)){
                if(p.eliteMobKills==null)p.eliteMobKills=new java.util.HashMap<>();
                p.eliteMobKills.compute(tier,(k,v)->v==null?1:v==Integer.MAX_VALUE?v:v+1);
                return;
            }
        }
    }
    static boolean ready(Progress p,boolean end){
        if(!p.eliteMobGateActive || (end?p.eliteEndGrandfathered:p.eliteNetherGrandfathered))return true;
        int[] required=end?p.eliteEndRequirements:p.eliteNetherRequirements;
        if(required==null)return true;
        for(int i=0;i<5;i++)if(count(p,i)<required[i])return false;
        return true;
    }
    static void addGoals(JsonObject group,Progress p,boolean end){
        if(!p.eliteMobGateActive)return;
        int[] required=end?p.eliteEndRequirements:p.eliteNetherRequirements;
        if(required==null)return;
        boolean exempt=end?p.eliteEndGrandfathered:p.eliteNetherGrandfathered;
        for(int i=0;i<5;i++)if(required[i]>0){
            JsonObject g=new JsonObject();
            g.addProperty("id","elite:"+TIERS[i]);
            g.addProperty("text","EliteMobs: eliminar "+LABELS[i]+" ("+count(p,i)+"/"+required[i]+")"+(exempt?" - acceso previo":""));
            g.addProperty("done",exempt||count(p,i)>=required[i]);
            group.getAsJsonArray("goals").add(g);
        }
    }
}
