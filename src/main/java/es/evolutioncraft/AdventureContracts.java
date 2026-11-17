package es.evolutioncraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Beta 2: tablón de contratos renovable, tres ofertas por categoría.
 * Cada oferta es una instancia con ID único. Todas las decisiones y premios
 * se validan en el servidor; el progreso sólo cuenta después de aceptar.
 */
public final class AdventureContracts {
    private AdventureContracts(){}
    public static final List<String> CATEGORIES=List.of("PESCA","RECOLECCION","COMBATE","EXPLORACION");
    private static final int OFFERS_PER_CATEGORY=3;
    private static final int ACTIVE_LIMIT=3;
    private static final int RECENT_LIMIT=4;

    public record Contract(String id,String title,String category,String tier,String description,
                           String kind,String target,int quantity,int minEra,int emeralds,int xp){}

    // Hay alternativas suficientes incluso en Madera para evitar repeticiones inmediatas.
    // Los contratos de eras superiores se incorporan al sorteo al desbloquearse.
    public static final List<Contract> CATALOG=List.of(
        new Contract("fish_15","El pescador del pueblo","PESCA","BRONCE","Captura 15 peces","fish","*",15,0,8,150),
        new Contract("fish_25","La cesta del pescador","PESCA","BRONCE","Captura 25 peces","fish","*",25,0,10,200),
        new Contract("fish_40","Jornada en el muelle","PESCA","BRONCE","Captura 40 peces","fish","*",40,0,12,250),
        new Contract("fish_60","Redes repletas","PESCA","PLATA","Captura 60 peces","fish","*",60,0,16,350),
        new Contract("fish_75","El mercado del pescado","PESCA","PLATA","Captura 75 peces","fish","*",75,0,18,400),
        new Contract("fish_100","Pescador incansable","PESCA","PLATA","Captura 100 peces","fish","*",100,0,20,500),
        new Contract("fish_150","Gran maestro de las aguas","PESCA","ORO","Captura 150 peces","fish","*",150,3,28,850),
        new Contract("fish_220","Leyenda de los océanos","PESCA","ORO","Captura 220 peces","fish","*",220,4,36,1200),

        new Contract("harvest_32","Cosecha abundante","RECOLECCION","BRONCE","Recoge 32 cultivos maduros","harvest","*",32,0,8,150),
        new Contract("harvest_48","La despensa del pueblo","RECOLECCION","BRONCE","Recoge 48 cultivos maduros","harvest","*",48,0,10,200),
        new Contract("harvest_64","Campos fértiles","RECOLECCION","BRONCE","Recoge 64 cultivos maduros","harvest","*",64,0,12,260),
        new Contract("harvest_96","Trabajo de temporada","RECOLECCION","PLATA","Recoge 96 cultivos maduros","harvest","*",96,0,15,330),
        new Contract("harvest_128","Gran cosecha","RECOLECCION","PLATA","Recoge 128 cultivos maduros","harvest","*",128,0,20,500),
        new Contract("harvest_160","El granjero diligente","RECOLECCION","PLATA","Recoge 160 cultivos maduros","harvest","*",160,0,22,550),
        new Contract("mine_copper","Mineral para el herrero","RECOLECCION","PLATA","Extrae 20 minerales de cobre naturales","mine","minecraft:copper_ore",20,2,16,400),
        new Contract("mine_diamond","El brillo de la mina","RECOLECCION","ORO","Extrae 8 minerales de diamante naturales","mine","minecraft:diamond_ore",8,4,32,1000),

        new Contract("hunt_zombies","Limpieza nocturna","COMBATE","BRONCE","Derrota 12 zombis","kill","minecraft:zombie",12,0,10,200),
        new Contract("hunt_zombies_20","Guardia de la aldea","COMBATE","BRONCE","Derrota 20 zombis","kill","minecraft:zombie",20,0,12,260),
        new Contract("hunt_skeletons_8","Huesos inquietos","COMBATE","BRONCE","Derrota 8 esqueletos","kill","minecraft:skeleton",8,0,8,180),
        new Contract("hunt_spiders","Ocho patas","COMBATE","BRONCE","Derrota 10 arañas","kill","minecraft:spider",10,0,10,210),
        new Contract("hunt_creepers","Sssssin explosiones","COMBATE","PLATA","Derrota 8 creepers","kill","minecraft:creeper",8,0,16,350),
        new Contract("hunt_skeletons","Arqueros en la oscuridad","COMBATE","PLATA","Derrota 18 esqueletos","kill","minecraft:skeleton",18,0,18,450),
        new Contract("hunt_drowned","Peligro bajo el agua","COMBATE","PLATA","Derrota 12 ahogados","kill","minecraft:drowned",12,0,18,420),
        new Contract("blaze_10","Cazador del Nether","COMBATE","ORO","Derrota 10 blazes","kill","minecraft:blaze",10,4,32,1000),
        new Contract("wither_skeleton_5","Huesos de la fortaleza","COMBATE","ORO","Derrota 5 esqueletos wither","kill","minecraft:wither_skeleton",5,4,32,1000),

        new Contract("explore_2","Explorador de ruinas","EXPLORACION","PLATA","Visita 2 estructuras distintas","structure","*",2,0,16,400),
        new Contract("explore_3","Cronista de caminos","EXPLORACION","PLATA","Visita 3 estructuras distintas","structure","*",3,0,20,450),
        new Contract("explore_biome_2","Huellas en el mapa","EXPLORACION","BRONCE","Visita 2 biomas distintos del Overworld","biome","*",2,0,8,180),
        new Contract("explore_biome_3","Senderos desconocidos","EXPLORACION","BRONCE","Visita 3 biomas distintos del Overworld","biome","*",3,0,10,220),
        new Contract("explore_biome_4","El atlas del aventurero","EXPLORACION","PLATA","Visita 4 biomas distintos del Overworld","biome","*",4,0,16,380),
        new Contract("explore_biome_5","Exploración sin fronteras","EXPLORACION","PLATA","Visita 5 biomas distintos del Overworld","biome","*",5,0,20,480),
        new Contract("explore_4","Tras las antiguas piedras","EXPLORACION","PLATA","Visita 4 estructuras distintas","structure","*",4,1,24,600),
        new Contract("nether_biomes","Cartógrafo infernal","EXPLORACION","ORO","Visita 3 biomas distintos del Nether","nether_biome","*",3,4,32,1000)
    );

