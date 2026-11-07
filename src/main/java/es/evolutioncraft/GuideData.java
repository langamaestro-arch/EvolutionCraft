package es.evolutioncraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

/** Snapshot generado por el servidor, único origen de verdad de la guía. */
public final class GuideData {
    private GuideData() {}
    private static JsonObject group(JsonObject root, String key, String name) {
        JsonObject result = new JsonObject();
        result.addProperty("name", name);
        result.add("goals", new JsonArray());
        root.add(key, result);
        return result;
    }
    private static void goal(JsonObject group, String name, boolean done) {
        JsonObject g = new JsonObject();
        g.addProperty("text", name);
        g.addProperty("done", done);
        group.getAsJsonArray("goals").add(g);
    }
    private static void equipment(JsonObject group, Progress p, String material, boolean armor, String label) {
        String[] suffixes = armor ? new String[]{"helmet", "chestplate", "leggings", "boots"} :
                new String[]{"pickaxe", "axe", "sword", "shovel", "hoe"};
        String[] names = armor ? new String[]{"Casco", "Peto", "Pantalones", "Botas"} :
                new String[]{"Pico", "Hacha", "Espada", "Pala", "Azada"};
        for (int i=0; i<suffixes.length; i++) goal(group, names[i]+" de "+label,
                p.crafted.contains("minecraft:"+material+"_"+suffixes[i]));
    }
    private static boolean trade(Progress p, String material) {
        return p.trades.stream().anyMatch(s -> s.startsWith("minecraft:"+material+"_") &&
                (s.endsWith("_pickaxe") || s.endsWith("_axe") || s.endsWith("_sword")
                || s.endsWith("_shovel") || s.endsWith("_hoe")));
    }
    public static String snapshot(ServerPlayer player) {
        Progress p=ProgressStore.get(player);
        JsonObject root=new JsonObject();
        root.addProperty("era",p.era());
        root.addProperty("fishing",p.fishingLevel());
        root.addProperty("nether",p.canNether());
        root.addProperty("end",p.canEnd());
        root.addProperty("advancements",p.advancements.size());
        root.addProperty("fish",p.fish);
        JsonObject eras=group(root,"eras","Progreso de eras");
        String[] mats={"wooden","stone","copper","iron","diamond"};
        String[] labels={"madera","piedra","cobre","hierro","diamante"};
        for(int era=0;era<5;era++) {
            JsonObject stage=new JsonObject();
            stage.addProperty("name",Progress.ERA_NAMES[era+1]);
            JsonArray goals=new JsonArray();
            stage.add("goals",goals);
            JsonObject tmp=new JsonObject();
            tmp.add("goals",goals);
            equipment(tmp,p,mats[era],false,labels[era]);
            if(era==1) equipment(tmp,p,"leather",true,"cuero");
            if(era>=2) equipment(tmp,p,mats[era],true,labels[era]);
            switch(era) {
                case 0 -> goal(tmp,"Avance: conseguir piedra",p.advancements.contains("minecraft:story/mine_stone"));
                case 1 -> goal(tmp,"Intercambiar con aldeanos ("+p.tradeCount+"/1)",p.tradeCount>=1);
                case 2 -> {
                    goal(tmp,"Comerciar una herramienta de hierro",trade(p,"iron"));
                    goal(tmp,"Avance: fundir hierro",p.advancements.contains("minecraft:story/smelt_iron"));
                }
                case 3 -> {
                    goal(tmp,"Comerciar una herramienta de diamante",trade(p,"diamond"));
                    goal(tmp,"Avance: conseguir diamante",p.advancements.contains("minecraft:story/mine_diamond"));
                }
                case 4 -> goal(tmp,"Avance: entrar al Nether",p.advancements.contains("minecraft:story/enter_the_nether"));
            }
            eras.getAsJsonArray("goals").add(stage);
        }
        JsonObject fish=group(root,"fishingGoals","Maestría de pesca");
        goal(fish,"Capturar 10 peces ("+p.fish+"/10)",p.fish>=10);
        goal(fish,"Capturar las 4 especies ("+p.fishSpecies.size()+"/4)",p.fishSpecies.size()>=4);
        goal(fish,"Obtener 3 tesoros distintos ("+p.fishingTreasures.size()+"/3)",p.fishingTreasures.size()>=3);
        goal(fish,"Capturar 100 peces ("+p.fish+"/100)",p.fish>=100);
        goal(fish,"Avance: pesca táctica",p.advancements.contains("minecraft:husbandry/tactical_fishing"));
        JsonObject nether=group(root,"netherGoals","Desbloquear Nether");
        goal(nether,"Alcanzar la Era de Hierro",p.era()>=3);
        goal(nether,"Pesca nivel III",p.fishingLevel()>=3);
        goal(nether,"Derrotar al Warden",p.bossKills.contains("minecraft:warden"));
        goal(nether,"Avances originales ("+p.advancements.size()+"/20)",p.advancements.size()>=20);
        goal(nether,"Explorar los biomas del Overworld",p.advancements.contains("minecraft:adventure/adventuring_time"));
        JsonObject end=group(root,"endGoals","Desbloquear End");
        goal(end,"Desbloquear el Nether",p.canNether());
        goal(end,"Pesca nivel V",p.fishingLevel()==5);
        goal(end,"Derrotar al Wither",p.bossKills.contains("minecraft:wither"));
        goal(end,"Avances originales ("+p.advancements.size()+"/45)",p.advancements.size()>=45);
        goal(end,"Explorar los biomas del Nether",p.advancements.contains("minecraft:nether/explore_nether"));
        return root.toString();
    }
}
