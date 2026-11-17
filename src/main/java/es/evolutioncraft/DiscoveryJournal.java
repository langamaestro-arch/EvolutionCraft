package es.evolutioncraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Diario de descubrimientos. No asigna fechas ficticias a registros de versiones previas.
 * Todos los datos proceden del progreso persistente del servidor.
 */
public final class DiscoveryJournal {
    private DiscoveryJournal(){}

    public static void record(Progress progress,ServerPlayer player,String kind,String id){
        if(progress.discoveryDetails==null)progress.discoveryDetails=new java.util.HashMap<>();
        String key=kind+":"+id;
        if(progress.discoveryDetails.containsKey(key))return;
        String dimension=player.level().dimension()==Level.OVERWORLD?"Overworld":
            player.level().dimension()==Level.NETHER?"Nether":
            player.level().dimension()==Level.END?"End":
            player.level().dimension().identifier().toString();
        long day=player.level().getOverworldClockTime()/24000L+1;
        progress.discoveryDetails.put(key,"Día "+day+" · "+dimension+
            " · X "+player.blockPosition().getX()+" Z "+player.blockPosition().getZ());
    }

    private static JsonObject entry(String id,String type,Map<String,String> details){
        JsonObject o=new JsonObject();
        o.addProperty("id",id);
        String label=id.substring(id.indexOf(':')+1).replace('_',' ').replace('/',' ');
        if(!label.isEmpty())label=Character.toUpperCase(label.charAt(0))+label.substring(1);
        o.addProperty("text",label);
        String meta=details.get(type+":"+id);
        o.addProperty("detail",meta==null?"Registro anterior · fecha y lugar no disponibles":meta);
        o.addProperty("done",true);
        return o;
    }
    private static JsonArray list(Set<String> ids,String type,Map<String,String> details){
        JsonArray out=new JsonArray();
        if(ids==null)return out;
        ids.stream().sorted().limit(150).forEach(id->out.add(entry(id,type,details)));
        return out;
    }
    private static void stat(JsonArray into,String label,long value){
        JsonObject o=new JsonObject();
        o.addProperty("text",label+": "+value);
        o.addProperty("done",true);
        into.add(o);
    }
    public static JsonObject snapshot(Progress p){
        Map<String,String> details=p.discoveryDetails==null?Map.of():p.discoveryDetails;
        JsonObject out=new JsonObject();
        out.add("overworld",list(p.overworldBiomes,"biome",details));
        out.add("nether",list(p.netherBiomes,"biome",details));
        out.add("structures",list(p.visitedStructures,"structure",details));
        Set<String> bosses=new java.util.TreeSet<>();
        if(p.discoveredBosses!=null)bosses.addAll(p.discoveredBosses);
        if(p.bossKills!=null)bosses.addAll(p.bossKills);
        if(p.elderGuardianKills>0)bosses.add("minecraft:elder_guardian");
        if(p.dragonSlain)bosses.add("minecraft:ender_dragon");
        out.add("bosses",list(bosses,"boss",details));

        JsonArray stats=new JsonArray();
        stat(stats,"Peces capturados",p.fish);
        stat(stats,"Tesoros pescados",p.treasuresCaught);
        stat(stats,"Intercambios con aldeanos",p.tradeCount);
        stat(stats,"Cosechas recolectadas",p.harvests);
        stat(stats,"Reproducciones de animales",p.breedings);
        stat(stats,"Noches sobrevividas",p.nightsSurvived);
        stat(stats,"Muertes",p.deathCount);
        stat(stats,"Bloques picados con madera",p.toolWood);
        stat(stats,"Bloques picados con piedra",p.toolStone);
        stat(stats,"Bloques picados con cobre",p.toolCopper);
        stat(stats,"Bloques picados con hierro",p.toolIron);
        stat(stats,"Bloques picados con diamante",p.toolDiamond);
        stat(stats,"Mineral de cobre natural",p.naturalCopperOre);
        stat(stats,"Mineral de diamante natural",p.naturalDiamondOre);
        stat(stats,"Biomas explorados en Overworld",p.overworldBiomes.size());
        stat(stats,"Biomas explorados en Nether",p.netherBiomes.size());
        stat(stats,"Estructuras visitadas",p.visitedStructures.size());
        stat(stats,"Jefes diferentes registrados",bosses.size());
        out.add("statistics",stats);

        JsonArray recent=new JsonArray();
        List<Map.Entry<String,String>> records=new ArrayList<>(details.entrySet());
        // Sólo se muestran los nuevos, sin atribuir tiempos imaginarios a los antiguos.
        // LinkedHashMap no se garantiza en las partidas guardadas: ordenar por día.
        records.sort(Comparator.comparingLong((Map.Entry<String,String> e)->day(e.getValue()))
            .reversed().thenComparing(Map.Entry::getKey));
        for(var e:records.stream().limit(25).toList()){
            String key=e.getKey();
            JsonObject o=new JsonObject();
            o.addProperty("text",key.substring(key.indexOf(':')+1).replace('_',' '));
            o.addProperty("detail",e.getValue());
            o.addProperty("done",true);
            recent.add(o);
        }
        out.add("recent",recent);
        return out;
    }
    private static long day(String metadata){
        try{
            int end=metadata.indexOf(' ');
            int sep=metadata.indexOf(" ·");
            return Long.parseLong(metadata.substring(end+1,sep));
        }catch(Exception ignored){return 0;}
    }
}
