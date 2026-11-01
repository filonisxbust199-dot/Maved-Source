package ess.ll.client.features.combat;

import ess.ll.client.core.ModelHistoryFlattener;
import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxModelMetadata;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import ai.onnxruntime.OrtSession.Result;
import ai.onnxruntime.OrtSession.SessionOptions;
import ai.onnxruntime.OrtSession.SessionOptions.OptLevel;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

public final class OnnxRotationModel {
    private static final Path MODELS_DIRECTORY = Path.of("Hex", "mlRotation", "models");
    private final int expectedFeatureCount;
    private final String expectedSchemaId;
    private final OrtEnvironment environment = OrtEnvironment.getEnvironment();
    private final ArrayDeque<float[]> history = new ArrayDeque<>();
    private final Random random = new Random();
    private final OnnxRotationModel.InferenceDebugInfo debugInfo = new OnnxRotationModel.InferenceDebugInfo();
    private final OnnxRotationModel.InferenceContext defaultContext = new OnnxRotationModel.InferenceContext();
    private float noiseRhoYaw;
    private float noiseRhoPitch;
    private float noiseScale;
    private boolean noiseStateConditioned;
    private float noiseRhoOnTarget;
    private float noiseScaleOnTarget;
    private float noiseEngageYaw;
    private float noiseEngagePitch;
    private float holdThreshold;
    private float holdMaxErrorYaw;
    private float holdMaxErrorPitch;
    private boolean holdInsideBox;
    private float mdnStickiness;
    private float pbStickiness;
    private boolean carryQuantum;
    private boolean labelsInDots;
    private float noiseYaw;
    private float noisePitch;
    private boolean noiseInitialized;
    private int previousComponent;
    private float carryYaw;
    private float carryPitch;
    private OrtSession session;
    private String modelName;
    private int featureCount;
    private int outputSize;
    private int windowSize;
    private String outputFormat;
    private int mdnComponents;
    private float logStdMin;
    private float logStdMax;

    public synchronized float[] predict(float[] var1) {
        this.defaultContext.set(Math.abs(var1[0]) * 180.0F, Math.abs(var1[1]) * 90.0F, true, true, false);
        return this.predict(var1, this.defaultContext);
    }

    public synchronized void resetState() {
        this.history.clear();
        this.noiseInitialized = false;
        this.previousComponent = -1;
        this.carryYaw = 0.0F;
        this.carryPitch = 0.0F;
        this.debugInfo.reset();
    }

    private float[] sampleOutput(float var1, float var2, float var3, float var4, OnnxRotationModel.InferenceContext var5) {
        float var6 = (float) Math.exp(clamp(var3, this.logStdMin, this.logStdMax));
        float var7 = (float) Math.exp(clamp(var4, this.logStdMin, this.logStdMax));
        float[] var8 = this.sampleNoise(var5);
        boolean var9 = this.shouldHold(var1, var5.yawError, var5.yawInsideBox, this.holdMaxErrorYaw);
        boolean var10 = this.shouldHold(var2, var5.pitchError, var5.pitchInsideBox, this.holdMaxErrorPitch);
        float var11 = var9 ? 0.0F : var1 + var6 * var8[0];
        float var12 = var10 ? 0.0F : var2 + var7 * var8[1];
        if (var9) {
            this.carryYaw = 0.0F;
        }
        if (var10) {
            this.carryPitch = 0.0F;
        }
        float var13 = var11;
        float var14 = var12;
        if (this.labelsInDots) {
            float var15 = var11 + (this.carryQuantum ? this.carryYaw : 0.0F);
            float var16 = var12 + (this.carryQuantum ? this.carryPitch : 0.0F);
            var13 = (float) Math.floor(var15 + 0.5F);
            var14 = (float) Math.floor(var16 + 0.5F);
            this.carryYaw = this.carryQuantum ? var15 - var13 : 0.0F;
            this.carryPitch = this.carryQuantum ? var16 - var14 : 0.0F;
        }
        this.debugInfo.meanYaw = var1;
        this.debugInfo.meanPitch = var2;
        this.debugInfo.stdYaw = var6;
        this.debugInfo.stdPitch = var7;
        this.debugInfo.sampledYaw = var11;
        this.debugInfo.sampledPitch = var12;
        this.debugInfo.carriedYaw = this.carryYaw;
        this.debugInfo.carriedPitch = this.carryPitch;
        this.debugInfo.outputYaw = var13;
        this.debugInfo.outputPitch = var14;
        this.debugInfo.yawHeld = var9;
        this.debugInfo.pitchHeld = var10;
        return new float[] { var13, var14 };
    }

