package es.evolutioncraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Administrative-only virtual objective progression. Never overwrites real
 * counters, advancements, Thoridrop statistics, or rewards. The server uses
 * adminEraOverride as the authoritative cap / unlock while in admin mode.
 *
 * Goal IDs are e<target era>_<1-based slot> from the live server guide.
 */
public final class AdminEraObjectives {
    private AdminEraObjectives(){}

    public static String id(int target,int slot){return "e"+target+"_"+slot;}

    public static void apply(JsonObject stage,Progress p,int target){
        if(p.adminEraObjectives==null)
            p.adminEraObjectives=new java.util.HashMap<>();
        JsonArray goals=stage.getAsJsonArray("goals");
        for(int i=0;i<goals.size();i++){
            JsonObject objective=goals.get(i).getAsJsonObject();
            String id=id(target,i+1);
            objective.addProperty("id",id);
            if(p.adminEraOverride>=0){
                // All requirements up to the current unlocked era are complete.
                // Requirements for future eras are pending even if their real
                // counters were once satisfied; this is an explicit admin reset.
                objective.addProperty("done",target<=p.adminEraOverride);
            }
            Boolean manual=p.adminEraObjectives.get(id);
            if(manual!=null)objective.addProperty("done",manual);
        }
    }

    public static int goalCount(JsonObject guide,int target){
        return goals(guide,target).size();
    }

    public static JsonArray goals(JsonObject guide,int target){
        if(target<1||target>5)throw new IllegalArgumentException("Invalid era");
        return guide.getAsJsonObject("eras").getAsJsonArray("goals")
            .get(target-1).getAsJsonObject().getAsJsonArray("goals");
    }

    public static boolean allComplete(JsonObject guide,int target){
        for(var element:goals(guide,target)){
            if(!element.getAsJsonObject().get("done").getAsBoolean())return false;
        }
        return true;
    }

    public static void clearFrom(Progress p,int target){
        if(p.adminEraObjectives==null)
            p.adminEraObjectives=new java.util.HashMap<>();
        p.adminEraObjectives.keySet().removeIf(k->{
            for(int i=target;i<=5;i++)if(k.startsWith("e"+i+"_"))return true;
            return false;
        });
    }

    public static void clearThrough(Progress p,int target){
        if(p.adminEraObjectives==null)
            p.adminEraObjectives=new java.util.HashMap<>();
        p.adminEraObjectives.keySet().removeIf(k->{
            for(int i=1;i<=target;i++)if(k.startsWith("e"+i+"_"))return true;
            return false;
        });
    }
}
