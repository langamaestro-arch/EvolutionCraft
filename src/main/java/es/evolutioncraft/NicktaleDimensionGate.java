package es.evolutioncraft;

import com.google.gson.JsonObject;

/**
 * Optional Nicktale 0.23.0 dimension gates. Verified against shipped
 * miniboss_assignments.json and twin_flames_bastion/arena.nbt.
 * Server-side kill credit, shared fire-centaur credit across both dimensions.
 */
public final class NicktaleDimensionGate {
    private NicktaleDimensionGate(){}

    public static final String FIRE_CENTAUR="nicktaleepicdungeons:fire_centaur";
    public static final String CERBERUS="nicktaleepicdungeons:cerberus";
    public static final String ECLIPSE_COLOSSUS="nicktaleepicdungeons:eclipse_colossus";
    public static final String VOLCANIC_GOLEM="nicktaleepicdungeons:volcanic_golem";

    public static boolean isRequiredBoss(String entity){
        return entity.equals(FIRE_CENTAUR) || entity.equals(CERBERUS) ||
               entity.equals(ECLIPSE_COLOSSUS) || entity.equals(VOLCANIC_GOLEM);
    }
    public static boolean defeated(Progress p,String id){
        return p.nicktaleDimensionBossKills!=null &&
            p.nicktaleDimensionBossKills.contains(id);
    }
    public static boolean netherReady(Progress p){
        return defeated(p,FIRE_CENTAUR) && defeated(p,CERBERUS) &&
               defeated(p,ECLIPSE_COLOSSUS);
    }
    public static boolean endReady(Progress p){
        return defeated(p,VOLCANIC_GOLEM) && defeated(p,FIRE_CENTAUR);
    }
    private static void goal(JsonObject group,String label,String location,boolean complete){
        JsonObject objective=new JsonObject();
        objective.addProperty("text",label);
        objective.addProperty("detail","Dónde encontrarlo: "+location);
        objective.addProperty("done",complete);
        group.getAsJsonArray("goals").add(objective);
    }
    public static void addNetherGoals(JsonObject group,Progress p){
        goal(group,"Derrotar al Centauro arquero de fuego",
            "Bastión de las Llamas Gemelas (Twin Flames Bastion)",
            p.nicktaleNetherGrandfathered || defeated(p,FIRE_CENTAUR));
        goal(group,"Derrotar a Cerbero de llamas",
            "Cripta de Cenizas (Ashen Crypt) o Catacumbas Rojas (Red Catacombs)",
            p.nicktaleNetherGrandfathered || defeated(p,CERBERUS));
        goal(group,"Derrotar al Coloso del Eclipse",
            "Montaña Enana (Dwarven Mountain) o Torre de Babel (Babel Tower)",
            p.nicktaleNetherGrandfathered || defeated(p,ECLIPSE_COLOSSUS));
    }
    public static void addEndGoals(JsonObject group,Progress p){
        goal(group,"Derrotar al Gólem volcánico",
            "Bastión de las Llamas Gemelas (Twin Flames Bastion)",
            p.nicktaleEndGrandfathered || defeated(p,VOLCANIC_GOLEM));
        goal(group,"Derrotar al Centauro arquero de fuego",
            "Bastión de las Llamas Gemelas (Twin Flames Bastion). Cuenta la victoria del Nether",
            p.nicktaleEndGrandfathered || defeated(p,FIRE_CENTAUR));
    }
}
