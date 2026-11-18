package es.evolutioncraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;

/** Extrae los objetivos de la era actual del diario existente. */
public final class EraHudData {
    private EraHudData(){}

    public static String snapshot(ServerPlayer player){
        Progress p=ProgressStore.get(player);
        int era=p.era();
        JsonObject result=new JsonObject();
        result.addProperty("era",era);
        result.addProperty("eraName",Progress.ERA_NAMES[era]);
        result.addProperty("maxObjectives",EvolutionConfig.get(player).hudMaxObjectives);
        JsonArray pending=new JsonArray();
        if(era<5){
            // Fuente de verdad compartida con el diario RPG: mismos requisitos.
            JsonObject guide=JsonParser.parseString(GuideData.snapshot(player)).getAsJsonObject();
            JsonArray goals=guide.getAsJsonObject("eras").getAsJsonArray("goals")
                .get(era).getAsJsonObject().getAsJsonArray("goals");
            for(var element:goals){
                JsonObject objective=element.getAsJsonObject();
                if(objective.get("done").getAsBoolean())continue;
                String label=objective.get("text").getAsString();
                String mastery="Maestria herramientas "+switch(era){
                    case 0 -> "I";
                    case 1 -> "II";
                    case 2 -> "III";
                    case 3 -> "IV";
                    default -> "V";
                };
                if(label.equals(mastery)){
                    int current=switch(era){
                        case 0 -> p.toolWood;
                        case 1 -> p.toolStone;
                        case 2 -> p.toolCopper;
                        case 3 -> p.toolIron;
                        default -> p.toolDiamond;
                    };
                    int target=switch(era){
                        case 0 -> 50;
                        case 1 -> 150;
                        case 2 -> 250;
                        case 3 -> 400;
                        default -> 600;
                    };
                    String material=switch(era){
                        case 0 -> "madera";
                        case 1 -> "piedra";
                        case 2 -> "cobre";
                        case 3 -> "hierro";
                        default -> "diamante";
                    };
                    label="Picar con "+material+" ("+Math.min(current,target)+"/"+target+")";
                    if(era>=1 && current>=target){
                        label="Usar 3 tipos de herramientas de "+material;
                    }
                }
                pending.add(label);
            }
        }
        result.add("pending",pending);
        result.add("contracts",AdventureContracts.hud(player));
        return result.toString();
    }
}
