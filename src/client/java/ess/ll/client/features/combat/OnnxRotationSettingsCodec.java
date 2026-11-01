package ess.ll.client.features.combat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public final class OnnxRotationSettingsCodec {
    @Deprecated
    public static final OnnxRotationSettingsCodec INSTANCE = new OnnxRotationSettingsCodec();
    private volatile boolean aimAssist = true;
    private volatile float assistYaw;
    private volatile float assistPitch;
    private volatile float assistWindow;

    public float getAssistYaw() {
        return this.assistYaw;
    }

    public boolean isAimAssist() {
        return this.aimAssist;
    }

    public void parseSettings(byte[] jsonBytes) {
        try {
            JsonObject settings = JsonParser.parseString(new String(jsonBytes, StandardCharsets.UTF_8)).getAsJsonObject();
            if (settings.has("aimAssist")) {
                this.aimAssist = settings.get("aimAssist").getAsBoolean();
            }
            if (settings.has("assistYaw")) {
                this.assistYaw = settings.get("assistYaw").getAsFloat();
            }
            if (settings.has("assistPitch")) {
                this.assistPitch = settings.get("assistPitch").getAsFloat();
            }
            if (settings.has("assistWindow")) {
                this.assistWindow = settings.get("assistWindow").getAsFloat();
            }
        } catch (Exception exception) {
        }
    }

    public float getAssistWindow() {
        return this.assistWindow;
    }

    public float getAssistPitch() {
        return this.assistPitch;
    }

    @Deprecated
    public void loadSettings(byte[] jsonBytes) {
        if (jsonBytes != null && jsonBytes.length > 0) {
            this.parseSettings(jsonBytes);
        }
    }

    public OnnxRotationSettingsCodec() {
        this.assistYaw = 30.0F;
        this.assistPitch = 30.0F;
        this.assistWindow = 350.0F;
    }

    public static void loadModelAsync(String modelName, byte[] modelBytes, Supplier<OnnxRotationModel> modelSupplier) {
        Thread thread = new Thread(() -> {
            OnnxRotationModel model = (OnnxRotationModel) modelSupplier.get();
            if (model != null) {
                try {
                    model.loadModel(modelBytes, modelName != null && !modelName.isBlank() ? modelName : "server");
                } catch (Exception exception) {
                }
            }
        }, "Neuro-Model-Load");
        thread.setDaemon(true);
        thread.start();
    }
}