    public List<String> listModels() {
        ArrayList<String> items = new ArrayList<>();
        if (!Files.isDirectory(MODELS_DIRECTORY)) {
            return items;
        }
        try (Stream<Path> stream = Files.list(MODELS_DIRECTORY)) {
            stream.filter(var0 -> var0.toString().endsWith(".onnx")).forEach(var1x -> items.add(var1x.getFileName().toString()));
        } catch (IOException iOException) {
        }
        return items;
    }

    public synchronized float[] predict(float[] var1, OnnxRotationModel.InferenceContext var2) {
        long var3 = System.nanoTime();
        this.debugInfo.reset();
        float[] var5 = this.runInference(var1);
        if (var5 == null) {
            this.debugInfo.inferenceNanos = System.nanoTime() - var3;
            return new float[] { 0.0F, 0.0F };
        }
        float[] var6;
        switch (this.outputFormat) {
            case "gaussian":
                var6 = this.decodeGaussian(var5, var2);
                break;
            case "mdn":
                var6 = this.decodeMdn(var5, var2);
                break;
            default:
                var6 = new float[] { var5[0], var5[1] };
                this.debugInfo.rawYaw = this.debugInfo.meanYaw = this.debugInfo.sampledYaw = this.debugInfo.outputYaw = var5[0];
                this.debugInfo.rawPitch = this.debugInfo.meanPitch = this.debugInfo.sampledPitch = this.debugInfo.outputPitch = var5[1];
        }
        this.debugInfo.inferenceNanos = System.nanoTime() - var3;
        return var6;
    }

    public boolean isLabelsInDots() {
        return this.labelsInDots;
    }

    public OnnxRotationModel() {
        this(13, null);
    }

    private float[] decodeMdn(float[] var1, OnnxRotationModel.InferenceContext var2) {
        int var3 = this.mdnComponents;
        float var4 = var2.pbStickEnabled ? this.pbStickiness : this.mdnStickiness;
        float NEGATIVE_INFINITY2 = Float.NEGATIVE_INFINITY;
        float NEGATIVE_INFINITY3 = Float.NEGATIVE_INFINITY;
        for (int var7 = 0; var7 < var3; var7++) {
            float var8 = var1[var7] + (var7 == this.previousComponent ? var4 : 0.0F);
            if (var1[var7] > NEGATIVE_INFINITY2) {
                NEGATIVE_INFINITY2 = var1[var7];
            }
            if (var8 > NEGATIVE_INFINITY3) {
                NEGATIVE_INFINITY3 = var8;
            }
        }
        double var22 = 0.0;
        double var9 = 0.0;
        double[] var11 = new double[var3];
        double[] var12 = new double[var3];
        for (int var13 = 0; var13 < var3; var13++) {
            var11[var13] = Math.exp(var1[var13] + (var13 == this.previousComponent ? var4 : 0.0F) - NEGATIVE_INFINITY3);
            var12[var13] = Math.exp(var1[var13] - NEGATIVE_INFINITY2);
            var22 += var11[var13];
            var9 += var12[var13];
        }
        double var23 = this.random.nextDouble() * var22;
        int var15 = var3 - 1;
        double var16 = 0.0;
        for (int var18 = 0; var18 < var3; var18++) {
            var16 += var11[var18];
            if (var23 <= var16) {
                var15 = var18;
                break;
            }
        }
        float var24 = 0.0F;
        float var19 = 0.0F;
        for (int var20 = 0; var20 < var3; var20++) {
            float var21 = (float) (var12[var20] / var9);
            var24 += var21 * var1[var3 + 2 * var20];
            var19 += var21 * var1[var3 + 2 * var20 + 1];
        }
        this.previousComponent = var15;
        this.debugInfo.component = var15;
        this.debugInfo.componentWeight = (float) (var11[var15] / var22);
        this.debugInfo.rawYaw = var24;
        this.debugInfo.rawPitch = var19;
        return this.sampleOutput(var1[var3 + 2 * var15], var1[var3 + 2 * var15 + 1], var1[3 * var3 + 2 * var15], var1[3 * var3 + 2 * var15 + 1], var2);
    }

