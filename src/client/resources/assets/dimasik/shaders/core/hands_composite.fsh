#version 330
#extension GL_ARB_separate_shader_objects : require

#include <dimasik:hands_item_color.glsl>

uniform sampler2D HandSampler;
uniform sampler2D HandDepthSampler;
uniform sampler2D HandMaskSampler;
uniform sampler2D HandSupportSampler;
uniform sampler2D BlurredSceneSampler;
uniform sampler2D BloomSampler;

layout(std140) uniform HandsData {
    vec4 Params0;
    vec4 Params1;
    vec4 Params2;
    vec4 Params3;
    vec4 Params4;
    vec4 Params5;
    vec4 Params6;
    vec4 ShaderColor1;
    vec4 ShaderColor2;
    vec4 FillColor;
    vec4 MirrorColor;
    vec4 GlowColor1;
    vec4 GlowColor2;
    vec2 Size;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const float TAU = 6.28318530718;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + 1.0), f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    mat2 rotation = mat2(1.6, 1.2, -1.2, 1.6);
    int octaves = Params6.z < 4.0 ? 3 : (Params6.z < 7.0 ? 4 : 5);
    for (int i = 0; i < 5; i++) {
        if (i >= octaves) break;
        value += noise(p) * amplitude;
        p = rotation * p;
        amplitude *= 0.5;
    }
    return value * (octaves == 3 ? 1.107142857 : (octaves == 4 ? 1.033333333 : 1.0));
}

vec3 hue(float h) {
    vec3 p = abs(fract(vec3(h) + vec3(0.0, 0.6666667, 0.3333333)) * 6.0 - 3.0);
    return clamp(p - 1.0, 0.0, 1.0);
}

vec3 saturateColor(vec3 color, float amount) {
    float maximum = max(color.r, max(color.g, color.b));
    if (maximum > 0.001) color /= maximum;
    float luminance = dot(color, vec3(0.299, 0.587, 0.114));
    return clamp(mix(vec3(luminance), color, amount), 0.0, 1.0);
}

float hash31(vec3 n) {
    return fract(sin(dot(n, vec3(12.9898, 4.1414, 5.28934))) * 43758.5453);
}

vec3 stars(vec3 p) {
    if (Params2.x < 0.5) return vec3(0.0);
    vec3 q = fract(p * 400.0) - 0.5;
    vec3 id = floor(p * 400.0);
    float random = hash31(id);
    if (random > 0.003) return vec3(0.0);
    float point = 1.0 - smoothstep(0.0, 0.6, length(q));
    point *= step(random, 0.003);
    vec3 tint = mix(vec3(1.0, 0.49, 0.1), vec3(0.75, 0.9, 1.0), hash31(id + 100.0));
    vec3 color = point * (tint * 0.1 + 0.9);
    return color * color * 0.8;
}

vec3 nebula(vec3 ray, vec2 domain) {
    float time = Params0.z * Params0.w * 0.15;
    vec2 q = domain * Params1.x * 1.5;
    float first = fbm(q + vec2(time, -time * 0.5));
    float second = fbm(q * 2.0 + vec2(-time * 0.7, time) + first * 2.0);
    float density = pow(clamp(first * second * 1.8, 0.0, 1.0), 1.6);
    vec3 color = mix(ShaderColor1.rgb, ShaderColor2.rgb, second);
    color *= density * (1.0 + Params1.y);
    color += ShaderColor2.rgb * (first * first * first) * 0.4 * Params1.y;
    float horizon = smoothstep(-0.2, 0.4, ray.y);
    color *= horizon * 0.8 + 0.2;
    if (ray.y > 0.02) color += stars(ray) * horizon * 1.2;
    return color;
}

vec3 starLayer(vec2 uv, float scale, float brightness) {
    vec2 grid = uv * scale;
    vec2 id = floor(grid);
    vec2 local = fract(grid) - 0.5;
    float random = hash21(id);
    if (random < 0.985) return vec3(0.0);
    float twinkle = 0.5 + 0.5 * sin(Params0.z * Params0.w * 3.0 + random * TAU);
    float point = (1.0 - smoothstep(0.0, 0.08, length(local))) * twinkle * brightness;
    vec3 tint = mix(vec3(0.7, 0.8, 1.0), vec3(1.0, 0.7, 0.5), hash21(id + 7.0));
    return point * tint;
}

