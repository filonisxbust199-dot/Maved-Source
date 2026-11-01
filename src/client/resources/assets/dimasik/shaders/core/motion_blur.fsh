#version 330
#extension GL_ARB_separate_shader_objects : require
#define DIMASIK_DEPTH_TO_CLIP(depth) (depth)

layout(std140) uniform MotionBlur {
    mat4 InverseProjection;
    mat4 InverseView;
    mat4 PreviousProjection;
    mat4 PreviousView;
    vec4 CameraDeltaStrength;
    vec4 TemporalParams;
    vec4 Params0;
    vec4 Params1;
    vec4 VelocityResolution;
};

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;

layout(location = 0) out vec4 fragColor;

const int MAX_SAMPLES = 12;

vec4 sampleScene(vec2 uv) {
    return textureLod(SceneSampler, uv, 0.0);
}

float sampleDepth(vec2 uv) {
    ivec2 size = textureSize(DepthSampler, 0);
    ivec2 pixel = clamp(ivec2(uv * vec2(size)), ivec2(0), size - ivec2(1));
    float depth = texelFetch(DepthSampler, pixel, 0).r;
    return isnan(depth) || isinf(depth) ? 0.0 : clamp(depth, 0.0, 1.0);
}


float saturate(float v) {
    return clamp(v, 0.0, 1.0);
}

vec2 safeUv(vec2 uv) {
    return clamp(uv, vec2(0.0005), vec2(0.9995));
}

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

vec3 reconstructRelativePos(vec2 uv, float depth) {
    vec4 clipSpace = vec4(uv * 2.0 - 1.0, DIMASIK_DEPTH_TO_CLIP(depth), 1.0);
    vec4 viewSpace = InverseProjection * clipSpace;
    float invW = abs(viewSpace.w) > 0.000001 ? 1.0 / viewSpace.w : 1.0;
    viewSpace *= invW;
    return (InverseView * viewSpace).xyz;
}

vec2 previousUv(vec3 relativePos, vec2 fallbackUv, out float valid) {
    vec3 relative = relativePos + CameraDeltaStrength.xyz;
    vec4 view = PreviousView * vec4(relative, 1.0);
    vec4 clip = PreviousProjection * view;
    if (any(isnan(clip)) || any(isinf(clip)) || clip.w <= 0.000001) {
        valid = 0.0;
        return fallbackUv;
    }
    vec2 ndc = clip.xy / clip.w;
    vec2 uv = ndc * 0.5 + 0.5;
    vec2 gate = step(vec2(-0.18), uv) * step(uv, vec2(1.18));
    valid = gate.x * gate.y * step(0.0, clip.w);
    return uv;
}

float structuralMask(vec2 uv, float centerDepth, vec3 centerColor) {
    vec2 resolution = max(VelocityResolution.zw, vec2(1.0));
    vec2 px = 1.0 / resolution;
    vec3 cL = sampleScene(safeUv(uv - vec2(px.x, 0.0))).rgb;
    vec3 cR = sampleScene(safeUv(uv + vec2(px.x, 0.0))).rgb;
    vec3 cD = sampleScene(safeUv(uv - vec2(0.0, px.y))).rgb;
    vec3 cU = sampleScene(safeUv(uv + vec2(0.0, px.y))).rgb;
    float lumaGrad = abs(luminance(cR) - luminance(cL)) + abs(luminance(cU) - luminance(cD));
    float localContrast = abs(luminance(centerColor) - (luminance(cL) + luminance(cR) + luminance(cD) + luminance(cU)) * 0.25);
    float dL = sampleDepth(safeUv(uv - vec2(px.x, 0.0)));
    float dR = sampleDepth(safeUv(uv + vec2(px.x, 0.0)));
    float dD = sampleDepth(safeUv(uv - vec2(0.0, px.y)));
    float dU = sampleDepth(safeUv(uv + vec2(0.0, px.y)));
    float depthGrad = abs(dR - dL) + abs(dU - dD);
    float validDepth = step(0.000001, centerDepth);
    float edge = lumaGrad * 1.28 + localContrast * 1.85 + depthGrad * mix(18.0, 46.0, Params0.w) * validDepth;
    float threshold = mix(0.105, 0.018, saturate(Params0.y / 1.6));
    return smoothstep(threshold, threshold * 3.8 + 0.0001, edge);
}