    private float[] runInference(float[] var1) {
        if (this.session == null) {
            return null;
        }
        this.history.addFirst((float[]) var1.clone());
        while (this.history.size() > this.windowSize) {
            this.history.removeLast();
        }
        float[] var2 = this.flattenHistory();
        OnnxTensor onnxTensor = null;
        try {
            onnxTensor = OnnxTensor.createTensor(this.environment, FloatBuffer.wrap(var2), new long[] { 1L, this.windowSize, this.featureCount });
            HashMap hashMap = new HashMap();
            hashMap.put("input", onnxTensor);
            try {
                Result result = this.session.run(hashMap);
                float[] var19;
                label185: {
                    try {
                        OnnxValue onnxValue = (OnnxValue) result.get("output").orElse(null);
                        if (onnxValue == null && result.size() > 0) {
                            onnxValue = result.get(0);
                        }
                        if (onnxValue == null) {
                            var19 = null;
                            break label185;
                        }
                        var19 = extractLastStep(onnxValue.getValue());
                    } catch (Throwable throwable) {
                        if (result != null) {
                            try {
                                result.close();
                            } catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }
                        throw throwable;
                    }
                    if (result != null) {
                        result.close();
                    }
                    return (float[]) var19;
                }
                if (result != null) {
                    result.close();
                }
                return (float[]) var19;
            } catch (OrtException ortException) {
                return null;
            }
        } catch (OrtException ortException2) {
            return null;
        } finally {
            if (onnxTensor != null) {
                onnxTensor.close();
            }
        }
    }

    private static float[] extractLastStep(Object var0) {
        if (var0 instanceof float[][][] var1 && var1.length > 0 && var1[0].length > 0) {
            float[][] var2 = var1[0];
            return var2[var2.length - 1];
        } else if (var0 instanceof float[][] var3 && var3.length > 0) {
            return var3[var3.length - 1];
        } else {
            return var0 instanceof float[] var4 ? var4 : null;
        }
    }

    public synchronized void loadModel(byte[] var1, String var2) throws OrtException {
        this.closeModel();
        SessionOptions sessionOptions = new SessionOptions();
        sessionOptions.setOptimizationLevel(OptLevel.ALL_OPT);
        sessionOptions.setIntraOpNumThreads(1);
        sessionOptions.setInterOpNumThreads(1);
        this.session = this.environment.createSession(var1, sessionOptions);
        this.modelName = var2 != null && !var2.isBlank() ? var2 : "server";
        try {
            this.readTensorShapes();
            this.readMetadata();
        } catch (OrtException ortException) {
            this.closeModel();
            throw ortException;
        } catch (RuntimeException runtimeException) {
            this.closeModel();
            throw runtimeException;
        }
        this.resetState();
    }

    private void readTensorShapes() throws OrtException {
        var inputInfo = this.session.getInputInfo();
        long[] shape = ((TensorInfo) ((NodeInfo) inputInfo.get("input")).getInfo()).getShape();
        this.featureCount = (int) shape[shape.length - 1];
        int var3 = shape.length >= 2 ? (int) shape[shape.length - 2] : 0;
        this.windowSize = var3 > 0 ? var3 : 40;
        if (this.featureCount != this.expectedFeatureCount) {
            throw new IllegalStateException("Neuro model feature_count=" + this.featureCount + " does not match the expected " + this.expectedFeatureCount + " -- refusing to load an incompatible model.");
        }
        this.outputSize = 0;
        var outputInfo = this.session.getOutputInfo();
        NodeInfo nodeInfo = outputInfo.containsKey("output") ? (NodeInfo) outputInfo.get("output") : (NodeInfo) outputInfo.values().stream().findFirst().orElse(null);
        if (nodeInfo != null && nodeInfo.getInfo() instanceof TensorInfo var6) {
            long[] shape2 = var6.getShape();
            if (shape2.length > 0) {
                this.outputSize = (int) shape2[shape2.length - 1];
            }
        }
    }

