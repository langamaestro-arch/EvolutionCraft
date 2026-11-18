package es.evolutioncraft;

import com.google.gson.JsonObject;

/**
 * Mandatory Nicktale boss kills for the three demanding era transitions.
 * Boss identifiers and HP values verified from Nicktale Epic Dungeons 0.22.0.
 * Vanilla combat credit belongs exclusively to the player who dealt the fatal blow.
 */
public final class NicktaleBossGate {
    private NicktaleBossGate(){}

    public static String entity(int target){
        return switch(target){
            case 3 -> "nicktaleepicdungeons:mutant_zombie";
            case 4 -> "nicktaleepicdungeons:abyssal_shagbeast";
            case 5 -> "nicktaleepicdungeons:widow_of_the_well";
            default -> "";
        };
    }
    public static String label(int target){
        return switch(target){
            case 3 -> "Zombi mutante (220 HP)";
            case 4 -> "Bestia abisal (320 HP)";
            case 5 -> "Viuda del pozo (520 HP)";
            default -> "";
        };
    }
    public static boolean defeated(Progress p,int target){
        return p.nicktaleEraBossKills!=null &&
            p.nicktaleEraBossKills.contains(entity(target));
    }
    public static void addGoal(JsonObject group,Progress p,int target){
        String id=entity(target);
        if(id.isEmpty())return;
        JsonObject objective=new JsonObject();
        objective.addProperty("text","Jefe Nicktale: derrotar "+label(target));
        objective.addProperty("done",target<=p.nicktaleBossGrandfatherEra || defeated(p,target));
        group.getAsJsonArray("goals").add(objective);
    }
}
