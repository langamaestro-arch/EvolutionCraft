package es.evolutioncraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        return world(path(player)).computeIfAbsent(player.getUUID(), id -> new Progress());
    }
    public static void save(ServerPlayer player) {
        Path path = path(player);
        try {
            Files.createDirectories(path.getParent());
            WorldFile file = new WorldFile();
            file.players = world(path);
            Files.writeString(path, GSON.toJson(file));
        } catch (IOException ex) {
            EvolutionCraftLogger.warn("No se ha podido guardar Evolution Craft: " + ex.getMessage());
        }
    }
    private static class WorldFile { Map<UUID, Progress> players = new HashMap<>(); }
}