float depthWeight(vec2 uv, float centerDepth) {
    float neighborDepth = sampleDepth(safeUv(uv));
    float valid = step(0.000001, centerDepth);
    float diff = abs(neighborDepth - centerDepth);
    float reject = smoothstep(0.004, 0.052, diff) * valid;
    return mix(1.0, 1.0 - reject * 0.86, Params0.w);
}

void main() {
    vec2 resolution = max(VelocityResolution.zw, vec2(1.0));
    vec2 uv = gl_FragCoord.xy / resolution;
    vec4 original = sampleScene(uv);
    float centerDepth = sampleDepth(uv);
    float reconstructionDepth = max(centerDepth, 0.000001);
    vec3 relativePos = reconstructRelativePos(uv, reconstructionDepth);
    float prevValid = 0.0;
    vec2 prevUv = previousUv(relativePos, uv, prevValid);
    vec2 centered = uv * 2.0 - 1.0;
    vec2 periphery = centered * dot(VelocityResolution.xy, centered) * 0.34;
    vec2 fallbackVelocity = (VelocityResolution.xy + periphery) * TemporalParams.x;
    vec2 velocity = fallbackVelocity;
    if (prevValid > 0.5) {
        velocity = (uv - prevUv) * TemporalParams.x;
    }
    vec2 velocityPx = velocity * resolution;
    float speedPx = length(velocityPx);
    if (speedPx > Params0.x) {
        velocity *= Params0.x / max(speedPx, 0.0001);
        speedPx = Params0.x;
    }
    float activationPx = max(0.03, Params1.y);
    float speedMask = smoothstep(activationPx, activationPx * 4.0 + 0.0001, speedPx);
    float edge = structuralMask(uv, centerDepth, original.rgb);
    float motionFill = smoothstep(0.55, 10.0, speedPx);
    float centerGuard = 1.0 - smoothstep(0.0, 0.46, length(centered));
    float modeMask = max(edge * 0.92, motionFill * mix(0.42, 0.74, 1.0 - centerGuard));
    float globalGate = smoothstep(0.0005, 0.045, Params1.z);
    float mask = saturate(speedMask * modeMask * mix(0.90, 1.0, globalGate) * CameraDeltaStrength.w);
    if (mask <= 0.001) {
        fragColor = original;
        return;
    }
    int samples = clamp(int(Params1.w + 0.5), 3, MAX_SAMPLES);
    float chroma = Params0.z;
    vec3 acc = vec3(0.0);
    vec3 maxHold = original.rgb;
    float weightSum = 0.0;
    for (int i = 0; i < MAX_SAMPLES; i++) {
        if (i >= samples) {
            break;
        }
        float t = float(i) / float(max(samples - 1, 1));
        float weight = exp(-t * Params1.x);
        vec2 baseOffset = velocity * t;
        vec2 uvR = safeUv(uv - baseOffset * mix(1.0, 0.76, chroma));
        vec2 uvG = safeUv(uv - baseOffset);
        vec2 uvB = safeUv(uv - baseOffset * mix(1.0, 1.32, chroma));
        float depthMix = depthWeight(uvG, centerDepth);
        vec3 sampleColor = vec3(
                sampleScene(uvR).r,
                sampleScene(uvG).g,
                sampleScene(uvB).b
        );
        float w = weight * depthMix;
        acc += sampleColor * w;
        weightSum += w;
        maxHold = max(maxHold, sampleColor * (0.92 + 0.08 * (1.0 - t)));
    }
    vec3 trail = acc / max(weightSum, 0.0001);
    float brightProtect = smoothstep(0.72, 1.0, luminance(original.rgb));
    vec3 persistent = mix(trail, maxHold, 0.18 + brightProtect * 0.10);
    vec3 color = mix(original.rgb, persistent, saturate(mask));
    float subpixelGlow = smoothstep(0.8, 10.0, speedPx) * max(edge, motionFill * 0.32) * chroma * 0.055;
    color += max(persistent - original.rgb, vec3(0.0)) * subpixelGlow;
    fragColor = vec4(clamp(color, 0.0, 1.0), original.a);
}
