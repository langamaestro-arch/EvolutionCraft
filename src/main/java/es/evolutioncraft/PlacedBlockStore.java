package es.evolutioncraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Libro de operaciones append-only: añadir/quitar posiciones cuesta O(1).
 * Evita reescribir un archivo JSON cada vez que se coloca un bloque.
 */
public final class PlacedBlockStore {
    private static final Map<Path,Set<String>> CACHE=new HashMap<>();
    private PlacedBlockStore(){}
    private static Path path(ServerPlayer p){
        return p.level().getServer().getWorldPath(LevelResource.ROOT)
            .resolve("evolutioncraft_placed_blocks.log");
    }
    private static Set<String> load(Path path){
        return CACHE.computeIfAbsent(path,k->{
            Set<String> result=new HashSet<>();
            try{
                if(Files.exists(k))for(String line:Files.readAllLines(k,StandardCharsets.UTF_8)){
                    if(line.length()<2)continue;
                    String key=line.substring(1);
                    if(line.charAt(0)=='+')result.add(key);
                    if(line.charAt(0)=='-')result.remove(key);
                }
            }catch(Exception ex){EvolutionCraftLogger.warn("Leyendo posiciones: "+ex.getMessage());}
            return result;
        });
    }
    private static String key(ServerPlayer p,BlockPos pos){
        return p.level().dimension().identifier()+":"+pos.getX()+":"+pos.getY()+":"+pos.getZ();
    }
    private static void append(Path path,String op){
        try{
            Files.createDirectories(path.getParent());
            Files.writeString(path,op+"\n",StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        }catch(Exception ex){EvolutionCraftLogger.warn("Guardando posiciones: "+ex.getMessage());}
    }
    public static void placed(ServerPlayer p,BlockPos pos){
        Path path=path(p);
        String key=key(p,pos);
        if(load(path).add(key))append(path,"+"+key);
    }
    public static boolean consumePlaced(ServerPlayer p,BlockPos pos){
        Path path=path(p);
        String key=key(p,pos);
        if(load(path).remove(key)){append(path,"-"+key);return true;}
        return false;
    }
}