    public int getWindowSize() {
        return this.windowSize;
    }

    public int getFeatureCount() {
        return this.featureCount;
    }

    private static float metadataFloat(Map<String, String> map, String var1, float var2) {
        String var3 = (String) map.get(var1);
        if (var3 == null) {
            return var2;
        }
        try {
            return Float.parseFloat(var3);
        } catch (NumberFormatException numberFormatException) {
            return var2;
        }
    }

    public Path modelPath(String var1) {
        String var2 = var1.endsWith(".onnx") ? var1 : var1 + ".onnx";
        return MODELS_DIRECTORY.resolve(var2);
    }

    public OnnxRotationModel.InferenceDebugInfo getDebugInfo() {
        return this.debugInfo;
    }

    public synchronized void closeModel() {
        if (this.session != null) {
            try {
                this.session.close();
            } catch (OrtException ortException) {
            }
            this.session = null;
        }
        this.modelName = null;
        this.history.clear();
    }

    public int getHistorySize() {
        return this.history.size();
    }

    public synchronized boolean isLoaded() {
        return this.session != null;
    }

    public OnnxRotationModel(int var1, String var2) {
        this.noiseRhoYaw = 0.6F;
        this.noiseRhoPitch = 0.55F;
        this.noiseScale = 0.6F;
        this.noiseRhoOnTarget = 0.4F;
        this.noiseScaleOnTarget = 0.6F;
        this.noiseEngageYaw = 8.0F;
        this.noiseEngagePitch = 5.0F;
        this.holdMaxErrorYaw = Float.MAX_VALUE;
        this.holdMaxErrorPitch = Float.MAX_VALUE;
        this.previousComponent = -1;
        this.windowSize = 40;
        this.outputFormat = "delta_degrees";
        this.logStdMin = (float) Math.log(0.15);
        this.logStdMax = (float) Math.log(40.0);
        this.expectedFeatureCount = var1;
        this.expectedSchemaId = var2;
    }

    public String getModelName() {
        return this.modelName;
    }

    private boolean shouldHold(float var1, float var2, boolean var3, float var4) {
        boolean var5 = Math.abs(var2) <= var4 && (!this.holdInsideBox || var3);
        return var5 && Math.abs(var1) < this.holdThreshold;
    }

    private float[] flattenHistory() {
        return ModelHistoryFlattener.flatten(this.history, this.windowSize, this.featureCount);
    }

