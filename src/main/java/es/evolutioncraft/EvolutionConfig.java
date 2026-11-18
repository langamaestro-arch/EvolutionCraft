package es.evolutioncraft;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuracion por mundo, cargada del servidor. Los valores por defecto
 * mantienen exactamente los requisitos de EvolutionCraft 2.0.
 *
 * Editar evolutioncraft-config.properties con el servidor apagado.
 * Solo se crean claves ausentes; nunca se sobrescriben los ajustes del usuario.
 */
public final class EvolutionConfig {
    private static final Map<Path, EvolutionConfig> CONFIGS=new HashMap<>();
    public final int netherAdvancements;
    public final int endAdvancements;
    public final int netherBiomes;
    public final int explorationInterval;
    public final int hudSyncInterval;
    public final int hudMaxObjectives;
    public final int thoridropSyncInterval;
    public final boolean rewardsEnabled;
    public final boolean contractsEnabled;
    public final boolean thoridropEnabled;
    public final boolean equipmentRestrictions;
    public final boolean craftingRestrictions;
    public final boolean arsenalForgeRestrictions;

    private static int bounded(Properties p,String name,int fallback,int min,int max){
        try{return Math.clamp(Integer.parseInt(p.getProperty(name).trim()),min,max);}
        catch(Exception ignored){return fallback;}
    }
    private static boolean booleanValue(Properties p,String key,boolean fallback){
        String v=p.getProperty(key);
        return v==null?fallback:v.equalsIgnoreCase("true")?true:v.equalsIgnoreCase("false")?false:fallback;
    }
    private EvolutionConfig(Properties p){
        netherAdvancements=bounded(p,"nether.advancements",40,0,500);
        endAdvancements=bounded(p,"end.advancements",70,0,500);
        netherBiomes=bounded(p,"nether.overworld_biomes",20,0,100);
        explorationInterval=bounded(p,"performance.scan_interval_ticks",80,40,400);
        hudSyncInterval=bounded(p,"performance.hud_sync_interval_ticks",40,20,200);
        hudMaxObjectives=bounded(p,"hud.max_objectives",24,4,24);
        thoridropSyncInterval=bounded(p,"performance.thoridrop_sync_interval_ticks",40,20,400);
        rewardsEnabled=booleanValue(p,"rewards.enabled",true);
        contractsEnabled=booleanValue(p,"contracts.enabled",true);
        thoridropEnabled=booleanValue(p,"thoridrop.era_requirements",true);
        equipmentRestrictions=booleanValue(p,"equipment.restrictions",true);
        craftingRestrictions=booleanValue(p,"crafting.restrictions",true);
        arsenalForgeRestrictions=booleanValue(p,"arsenal_forge.restrictions",true);
    }
    private static void def(Properties p,String key,String value){
        if(!p.containsKey(key))p.setProperty(key,value);
    }
    private static EvolutionConfig load(Path configPath){
        Properties p=new Properties();
        try{
            Files.createDirectories(configPath.getParent());
            if(Files.exists(configPath)){
                try(Reader in=Files.newBufferedReader(configPath)){p.load(in);}
            }
        }catch(IOException e){
            EvolutionCraftLogger.warn("No se pudo leer configuracion: "+e.getMessage());
        }
        int oldCount=p.size();
        def(p,"nether.advancements","40");
        def(p,"end.advancements","70");
        def(p,"nether.overworld_biomes","20");
        def(p,"performance.scan_interval_ticks","80");
        def(p,"performance.hud_sync_interval_ticks","40");
        def(p,"performance.thoridrop_sync_interval_ticks","40");
        def(p,"hud.max_objectives","24");
        def(p,"rewards.enabled","true");
        def(p,"contracts.enabled","true");
        def(p,"thoridrop.era_requirements","true");
        def(p,"equipment.restrictions","true");
        def(p,"crafting.restrictions","true");
        def(p,"arsenal_forge.restrictions","true");
        if(p.size()!=oldCount || !Files.exists(configPath)){
            try(Writer out=Files.newBufferedWriter(configPath)){
                p.store(out,"EvolutionCraft 2.1 - configuracion del mundo. Reinicia el servidor tras editar.");
            }catch(IOException e){
                EvolutionCraftLogger.warn("No se pudo crear configuracion: "+e.getMessage());
            }
        }
        return new EvolutionConfig(p);
    }
    public static EvolutionConfig get(ServerPlayer player){
        Path root=player.level().getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        return CONFIGS.computeIfAbsent(root,p->load(p.resolve("evolutioncraft-config.properties")));
    }
}
