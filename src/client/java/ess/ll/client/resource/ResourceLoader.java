package ess.ll.client.resource;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ResourceLoader {
    private static final Gson GSON = new Gson();

    public static Identifier coreResource(String var0) {
        return Identifier.fromNamespaceAndPath("Hex".toLowerCase(), "core/" + var0);
    }

    public static <T> T readJson(Identifier identifier, Class<T> var1) {
        try (InputStream input = resourceManager().open(identifier);
            InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return (T) (Object) GSON.fromJson(reader, var1);
        } catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    public static String readText(Identifier identifier, String var1) {
        try (InputStream input = resourceManager().open(identifier);
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder stringBuilder = new StringBuilder();
            String var5;
            for (boolean var6 = true; (var5 = reader.readLine()) != null; var6 = false) {
                if (!var6) {
                    stringBuilder.append(var1);
                }
                stringBuilder.append(var5);
            }
            return stringBuilder.toString();
        } catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    public static JsonObject readJsonObject(Identifier identifier) {
        try (InputStream input = resourceManager().open(identifier);
            InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    private static ResourceManager resourceManager() {
        Minecraft instance = Minecraft.getInstance();
        if (instance != null && instance.getResourceManager() != null) {
            return instance.getResourceManager();
        } else {
            throw new IllegalStateException("Minecraft ResourceManager is not initialized yet");
        }
    }

    public static String readText(Identifier identifier) {
        return readText(identifier, "\n");
    }
}
