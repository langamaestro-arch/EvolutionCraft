package es.evolutioncraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Guardado por mundo y UUID. El servidor es la fuente de verdad. */
public final class ProgressStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Path, Map<UUID, Progress>> WORLDS = new HashMap<>();
    private ProgressStore() {}
    private static Path path(ServerPlayer player) {
        return player.level().getServer().getWorldPath(LevelResource.ROOT).resolve("evolutioncraft_players.json");
    }
    private static Map<UUID, Progress> world(Path path) {
        return WORLDS.computeIfAbsent(path, p -> {
            try {
                if (Files.exists(p)) {
                    WorldFile file = GSON.fromJson(Files.readString(p), WorldFile.class);
                    if (file != null && file.players != null) return file.players;
                }
            } catch (Exception ignored) { /* Archivo ilegible: no bloquear el inicio del mundo. */ }
            return new HashMap<>();
        });
    }
    public static Progress get(ServerPlayer player) {
        Progress progress=world(path(player)).computeIfAbsent(player.getUUID(), id -> new Progress());
        ThoridropTribute.sync(player,progress);
        if(!progress.exigentMigrated){
            // Capture the ERA ACTUALLY reached on the old rules, after Thoridrop
            // is synchronized. Do not reset or delete any player achievements.
            progress.exigentGrandfatherEra=progress.era();
            progress.exigentMigrated=true;
            save(player);
        }
        // Optional compatibility: when Nicktale is absent, no impossible boss gates.
        // Snapshot old unlocked era before enabling the new requirements.
        progress.nicktaleBossGateActive=
            net.neoforged.fml.ModList.get().isLoaded("nicktaleepicdungeons");
        if(progress.nicktaleBossGateActive && !progress.nicktaleBossMigrated){
            progress.nicktaleBossGateActive=false;
            progress.nicktaleBossGrandfatherEra=progress.era();
            progress.nicktaleBossMigrated=true;
            progress.nicktaleBossGateActive=true;
            if(progress.nicktaleEraBossKills==null)
                progress.nicktaleEraBossKills=new java.util.HashSet<>();
            save(player);
        }
        // Migración no destructiva de las restricciones de dimensiones.
        // Conservar permisos de viaje que el jugador ya había desbloqueado.
        progress.nicktaleDimensionGateActive=
            net.neoforged.fml.ModList.get().isLoaded("nicktaleepicdungeons");
        if(progress.nicktaleDimensionGateActive && !progress.nicktaleDimensionMigrated){
            progress.nicktaleDimensionGateActive=false;
            EvolutionConfig cfg=EvolutionConfig.get(player);
            progress.nicktaleNetherGrandfathered=progress.canNether(cfg);
            progress.nicktaleEndGrandfathered=progress.canEnd(cfg);
            progress.nicktaleDimensionMigrated=true;
            progress.nicktaleDimensionGateActive=true;
            if(progress.nicktaleDimensionBossKills==null)
                progress.nicktaleDimensionBossKills=new java.util.HashSet<>();
            save(player);
        }
        EvolutionConfig eliteConfig=EvolutionConfig.get(player);
        boolean eliteActive=eliteConfig.eliteMobsEnabled && net.neoforged.fml.ModList.get().isLoaded("elitemobs");
        progress.eliteMobGateActive=false;
        if(eliteActive && !progress.eliteMobMigrated){
            progress.eliteNetherGrandfathered=progress.canNether(eliteConfig);
            progress.eliteEndGrandfathered=progress.canEnd(eliteConfig);
            progress.eliteMobMigrated=true;
            save(player);
        }
        progress.eliteNetherRequirements=eliteConfig.eliteNether;
        progress.eliteEndRequirements=eliteConfig.eliteEnd;
        progress.eliteMobGateActive=eliteActive;
        return progress;
    }
    public static void save(ServerPlayer player) {
        Path path = path(player);
        try {
            Files.createDirectories(path.getParent());
            WorldFile file = new WorldFile();
            file.players = world(path);
            // Nunca truncar directamente el guardado principal.
            // Un cierre entre escrituras no debe dejar JSON incompleto.
            Path temporary=path.resolveSibling(path.getFileName()+".tmp");
            Files.writeString(temporary,GSON.toJson(file));
            try {
                Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch(AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            EvolutionCraftLogger.warn("No se ha podido guardar Evolution Craft: " + ex.getMessage());
        }
    }
    private static class WorldFile { Map<UUID, Progress> players = new HashMap<>(); }
}