vec3 cosmos(vec3 ray, vec2 domain) {
    vec2 base = domain * Params1.x * 2.0;
    float drift = Params0.z * Params0.w * 0.05;
    vec3 color = vec3(0.0);
    if (Params2.x > 0.5) {
        color += starLayer(base + vec2(drift, 0.0), 8.0, 1.0);
        color += starLayer(base * 1.7 + vec2(-drift * 1.3, drift), 14.0, 0.6);
        color += starLayer(base * 3.1 + vec2(drift * 0.5, -drift), 26.0, 0.35);
    }
    float cloud = fbm(base * 0.6 + drift);
    color += mix(ShaderColor1.rgb, ShaderColor2.rgb, cloud) * (cloud * cloud) * Params1.y * 0.6;
    color += mix(ShaderColor1.rgb * 0.05, ShaderColor2.rgb * 0.08, clamp(ray.y * 0.5 + 0.5, 0.0, 1.0));
    return color;
}

vec3 waves(vec3 ray, vec2 domain) {
    vec2 p = domain * Params1.x * TAU - 250.0;
    vec2 iterated = p;
    float accumulated = 1.0;
    float intensity = 0.005;
    int iterations = clamp(int(Params6.z + 0.5), 1, 8);
    for (int n = 0; n < 8; n++) {
        if (n >= iterations) break;
        float iterationTime = Params0.z * Params0.w * (1.0 - 3.5 / float(n + 1));
        iterated = p + vec2(cos(iterationTime - iterated.x) + sin(iterationTime + iterated.y),
                            sin(iterationTime - iterated.y) + cos(iterationTime + iterated.x));
        float waveX = sin(iterated.x + iterationTime);
        float waveY = cos(iterated.y + iterationTime);
        vec2 denominator = p * vec2(waveY, waveX) * intensity;
        accumulated += abs(waveX * waveY) * inversesqrt(max(dot(denominator, denominator), 1e-20));
    }
    accumulated /= float(iterations);
    accumulated = 1.17 - pow(accumulated, 1.4);
    float pattern = min(abs(accumulated), 1.0);
    pattern *= pattern;
    pattern *= pattern;
    pattern *= pattern;
    vec3 base = mix(ShaderColor1.rgb, ShaderColor2.rgb, pattern);
    vec3 color = base * (1.0 + pattern * Params1.y * 1.5);
    float horizon = smoothstep(-0.25, 0.35, ray.y);
    color *= horizon * 0.7 + 0.3;
    if (ray.y > 0.05) color += stars(ray) * (1.0 - pattern) * horizon;
    color += ShaderColor1.rgb * 0.06 * (1.0 - abs(ray.y)) * horizon;
    return color;
}

float handRidged(vec2 p) {
    float value = 0.0;
    float amplitude = 0.55;
    for (int i = 0; i < 4; i++) {
        float ridge = 1.0 - abs(noise(p) * 2.0 - 1.0);
        value += ridge * amplitude;
        p = p * 2.18 + vec2(3.1, 9.2);
        amplitude *= 0.52;
    }
    return value;
}

vec3 handShader(vec2 uv, vec3 source) {
    float time = Params0.z * Params0.w;
    vec2 flow = uv * 2.5 * max(Params1.x, 0.2);
    vec2 drift = vec2(time * 0.20, -time * 0.15);
    vec2 warp = vec2(
            fbm(flow * 0.90 + drift * 0.75 + vec2(0.0, 4.1)),
            fbm(flow * 0.78 - drift * 0.48 + vec2(3.7, 1.8))
    );
    vec2 q = flow + (warp - 0.5) * 1.8;
    float mist = fbm(q * 0.72 - drift * 0.24 + vec2(4.2, 8.1));
    float veins = pow(clamp(handRidged(q * 1.85 + vec2(mist * 2.5, mist * 1.6) - drift * 0.55), 0.0, 1.0), 2.4);
    float stripeA = pow(clamp(1.0 - abs(sin((q.x * 1.08 + q.y * 0.42) * 1.7 + time * 0.85 + mist * 4.3)), 0.0, 1.0), 4.8);
    float stripeB = pow(clamp(1.0 - abs(sin((q.x * -0.58 + q.y * 1.12) * 1.45 - time * 0.65 - mist * 2.9)), 0.0, 1.0), 5.4);
    float energy = clamp(mist * 0.22 + veins * 0.88 + stripeA * 0.55 + stripeB * 0.32, 0.0, 1.0);
    float core = smoothstep(0.18, 0.98, energy);
    float accent = pow(clamp(max(veins, stripeA), 0.0, 1.0), 1.25);
    vec3 color = mix(ShaderColor1.rgb, mix(ShaderColor1.rgb, vec3(1.0), 0.4), clamp(core * 0.75 + stripeB * 0.25, 0.0, 1.0));
    float fill = (0.26 + core * 0.82 + accent * 0.28) * (1.0 + Params1.y * 0.25);
    return mix(source, color, clamp(Params1.z * fill * 0.92, 0.0, 1.0));
}

