package es.evolutioncraft;

import com.google.gson.Gson;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Guarda por mundo todas las posiciones colocadas por jugadores, compartidas en multijugador. */
public final class PlacedBlockStore {
    private static final Gson GSON=new Gson();
    private static final Map<Path,Set<String>> CACHE=new HashMap<>();
    private PlacedBlockStore(){}
    private static Path path(ServerPlayer p){
        return p.level().getServer().getWorldPath(LevelResource.ROOT).resolve("evolutioncraft_placed_blocks.json");
    }
    private static Set<String> load(Path path){
        return CACHE.computeIfAbsent(path,k->{
            try{
                if(Files.exists(k)){
                    WorldFile saved=GSON.fromJson(Files.readString(k),WorldFile.class);
                    if(saved!=null && saved.positions!=null)return saved.positions;
                }
            }catch(Exception ex){EvolutionCraftLogger.warn("Bloques colocados: "+ex.getMessage());}
            return new HashSet<>();
        });
    }
    private static String key(ServerPlayer p,BlockPos pos){
        return p.level().dimension().identifier()+":"+pos.getX()+":"+pos.getY()+":"+pos.getZ();
    }
    private static void save(Path path) {
        try{
            Files.createDirectories(path.getParent());
            WorldFile file=new WorldFile();
            file.positions=load(path);
            Files.writeString(path,GSON.toJson(file));
        }catch(Exception ex){EvolutionCraftLogger.warn("Guardando bloques: "+ex.getMessage());}
    }
    public static void placed(ServerPlayer p,BlockPos pos){
        Path path=path(p);
        if(load(path).add(key(p,pos)))save(path);
    }
    /** Elimina la marca tras romperlo y devuelve true si lo colocó un jugador. */
    public static boolean consumePlaced(ServerPlayer p,BlockPos pos){
        Path path=path(p);
        if(load(path).remove(key(p,pos))){save(path);return true;}
        return false;
    }
    private static class WorldFile{Set<String> positions=new HashSet<>();}
}