    private void readMetadata() throws OrtException {
        OnnxModelMetadata metadata = this.session.getMetadata();
        var customMetadata = metadata.getCustomMetadata();
        int var3 = (int) metadataFloat(customMetadata, "window_size", 0.0F);
        if (var3 > 0 && this.windowSize <= 0) {
            this.windowSize = var3;
        }
        if (this.expectedSchemaId != null) {
            String orDefault = customMetadata.getOrDefault("schema_id", "");
            if (!this.expectedSchemaId.equals(orDefault)) {
                throw new IllegalStateException("Neuro model schema_id=\"" + orDefault + "\" does not match the expected \"" + this.expectedSchemaId + "\" -- refusing to load a model built for another feature schema.");
            }
        }
        this.outputFormat = customMetadata.getOrDefault("output_format", "delta_degrees");
        this.labelsInDots = "dots".equals(customMetadata.getOrDefault("label_units", ""));
        this.mdnComponents = (int) metadataFloat(customMetadata, "mdn_components", 0.0F);
        this.logStdMin = metadataFloat(customMetadata, "log_std_min", (float) Math.log(0.15));
        this.logStdMax = metadataFloat(customMetadata, "log_std_max", (float) Math.log(40.0));
        this.noiseRhoYaw = metadataFloat(customMetadata, "noise_rho_yaw", 0.6F);
        this.noiseRhoPitch = metadataFloat(customMetadata, "noise_rho_pitch", 0.55F);
        this.noiseScale = metadataFloat(customMetadata, "noise_scale", 0.6F);
        this.noiseStateConditioned = metadataFloat(customMetadata, "noise_state_conditioned", 0.0F) > 0.5F;
        this.noiseRhoOnTarget = metadataFloat(customMetadata, "noise_rho_ontarget", 0.4F);
        this.noiseScaleOnTarget = metadataFloat(customMetadata, "noise_scale_ontarget", 0.6F);
        this.noiseEngageYaw = Math.max(0.001F, metadataFloat(customMetadata, "noise_engage_yaw_deg", 8.0F));
        this.noiseEngagePitch = Math.max(0.001F, metadataFloat(customMetadata, "noise_engage_pitch_deg", 5.0F));
        this.holdThreshold = Math.max(0.0F, metadataFloat(customMetadata, "hold_threshold", 0.0F));
        this.holdMaxErrorYaw = metadataFloat(customMetadata, "hold_max_err_deg", Float.MAX_VALUE);
        this.holdMaxErrorPitch = metadataFloat(customMetadata, "hold_max_err_pitch_deg", this.holdMaxErrorYaw);
        this.holdInsideBox = metadataFloat(customMetadata, "hold_inside_box", 0.0F) > 0.5F;
        this.mdnStickiness = metadataFloat(customMetadata, "mdn_stick", 0.0F);
        this.pbStickiness = metadataFloat(customMetadata, "pb_stick", 0.0F);
        this.carryQuantum = metadataFloat(customMetadata, "carry_quantum", 0.0F) > 0.5F;
        if ("mdn".equals(this.outputFormat) && this.mdnComponents <= 0) {
            throw new IllegalStateException("Neuro model declares output_format=mdn with mdn_components=" + this.mdnComponents + " -- refusing to load a model with an inconsistent head.");
        }
        int var7 = switch (this.outputFormat) {
            case "gaussian" ->
                4;
            case "mdn" ->
                5 * this.mdnComponents;
            default ->
                2;
        };
        if (this.outputSize > 0 && this.outputSize < var7) {
            throw new IllegalStateException("Neuro model output width " + this.outputSize + " is smaller than the " + var7 + " required by output_format=" + this.outputFormat + " -- refusing to load a model with an inconsistent head.");
        }
    }

    private static float clamp(float var0, float var1, float var2) {
        return var0 < var1 ? var1 : (var0 > var2 ? var2 : var0);
    }

    private float[] decodeGaussian(float[] var1, OnnxRotationModel.InferenceContext var2) {
        this.debugInfo.component = 0;
        this.debugInfo.componentWeight = 1.0F;
        this.debugInfo.rawYaw = var1[0];
        this.debugInfo.rawPitch = var1[1];
        return this.sampleOutput(var1[0], var1[1], var1[2], var1[3], var2);
    }

    private float[] sampleNoise(OnnxRotationModel.InferenceContext var1) {
        float var2 = (float) this.random.nextGaussian();
        float var3 = (float) this.random.nextGaussian();
        float var4 = this.noiseRhoYaw;
        float var5 = this.noiseRhoPitch;
        float var6 = this.noiseScale;
        float var7 = this.noiseScale;
        if (this.noiseStateConditioned) {
            float var8 = Math.min(1.0F, Math.abs(var1.yawError) / this.noiseEngageYaw);
            float var9 = Math.min(1.0F, Math.abs(var1.pitchError) / this.noiseEngagePitch);
            var4 = this.noiseRhoOnTarget + (this.noiseRhoYaw - this.noiseRhoOnTarget) * var8;
            var5 = this.noiseRhoOnTarget + (this.noiseRhoPitch - this.noiseRhoOnTarget) * var9;
            var6 = this.noiseScaleOnTarget + (this.noiseScale - this.noiseScaleOnTarget) * var8;
            var7 = this.noiseScaleOnTarget + (this.noiseScale - this.noiseScaleOnTarget) * var9;
        }
        if (this.noiseInitialized) {
            this.noiseYaw = var4 * this.noiseYaw + (float) Math.sqrt(1.0 - var4 * var4) * var2;
            this.noisePitch = var5 * this.noisePitch + (float) Math.sqrt(1.0 - var5 * var5) * var3;
        } else {
            this.noiseYaw = var2;
            this.noisePitch = var3;
            this.noiseInitialized = true;
        }
        return new float[] { this.noiseYaw * var6, this.noisePitch * var7 };
    }

