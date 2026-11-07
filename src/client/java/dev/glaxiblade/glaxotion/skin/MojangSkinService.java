package dev.glaxiblade.glaxotion.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Resolves public skin texture metadata from Mojang; no credentials are used. */
public final class MojangSkinService {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final String TEXTURE_PREFIX = "https://textures.minecraft.net/texture/";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ConcurrentHashMap<String, CompletableFuture<Optional<SkinData>>> cache = new ConcurrentHashMap<>();

    public CompletableFuture<Optional<SkinData>> resolve(String input) {
        if (input == null || !USERNAME.matcher(input.trim()).matches()) return CompletableFuture.completedFuture(Optional.empty());
        String name = input.trim();
        return cache.computeIfAbsent(name.toLowerCase(), key -> {
            CompletableFuture<Optional<SkinData>> request = lookup(name);
            request.whenComplete((value, error) -> {
                // Do not permanently cache transient network/server failures.
                if (error != null) cache.remove(key, request);
            });
            return request;
        });
    }
    private CompletableFuture<Optional<SkinData>> lookup(String name) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name)).timeout(Duration.ofSeconds(10)).GET().build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenCompose(response -> {
            if (response.statusCode() == 204 || response.statusCode() == 404) return CompletableFuture.completedFuture(Optional.empty());
            if (response.statusCode() != 200) return CompletableFuture.failedFuture(new IllegalStateException("Mojang username lookup failed: HTTP " + response.statusCode()));
            JsonObject profile = JsonParser.parseString(response.body()).getAsJsonObject();
            String rawId = profile.get("id").getAsString();
            UUID uuid = UUID.fromString(rawId.replaceFirst("^(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})$", "$1-$2-$3-$4-$5"));
            HttpRequest session = HttpRequest.newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + rawId + "?unsigned=false")).timeout(Duration.ofSeconds(10)).GET().build();
            return http.sendAsync(session, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(result -> {
                if (result.statusCode() != 200) throw new IllegalStateException("Mojang texture lookup failed: HTTP " + result.statusCode());
                JsonObject json = JsonParser.parseString(result.body()).getAsJsonObject();
                for (var element : json.getAsJsonArray("properties")) {
                    JsonObject property = element.getAsJsonObject();
                    if (!"textures".equals(property.get("name").getAsString())) continue;
                    String decoded = new String(Base64.getDecoder().decode(property.get("value").getAsString()), StandardCharsets.UTF_8);
                    JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject().getAsJsonObject("textures");
                    if (textures == null || !textures.has("SKIN")) return Optional.<SkinData>empty();
                    JsonObject skin = textures.getAsJsonObject("SKIN");
                    String url = skin.get("url").getAsString();
                    if (!url.startsWith(TEXTURE_PREFIX)) throw new IllegalStateException("Unexpected Mojang texture URL");
                    boolean slim = skin.has("metadata") && skin.getAsJsonObject("metadata").has("model") && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());
                    return Optional.of(new SkinData(uuid, profile.get("name").getAsString(), url, slim));
                }
                return Optional.empty();
            });
        });
    }
    public record SkinData(UUID uuid, String canonicalName, String textureUrl, boolean slimModel) {}
}
