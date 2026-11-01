package ess.ll.client.features.combat;

import ess.ll.client.core.ModelHistoryFlattener;
import ess.ll.client.render.EncryptedTextureUtils;
import ess.ll.client.resource.EncryptedAssetArchive;
import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxJavaType;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import ai.onnxruntime.OrtSession.Result;
import ai.onnxruntime.OrtSession.SessionOptions;
import ai.onnxruntime.OrtSession.SessionOptions.OptLevel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.util.Mth;

public final class RotationModelRepository {
    private final ArrayDeque<float[]> featureHistory = new ArrayDeque<>();
    private final ArrayDeque<RotationModelSnapshot> snapshotHistory = new ArrayDeque<>();
    private OrtEnvironment environment;
    private OrtSession session;
    private String featureInputName = "";
    private String predictionOutputName = "";
    private String stateInputName = "";
    private String stateOutputName = "";
    private String modelName = "";
    private int windowSize = 16;
    private int featureCount = 70;
    private long[] stateShape;
    private float[] hiddenState;
    private long lastSessionId;
    private boolean featureSizeMismatch;
    private static final long INITIAL_SESSION_ID = Long.MIN_VALUE;
    private static final long RESET_SESSION_ID = Long.MIN_VALUE;
    private static final long UNSET_SESSION_ID = Long.MIN_VALUE;

