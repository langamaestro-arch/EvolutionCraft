package es.evolutioncraft;

import com.google.gson.JsonObject;

/**
 * Optional Nicktale Epic Dungeons 0.23.0 dimension boss gates.
 * The four entity registry IDs and their health are provided by that JAR.
 * Player kill credit is recorded only on the authoritative server.
 */
public final class NicktaleDimensionGate {
    private NicktaleDimensionGate(){}

    public static final String VIOLET_WITCH="nicktaleepicdungeons:violet_flying_witch";
    public static final String FORTRESS_GUARD="nicktaleepicdungeons:fortress_guard";
    public static final String SULTAN="nicktaleepicdungeons:sultan_eternal_sands";
    public static final String ECLIPSE_COLOSSUS="nicktaleepicdungeons:eclipse_colossus";

    public static boolean isRequiredBoss(String entity){
        return entity.equals(VIOLET_WITCH) || entity.equals(FORTRESS_GUARD) ||
               entity.equals(SULTAN) || entity.equals(ECLIPSE_COLOSSUS);
    }
    public static boolean defeated(Progress p,String id){
        return p.nicktaleDimensionBossKills!=null &&
            p.nicktaleDimensionBossKills.contains(id);
    }
    public static boolean netherReady(Progress p){
        return defeated(p,VIOLET_WITCH) && defeated(p,FORTRESS_GUARD);
    }
    public static boolean endReady(Progress p){
        return defeated(p,SULTAN) && defeated(p,ECLIPSE_COLOSSUS);
    }
    private static void goal(JsonObject group,String label,boolean complete){
        JsonObject objective=new JsonObject();
        objective.addProperty("text",label);
        objective.addProperty("done",complete);
        group.getAsJsonArray("goals").add(objective);
    }
    public static void addNetherGoals(JsonObject group,Progress p){
        goal(group,"Nicktale: vencer Bruja voladora violeta (180 HP)",
            p.nicktaleNetherGrandfathered || defeated(p,VIOLET_WITCH));
        goal(group,"Nicktale: vencer Guardia de fortaleza (230 HP)",
            p.nicktaleNetherGrandfathered || defeated(p,FORTRESS_GUARD));
    }
    public static void addEndGoals(JsonObject group,Progress p){
        goal(group,"Nicktale: vencer Sultan de las arenas eternas (650 HP)",
            p.nicktaleEndGrandfathered || defeated(p,SULTAN));
        goal(group,"Nicktale: vencer Coloso del eclipse (700 HP)",
            p.nicktaleEndGrandfathered || defeated(p,ECLIPSE_COLOSSUS));
    }
}