vec3 shaderPattern(vec2 uv, vec3 source) {
#if HANDS_STYLE < 0
    return source;
#else
    if (Params1.z <= 0.001) return source;
#if HANDS_STYLE == 4
    return handShader(uv, source);
#else
    vec3 color;
#if HANDS_STYLE == 3
    color = mix(ShaderColor2.rgb, ShaderColor1.rgb, smoothstep(0.0, 1.0, uv.y));
#else
    vec2 ndc = uv * 2.0 - 1.0;
    vec2 screen = vec2(ndc.x * Params6.x * Params6.y, ndc.y * Params6.y);
    vec3 ray = normalize(vec3(screen, 1.0));
    vec2 domain = ray.xy / (abs(ray.z) + 0.5);
#if HANDS_STYLE == 1
    color = nebula(ray, domain);
#elif HANDS_STYLE == 2
    color = cosmos(ray, domain);
#else
    color = waves(ray, domain);
#endif
#endif
    if (Params2.y > 0.5) {
        float shading = dot(source, vec3(0.299, 0.587, 0.114));
        color *= mix(1.0, shading, Params1.w);
    }
    return mix(source, color, Params1.z);
#endif
#endif
}

vec2 mirrorSafeUv(vec2 uv, vec2 texel) {
    return clamp(uv, texel * 2.0, vec2(1.0) - texel * 2.0);
}

float mirrorSurfaceHeight(vec2 uv) {
    return texture(HandDepthSampler, uv).r;
}

vec3 mirrorRefraction(vec2 uv) {
    vec2 texel = 1.0 / max(Size, vec2(1.0));
    vec2 refractedUv = uv;
    vec2 surfaceNormal = vec2(0.0);
    float distortion = clamp(Params4.y, 0.0, 1.0);
    if (distortion > 0.001) {
        vec2 normalStep = texel * 2.0;
        float left = mirrorSurfaceHeight(mirrorSafeUv(uv - vec2(normalStep.x, 0.0), texel));
        float right = mirrorSurfaceHeight(mirrorSafeUv(uv + vec2(normalStep.x, 0.0), texel));
        float down = mirrorSurfaceHeight(mirrorSafeUv(uv - vec2(0.0, normalStep.y), texel));
        float up = mirrorSurfaceHeight(mirrorSafeUv(uv + vec2(0.0, normalStep.y), texel));
        surfaceNormal = vec2(left - right, down - up);
        vec2 detail = vec2(
                noise(uv * vec2(37.0, 31.0) + vec2(2.7, 8.1)),
                noise(uv * vec2(29.0, 43.0) + vec2(7.4, 1.9))
        ) - 0.5;
        refractedUv += (surfaceNormal * 48.0 + detail * 18.0) * distortion * texel;
    }
    vec3 blurred = texture(BlurredSceneSampler, mirrorSafeUv(refractedUv, texel)).rgb;
    float edge = smoothstep(0.015, 0.35, length(surfaceNormal));
    vec3 accent = saturateColor(MirrorColor.rgb, 1.3);
    vec3 tinted = blurred * mix(vec3(1.0), accent, 0.3) + accent * 0.08;
    return mix(tinted, accent, edge * distortion * 0.18);
}

float rawHandCoverageAt(vec2 uv, vec2 texel) {
    vec2 safeUv = clamp(uv, texel * 0.5, vec2(1.0) - texel * 0.5);
    return texture(HandMaskSampler, safeUv).r;
}