    public synchronized RotationModelRepository.RotationPrediction predict(RotationModelSnapshot rotationModelSnapshot) {
        if (this.session != null && this.environment != null) {
            if (this.lastSessionId != UNSET_SESSION_ID && this.lastSessionId != rotationModelSnapshot.sessionId()) {
                this.clearHistory();
            }
            this.lastSessionId = rotationModelSnapshot.sessionId();
            float[] var2 = rotationModelSnapshot.toFeatureVector();
            if (var2.length < 70) {
                return null;
            }
            RotationModelSnapshot rotationModelSnapshot2 = this.snapshotHistory.peekFirst();
            if (rotationModelSnapshot2 != null) {
                int var4 = rotationModelSnapshot.sampleTick() - rotationModelSnapshot2.sampleTick();
                if (var4 != 1) {
                    this.featureHistory.clear();
                    this.snapshotHistory.clear();
                    this.clearHiddenState();
                    rotationModelSnapshot2 = null;
                }
            }
            if (rotationModelSnapshot2 != null) {
                this.addTemporalFeatures(var2, rotationModelSnapshot, rotationModelSnapshot2);
            }
            this.featureHistory.addFirst(this.resizeFeatureVector(var2));
            this.snapshotHistory.addFirst(rotationModelSnapshot);
            while (this.featureHistory.size() > this.windowSize) {
                this.featureHistory.removeLast();
            }
            while (this.snapshotHistory.size() > this.windowSize) {
                this.snapshotHistory.removeLast();
            }
            float[] var27 = this.flattenHistory();
            long[] var5 = new long[] { 1L, this.windowSize, this.featureCount };
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            RotationModelRepository.RotationPrediction var31;
            try {
                linkedHashMap.put(this.featureInputName, OnnxTensor.createTensor(this.environment, FloatBuffer.wrap(var27), var5));
                if (this.hiddenState != null && this.stateShape != null && !this.stateInputName.isEmpty()) {
                    linkedHashMap.put(this.stateInputName, OnnxTensor.createTensor(this.environment, FloatBuffer.wrap(this.hiddenState), this.stateShape));
                }
                Result result = this.session.run(linkedHashMap);
                label329: {
                    label330: {
                        Object var28;
                        label331: {
                            Object var29;
                            label332: {
                                Object var11;
                                try {
                                    if (result.size() < 1) {
                                        var28 = null;
                                        break label331;
                                    }
                                    OnnxValue onnxValue = this.predictionOutput(result);
                                    if (onnxValue == null) {
                                        var29 = null;
                                        break label332;
                                    }
                                    var29 = onnxValue.getValue();
                                    float[] var10 = firstTensorVector(var29);
                                    if (var10 != null && var10.length >= 2) {
                                        float var30 = var10[0];
                                        float var12 = var10[1];
                                        float NaN2 = Float.NaN;
                                        float NaN3 = Float.NaN;
                                        float NaN4 = Float.NaN;
                                        float NaN5 = Float.NaN;
                                        if (var10.length >= 6) {
                                            NaN2 = var10[2];
                                            NaN3 = var10[3];
                                            NaN4 = var10[4];
                                            NaN5 = var10[5];
                                        } else if (var10.length >= 4) {
                                            NaN2 = var10[2];
                                            NaN3 = var10[3];
                                        }
                                        this.updateHiddenState(result);
                                        if (!Float.isFinite(var30) || !Float.isFinite(var12)) {
                                            var31 = null;
                                            break label329;
                                        }
                                        var31 = new RotationModelRepository.RotationPrediction(var30, var12, Float.isFinite(NaN2) ? NaN2 : turnSpeedForDelta(var30, true), Float.isFinite(NaN3) ? NaN3 : turnSpeedForDelta(var12, false), Float.isFinite(NaN4) ? NaN4 : turnSpeedForDelta(var30, true), Float.isFinite(NaN5) ? NaN5 : turnSpeedForDelta(var12, false));
                                        break label330;
                                    }
                                    var11 = null;
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
                                return (RotationModelRepository.RotationPrediction) var11;
                            }
                            if (result != null) {
                                result.close();
                            }
                            return (RotationModelRepository.RotationPrediction) var29;
                        }
                        if (result != null) {
                            result.close();
                        }
                        return (RotationModelRepository.RotationPrediction) var28;
                    }
                    if (result != null) {
                        result.close();
                    }
                    return var31;
                }
                if (result != null) {
                    result.close();
                }
            } catch (Exception exception) {
                return null;
            } finally {
                OnnxValue.close(linkedHashMap.values());
            }
            return var31;
        } else {
            return null;
        }
    }

    private static int copyTensorValues(Object var0, float[] var1, int var2) {
        if (var2 >= var1.length || var0 == null) {
            return var2;
        } else if (var0 instanceof float[] var10) {
            int var12 = Math.min(var10.length, var1.length - var2);
            System.arraycopy(var10, 0, var1, var2, var12);
            return var2 + var12;
        } else if (var0 instanceof float[][] var9) {
            int var11 = var2;
            for (float[] var16 : var9) {
                var11 = copyTensorValues(var16, var1, var11);
                if (var11 >= var1.length) {
                    break;
                }
            }
            return var11;
        } else if (!(var0 instanceof float[][][] var3)) {
            return var2;
        } else {
            int var4 = var2;
            for (float[][] var8 : var3) {
                var4 = copyTensorValues(var8, var1, var4);
                if (var4 >= var1.length) {
                    break;
                }
            }
            return var4;
        }
    }

    private static int dimensionOrDefault(long[] var0, int var1, int var2, int var3) {
        if (var0.length > var1 && var0[var1] > 0L && var0[var1] <= 2147483647L) {
            return (int) var0[var1];
        } else {
            return var2 > 0 ? var2 : var3;
        }
    }

    private float[] flattenHistory() {
        return ModelHistoryFlattener.flatten(this.featureHistory, this.windowSize, this.featureCount);
    }

    private static String selectPredictionOutput(Map<String, NodeInfo> map, String var1) {
        if (hasNamedNode(map, var1)) {
            return var1;
        }
        if (map.containsKey("mu")) {
            return "mu";
        }
        if (map.containsKey("output")) {
            return "output";
        }
        for (var var3 : map.entrySet()) {
            TensorInfo tensorInfo = tensorInfo((NodeInfo) var3.getValue());
            if (tensorInfo != null && tensorInfo.type == OnnxJavaType.FLOAT) {
                long[] shape = tensorInfo.getShape();
                if (shape.length >= 1 && (shape[shape.length - 1] == 2L || shape[shape.length - 1] < 0L)) {
                    return (String) var3.getKey();
                }
            }
        }
        return map.keySet().stream().findFirst().orElse("");
    }

    private OnnxValue predictionOutput(Result result) {
        if (!this.predictionOutputName.isEmpty()) {
            var var2 = result.get(this.predictionOutputName);
            if (var2.isPresent()) {
                return (OnnxValue) var2.get();
            }
        }
        return result.size() > 0 ? result.get(0) : null;
    }

    public String modelName() {
        return this.modelName;
    }

    private static String selectStateOutput(Map<String, NodeInfo> map, String var1) {
        if (hasNamedNode(map, var1)) {
            return var1;
        }
        if (map.containsKey("h_n")) {
            return "h_n";
        }
        for (var var3 : map.entrySet()) {
            TensorInfo tensorInfo = tensorInfo((NodeInfo) var3.getValue());
            if (tensorInfo != null && tensorInfo.type == OnnxJavaType.FLOAT && tensorInfo.getShape().length == 3) {
                return (String) var3.getKey();
            }
        }
        return "";
    }

    private static String selectFeatureInput(Map<String, NodeInfo> map, String var1, String var2) throws IOException {
        if (hasNamedNode(map, var1)) {
            return var1;
        }
        if (map.containsKey("input")) {
            return "input";
        }
        for (var var4 : map.entrySet()) {
            TensorInfo tensorInfo = tensorInfo((NodeInfo) var4.getValue());
            if (tensorInfo != null && tensorInfo.type == OnnxJavaType.FLOAT) {
                long[] shape = tensorInfo.getShape();
                if (shape.length >= 3 && (shape[0] != 1L || shape[2] <= 70L)) {
                    return (String) var4.getKey();
                }
            }
        }
        return (String) map.keySet().stream().findFirst().orElseThrow(() -> new IOException("FuntimeModel: нет входов в модели: " + var2));
    }

    private static float[] flattenTensor(Object var0, int var1) {
        float[] var2 = new float[var1];
        int var3 = copyTensorValues(var0, var2, 0);
        return var3 > 0 ? var2 : null;
    }

    public synchronized List<String> listModels() {
        Path path = this.modelsDirectory();
        if (!Files.isDirectory(path)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(path)) {
            return stream.filter(var0 -> Files.isRegularFile(var0)).map(Path::getFileName).map(Path::toString).filter(var0 -> var0.toLowerCase(Locale.ROOT).endsWith(".onnx")).sorted(Comparator.naturalOrder()).toList();
        } catch (IOException iOException) {
            return List.of();
        }
    }

    public RotationModelRepository() {
        this.lastSessionId = INITIAL_SESSION_ID;
    }

    private RotationModelRepository.ModelMetadata readMetadata(Path path) {
        String var2 = path.getFileName().toString();
        int var3 = var2.lastIndexOf(46);
        String var4 = var3 > 0 ? var2.substring(0, var3) : var2;
        Path path2 = path.getParent().resolve(var4 + ".json");
        if (!Files.isRegularFile(path2)) {
            return RotationModelRepository.ModelMetadata.defaults();
        }
        try (BufferedReader reader = Files.newBufferedReader(path2, StandardCharsets.UTF_8)) {
            JsonObject asJsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            int var8 = asJsonObject.has("window_size") ? asJsonObject.get("window_size").getAsInt() : 16;
            int var9 = asJsonObject.has("snapshot_features") ? asJsonObject.get("snapshot_features").getAsInt() : 70;
            String var10 = stringOrDefault(asJsonObject, "input_name", "input");
            String var11 = stringOrDefault(asJsonObject, "output_mu_name", stringOrDefault(asJsonObject, "output_name", "output"));
            String var12 = stringOrDefault(asJsonObject, "state_input_name", "");
            String var13 = stringOrDefault(asJsonObject, "output_state_name", "");
            return new RotationModelRepository.ModelMetadata(var8, var9, var10, var11, var12, var13);
        } catch (Exception exception) {
            return RotationModelRepository.ModelMetadata.defaults();
        }
    }

    private static boolean hasNamedNode(Map<String, NodeInfo> map, String var1) {
        return var1 != null && !var1.isBlank() && map.containsKey(var1);
    }

    public synchronized void loadBundledModel() {
        if (this.session == null) {
            try {
                InputStream resourceAsStream = RotationModelRepository.class.getResourceAsStream(EncryptedAssetArchive.resolveResourcePath("/assets/hex/models/slothOLD.onnx"));
                if (resourceAsStream == null) {
                    return;
                }
                byte[] var2;
                try (InputStream input = EncryptedTextureUtils.decodeStream(resourceAsStream)) {
                    var2 = input.readAllBytes();
                }
                if (this.environment == null) {
                    this.environment = OrtEnvironment.getEnvironment();
                }
                SessionOptions sessionOptions = new SessionOptions();
                sessionOptions.setOptimizationLevel(OptLevel.ALL_OPT);
                this.session = this.environment.createSession(var2, sessionOptions);
                this.configureTensorBindings(this.session.getInputInfo(), this.session.getOutputInfo(), RotationModelRepository.ModelMetadata.defaults(), "slothOLD.onnx");
                this.modelName = "slothOLD.onnx";
                this.featureSizeMismatch = false;
                this.clearHistory();
            } catch (Exception exception) {
            }
        }
    }

    public Path modelsDirectory() {
        return Path.of("Hex", "mlRotation", "models");
    }

    private void clearHiddenState() {
        if (this.hiddenState != null) {
            Arrays.fill(this.hiddenState, 0.0F);
        }
    }

    private RotationModelSnapshot secondPreviousSnapshot() {
        var iterator2 = this.snapshotHistory.iterator();
        if (!iterator2.hasNext()) {
            return null;
        }
        iterator2.next();
        return iterator2.hasNext() ? (RotationModelSnapshot) iterator2.next() : null;
    }

    public synchronized void loadModel(Path path) throws OrtException, IOException {
        Path path2 = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(path2)) {
            throw new IOException("Файл модели не найден: " + path2);
        }
        Files.createDirectories(this.modelsDirectory());
        this.unload();
        String var3 = path2.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!var3.endsWith(".onnx")) {
            throw new IOException("Неподдерживаемый формат Funtime модели: " + path2.getFileName());
        }
        if (this.environment == null) {
            this.environment = OrtEnvironment.getEnvironment();
        }
        SessionOptions sessionOptions = new SessionOptions();
        sessionOptions.setOptimizationLevel(OptLevel.ALL_OPT);
        this.session = this.environment.createSession(path2.toString(), sessionOptions);
        RotationModelRepository.ModelMetadata var5 = this.readMetadata(path2);
        this.configureTensorBindings(this.session.getInputInfo(), this.session.getOutputInfo(), var5, path2.getFileName().toString());
        this.modelName = path2.getFileName().toString();
        this.featureSizeMismatch = false;
        this.clearHistory();
    }

    public synchronized void resetSession() {
        this.clearHistory();
    }

    private static float[] firstTensorVector(Object var0) {
        if (var0 instanceof float[] var3) {
            return var3;
        } else if (var0 instanceof float[][] var1 && var1.length > 0) {
            return var1[0];
        } else {
            return var0 instanceof float[][][] var2 && var2.length > 0 && var2[0].length > 0 ? var2[0][0] : null;
        }
    }

    private static TensorInfo tensorInfo(NodeInfo nodeInfo) {
        return nodeInfo != null && nodeInfo.getInfo() instanceof TensorInfo var1 ? var1 : null;
    }

    public boolean isLoaded() {
        return this.session != null;
    }

    private static String selectStateInput(Map<String, NodeInfo> map, String var1, String var2) {
        if (hasNamedNode(map, var1) && !var1.equals(var2)) {
            return var1;
        }
        if (map.containsKey("h0") && !"h0".equals(var2)) {
            return "h0";
        }
        for (var var4 : map.entrySet()) {
            if (!((String) var4.getKey()).equals(var2)) {
                TensorInfo tensorInfo = tensorInfo((NodeInfo) var4.getValue());
                if (tensorInfo != null && tensorInfo.type == OnnxJavaType.FLOAT && tensorInfo.getShape().length == 3) {
                    return (String) var4.getKey();
                }
            }
        }
        return "";
    }

    private void configureTensorBindings(Map<String, NodeInfo> map, Map<String, NodeInfo> map2, RotationModelRepository.ModelMetadata var3, String var4) throws IOException {
        this.featureInputName = selectFeatureInput(map, var3.inputName(), var4);
        TensorInfo tensorInfo = requireFloatTensor((NodeInfo) map.get(this.featureInputName), "вход", this.featureInputName);
        long[] shape = tensorInfo.getShape();
        this.windowSize = dimensionOrDefault(shape, 1, var3.windowSize(), 16);
        this.featureCount = dimensionOrDefault(shape, 2, var3.snapshotFeatures(), 70);
        this.predictionOutputName = selectPredictionOutput(map2, var3.outputName());
        this.stateInputName = selectStateInput(map, var3.stateInputName(), this.featureInputName);
        this.stateOutputName = "";
        this.stateShape = null;
        this.hiddenState = null;
        if (!this.stateInputName.isEmpty()) {
            TensorInfo tensorInfo2 = requireFloatTensor((NodeInfo) map.get(this.stateInputName), "state-вход", this.stateInputName);
            this.stateShape = resolveStateShape(tensorInfo2.getShape());
            this.hiddenState = new float[tensorElementCount(this.stateShape)];
            this.stateOutputName = selectStateOutput(map2, var3.stateOutputName());
        }
    }

    private static long[] resolveStateShape(long[] var0) throws IOException {
        if (var0.length == 0) {
            throw new IOException("FuntimeModel: state-вход имеет пустую форму");
        }
        long[] var1 = (long[]) var0.clone();
        for (int var2 = 0; var2 < var1.length; var2++) {
            if (var2 == 1) {
                var1[var2] = 1L;
            } else if (var1[var2] <= 0L) {
                var1[var2] = 1L;
            }
        }
        return var1;
    }

    private static float turnSpeedForDelta(float var0, boolean var1) {
        float var2 = Math.abs(var0);
        float var3 = var1 ? 35.0F : 28.0F;
        float var4 = var1 ? 5.5F : 5.0F;
        return Mth.clamp(var3 + var2 * var4, 0.01F, 9999.0F);
    }

    private static TensorInfo requireFloatTensor(NodeInfo nodeInfo, String var1, String var2) throws IOException {
        TensorInfo tensorInfo = tensorInfo(nodeInfo);
        if (tensorInfo != null && tensorInfo.type == OnnxJavaType.FLOAT) {
            return tensorInfo;
        } else {
            throw new IOException("FuntimeModel ONNX " + var1 + " должен быть float tensor: " + var2);
        }
    }

    private void addTemporalFeatures(float[] var1, RotationModelSnapshot rotationModelSnapshot, RotationModelSnapshot rotationModelSnapshot2) {
        var1[6] = RotationModelFeatures.normalizeYaw(Mth.wrapDegrees(rotationModelSnapshot2.deltaYaw()));
        var1[7] = RotationModelFeatures.normalizePitch(rotationModelSnapshot2.deltaPitch());
        var1[8] = RotationModelFeatures.normalizeYaw(Mth.wrapDegrees(rotationModelSnapshot.deltaYaw() - rotationModelSnapshot2.deltaYaw()));
        var1[9] = RotationModelFeatures.normalizePitch(rotationModelSnapshot.deltaPitch() - rotationModelSnapshot2.deltaPitch());
        var1[10] = RotationModelFeatures.normalizeYaw(Mth.wrapDegrees(rotationModelSnapshot.currentYaw() - rotationModelSnapshot2.currentYaw()));
        var1[11] = RotationModelFeatures.normalizePitch(rotationModelSnapshot.currentPitch() - rotationModelSnapshot2.currentPitch());
        RotationModelSnapshot rotationModelSnapshot3 = this.secondPreviousSnapshot();
        if (rotationModelSnapshot3 != null && rotationModelSnapshot2.sampleTick() - rotationModelSnapshot3.sampleTick() == 1) {
            float var5 = Mth.wrapDegrees(rotationModelSnapshot.currentYaw() - rotationModelSnapshot2.currentYaw());
            float var6 = Mth.wrapDegrees(rotationModelSnapshot2.currentYaw() - rotationModelSnapshot3.currentYaw());
            float var7 = rotationModelSnapshot.currentPitch() - rotationModelSnapshot2.currentPitch();
            float var8 = rotationModelSnapshot2.currentPitch() - rotationModelSnapshot3.currentPitch();
            var1[42] = RotationModelFeatures.normalizeYaw(Mth.wrapDegrees(var5 - var6));
            var1[43] = RotationModelFeatures.normalizePitch(var7 - var8);
            var1[44] = var5 * var6 < 0.0F && Math.abs(var5) > 0.08F && Math.abs(var6) > 0.08F ? 1.0F : 0.0F;
            var1[45] = var7 * var8 < 0.0F && Math.abs(var7) > 0.05F && Math.abs(var8) > 0.05F ? 1.0F : 0.0F;
        }
        var1[62] = RotationModelFeatures.normalizeYaw(Mth.wrapDegrees(rotationModelSnapshot.targetYaw() - rotationModelSnapshot2.targetYaw()));
        var1[63] = RotationModelFeatures.normalizePitch(rotationModelSnapshot.targetPitch() - rotationModelSnapshot2.targetPitch());
        float var9 = Mth.wrapDegrees(rotationModelSnapshot.currentYaw() - rotationModelSnapshot.targetYaw());
        float var10 = Mth.wrapDegrees(rotationModelSnapshot2.currentYaw() - rotationModelSnapshot2.targetYaw());
        var1[38] = RotationModelFeatures.normalizeYaw(Mth.wrapDegrees(var9 - var10));
        float var11 = rotationModelSnapshot.currentPitch() - rotationModelSnapshot.targetPitch();
        float var12 = rotationModelSnapshot2.currentPitch() - rotationModelSnapshot2.targetPitch();
        var1[39] = RotationModelFeatures.normalizePitch(var11 - var12);
    }

    public synchronized void clearHistory() {
        this.featureHistory.clear();
        this.snapshotHistory.clear();
        this.lastSessionId = RESET_SESSION_ID;
        this.clearHiddenState();
    }

    public synchronized void unload() {
        this.clearHistory();
        this.modelName = "";
        this.windowSize = 16;
        this.featureCount = 70;
        this.featureInputName = "";
        this.predictionOutputName = "";
        this.stateInputName = "";
        this.stateOutputName = "";
        this.stateShape = null;
        this.hiddenState = null;
        this.featureSizeMismatch = false;
        if (this.session != null) {
            try {
                this.session.close();
            } catch (OrtException ortException) {
            } finally {
                this.session = null;
            }
        }
    }

    private float[] resizeFeatureVector(float[] var1) {
        if (var1.length == this.featureCount) {
            return var1;
        }
        float[] var2 = new float[this.featureCount];
        System.arraycopy(var1, 0, var2, 0, Math.min(var1.length, this.featureCount));
        if (!this.featureSizeMismatch) {
            this.featureSizeMismatch = true;
        }
        return var2;
    }

    private static String stringOrDefault(JsonObject jsonObject, String var1, String var2) {
        if (jsonObject.has(var1) && jsonObject.get(var1).isJsonPrimitive()) {
            String asString = jsonObject.get(var1).getAsString();
            return asString != null && !asString.isBlank() ? asString : var2;
        } else {
            return var2;
        }
    }

    private static int tensorElementCount(long[] var0) throws IOException {
        long var1 = 1L;
        for (long var6 : var0) {
            var1 = Math.multiplyExact(var1, var6);
            if (var1 > 2147483647L) {
                throw new IOException("FuntimeModel: state-вход слишком большой: " + Arrays.toString(var0));
            }
        }
        return (int) var1;
    }

    private void updateHiddenState(Result result) throws OrtException {
        if (this.hiddenState != null && !this.stateOutputName.isEmpty()) {
            var var2 = result.get(this.stateOutputName);
            if (!var2.isEmpty()) {
                float[] var3 = flattenTensor(((OnnxValue) var2.get()).getValue(), this.hiddenState.length);
                if (var3 != null) {
                    System.arraycopy(var3, 0, this.hiddenState, 0, this.hiddenState.length);
                }
            }
        }
    }

    private record ModelMetadata(int windowSize, int snapshotFeatures, String inputName, String outputName, String stateInputName, String stateOutputName) {
        static RotationModelRepository.ModelMetadata defaults() {
            return new RotationModelRepository.ModelMetadata(16, 70, "", "", "", "");
        }
    }

    public record RotationPrediction(float yawStep, float pitchStep, float turnSpeedYaw, float turnSpeedPitch, float reverbSpeedYaw, float reverbSpeedPitch) {
    }
}
