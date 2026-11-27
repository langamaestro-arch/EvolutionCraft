package es.evolutioncraft;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
final class DimensionObjectives {
    private static void goal(JsonObject group,String id,String label,boolean done){
        JsonObject g=new JsonObject();
        g.addProperty("id",id);
        g.addProperty("text",label);
        g.addProperty("done",done);
        group.getAsJsonArray("goals").add(g);
    }
    static JsonObject group(Progress p,boolean end,int achievements,int biomes,int endAchievements){
        JsonObject g=new JsonObject();
        g.addProperty("name",end?"Desbloquear End":"Desbloquear Nether");
        g.add("goals",new JsonArray());
        if(!end){
            goal(g,"era","Alcanzar la Era de Diamante",p.era()>=4);
            goal(g,"fishing","Pesca nivel III",p.fishingLevel()>=3);
            goal(g,"farming","Agricultura nivel III",p.agricultureLevel()>=3);
            goal(g,"husbandry","Ganadería nivel III",p.husbandryLevel()>=3);
            goal(g,"warden","Derrotar al Warden",p.bossKills.contains("minecraft:warden"));
            g.getAsJsonArray("goals").get(4).getAsJsonObject().addProperty("detail","Dónde encontrarlo: Oscuridad Profunda (Deep Dark), especialmente ciudades antiguas; se invoca mediante chilladores de sculk");
            if(p.nicktaleDimensionGateActive)NicktaleDimensionGate.addNetherGoals(g,p);
            goal(g,"advancements","Avances originales ("+p.advancements.size()+"/"+achievements+")",p.advancements.size()>=achievements);
            goal(g,"biomes","Biomas del Overworld ("+p.overworldBiomes.size()+"/"+biomes+")",p.overworldBiomes.size()>=biomes);
            goal(g,"exploration","Exploracion nivel IV",p.explorationLevel()>=4);
        }else{
            goal(g,"nether","Desbloquear el Nether",ready(p,false,achievements,biomes,endAchievements));
            goal(g,"fishing","Pesca nivel V",p.fishingLevel()==5);
            goal(g,"exploration","Exploracion nivel V",p.explorationLevel()>=5);
            goal(g,"alchemy","Alquimia nivel III",p.alchemyLevel()>=3);
            goal(g,"farming","Agricultura nivel V",p.agricultureLevel()>=5);
            goal(g,"husbandry","Ganadería nivel V",p.husbandryLevel()>=5);
            goal(g,"wither","Derrotar al Wither",p.bossKills.contains("minecraft:wither"));
            g.getAsJsonArray("goals").get(6).getAsJsonObject().addProperty("detail","Invócalo con 4 bloques de arena o tierra de almas y 3 cabezas de esqueleto del Wither");
            if(p.nicktaleDimensionGateActive)NicktaleDimensionGate.addEndGoals(g,p);
            goal(g,"advancements","Avances originales ("+p.advancements.size()+"/"+endAchievements+")",p.advancements.size()>=endAchievements);
            goal(g,"nether_biomes","Explorar los biomas del Nether",p.advancements.contains("minecraft:nether/explore_nether"));
        }
        EliteMobGate.addGoals(g,p,end);
        if(p.adminDimensionObjectives!=null){
            for(var entry:g.getAsJsonArray("goals")){
                JsonObject o=entry.getAsJsonObject();
                Boolean manual=p.adminDimensionObjectives.get((end?"end:":"nether:")+o.get("id").getAsString());
                if(manual!=null){
                    o.addProperty("done",manual);
                    o.addProperty("text",o.get("text").getAsString()+(manual?" [Admin: completo]":" [Admin: pendiente]"));
                }
            }
        }
        return g;
    }
    static boolean ready(Progress p,boolean end,int achievements,int biomes,int endAchievements){
        for(var entry:group(p,end,achievements,biomes,endAchievements).getAsJsonArray("goals")){
            if(!entry.getAsJsonObject().get("done").getAsBoolean())return false;
        }
        return true;
    }
}