vec4 repairedHandSample(vec2 uv, out float coverage) {
    vec2 texel = 1.0 / max(Size, vec2(1.0));
    vec2 safeUv = clamp(uv, texel * 0.5, vec2(1.0) - texel * 0.5);
    vec4 center = texture(HandSampler, safeUv);
    coverage = center.a;
    if (center.a >= 0.999) return center;
    if (center.a <= 0.001 && texture(HandSupportSampler, safeUv).r <= 0.0) return center;
    float mask = rawHandCoverageAt(safeUv, texel);
    coverage = max(coverage, mask);
    if (center.a > 0.001) return center;
    if (mask >= 0.5) return vec4(center.rgb, 1.0);

    float bridge = max(max(
            min(rawHandCoverageAt(safeUv - vec2(texel.x, 0.0), texel),
                    rawHandCoverageAt(safeUv + vec2(texel.x, 0.0), texel)),
            min(rawHandCoverageAt(safeUv - vec2(0.0, texel.y), texel),
                    rawHandCoverageAt(safeUv + vec2(0.0, texel.y), texel))),
            max(
                    min(rawHandCoverageAt(safeUv - texel, texel),
                            rawHandCoverageAt(safeUv + texel, texel)),
                    min(rawHandCoverageAt(safeUv + vec2(-texel.x, texel.y), texel),
                            rawHandCoverageAt(safeUv + vec2(texel.x, -texel.y), texel))));
    coverage = max(coverage, bridge);
    if (bridge < 0.5) return center;

    vec4 left = texture(HandSampler, safeUv - vec2(texel.x, 0.0));
    vec4 right = texture(HandSampler, safeUv + vec2(texel.x, 0.0));
    vec4 down = texture(HandSampler, safeUv - vec2(0.0, texel.y));
    vec4 up = texture(HandSampler, safeUv + vec2(0.0, texel.y));
    vec4 downLeft = texture(HandSampler, safeUv - texel);
    vec4 upRight = texture(HandSampler, safeUv + texel);
    vec4 upLeft = texture(HandSampler, safeUv + vec2(-texel.x, texel.y));
    vec4 downRight = texture(HandSampler, safeUv + vec2(texel.x, -texel.y));

    float weight = left.a + right.a + down.a + up.a
            + downLeft.a + upRight.a + upLeft.a + downRight.a;
    if (weight <= 0.001) return center;
    vec3 color = (left.rgb * left.a + right.rgb * right.a
            + down.rgb * down.a + up.rgb * up.a
            + downLeft.rgb * downLeft.a + upRight.rgb * upRight.a
            + upLeft.rgb * upLeft.a + downRight.rgb * downRight.a) / weight;
    return vec4(color, 1.0);
}

vec3 modeColor(vec4 hand) {
    int mode = int(Params0.x + 0.5);
    if (mode == 4) return hand.rgb;
    if (mode == 0) {
        vec3 fill = FillColor.rgb;
        if (Params2.z > 0.5) {
            fill = hue(fract(texCoord.y * Params3.w + Params0.z * Params3.z));
        }
        if (Params2.w > 0.5) {
            float shading = dot(hand.rgb, vec3(0.299, 0.587, 0.114));
            fill *= mix(1.0, shading, Params3.y);
        }
        return mix(hand.rgb, fill, Params3.x);
    }
    if (mode == 1) return shaderPattern(texCoord, hand.rgb);

    float mirrorMix = clamp(Params4.x, 0.0, 1.0);
    if (mirrorMix <= 0.001) return mode == 3 ? shaderPattern(texCoord, hand.rgb) : hand.rgb;
    vec3 mirror = mirrorRefraction(texCoord);
    if (mirrorMix >= 0.999) return mirror;
    vec3 source = mode == 3 ? shaderPattern(texCoord, hand.rgb) : hand.rgb;
    return mix(source, mirror, mirrorMix);
}

void main() {
    float coverage;
    vec4 hand = repairedHandSample(texCoord, coverage);
    int mode = int(Params0.x + 0.5);
    if (mode != 2 && mode != 3) coverage = hand.a;
    if (coverage <= 0.001) discard;
    vec3 result = modeColor(hand);
    if (Params4.z > 0.5 && Params5.w > 0.001 && coverage > 0.08) {
        vec4 bloom = texture(BloomSampler, texCoord);
        float bloomMask = clamp(handsBloomMask(bloom, Params5.y), 0.0, 1.0);
        if (bloomMask > 0.008 && bloomMask < 0.92) {
            float edgeDistance = 1.0 - smoothstep(0.18, 0.92, bloomMask);
            float edgeSupport = smoothstep(0.008, 0.22, bloomMask);
            float innerGlow = coverage * pow(edgeDistance, 0.72) * edgeSupport * Params5.w;
            if (innerGlow > 0.08) {
                float stableHandMask = smoothstep(0.08, 0.32, coverage);
                float innerCore = smoothstep(0.08, 0.72, innerGlow) * stableHandMask;
                float core = pow(clamp(innerGlow, 0.0, 1.0), 0.55);
                vec3 innerColor = mix(GlowColor2.rgb, GlowColor1.rgb, core);
                if (Params5.y > 0.5) innerColor = handsItemColor(hand);
                float innerStrength = mode == 2 || mode == 3 ? 0.18 : 0.42;
                result = mix(result, innerColor, innerCore * innerStrength);
                result *= 1.0 + innerCore * (mode == 2 || mode == 3 ? 0.06 : 0.14);
            }
        }
    }
    fragColor = vec4(max(result, vec3(0.0)), smoothstep(0.01, 0.16, coverage));
}
