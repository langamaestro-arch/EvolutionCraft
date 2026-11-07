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
        root.addProperty("agriculture",p.agricultureLevel());
        root.addProperty("husbandry",p.husbandryLevel());
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
            if (era>=1 && era<=3) {
                goal(tmp,"Agricultura nivel "+era,p.agricultureLevel()>=era);
                goal(tmp,"Ganadería nivel "+era,p.husbandryLevel()>=era);
            }
            eras.getAsJsonArray("goals").add(stage);
        }
        JsonObject fish=group(root,"fishingGoals","Maestría de pesca");
        goal(fish,"Nivel I: pescar 25 peces ("+p.fish+"/25)",p.fish>=25);
        goal(fish,"Nivel II: pescar 60 peces ("+p.fish+"/60)",p.fish>=60);
        goal(fish,"Nivel II: 3 especies ("+p.fishSpecies.size()+"/3)",p.fishSpecies.size()>=3);
        goal(fish,"Nivel III: pescar 120 peces ("+p.fish+"/120)",p.fish>=120);
        goal(fish,"Nivel III: 4 especies ("+p.fishSpecies.size()+"/4)",p.fishSpecies.size()>=4);
        goal(fish,"Nivel III: 2 tesoros ("+p.fishingTreasures.size()+"/2)",p.fishingTreasures.size()>=2);
        goal(fish,"Nivel IV: pescar 250 peces ("+p.fish+"/250)",p.fish>=250);
        goal(fish,"Nivel IV: 3 tesoros ("+p.fishingTreasures.size()+"/3)",p.fishingTreasures.size()>=3);
        goal(fish,"Nivel V: pescar 500 peces ("+p.fish+"/500)",p.fish>=500);
        goal(fish,"Nivel V: avance Pesca táctica",p.advancements.contains("minecraft:husbandry/tactical_fishing"));
        JsonObject farm=group(root,"agricultureGoals","Agricultura");
        goal(farm,"Nivel I: cosechar 32 cosechas de trigo ("+p.wheatHarvests+"/32)",p.wheatHarvests>=32);
        goal(farm,"Nivel II: trigo, zanahoria, patata y remolacha ("+p.harvested.stream().filter(s->java.util.Set.of("minecraft:wheat","minecraft:carrots","minecraft:potatoes","minecraft:beetroots").contains(s)).count()+"/4)",p.harvested.containsAll(java.util.Set.of("minecraft:wheat","minecraft:carrots","minecraft:potatoes","minecraft:beetroots")));
        goal(farm,"Nivel III: 250 cosechas ("+p.harvests+"/250)",p.harvests>=250);
        goal(farm,"Nivel III: melones y calabazas",p.harvested.containsAll(java.util.Set.of("minecraft:melon","minecraft:pumpkin")));
        goal(farm,"Nivel IV: 500 cosechas ("+p.harvests+"/500)",p.harvests>=500);
        goal(farm,"Nivel IV: cacao y verrugas del Nether",p.harvested.containsAll(java.util.Set.of("minecraft:cocoa","minecraft:nether_wart")));
        goal(farm,"Nivel V: 1.000 cosechas ("+p.harvests+"/1000)",p.harvests>=1000);
        JsonObject animal=group(root,"husbandryGoals","Ganadería");
        goal(animal,"Nivel I: 5 reproducciones ("+p.breedings+"/5)",p.breedings>=5);
        goal(animal,"Nivel II: criar vacas, ovejas, cerdos y gallinas ("+p.bred.stream().filter(s->java.util.Set.of("minecraft:cow","minecraft:sheep","minecraft:pig","minecraft:chicken").contains(s)).count()+"/4)",p.bred.containsAll(java.util.Set.of("minecraft:cow","minecraft:sheep","minecraft:pig","minecraft:chicken")));
        goal(animal,"Nivel III: 30 reproducciones ("+p.breedings+"/30)",p.breedings>=30);
        goal(animal,"Nivel III: domesticar lobo y gato ("+p.tamed.size()+"/2)",p.tamed.containsAll(java.util.Set.of("minecraft:wolf","minecraft:cat")));
        goal(animal,"Nivel IV: 75 reproducciones ("+p.breedings+"/75)",p.breedings>=75);
        goal(animal,"Nivel IV: criar caballos",p.bred.contains("minecraft:horse"));
        goal(animal,"Nivel V: 150 reproducciones ("+p.breedings+"/150)",p.breedings>=150);
        JsonObject nether=group(root,"netherGoals","Desbloquear Nether");
        goal(nether,"Alcanzar la Era de Hierro",p.era()>=3);
        goal(nether,"Pesca nivel III",p.fishingLevel()>=3);
        goal(nether,"Agricultura nivel III",p.agricultureLevel()>=3);
        goal(nether,"Ganadería nivel III",p.husbandryLevel()>=3);
        goal(nether,"Derrotar al Warden",p.bossKills.contains("minecraft:warden"));
        goal(nether,"Avances originales ("+p.advancements.size()+"/20)",p.advancements.size()>=20);
        goal(nether,"Explorar los biomas del Overworld",p.advancements.contains("minecraft:adventure/adventuring_time"));
        JsonObject end=group(root,"endGoals","Desbloquear End");
        goal(end,"Desbloquear el Nether",p.canNether());
        goal(end,"Pesca nivel V",p.fishingLevel()==5);
        goal(end,"Agricultura nivel V",p.agricultureLevel()>=5);
        goal(end,"Ganadería nivel V",p.husbandryLevel()>=5);
        goal(end,"Derrotar al Wither",p.bossKills.contains("minecraft:wither"));
        goal(end,"Avances originales ("+p.advancements.size()+"/45)",p.advancements.size()>=45);
        goal(end,"Explorar los biomas del Nether",p.advancements.contains("minecraft:nether/explore_nether"));
        return root.toString();
    }
}
