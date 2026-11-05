package dev.glaxiblade.glaxotion.identity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Stores local player identity presets in config/glaxotion.json. */
public final class IdentityStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("glaxotion.json");
    private final Map<UUID, List<IdentityProfile>> profiles = new LinkedHashMap<>();
    private final Map<UUID, UUID> active = new LinkedHashMap<>();

    public synchronized List<IdentityProfile> profiles(UUID player) { return List.copyOf(profiles.getOrDefault(player, List.of())); }
    public synchronized IdentityProfile activeProfile(UUID player) {
        UUID id = active.get(player);
        return id == null ? null : profiles.getOrDefault(player, List.of()).stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
    }
    public synchronized void add(UUID player, IdentityProfile profile) {
        List<IdentityProfile> list = profiles.computeIfAbsent(player, ignored -> new ArrayList<>());
        if (list.stream().anyMatch(p -> p.id().equals(profile.id()))) throw new IllegalArgumentException("Profile already exists");
        list.add(profile); active.putIfAbsent(player, profile.id()); save();
    }
    public synchronized void update(UUID player, IdentityProfile profile) {
        List<IdentityProfile> list = profiles.computeIfAbsent(player, ignored -> new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(profile.id())) {
                list.set(i, profile);
                active.put(player, profile.id());
                save();
                return;
            }
        }
        list.add(profile);
        active.put(player, profile.id());
        save();
    }
    public synchronized void select(UUID player, UUID profile) {
        if (profiles.getOrDefault(player, List.of()).stream().noneMatch(p -> p.id().equals(profile))) throw new IllegalArgumentException("Unknown profile");
        active.put(player, profile); save();
    }
    public synchronized void remove(UUID player, UUID profile) {
        List<IdentityProfile> list = profiles.get(player); if (list == null) return;
        list.removeIf(p -> p.id().equals(profile));
        if (list.isEmpty()) { profiles.remove(player); active.remove(player); }
        else if (profile.equals(active.get(player))) active.put(player, list.get(0).id());
        save();
    }
    public synchronized void load() {
        if (!Files.exists(FILE)) return;
        try (Reader reader = Files.newBufferedReader(FILE)) {
            Data data = GSON.fromJson(reader, Data.class); if (data == null) return;
            profiles.clear(); active.clear();
            if (data.profiles != null) data.profiles.forEach((k,v) -> { try { profiles.put(UUID.fromString(k), new ArrayList<>(v)); } catch (RuntimeException ignored) {} });
            if (data.active != null) data.active.forEach((k,v) -> { try { active.put(UUID.fromString(k), UUID.fromString(v)); } catch (RuntimeException ignored) {} });
        } catch (Exception e) { throw new IllegalStateException("Unable to load Glaxotion profiles", e); }
    }
    public synchronized void save() {
        Data data = new Data(); data.profiles = new LinkedHashMap<>(); data.active = new LinkedHashMap<>();
        profiles.forEach((k,v) -> data.profiles.put(k.toString(), v)); active.forEach((k,v) -> data.active.put(k.toString(), v.toString()));
        try { Files.createDirectories(FILE.getParent()); try (Writer writer = Files.newBufferedWriter(FILE)) { GSON.toJson(data, writer); } }
        catch (Exception e) { throw new IllegalStateException("Unable to save Glaxotion profiles", e); }
    }
    private static final class Data { Map<String,List<IdentityProfile>> profiles; Map<String,String> active; }
}