    private static void init(Progress p){
        if(p.activeContracts==null)p.activeContracts=new HashSet<>();
        if(p.claimedContracts==null)p.claimedContracts=new HashSet<>();
        if(p.contractProgress==null)p.contractProgress=new HashMap<>();
        if(p.contractOffers==null)p.contractOffers=new HashMap<>();
        if(p.contractRecentTemplates==null)p.contractRecentTemplates=new HashMap<>();
        if(p.contractExplorationSeen==null)p.contractExplorationSeen=new HashMap<>();
    }
    public static Contract byId(String id){
        for(Contract c:CATALOG)if(c.id().equals(id))return c;
        return null;
    }
    private static Contract instance(String instanceId){
        Contract legacy=byId(instanceId);
        if(legacy!=null)return legacy;
        // c<numero>_<id plantilla>. Identificadores no reutilizables.
        if(instanceId==null||!instanceId.matches("c[0-9]+_[a-z0-9_]+"))return null;
        int underscore=instanceId.indexOf('_');
        return byId(instanceId.substring(underscore+1));
    }
    private static String templateId(String instanceId){
        Contract c=instance(instanceId);
        return c==null?"":c.id();
    }
    private static String slot(String category,int index){return category+":"+index;}

    private static String pick(Progress p,String category,int era){
        List<Contract> pool=new ArrayList<>();
        Set<String> inUse=new HashSet<>();
        for(String id:p.contractOffers.values())inUse.add(templateId(id));
        for(String id:p.activeContracts)inUse.add(templateId(id));
        for(Contract c:CATALOG)if(c.category().equals(category)&&c.minEra()<=era&&!inUse.contains(c.id()))
            pool.add(c);
        if(pool.isEmpty()){
            // Mantener siempre tres ofertas incluso con tres activos de una era
            // temprana: permitimos repetir plantilla en activos, nunca en ofertas.
            inUse.clear();
            for(String id:p.contractOffers.values())inUse.add(templateId(id));
            for(Contract c:CATALOG)if(c.category().equals(category)&&c.minEra()<=era
                &&!inUse.contains(c.id()))pool.add(c);
        }
        if(pool.isEmpty())return null;
        List<String> recent=p.contractRecentTemplates.computeIfAbsent(category,k->new ArrayList<>());
        List<Contract> filtered=new ArrayList<>();
        for(Contract c:pool)if(!recent.contains(c.id()))filtered.add(c);
        // Prefiere novedades. Si el pool es pequeño, relaja solo el historial,
        // nunca la era ni los contratos ya activos u ofertados.
        if(filtered.isEmpty())filtered=pool;
        Contract chosen=filtered.get(ThreadLocalRandom.current().nextInt(filtered.size()));
        recent.add(chosen.id());
        while(recent.size()>RECENT_LIMIT)recent.remove(0);
        return "c"+(++p.contractSequence)+"_"+chosen.id();
    }
    private static boolean fillOffers(Progress p,int era){
        boolean changed=false;
        // Depurar referencias obsoletas sin invalidar contratos activos de Beta 1.
        for(String category:CATEGORIES){
            for(int i=0;i<OFFERS_PER_CATEGORY;i++){
                String key=slot(category,i);
                String id=p.contractOffers.get(key);
                Contract c=instance(id);
                if(c!=null&&c.category().equals(category)&&c.minEra()<=era&&!p.activeContracts.contains(id))
                    continue;
                p.contractOffers.remove(key);
                String chosen=pick(p,category,era);
                if(chosen!=null)p.contractOffers.put(key,chosen);
                changed=true;
            }
        }
        return changed;
    }
    public static boolean accept(ServerPlayer player,String id){
        if(!EvolutionConfig.get(player).contractsEnabled)return false;
        Progress p=ProgressStore.get(player);init(p);
        boolean synced=fillOffers(p,p.era());
        if(synced)ProgressStore.save(player);
        Contract c=instance(id);
        if(c==null||p.era()<c.minEra()||p.activeContracts.size()>=ACTIVE_LIMIT||
            p.claimedContracts.contains(id)||p.activeContracts.contains(id)||
            !p.contractOffers.containsValue(id))return false;
        p.activeContracts.add(id);
        p.contractProgress.put(id,0);
        p.contractExplorationSeen.put(id,new HashSet<>());
        p.contractOffers.values().removeIf(v->v.equals(id));
        fillOffers(p,p.era());  // El hueco queda repuesto inmediatamente.
        ProgressStore.save(player);
        player.sendSystemMessage(Component.literal("§6[EvolutionCraft] Contrato aceptado: §f"+c.title()),true);
        return true;
    }
    public static boolean abandon(ServerPlayer player,String id){
        if(!EvolutionConfig.get(player).contractsEnabled)return false;
        Progress p=ProgressStore.get(player);init(p);
        if(!p.activeContracts.remove(id))return false;
        p.contractProgress.remove(id);
        p.contractExplorationSeen.remove(id);
        // No devolver el mismo contrato a las ofertas; renovar una oferta del tipo.
        Contract old=instance(id);
        if(old!=null)rotateOne(p,old.category(),p.era());
        fillOffers(p,p.era());
        ProgressStore.save(player);
        player.sendSystemMessage(Component.literal("§e[EvolutionCraft] Contrato abandonado: "+(old==null?id:old.title())),true);
        return true;
    }
    private static void rotateOne(Progress p,String category,int era){
        int index=ThreadLocalRandom.current().nextInt(OFFERS_PER_CATEGORY);
        p.contractOffers.remove(slot(category,index));
        fillOffers(p,era);
    }
    public static void onAction(ServerPlayer player,String kind,String target,int amount){
        if(amount<=0||!EvolutionConfig.get(player).contractsEnabled)return;
        Progress p=ProgressStore.get(player);init(p);
        boolean changed=false;
        for(String id:p.activeContracts){
            Contract c=instance(id);
            if(c==null||!c.kind().equals(kind))continue;
            if(!c.target().equals("*")&&!c.target().equals(target))continue;
            int before=p.contractProgress.getOrDefault(id,0);
            if(before>=c.quantity())continue;
            if(kind.equals("biome")||kind.equals("nether_biome")||kind.equals("structure")){
                // Sólo una vez por tipo de lugar y contrato. Revisitas posteriores
                // a aceptar cuentan incluso si el diario ya conocía el lugar.
                if(!p.contractExplorationSeen.computeIfAbsent(id,k->new HashSet<>()).add(kind+":"+target))
                    continue;
            }
            int after=Math.min(c.quantity(),before+amount);
            if(after!=before){
                p.contractProgress.put(id,after);
                changed=true;
                if(after==c.quantity())player.sendSystemMessage(Component.literal(
                    "§a[EvolutionCraft] Contrato completado: "+c.title()+" · Pulsa P para reclamarlo."),true);
            }
        }
        if(changed)ProgressStore.save(player);
    }
    public static boolean claim(ServerPlayer player,String id){
        if(!EvolutionConfig.get(player).contractsEnabled)return false;
        Progress p=ProgressStore.get(player);init(p);
        Contract c=instance(id);
        if(c==null||!p.activeContracts.contains(id)||p.claimedContracts.contains(id)||
            p.contractProgress.getOrDefault(id,0)<c.quantity()||p.era()<c.minEra())return false;
        ItemStack emeralds=new ItemStack(Items.EMERALD,c.emeralds());
        boolean capacity=false;
        for(int i=0;i<36;i++){
            ItemStack slot=player.getInventory().getItem(i);
            if(slot.isEmpty()||(ItemStack.isSameItemSameComponents(slot,emeralds)&&
                slot.getCount()+emeralds.getCount()<=slot.getMaxStackSize())){capacity=true;break;}
        }
        if(!capacity){
            player.sendSystemMessage(Component.literal("§c[EvolutionCraft] Haz espacio en el inventario antes de reclamar."),true);
            return false;
        }
        p.claimedContracts.add(id);
        p.activeContracts.remove(id);
        p.contractProgress.remove(id);
        p.contractExplorationSeen.remove(id);
        rotateOne(p,c.category(),p.era());
        ProgressStore.save(player);
        player.getInventory().add(emeralds);
        player.giveExperiencePoints(c.xp());
        player.sendSystemMessage(Component.literal("§6[EvolutionCraft] Premio de contrato: "+
            c.emeralds()+" esmeraldas y "+c.xp()+" XP."),true);
        return true;
    }
    private static JsonObject row(Contract c,String instanceId,Progress p,boolean on,boolean claimed,
                                  boolean offered,int era){
        JsonObject result=new JsonObject();
        int current=p.contractProgress.getOrDefault(instanceId,0);
        result.addProperty("id",instanceId);
        result.addProperty("text",c.title());
        result.addProperty("category",c.category());
        result.addProperty("tier",c.tier());
        result.addProperty("description",c.description());
        result.addProperty("progress",current);
        result.addProperty("target",c.quantity());
        result.addProperty("minEra",c.minEra());
        result.addProperty("reward",c.emeralds()+" esmeraldas + "+c.xp()+" XP");
        result.addProperty("ready",on&&current>=c.quantity());
        result.addProperty("canAccept",offered&&era>=c.minEra()&&p.activeContracts.size()<ACTIVE_LIMIT);
        result.addProperty("done",claimed);
        return result;
    }
    public static JsonObject snapshot(ServerPlayer player){
        Progress p=ProgressStore.get(player);init(p);
        boolean enabled=EvolutionConfig.get(player).contractsEnabled;
        if(enabled&&fillOffers(p,p.era()))ProgressStore.save(player);
        JsonObject root=new JsonObject();
        root.addProperty("enabled",enabled);
        root.addProperty("activeCount",p.activeContracts.size());
        root.addProperty("slotsPerCategory",OFFERS_PER_CATEGORY);
        JsonArray available=new JsonArray(),active=new JsonArray(),completed=new JsonArray();
        for(String category:CATEGORIES){
            for(int i=0;i<OFFERS_PER_CATEGORY;i++){
                String id=p.contractOffers.get(slot(category,i));
                Contract c=instance(id);
                if(c!=null)available.add(row(c,id,p,false,false,true,p.era()));
            }
        }
        p.activeContracts.stream().sorted().forEach(id->{
            Contract c=instance(id);
            if(c!=null)active.add(row(c,id,p,true,false,false,p.era()));
        });
        p.claimedContracts.stream().sorted(Comparator.reverseOrder()).limit(60).forEach(id->{
            Contract c=instance(id);
            if(c!=null)completed.add(row(c,id,p,false,true,false,p.era()));
        });
        root.add("available",available);
        root.add("active",active);
        root.add("completed",completed);
        return root;
    }
}