    public synchronized void loadModel(Path path) throws OrtException {
        this.closeModel();
        SessionOptions sessionOptions = new SessionOptions();
        sessionOptions.setOptimizationLevel(OptLevel.ALL_OPT);
        sessionOptions.setIntraOpNumThreads(1);
        sessionOptions.setInterOpNumThreads(1);
        this.session = this.environment.createSession(path.toString(), sessionOptions);
        this.modelName = path.getFileName().toString();
        try {
            this.readTensorShapes();
            this.readMetadata();
        } catch (OrtException ortException) {
            this.closeModel();
            throw ortException;
        } catch (RuntimeException runtimeException) {
            this.closeModel();
            throw runtimeException;
        }
        this.resetState();
    }

    public static final class InferenceContext {
        private float yawError;
        private float pitchError;
        private boolean yawInsideBox = true;
        private boolean pitchInsideBox = true;
        private boolean pbStickEnabled;

        public boolean isPbStickEnabled() {
            return this.pbStickEnabled;
        }

        public boolean isPitchInsideBox() {
            return this.pitchInsideBox;
        }

        public void setYawInsideBox(boolean var1) {
            this.yawInsideBox = var1;
        }

        public boolean isYawInsideBox() {
            return this.yawInsideBox;
        }

        public float getPitchError() {
            return this.pitchError;
        }

        public void setYawError(float var1) {
            this.yawError = var1;
        }

        public void setPitchError(float var1) {
            this.pitchError = var1;
        }

        public void setPitchInsideBox(boolean var1) {
            this.pitchInsideBox = var1;
        }

        public OnnxRotationModel.InferenceContext set(float var1, float var2, boolean var3, boolean var4, boolean var5) {
            this.yawError = var1;
            this.pitchError = var2;
            this.yawInsideBox = var3;
            this.pitchInsideBox = var4;
            this.pbStickEnabled = var5;
            return this;
        }

        public void setPbStickEnabled(boolean var1) {
            this.pbStickEnabled = var1;
        }

        public float getYawError() {
            return this.yawError;
        }
    }

    public static final class InferenceDebugInfo {
        private int component = -1;
        private float componentWeight;
        private float rawYaw;
        private float rawPitch;
        private float meanYaw;
        private float meanPitch;
        private float stdYaw;
        private float stdPitch;
        private float sampledYaw;
        private float sampledPitch;
        private float carriedYaw;
        private float carriedPitch;
        private float outputYaw;
        private float outputPitch;
        private boolean yawHeld;
        private boolean pitchHeld;
        private long inferenceNanos;

        public boolean isPitchHeld() {
            return this.pitchHeld;
        }

        public float getCarriedYaw() {
            return this.carriedYaw;
        }

        public float getStdYaw() {
            return this.stdYaw;
        }

        public float getComponentWeight() {
            return this.componentWeight;
        }

        public boolean isYawHeld() {
            return this.yawHeld;
        }

        private void reset() {
            this.component = -1;
            this.componentWeight = 0.0F;
            this.rawYaw = this.rawPitch = this.meanYaw = this.meanPitch = this.stdYaw = this.stdPitch = 0.0F;
            this.sampledYaw = this.sampledPitch = this.carriedYaw = this.carriedPitch = this.outputYaw = this.outputPitch = 0.0F;
            this.yawHeld = this.pitchHeld = false;
        }

        public float getRawPitch() {
            return this.rawPitch;
        }

        public float getMeanYaw() {
            return this.meanYaw;
        }

        public float getOutputPitch() {
            return this.outputPitch;
        }

        public float getRawYaw() {
            return this.rawYaw;
        }

        public float getOutputYaw() {
            return this.outputYaw;
        }

        public int getComponent() {
            return this.component;
        }

        public float getSampledYaw() {
            return this.sampledYaw;
        }

        public long getInferenceNanos() {
            return this.inferenceNanos;
        }

        public float getSampledPitch() {
            return this.sampledPitch;
        }

        public float getCarriedPitch() {
            return this.carriedPitch;
        }

        public float getStdPitch() {
            return this.stdPitch;
        }

        public float getMeanPitch() {
            return this.meanPitch;
        }
    }
}
