#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform SkySettings {
    float time;
    float scale;
    int mode;
    vec4 primaryColor;
    vec4 secondaryColor;
    vec4 accentColor;
    vec4 horizonColor;
};

layout(location = 0) in vec3 skyDirection;
layout(location = 0) out vec4 fragColor;

// --- shared helpers -------------------------------------------------------

float hash3(vec3 p) {
    uvec3 bits = floatBitsToUint(p + vec3(0.0));
    uint h = bits.x * 1597334677u ^ bits.y * 3812015801u ^ bits.z * 2798796415u;
    h = (h ^ (h >> 16u)) * 2246822519u;
    h = (h ^ (h >> 13u)) * 3266489917u;
    return float((h ^ (h >> 16u)) & 0x00ffffffu) / 16777216.0;
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);

    float n000 = hash3(i);
    float n100 = hash3(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash3(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash3(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash3(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash3(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash3(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash3(i + vec3(1.0, 1.0, 1.0));

    return mix(mix(mix(n000, n100, u.x), mix(n010, n110, u.x), u.y),
               mix(mix(n001, n101, u.x), mix(n011, n111, u.x), u.y), u.z);
}

float fbm3(vec3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    mat3 rot = mat3(
        0.00, 0.80, 0.60,
       -0.80, 0.36, -0.48,
       -0.60, -0.48, 0.64
    );

    for (int i = 0; i < 5; i++) {
        value += noise3(p) * amplitude;
        p = rot * p * 1.95 + vec3(8.7, 4.9, 6.3);
        amplitude *= 0.56;
    }

    return value;
}

// flattened sky-dome coordinates: x runs around, y toward the zenith
vec2 skyPlane(vec3 direction, float bias) {
    return normalize(direction).xz / (abs(direction.y) + bias);
}

// grid-cell starfield with per-star twinkle
vec3 stars(vec3 dir, float density, float threshold, float t, vec3 tint) {
    vec3 p = dir * density;
    vec3 cell = floor(p);
    float h = hash3(cell);
    if (h < threshold) return vec3(0.0);

    vec3 f = fract(p) - 0.5;
    float disc = 1.0 - smoothstep(0.0, 0.09, dot(f, f));
    float twinkle = 0.55 + 0.45 * sin(t * (1.2 + h * 3.0) + h * 71.0);
    return mix(vec3(0.92, 0.95, 1.0), tint, 0.25) * disc * twinkle * ((h - threshold) / (1.0 - threshold));
}

// ==========================================================================
// Mode 0 — AURORA: waving curtains of light over a cold night sky
// ==========================================================================

float curtain(vec2 plane, float center, float freq, float amp, float phase, float n) {
    float curve = center
        + sin(plane.x * freq + phase) * amp
        + sin(plane.x * freq * 2.3 - phase * 0.7) * amp * 0.35
        + n * 0.22;
    float d = plane.y - curve;
    // sharp lower edge, long soft falloff upward — the classic aurora profile
    return exp(-d * d * 9.0) * (0.55 + 0.45 * smoothstep(-0.35, 0.15, d));
}

// --- ported from Jabalin SkyShader: extra helpers -------------------------

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

float glow(vec3 direction, vec3 focus, float power) {
    return pow(max(dot(normalize(direction), normalize(focus)), 0.0), power);
}

float starField(vec3 direction, float t, float density, float twinkleSpeed) {
    vec3 dir = normalize(direction);
    float n1 = noise3(dir * density + vec3(9.1, 17.3, 23.7));
    float n2 = noise3(dir * (density * 2.1) + vec3(31.8, 7.4, 12.6));
    float sparse = smoothstep(0.92, 0.995, n1);
    float core = smoothstep(0.78, 0.99, n2);
    float seed = hash3(floor(dir * density * 3.0) + vec3(13.0, 27.0, 41.0));
    float twinkle = 0.6 + 0.4 * sin(t * twinkleSpeed + seed * 45.0);
    return sparse * core * twinkle;
}

float softSkyBand(vec2 plane, float center, float width, float bend, float frequency, float phase) {
    float curve = center + sin(plane.x * frequency + phase) * bend + sin(plane.x * (frequency * 0.53) - phase * 0.72) * bend * 0.45;
    return 1.0 - smoothstep(width * 0.20, width, abs(plane.y - curve));
}

float causticPattern(vec2 uv, float t) {
    vec2 p = mod(uv * 6.28318, 6.28318) - 250.0;
    vec2 i = p;
    float c = 1.0;
    float intensity = 0.0052;

    for (int n = 0; n < 5; n++) {
        float tn = t * 0.5 + float(n);
        i = p + vec2(
            cos(tn - i.x) + sin(tn + i.y),
            sin(tn - i.y) + cos(tn + i.x)
        );
        c += 1.0 / length(vec2(
            p.x / (sin(i.x + tn) / intensity),
            p.y / (cos(i.y + tn) / intensity)
        ));
    }

    c /= 5.0;
    c = 1.17 - pow(c, 1.4);
    return clamp(pow(abs(c), 8.0), 0.0, 5.0);
}

vec3 causticStars(vec3 direction, float t, vec3 tint) {
    vec3 p = direction * 180.0;
    vec3 cell = floor(p);
    float h = hash3(cell);
    if (h < 0.991) {
        return vec3(0.0);
    }

    vec3 f = fract(p) - 0.5;
    float disc = exp(-dot(f, f) * 1400.0);
    float twinkle = 0.5 + 0.5 * sin(t * 2.0 + h * 113.0);
    return mix(vec3(0.95, 0.96, 1.0), tint, 0.20) * disc * twinkle * 1.1;
}

float nebulaHash13(vec3 p) {
    // Adjacent noise cells must hash shared corners identically on every GPU backend.
    return hash3(p);
}

float nebulaNoise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);

    float a = mix(nebulaHash13(i + vec3(0.0, 0.0, 0.0)), nebulaHash13(i + vec3(1.0, 0.0, 0.0)), f.x);
    float b = mix(nebulaHash13(i + vec3(0.0, 1.0, 0.0)), nebulaHash13(i + vec3(1.0, 1.0, 0.0)), f.x);
    float c = mix(nebulaHash13(i + vec3(0.0, 0.0, 1.0)), nebulaHash13(i + vec3(1.0, 0.0, 1.0)), f.x);
    float d = mix(nebulaHash13(i + vec3(0.0, 1.0, 1.0)), nebulaHash13(i + vec3(1.0, 1.0, 1.0)), f.x);
    return mix(mix(a, b, f.y), mix(c, d, f.y), f.z);
}

float nebulaFbm3(vec3 p) {
    float value = 0.0;
    float amplitude = 0.55;
    value += nebulaNoise(p) * amplitude;
    p = p * 2.03 + vec3(7.1, 3.2, 5.4);
    amplitude *= 0.52;
    value += nebulaNoise(p) * amplitude;
    p = p * 2.11 + vec3(1.9, 8.3, 2.7);
    amplitude *= 0.52;
    value += nebulaNoise(p) * amplitude;
    return value;
}

vec3 nebulaStarLayer(vec3 direction, float density, float threshold, vec3 tint, float t) {
    vec3 p = direction * density;
    vec3 cell = floor(p);
    float h = nebulaHash13(cell);
    if (h < threshold) {
        return vec3(0.0);
    }

    vec3 f = fract(p) - 0.5;
    float disc = 1.0 - smoothstep(0.0, 0.045, length(f));
    float twinkle = 0.65 + 0.35 * sin(t * 2.4 + h * 71.0);
    return mix(vec3(1.0), tint, 0.22) * disc * twinkle * ((h - threshold) / (1.0 - threshold));
}

vec3 rotateY(vec3 p, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vec3(p.x * c - p.z * s, p.y, p.x * s + p.z * c);
}

// --- ported from Jabalin SkyShader: 10 modes ----------------------------

vec3 renderBloom(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    vec3 bloomDir = normalize(vec3(-0.24, 0.88, 0.31));
    vec3 echoDir = normalize(vec3(0.36, 0.76, -0.28));
    vec3 domain = dir * (2.30 / max(scale, 0.001));

    float warpA = fbm3(domain * 0.9 + vec3(t * 0.035, -t * 0.016, t * 0.028));
    float warpB = fbm3(domain * 1.45 + vec3(-t * 0.021, t * 0.019, -t * 0.031));
    float petals = fbm3(domain + vec3(warpA * 0.92, warpB * 0.58, warpA * 0.36));

    float bloom = glow(dir, bloomDir, 4.8);
    float echo = glow(dir, echoDir, 7.5);
    float blossom = smoothstep(0.42, 0.90, petals + warpA * 0.22 + bloom * 0.42 - abs(dir.y - 0.18) * 0.18);
    float inner = pow(saturate(petals * 0.92 + warpB * 0.35), 2.2);

    vec3 gradient = mix(secondaryColor.rgb * 0.48, primaryColor.rgb, smoothstep(-0.25, 0.88, dir.y));
    vec3 color = gradient * (0.18 + petals * 0.42);
    color += mix(primaryColor.rgb, accentColor.rgb, 0.54) * blossom * 0.56;
    color += accentColor.rgb * inner * 0.20;
    color += vec3(1.0) * glow(dir, bloomDir, 22.0) * 0.16;
    color += secondaryColor.rgb * echo * 0.22;

    float stars = starField(dir, t, 30.0, 1.7);
    float dust = starField(dir, t, 56.0, 1.15) * 0.38;
    color += vec3(1.0) * (stars * 0.44 + dust * 0.22) * smoothstep(-0.05, 0.92, dir.y);
    return color;
}

vec3 renderVeil(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    vec3 moonDir = normalize(vec3(0.42, 0.72, 0.26));
    vec3 samplePos = dir * (2.85 / max(scale, 0.001));

    float layerA = fbm3(samplePos * 0.76 + vec3(t * 0.024, -t * 0.018, t * 0.020));
    float layerB = fbm3(samplePos * 1.45 + vec3(-t * 0.028, t * 0.022, -t * 0.016));
    float layerC = fbm3(samplePos + vec3(layerA * 0.74, layerB * 0.56, -layerA * 0.24));

    float mist = saturate(layerA * 0.74 + layerB * 0.38 - abs(dir.y + 0.06) * 0.16);
    float veil = smoothstep(0.40, 0.93, layerC + mist * 0.32);
    float pearl = pow(saturate(layerC * 0.78 + layerB * 0.34), 2.8);

    vec3 gradient = mix(secondaryColor.rgb * 0.52, primaryColor.rgb, smoothstep(-0.35, 0.78, dir.y));
    vec3 color = gradient * (0.18 + mist * 0.48);
    color += accentColor.rgb * veil * 0.28;
    color += vec3(1.0) * pearl * 0.22;

    float moonCore = glow(dir, moonDir, 72.0);
    float halo = glow(dir, moonDir, 13.0);
    color += vec3(1.0) * moonCore * 1.18;
    color += accentColor.rgb * halo * 0.30;

    float horizonPearl = smoothstep(-0.35, 0.02, dir.y) * (1.0 - smoothstep(0.02, 0.18, dir.y));
    color += accentColor.rgb * horizonPearl * 0.12;

    float stars = starField(dir, t, 26.0, 1.5) * smoothstep(-0.05, 0.98, dir.y) * (1.0 - veil * 0.35);
    color += vec3(1.0) * stars * 0.48;
    color += secondaryColor.rgb * stars * 0.16;
    return color;
}

vec3 renderHalo(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    vec3 crownDir = normalize(vec3(0.12, 0.96, -0.21));
    vec3 domain = dir * (2.20 / max(scale, 0.001));

    float dustA = fbm3(domain * 0.95 + vec3(t * 0.018, -t * 0.012, t * 0.011));
    float dustB = fbm3(domain * 1.82 + vec3(-t * 0.016, t * 0.024, -t * 0.021));
    float focus = max(dot(dir, crownDir), 0.0);
    float halo = smoothstep(0.48, 0.78, focus) * (1.0 - smoothstep(0.78, 0.94, focus));
    float corona = pow(focus, 24.0);
    float cloud = smoothstep(0.44, 0.92, dustA + dustB * 0.22 + focus * 0.28);
    float caustic = pow(saturate(dustB * 0.92 + focus * 0.25), 3.2);

    vec3 gradient = mix(secondaryColor.rgb * 0.42, primaryColor.rgb, smoothstep(-0.22, 0.92, dir.y));
    vec3 color = gradient * (0.18 + dustA * 0.35);
    color += primaryColor.rgb * cloud * 0.22;
    color += accentColor.rgb * halo * 0.85;
    color += vec3(1.0) * corona * 1.25;
    color += accentColor.rgb * caustic * 0.18;

    float horizonWarm = smoothstep(-0.28, 0.12, dir.y) * (0.35 + dustA * 0.20);
    color += mix(primaryColor.rgb, accentColor.rgb, 0.45) * horizonWarm * 0.16;

    float stars = starField(dir, t, 32.0, 1.35) * (1.0 - halo * 0.65);
    float dustStars = starField(dir, t, 60.0, 0.92) * 0.34;
    color += vec3(1.0) * (stars * 0.42 + dustStars * 0.20) * smoothstep(-0.08, 0.96, dir.y);
    return color;
}

vec3 renderRift(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    vec3 domain = dir * (2.46 / max(scale, 0.001));
    float warpA = fbm3(domain * 0.92 + vec3(t * 0.026, -t * 0.020, t * 0.023));
    float warpB = fbm3(domain * 1.68 + vec3(-t * 0.030, t * 0.017, -t * 0.014));
    float foldA = fbm3(domain + vec3(warpA * 0.86, warpB * 0.58, -warpA * 0.34));
    float foldB = fbm3(domain * 1.35 + vec3(-warpB * 0.64, warpA * 0.44, warpB * 0.25));
    float canopy = smoothstep(0.34, 0.94, foldA * 0.72 + foldB * 0.24 - abs(dir.y - 0.10) * 0.10);
    float riftBody = 1.0 - abs(foldB * 2.0 - 1.0);
    float veins = pow(saturate(riftBody * 0.86 + canopy * 0.24 - abs(dir.y - 0.02) * 0.06), 2.8);
    float embers = pow(saturate(foldA * 0.72 + warpB * 0.20), 3.2);

    vec3 gradient = mix(secondaryColor.rgb * 0.34, primaryColor.rgb, smoothstep(-0.28, 0.82, dir.y));
    vec3 color = gradient * (0.14 + foldA * 0.36);
    color += primaryColor.rgb * canopy * 0.46;
    color += mix(primaryColor.rgb, accentColor.rgb, 0.62) * veins * 0.40;
    color += accentColor.rgb * embers * 0.16;

    float hotWind = smoothstep(-0.18, 0.22, dir.y) * (0.22 + warpA * 0.12);
    color += mix(primaryColor.rgb, accentColor.rgb, 0.48) * hotWind * 0.14;

    float stars = starField(dir, t, 34.0, 1.55) * (1.0 - canopy * 0.54);
    float ash = starField(dir, t, 58.0, 1.05) * 0.22;
    color += vec3(1.0) * (stars * 0.28 + ash * 0.12) * smoothstep(-0.04, 0.94, dir.y);
    return color;
}

vec3 renderEclipse(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    vec3 samplePos = dir * (2.34 / max(scale, 0.001));
    float cloudA = fbm3(samplePos * 0.84 + vec3(t * 0.012, -t * 0.008, t * 0.010));
    float cloudB = fbm3(samplePos * 1.48 + vec3(-t * 0.016, t * 0.013, -t * 0.010));
    float cloudC = fbm3(samplePos + vec3(cloudA * 0.76, -cloudB * 0.42, cloudA * 0.24));
    float lace = 1.0 - abs(cloudC * 2.0 - 1.0);

    float canopy = smoothstep(0.28, 0.92, cloudA * 0.60 + cloudB * 0.20 + lace * 0.20 - abs(dir.y - 0.10) * 0.08);
    float shadowFront = smoothstep(-0.26, 0.62, dir.x * -0.42 + dir.y * 0.22 + cloudB * 0.18);
    float silver = pow(saturate(lace * 0.80 + canopy * 0.18), 3.0) * (0.34 + shadowFront * 0.30);
    float underlight = smoothstep(-0.34, 0.10, dir.y) * (0.24 + cloudA * 0.10);
    float veil = smoothstep(0.24, 0.90, cloudA * 0.52 + cloudC * 0.16 - abs(dir.y + 0.04) * 0.10);

    vec3 gradient = mix(secondaryColor.rgb * 0.34, primaryColor.rgb * 0.84, smoothstep(-0.28, 0.90, dir.y));
    vec3 color = gradient * (0.16 + cloudA * 0.18);
    color += primaryColor.rgb * canopy * 0.16;
    color += accentColor.rgb * silver * 0.24;
    color += mix(primaryColor.rgb, accentColor.rgb, 0.40) * underlight * 0.10;
    color += accentColor.rgb * veil * 0.06;
    color *= mix(0.92, 1.06, shadowFront);

    float stars = starField(dir, t, 38.0, 1.14) * (1.0 - canopy * 0.50);
    float deep = starField(dir, t, 68.0, 0.82) * 0.18;
    color += vec3(1.0) * (stars * 0.40 + deep * 0.10) * smoothstep(-0.01, 0.99, dir.y);
    return color;
}

vec3 renderStars(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    vec3 domain = dir * (2.08 / max(scale, 0.001));
    vec2 plane = skyPlane(dir, 0.58) * (0.92 / max(scale, 0.001));

    float mistA = fbm3(domain * 0.72 + vec3(t * 0.010, -t * 0.006, t * 0.012));
    float mistB = fbm3(domain * 1.36 + vec3(-t * 0.014, t * 0.009, -t * 0.010));
    float bend = sin(plane.x * 1.35 + t * 0.055) * 0.16 + sin(plane.x * 2.8 - t * 0.034) * 0.06;
    float galaxyBand = 1.0 - smoothstep(0.08, 1.12, abs(plane.y + bend));
    galaxyBand *= smoothstep(-0.35, 0.95, dir.y);

    float nebula = smoothstep(0.30, 0.95, mistA * 0.72 + mistB * 0.30 + galaxyBand * 0.42);
    float coreDust = pow(saturate(1.0 - abs(mistB * 2.0 - 1.0)), 2.2) * galaxyBand;

    vec3 gradient = mix(secondaryColor.rgb * 0.30, primaryColor.rgb, smoothstep(-0.30, 0.88, dir.y));
    vec3 color = gradient * (0.13 + mistA * 0.18);
    color += mix(primaryColor.rgb, accentColor.rgb, 0.62) * nebula * 0.36;
    color += accentColor.rgb * coreDust * 0.34;

    float starsFine = starField(dir, t, 58.0, 2.15);
    float starsSmall = starField(dir, t, 92.0, 1.65) * 0.42;
    float starsBig = starField(dir, t, 24.0, 0.95) * 0.78;
    float starMask = smoothstep(-0.08, 0.98, dir.y);
    color += vec3(1.0) * (starsFine * 0.74 + starsSmall * 0.36 + starsBig * 0.62) * starMask;
    color += accentColor.rgb * starsBig * 0.22;

    float cometPhase = fract(t * 0.030);
    vec2 head = vec2(mix(1.40, -1.15, cometPhase), 0.48 + sin(t * 0.19) * 0.10);
    vec2 delta = plane - head;
    float headGlow = exp(-dot(delta, delta) * 42.0);
    float streakLine = 1.0 - smoothstep(0.00, 0.040, abs(delta.y + delta.x * 0.34));
    float tail = streakLine * smoothstep(0.00, 0.95, delta.x) * (1.0 - smoothstep(0.95, 1.85, delta.x));
    color += vec3(1.0) * headGlow * 0.85;
    color += accentColor.rgb * tail * 0.22;

    return color;
}

vec3 renderAether(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    float invScale = 1.0 / max(scale, 0.001);
    vec3 domain = dir * (2.22 * invScale);
    vec2 plane = skyPlane(dir, 0.60) * (0.96 * invScale);

    float mistA = fbm3(domain * 0.74 + vec3(t * 0.016, -t * 0.010, t * 0.014));
    float mistB = fbm3(domain * 1.48 + vec3(-t * 0.021, t * 0.014, -t * 0.017));
    float mistC = fbm3(domain * 2.05 + vec3(t * 0.024, -t * 0.018, t * 0.012));

    vec3 bloomDir = normalize(vec3(-0.26, 0.78, 0.36));
    vec3 echoDir = normalize(vec3(0.45, 0.70, -0.22));
    float bloom = glow(dir, bloomDir, 7.2);
    float bloomCore = glow(dir, bloomDir, 35.0);
    float echo = glow(dir, echoDir, 9.0);

    float veilA = softSkyBand(plane, 0.18, 0.34, 0.065, 2.8, t * 0.060);
    float veilB = softSkyBand(plane, 0.35, 0.42, 0.045, 4.6, -t * 0.044 + 1.2);
    float silk = smoothstep(0.30, 0.93, mistA * 0.62 + mistB * 0.28 + veilA * 0.42 + bloom * 0.26 - abs(dir.y - 0.18) * 0.08);
    float pearlDust = pow(saturate(1.0 - abs(mistC * 2.0 - 1.0)), 2.35) * (silk * 0.58 + veilB * 0.42);

    vec3 gradient = mix(secondaryColor.rgb * 0.34, primaryColor.rgb * 0.92, smoothstep(-0.30, 0.90, dir.y));
    vec3 color = gradient * (0.13 + mistA * 0.30);
    color += mix(primaryColor.rgb, accentColor.rgb, 0.36) * silk * 0.46;
    color += accentColor.rgb * veilA * 0.34;
    color += mix(primaryColor.rgb, vec3(1.0), 0.35) * veilB * 0.12;
    color += mix(accentColor.rgb, vec3(1.0), 0.52) * pearlDust * 0.15;
    color += primaryColor.rgb * bloom * 0.30;
    color += accentColor.rgb * echo * 0.18;
    color += vec3(1.0) * bloomCore * 0.48;

    float horizonMist = smoothstep(-0.25, 0.12, dir.y) * (1.0 - smoothstep(0.12, 0.32, dir.y));
    color += mix(primaryColor.rgb, accentColor.rgb, 0.42) * horizonMist * (0.12 + mistB * 0.10);

    float starsFine = starField(dir, t, 44.0, 1.85) * (1.0 - silk * 0.28);
    float starsDeep = starField(dir, t, 82.0, 1.20) * 0.30;
    float starMask = smoothstep(-0.06, 0.98, dir.y);
    color += vec3(1.0) * (starsFine * 0.44 + starsDeep * 0.20) * starMask;
    color += accentColor.rgb * starsFine * 0.10;

    return color;
}

vec3 renderPearl(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    float invScale = 1.0 / max(scale, 0.001);
    vec3 domain = dir * (2.36 * invScale);
    vec2 plane = skyPlane(dir, 0.62) * (0.88 * invScale);

    float mistA = fbm3(domain * 0.68 + vec3(t * 0.010, -t * 0.007, t * 0.009));
    float mistB = fbm3(domain * 1.32 + vec3(-t * 0.015, t * 0.012, -t * 0.010));
    float mistC = fbm3(domain * 1.92 + vec3(t * 0.018, -t * 0.014, t * 0.012));

    vec3 moonDir = normalize(vec3(0.42, 0.73, 0.27));
    float moonCore = glow(dir, moonDir, 86.0);
    float moonHalo = glow(dir, moonDir, 12.0);
    float wideHalo = glow(dir, moonDir, 3.8);

    float veilA = softSkyBand(plane, 0.26, 0.40, 0.045, 2.5, -t * 0.040);
    float veilB = softSkyBand(plane, 0.47, 0.48, 0.034, 5.0, t * 0.034 + 0.8);
    float lowFog = smoothstep(-0.34, 0.08, dir.y) * (1.0 - smoothstep(0.08, 0.28, dir.y));
    float pearlVeil = smoothstep(0.29, 0.92, mistA * 0.68 + mistB * 0.27 + veilA * 0.38 + moonHalo * 0.12 - abs(dir.y - 0.08) * 0.12);
    float lace = pow(saturate(1.0 - abs(mistC * 2.0 - 1.0)), 2.65) * (pearlVeil * 0.68 + veilB * 0.38);

    vec3 gradient = mix(secondaryColor.rgb * 0.36, primaryColor.rgb * 0.90, smoothstep(-0.32, 0.84, dir.y));
    vec3 color = gradient * (0.15 + mistA * 0.25);
    color += primaryColor.rgb * pearlVeil * 0.24;
    color += accentColor.rgb * veilA * 0.26;
    color += mix(accentColor.rgb, vec3(1.0), 0.62) * lace * 0.16;
    color += accentColor.rgb * lowFog * (0.10 + mistB * 0.09);
    color += accentColor.rgb * moonHalo * 0.28;
    color += primaryColor.rgb * wideHalo * 0.08;
    color += vec3(1.0) * moonCore * 1.28;

    float stars = starField(dir, t, 32.0, 1.24) * (1.0 - pearlVeil * 0.32);
    float dust = starField(dir, t, 68.0, 0.92) * 0.24;
    color += vec3(1.0) * (stars * 0.42 + dust * 0.14) * smoothstep(-0.04, 0.98, dir.y);
    color += accentColor.rgb * stars * 0.08;

    return color;
}

vec3 renderCaustic(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    vec3 accent = max(accentColor.rgb, vec3(0.05));
    vec3 deep = mix(secondaryColor.rgb * 0.44, accent * 0.05 + vec3(0.005, 0.008, 0.020), 0.55);
    vec3 mid = mix(primaryColor.rgb * 0.52, accent * 0.42, 0.50);
    vec3 hot = mix(accent, vec3(1.0), 0.55);

    vec2 uv = dir.xz / (abs(dir.y) + 0.30) * (0.50 / max(scale, 0.001));
    float flowTime = t * 0.30;

    float causticA = causticPattern(uv, flowTime);
    float causticB = causticPattern(uv * 1.30 + vec2(7.3, 11.1), flowTime * 0.78 + 3.1) * 0.55;
    float caustic = causticA + causticB;

    vec3 sky = mix(mid, deep, smoothstep(0.0, 0.75, dir.y));
    float skyMask = smoothstep(-0.05, 0.22, dir.y);
    vec3 color = sky + (accent * 0.95 + hot * 0.40) * caustic * skyMask;

    float core = smoothstep(0.65, 1.6, caustic);
    color += hot * core * 0.75;

    float starOcc = 1.0 - smoothstep(0.08, 0.55, caustic);
    color += causticStars(dir, t, accent) * starOcc;

    color = color / (1.0 + color * 0.55);
    color = pow(color, vec3(0.92));
    return color;
}

vec3 renderNebula(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    vec3 accent = max(accentColor.rgb, vec3(0.05));
    float vertical = abs(dir.y);

    vec3 deep = mix(secondaryColor.rgb * 0.28, accent * 0.16 + vec3(0.004, 0.003, 0.014), 0.55);
    vec3 horizon = mix(deep, primaryColor.rgb * 0.42 + accent * 0.35, 0.55);
    vec3 color = mix(horizon, deep, smoothstep(0.0, 0.9, vertical));

    vec3 rd = rotateY(dir, t * 0.035);
    float invScale = 1.0 / max(scale, 0.001);
    vec3 q = rd * (2.0 * invScale) + vec3(t * 0.035, t * 0.018, -t * 0.025);
    float clouds = nebulaFbm3(q);
    float filaments = nebulaFbm3(rd * (5.0 * invScale) + vec3(-t * 0.02, t * 0.03, t * 0.015));

    float nebula = smoothstep(0.42, 0.92, clouds) * 0.78 + smoothstep(0.55, 0.96, filaments) * 0.32;
    float band = 1.0 - abs(dot(dir, normalize(vec3(0.52, 0.25, 0.82)))) * 1.35;
    band = smoothstep(0.18, 0.9, band);
    nebula += band * (0.18 + 0.45 * clouds);

    vec3 warm = mix(accent, mix(primaryColor.rgb, vec3(1.0, 0.54, 0.2), 0.65), 0.42);
    vec3 cool = mix(accent, mix(secondaryColor.rgb, vec3(0.16, 0.52, 1.0), 0.72), 0.45);
    vec3 nebulaColor = mix(cool, warm, smoothstep(0.2, 0.95, clouds));
    vec3 core = mix(accent * 1.45, vec3(1.0, 0.95, 0.88), 0.35);
    color += nebulaColor * nebula * 1.2;
    color += core * pow(max(nebula - 0.52, 0.0), 1.7) * 1.25;

    float horizonGlow = pow(1.0 - smoothstep(0.0, 0.42, abs(dir.y)), 1.8);
    color += mix(deep * 2.0, warm, 0.55) * horizonGlow * 0.35;

    vec3 starDir = rotateY(dir, t * 0.008);
    color += nebulaStarLayer(starDir, 180.0, 0.989, mix(accent, vec3(1.0), 0.65), t) * (1.0 - nebula * 0.45);
    color += nebulaStarLayer(starDir, 72.0, 0.994, accent, t) * 1.25;

    color = color / (1.0 + color * 0.45);
    color = pow(color, vec3(0.92));
    return color;
}

vec3 renderAurora(vec3 dir, float t) {
    float invScale = 1.0 / max(scale, 0.001);
    vec2 plane = skyPlane(dir, 0.55) * (0.9 * invScale);
    float horizon = smoothstep(-0.35, 0.55, dir.y);

    // cold gradient backdrop
    vec3 color = mix(secondaryColor.rgb * 0.55, primaryColor.rgb * 0.65, horizon);

    float n = fbm3(dir * (2.0 * invScale) + vec3(t * 0.05, 0.0, t * 0.03));

    // three curtains drifting at different speeds and heights
    float bandA = curtain(plane, 0.30, 1.7, 0.16, t * 0.24, n);
    float bandB = curtain(plane, 0.62, 1.1, 0.11, -t * 0.17 + 2.1, n);
    float bandC = curtain(plane, 0.14, 2.6, 0.07, t * 0.31 + 4.4, n);

    // vertical shimmer running along the curtains
    float shimmer = 0.72 + 0.28 * sin(plane.x * 6.0 - t * 1.4 + n * 5.0);

    vec3 auroraLow = accentColor.rgb;                       // bright edge color
    vec3 auroraHigh = mix(primaryColor.rgb, accentColor.rgb, 0.35);

    color += auroraLow * bandA * shimmer * 0.85 * horizon;
    color += auroraHigh * bandB * shimmer * 0.55 * horizon;
    color += mix(auroraLow, vec3(1.0), 0.25) * bandC * shimmer * 0.40 * horizon;

    // faint reflected glow near the horizon
    float glow = pow(1.0 - abs(dir.y), 3.0);
    color += accentColor.rgb * glow * 0.10;

    float occlusion = clamp(bandA + bandB * 0.7, 0.0, 1.0);
    color += stars(dir, 150.0, 0.986, t, primaryColor.rgb) * (1.0 - occlusion * 0.75);

    return color;
}

// ==========================================================================
// Mode 1 — LIQUID: slow molten waves flowing across the dome
// ==========================================================================

vec3 renderLiquid(vec3 dir, float t) {
    float invScale = 1.0 / max(scale, 0.001);
    vec3 domain = dir * (2.4 * invScale);

    float flow = fbm3(domain + vec3(t * 0.045, -t * 0.028, t * 0.036));
    float detail = fbm3(domain * 1.8 - vec3(t * 0.030, t * 0.022, -t * 0.026));
    float wave = 0.5 + 0.5 * sin(flow * 5.2 + detail * 2.6 + t * 0.22);

    vec3 deep = secondaryColor.rgb * 0.55;
    vec3 mid = mix(secondaryColor.rgb, primaryColor.rgb, clamp(flow * 1.2, 0.0, 1.0));
    vec3 crest = mix(primaryColor.rgb, accentColor.rgb, 0.65);

    vec3 color = mix(deep, mid, clamp(flow * 1.15, 0.0, 1.0));
    color = mix(color, crest, smoothstep(0.55, 0.95, wave) * 0.65);

    // foam sparks on the wave crests
    float foam = smoothstep(0.82, 1.0, wave) * (0.6 + 0.4 * sin(t * 0.9 + detail * 8.0));
    color += mix(accentColor.rgb, vec3(1.0), 0.55) * foam * 0.30;

    // depth shading toward the horizon so the dome reads as a volume
    float horizon = smoothstep(-0.4, 0.7, dir.y);
    color *= 0.55 + horizon * 0.45;

    color += stars(dir, 120.0, 0.992, t, accentColor.rgb) * 0.35 * horizon;

    return color;
}

// ==========================================================================
// Mode 2 — VOID: deep space, twin nebula veils, dense stars, a comet
// ==========================================================================

vec3 renderVoid(vec3 dir, float t) {
    float invScale = 1.0 / max(scale, 0.001);

    // near-black base with a whisper of color toward the horizon
    float horizon = pow(1.0 - abs(dir.y), 2.2);
    vec3 color = secondaryColor.rgb * 0.22 + primaryColor.rgb * horizon * 0.10;

    // two slowly drifting nebula veils
    float veilA = fbm3(dir * (2.1 * invScale) + vec3(t * 0.020, t * 0.012, -t * 0.016));
    float veilB = fbm3(dir * (3.4 * invScale) - vec3(t * 0.014, -t * 0.010, t * 0.018));

    float nebula = smoothstep(0.45, 0.95, veilA * 0.72 + veilB * 0.30);
    float wisps = pow(clamp(1.0 - abs(veilB * 2.0 - 1.0), 0.0, 1.0), 2.6);

    color += mix(primaryColor.rgb, accentColor.rgb, veilB) * nebula * 0.42;
    color += accentColor.rgb * wisps * nebula * 0.22;

    // layered starfield: dust, mid stars, rare bright ones
    color += stars(dir, 220.0, 0.990, t, primaryColor.rgb) * 0.5;
    color += stars(dir, 110.0, 0.992, t, accentColor.rgb);
    color += stars(dir, 45.0, 0.996, t * 0.7, vec3(1.0)) * 1.4;

    // a comet streaking across every ~8 seconds
    vec2 plane = skyPlane(dir, 0.6);
    float phase = fract(t * 0.125);
    vec2 head = vec2(mix(1.5, -1.5, phase), 0.42 + sin(t * 0.11) * 0.15);
    vec2 delta = plane - head;
    float headGlow = exp(-dot(delta, delta) * 60.0);
    float tail = (1.0 - smoothstep(0.0, 0.035, abs(delta.y + delta.x * 0.22)))
               * smoothstep(0.0, 0.9, delta.x) * (1.0 - smoothstep(0.9, 1.7, delta.x));
    float visible = smoothstep(0.0, 0.05, phase) * (1.0 - smoothstep(0.9, 1.0, phase));
    color += vec3(0.95, 0.97, 1.0) * headGlow * visible;
    color += mix(accentColor.rgb, vec3(1.0), 0.4) * tail * 0.30 * visible;

    return color;
}

// --- shared helpers for the new modes ------------------------------------

float segDist(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a, ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 1.0e-6), 0.0, 1.0);
    return length(pa - ba * h);
}

float wrapAngle(float a) {
    return mod(a + 3.14159265, 6.2831853) - 3.14159265;
}

// ==========================================================================
// Mode 3 — METEORS: night sky with streaking meteors and a rare bolide
// ==========================================================================

vec3 renderMeteors(vec3 dir, float t) {
    float horizon = smoothstep(-0.35, 0.6, dir.y);
    vec3 color = secondaryColor.rgb * 0.35 + primaryColor.rgb * pow(1.0 - abs(dir.y), 2.5) * 0.18;

    color += stars(dir, 190.0, 0.990, t, primaryColor.rgb) * 0.6;
    color += stars(dir, 90.0, 0.994, t, vec3(1.0));

    vec2 plane = skyPlane(dir, 0.6);
    vec2 fall = normalize(vec2(-0.82, -0.57));

    for (int i = 0; i < 6; i++) {
        float fi = float(i);
        float cyc = 2.2 + hash3(vec3(fi, 3.1, 7.7)) * 2.6;
        float ph = fract(t / cyc + hash3(vec3(fi, 9.2, 1.3)));
        float gen = floor(t / cyc + hash3(vec3(fi, 9.2, 1.3)));

        vec2 start = vec2(
            mix(-1.3, 1.5, hash3(vec3(fi, gen, 4.6))),
            mix(0.45, 1.15, hash3(vec3(fi, gen, 8.1))));
        float speed = 2.4 + hash3(vec3(fi, gen, 2.2)) * 1.4;
        vec2 head = start + fall * ph * speed;

        // big rare bolide on slot 0
        float big = (i == 0 && hash3(vec3(gen, 5.5, fi)) > 0.7) ? 2.2 : 1.0;
        float tailLen = (0.32 + hash3(vec3(fi, gen, 6.6)) * 0.25) * big;

        float d = segDist(plane, head, head - fall * tailLen);
        float along = clamp(dot(plane - head, -fall) / tailLen, 0.0, 1.0);
        float vis = smoothstep(0.0, 0.06, ph) * (1.0 - smoothstep(0.55, 0.8, ph));

        float streak = exp(-d * d * (2600.0 / big)) * (1.0 - along) * vis;
        color += mix(vec3(1.0), accentColor.rgb, along) * streak * (0.9 * big);
        color += accentColor.rgb * exp(-d * d * 500.0) * (1.0 - along) * vis * 0.18 * big;
    }

    return color * (0.45 + horizon * 0.55);
}

// ==========================================================================
// Mode 4 — CONSTELLATIONS: line figures igniting between the stars
// ==========================================================================


// ==========================================================================
// Mode 5 — SOLAR: boiling plasma granulation with horizon prominences
// ==========================================================================

vec3 renderSolar(vec3 dir, float t) {
    float invScale = 1.0 / max(scale, 0.001);
    vec3 domain = dir * (3.2 * invScale);

    // domain-warped boiling cells (noise-warp: дешевле fbm, кипение даёт сам fbm ниже)
    vec3 warp = vec3(
        noise3(domain + vec3(t * 0.10, 0.0, t * 0.07)),
        noise3(domain + vec3(4.2, t * 0.08, 1.7)),
        noise3(domain - vec3(t * 0.06, 2.9, t * 0.09)));
    float cells = fbm3(domain * 1.9 + (warp - 0.5) * 1.6 + vec3(0.0, t * 0.05, 0.0));
    float granule = smoothstep(0.30, 0.85, cells);
    float core = smoothstep(0.62, 0.95, cells);

    vec3 color = mix(secondaryColor.rgb * 0.7, primaryColor.rgb, granule);
    color = mix(color, accentColor.rgb, core * 0.75);
    color = mix(color, vec3(1.0, 0.97, 0.85), smoothstep(0.80, 0.98, cells) * 0.45);

    // slow global breathing
    color *= 0.9 + 0.1 * sin(t * 0.6 + cells * 4.0);

    // prominence arcs licking up from the horizon
    float band = pow(1.0 - abs(dir.y), 5.0);
    float flame = fbm3(vec3(dir.x * 4.0, dir.y * 9.0 - t * 0.35, dir.z * 4.0));
    color += accentColor.rgb * band * smoothstep(0.45, 0.85, flame) * 0.8;

    return color;
}

// ==========================================================================
// Mode 6 — PLASMA: branching discharges crawling down from the zenith core
// ==========================================================================



/**
 * Storm — плотная штормовая пелена, снятая с записи грозы, а не придуманная.
 *
 * Замеры кадра без молнии (небо, 2560x1440):
 *   средний цвет RGB(66, 75, 99) — сине-серый, не белый и не чёрный;
 *   яркость по перцентилям 10%=39, 50%=75, 90%=106 — диапазон УЗКИЙ,
 *   пелена малоконтрастная, ярких просветов нет;
 *   характерный размер клубов ~2.2% ширины кадра.
 *
 * Отсюда два решения, которые легко сделать неправильно:
 *   • клубы ВЫТЯНУТЫ по горизонтали (сжимаем x у координат шума) — на видео это
 *     слоистая стратус-пелена, а не кучевые барашки;
 *   • контраст намеренно задавлен: разгони его, и получится красивое драматичное
 *     небо, которого на записи нет.
 */
vec3 renderStorm(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    float up = clamp(dir.y, 0.0, 1.0);
    float invScale = 1.0 / max(scale, 0.001);

    // Ливневое небо: МНОГО отдельных серых клубов, а не сплошная пелена.
    // Частота выше и растяжка по горизонтали умеренная — комки должны читаться
    // поштучно, как на записи, а не сливаться в слои.
    vec3 rd = rotateY(dir, t * 0.010);
    vec3 q = vec3(rd.x * 0.72, rd.y * 1.9, rd.z * 0.72) * (3.4 * invScale);
    q += vec3(t * 0.040, -t * 0.010, t * 0.026);

    float base = nebulaFbm3(q);
    float det = nebulaFbm3(q * 2.5 + vec3(-t * 0.04, t * 0.02, t * 0.03));
    float dens = clamp(base * 0.72 + det * 0.38, 0.0, 1.0);

    // Просветы между клубами — тёмный сине-серый фон
    vec3 zenith = vec3(0.049, 0.057, 0.087);
    vec3 low    = vec3(0.116, 0.126, 0.158);
    vec3 sky = mix(low, zenith, smoothstep(0.05, 0.75, up));

    // Облако: серое тело, темнеющая сердцевина, светлая кромка. Кромка — то, что
    // делает клубы «облаками»: без неё плотность читается как грязь на фоне.
    float cover = smoothstep(0.34, 0.58, dens);
    float heart = smoothstep(0.58, 0.95, dens);
    float rim = cover * (1.0 - heart);
    vec3 body = mix(vec3(0.210, 0.221, 0.249), vec3(0.105, 0.115, 0.144), heart);
    body += rim * vec3(0.074, 0.074, 0.081);
    // к горизонту облака светлеют — как на кадре с записи
    body *= mix(1.16, 0.85, smoothstep(0.0, 0.8, up));

    vec3 color = mix(sky, body, cover);

    // тема подмешивается слабо — небо остаётся серым, а не перекрашивается
    color = mix(color, color * (0.72 + 0.56 * primaryColor.rgb), 0.14);
    return color;
}

// Sakura dusk: rose clouds, a pearl moon and slowly falling luminous petals.
vec3 renderSakura(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    float invScale = 1.0 / max(scale, 0.001);
    vec2 plane = skyPlane(dir, 0.62) * invScale;
    vec3 domain = dir * (2.30 * invScale);

    float dusk = smoothstep(-0.32, 0.90, dir.y);
    vec3 color = mix(secondaryColor.rgb * 0.44, primaryColor.rgb * 0.82, dusk);

    float mistA = fbm3(domain * 0.78 + vec3(t * 0.012, -t * 0.006, t * 0.010));
    float mistB = fbm3(domain * 1.52 + vec3(-t * 0.018, t * 0.011, -t * 0.014));
    float ribbonA = softSkyBand(plane, 0.12, 0.34, 0.055, 2.6, t * 0.035);
    float ribbonB = softSkyBand(plane, 0.39, 0.42, 0.038, 4.3, -t * 0.028 + 1.7);
    float blossomCloud = smoothstep(0.39, 0.88, mistA * 0.63 + mistB * 0.24 + ribbonA * 0.34);
    float silverEdge = pow(saturate(1.0 - abs(mistB * 2.0 - 1.0)), 3.0) * blossomCloud;
    color += mix(primaryColor.rgb, accentColor.rgb, 0.58) * blossomCloud * 0.42;
    color += mix(accentColor.rgb, vec3(1.0), 0.62) * silverEdge * 0.15;
    color += accentColor.rgb * ribbonB * 0.10;

    vec3 moonDir = normalize(vec3(0.46, 0.73, 0.31));
    float moonFocus = max(dot(dir, moonDir), 0.0);
    float moon = smoothstep(0.9928, 0.9972, moonFocus);
    float moonHalo = pow(moonFocus, 18.0);
    color += mix(accentColor.rgb, vec3(1.0), 0.76) * moon * 0.92;
    color += accentColor.rgb * moonHalo * 0.22;

    float starMask = smoothstep(-0.04, 0.90, dir.y) * (1.0 - blossomCloud * 0.58);
    color += vec3(1.0) * starField(dir, t, 48.0, 1.55) * starMask * 0.50;

    vec2 fall = plane * 7.0 + vec2(t * 0.035, -t * 0.070);
    vec2 cell = floor(fall);
    vec2 local = fract(fall) - 0.5;
    float seed = hash3(vec3(cell, 19.0));
    local -= vec2(hash3(vec3(cell, 7.0)) - 0.5, hash3(vec3(cell, 31.0)) - 0.5) * 0.46;
    float angle = seed * 6.28318 + t * (0.34 + seed * 0.26);
    vec2 petalUv = mat2(cos(angle), -sin(angle), sin(angle), cos(angle)) * local;
    float petal = step(0.73, seed) * exp(-(petalUv.x * petalUv.x * 105.0 + petalUv.y * petalUv.y * 360.0));
    petal *= smoothstep(-0.12, 0.82, dir.y);
    color += mix(primaryColor.rgb, vec3(1.0), 0.68) * petal * 0.70;

    float horizonGlow = exp(-abs(dir.y + 0.02) * 8.0);
    color += mix(primaryColor.rgb, accentColor.rgb, 0.52) * horizonGlow * 0.16;
    return color;
}

// Celestial: a violet galaxy crossed by a luminous ringed planet.
vec3 renderCelestial(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    float invScale = 1.0 / max(scale, 0.001);
    vec3 rd = rotateY(dir, t * 0.006);
    vec2 plane = skyPlane(rd, 0.58) * invScale;
    vec3 domain = rd * (2.55 * invScale);

    float mistA = fbm3(domain * 0.75 + vec3(t * 0.009, -t * 0.005, t * 0.007));
    float mistB = fbm3(domain * 1.48 + vec3(-t * 0.012, t * 0.008, -t * 0.010));
    float galaxyCurve = plane.y + sin(plane.x * 1.48 + t * 0.025) * 0.16;
    float galaxy = 1.0 - smoothstep(0.10, 0.86, abs(galaxyCurve));
    galaxy *= smoothstep(-0.28, 0.94, dir.y);
    float nebula = smoothstep(0.34, 0.94, mistA * 0.64 + mistB * 0.28 + galaxy * 0.48);
    float dustLane = pow(saturate(1.0 - abs(mistB * 2.0 - 1.0)), 2.2) * galaxy;

    vec3 color = mix(secondaryColor.rgb * 0.31, primaryColor.rgb * 0.72,
                     smoothstep(-0.34, 0.91, dir.y));
    color += mix(primaryColor.rgb, accentColor.rgb, 0.56) * nebula * 0.42;
    color += accentColor.rgb * dustLane * 0.28;

    float fineStars = starField(dir, t, 62.0, 2.10);
    float brightStars = starField(dir, t, 27.0, 0.92);
    color += vec3(1.0) * (fineStars * 0.70 + brightStars * 0.62) * smoothstep(-0.08, 0.96, dir.y);
    color += accentColor.rgb * brightStars * 0.18;

    vec3 planetDir = normalize(vec3(-0.45, 0.61, 0.34));
    vec3 tangent = normalize(cross(vec3(0.0, 1.0, 0.0), planetDir));
    vec3 bitangent = normalize(cross(planetDir, tangent));
    float planetFocus = dot(dir, planetDir);
    float planet = smoothstep(0.9918, 0.9960, planetFocus);
    float limb = smoothstep(0.9918, 0.9944, planetFocus) - smoothstep(0.9966, 0.9994, planetFocus);
    float px = dot(dir, tangent);
    float py = dot(dir, bitangent);
    float ringRadius = length(vec2(px, py * 3.1));
    float ring = (1.0 - smoothstep(0.004, 0.011, abs(ringRadius - 0.145))) * (1.0 - planet);
    color += mix(primaryColor.rgb, accentColor.rgb, 0.68) * planet * 0.78;
    color += mix(accentColor.rgb, vec3(1.0), 0.72) * limb * 0.30;
    color += mix(accentColor.rgb, vec3(1.0), 0.52) * ring * 0.70;
    color += accentColor.rgb * pow(max(planetFocus, 0.0), 20.0) * 0.13;
    return color;
}

// Golden sunset: a low sun and wide, slowly drifting fire-tipped clouds.
vec3 renderSunset(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    float invScale = 1.0 / max(scale, 0.001);
    float up = clamp(dir.y, -0.35, 1.0);

    float horizon = exp(-abs(dir.y + 0.025) * 5.8);
    float high = smoothstep(-0.08, 0.84, up);
    vec3 color = mix(accentColor.rgb * 0.72, primaryColor.rgb * 0.72, high);
    color = mix(color, secondaryColor.rgb * 0.76, smoothstep(0.48, 1.0, up));
    color += accentColor.rgb * horizon * 0.34;

    vec3 sunDir = normalize(vec3(-0.54, 0.13, 0.48));
    float sunFocus = max(dot(dir, sunDir), 0.0);
    float sun = smoothstep(0.9902, 0.9964, sunFocus);
    float sunHalo = pow(sunFocus, 18.0);
    color += mix(accentColor.rgb, vec3(1.0), 0.68) * sun * 1.05;
    color += accentColor.rgb * sunHalo * 0.38;

    vec3 q = vec3(dir.x * 0.66, dir.y * 2.25, dir.z * 0.66) * (3.0 * invScale);
    q += vec3(t * 0.022, -t * 0.003, t * 0.014);
    float cloudA = fbm3(q);
    float cloudB = fbm3(q * 1.75 + vec3(-t * 0.016, t * 0.008, t * 0.010));
    float density = cloudA * 0.73 + cloudB * 0.32;
    float cloud = smoothstep(0.48, 0.70, density) * smoothstep(-0.18, 0.72, dir.y);
    float cloudCore = smoothstep(0.65, 0.90, density);
    float fireRim = cloud * (1.0 - cloudCore);
    vec3 cloudColor = mix(primaryColor.rgb * 0.66, secondaryColor.rgb * 0.70, cloudCore);
    cloudColor += accentColor.rgb * fireRim * (0.28 + sunHalo * 0.35);
    color = mix(color, cloudColor, cloud * 0.74);
    return color;
}

// Dreamscape: pastel silk ribbons, twin moons and a field of soft stars.
vec3 renderDreamscape(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    float invScale = 1.0 / max(scale, 0.001);
    vec2 plane = skyPlane(dir, 0.64) * invScale;
    vec3 domain = dir * (2.16 * invScale);

    float mistA = fbm3(domain * 0.66 + vec3(t * 0.010, -t * 0.007, t * 0.009));
    float mistB = fbm3(domain * 1.30 + vec3(-t * 0.014, t * 0.010, -t * 0.012));
    float silkA = softSkyBand(plane, 0.08, 0.30, 0.072, 2.2, t * 0.045);
    float silkB = softSkyBand(plane, 0.34, 0.38, 0.050, 3.7, -t * 0.036 + 1.4);
    float silkC = softSkyBand(plane, 0.58, 0.46, 0.032, 5.1, t * 0.027 + 2.6);

    vec3 color = mix(secondaryColor.rgb * 0.40, primaryColor.rgb * 0.78,
                     smoothstep(-0.30, 0.92, dir.y));
    float cloud = smoothstep(0.38, 0.90, mistA * 0.64 + mistB * 0.27 + silkA * 0.30);
    color += primaryColor.rgb * cloud * 0.28;
    color += mix(primaryColor.rgb, accentColor.rgb, 0.45) * silkA * 0.34;
    color += mix(accentColor.rgb, vec3(1.0), 0.48) * silkB * 0.18;
    color += primaryColor.rgb * silkC * 0.12;

    vec3 moonA = normalize(vec3(0.48, 0.70, 0.24));
    vec3 moonB = normalize(vec3(-0.61, 0.48, -0.28));
    float focusA = max(dot(dir, moonA), 0.0);
    float focusB = max(dot(dir, moonB), 0.0);
    float discA = smoothstep(0.9940, 0.9976, focusA);
    float discB = smoothstep(0.9964, 0.9984, focusB);
    color += mix(accentColor.rgb, vec3(1.0), 0.70) * discA * 0.86;
    color += mix(primaryColor.rgb, vec3(1.0), 0.62) * discB * 0.66;
    color += accentColor.rgb * pow(focusA, 17.0) * 0.16;
    color += primaryColor.rgb * pow(focusB, 24.0) * 0.12;

    float starMask = smoothstep(-0.04, 0.95, dir.y) * (1.0 - cloud * 0.35);
    color += vec3(1.0) * starField(dir, t, 52.0, 1.42) * starMask * 0.48;
    color += accentColor.rgb * starField(dir, t, 84.0, 2.10) * starMask * 0.17;
    return color;
}

// Emerald: an obsidian sky split by a rotating green cosmic vortex.
vec3 renderEmerald(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    float invScale = 1.0 / max(scale, 0.001);
    vec3 rd = rotateY(dir, t * 0.012);
    vec2 plane = skyPlane(rd, 0.60) * invScale;
    vec3 domain = rd * (2.70 * invScale);

    float mistA = fbm3(domain * 0.78 + vec3(t * 0.014, -t * 0.009, t * 0.012));
    float mistB = fbm3(domain * 1.62 + vec3(-t * 0.020, t * 0.013, -t * 0.016));
    float radius = length(plane);
    float angle = atan(plane.y, plane.x);
    float spiralWave = sin(angle * 3.0 - log(radius + 0.16) * 7.0 + t * 0.18);
    float spiral = smoothstep(0.18, 0.92, spiralWave * 0.5 + 0.5);
    spiral *= exp(-radius * 0.58) * smoothstep(-0.20, 0.96, dir.y);
    float plasma = smoothstep(0.39, 0.91, mistA * 0.58 + mistB * 0.26 + spiral * 0.48);
    float filaments = pow(saturate(1.0 - abs(mistB * 2.0 - 1.0)), 3.0) * plasma;

    vec3 color = mix(secondaryColor.rgb * 0.30, primaryColor.rgb * 0.48,
                     smoothstep(-0.36, 0.90, dir.y));
    color += primaryColor.rgb * plasma * 0.48;
    color += accentColor.rgb * spiral * 0.32;
    color += mix(accentColor.rgb, vec3(1.0), 0.46) * filaments * 0.20;

    float core = exp(-radius * radius * 8.0) * smoothstep(-0.06, 0.92, dir.y);
    color += accentColor.rgb * core * 0.44;
    color += vec3(1.0) * core * core * 0.24;

    float starsFine = starField(dir, t, 61.0, 1.85) * (1.0 - plasma * 0.52);
    float starsBright = starField(dir, t, 25.0, 0.90);
    color += vec3(1.0) * (starsFine * 0.56 + starsBright * 0.42) * smoothstep(-0.08, 0.96, dir.y);
    color += accentColor.rgb * starsBright * 0.22;
    return color;
}

// Mode 21 — GALAXY
vec3 renderGalaxy(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    float invScale = 1.0 / max(scale, 0.001);
    vec3 rd = rotateY(dir, t * 0.007);
    vec2 plane = skyPlane(rd, 0.55) * (0.95 * invScale);
    vec3 domain = rd * (2.6 * invScale);
    float up = smoothstep(-0.35, 0.92, dir.y);
    vec3 color = mix(secondaryColor.rgb * 0.30, secondaryColor.rgb * 0.10, up);
    color += primaryColor.rgb * pow(1.0 - abs(dir.y), 2.4) * 0.06;
    float bend = sin(plane.x * 1.15 + t * 0.02) * 0.18 + sin(plane.x * 2.7 - t * 0.014) * 0.06;
    float band = (1.0 - smoothstep(0.05, 0.95, abs(plane.y + bend))) * smoothstep(-0.30, 0.95, dir.y);
    float cloudsA = fbm3(domain * 0.85 + vec3(t * 0.010, -t * 0.006, t * 0.008));
    float cloudsB = fbm3(domain * 1.9 + vec3(-t * 0.012, t * 0.009, -t * 0.011));
    float milk = smoothstep(0.34, 0.95, cloudsA * 0.62 + cloudsB * 0.28 + band * 0.55);
    float dust = smoothstep(0.35, 0.62, cloudsB) * (1.0 - smoothstep(0.62, 0.85, cloudsB));
    float bandBody = clamp(band * (0.55 + milk * 0.75) - dust * band * 0.85, 0.0, 1.0);
    vec3 bandColor = mix(mix(primaryColor.rgb, vec3(0.72, 0.80, 1.0), 0.45),
                         mix(accentColor.rgb, vec3(1.0, 0.90, 0.78), 0.35),
                         smoothstep(0.2, 0.9, cloudsA));
    color += bandColor * bandBody * 0.62;
    vec3 coreDir = normalize(vec3(0.28, 0.42, -0.86));
    float coreFocus = max(dot(dir, coreDir), 0.0);
    color += mix(accentColor.rgb, vec3(1.0, 0.94, 0.82), 0.55) * pow(coreFocus, 6.0) * band * 0.85;
    color += vec3(1.0, 0.97, 0.90) * pow(coreFocus, 30.0) * 0.70;
    float starMask = smoothstep(-0.06, 0.98, dir.y);
    float bandBoost = 0.55 + band * 0.85;
    color += vec3(1.0) * (starField(dir, t, 70.0, 2.20) * 0.55 + starField(dir, t, 40.0, 1.40) * 0.40
            + starField(dir, t, 20.0, 0.85) * 0.60) * starMask * bandBoost;
    return color / (1.0 + color * 0.30);
}

// Mode 22 — TWILIGHT
vec3 renderTwilight(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    float invScale = 1.0 / max(scale, 0.001);
    float up = clamp(dir.y, -0.3, 1.0);
    vec3 sunDir = normalize(vec3(-0.48, 0.06, 0.52));
    float sunFocus = max(dot(dir, sunDir), 0.0);
    vec3 sky = mix(accentColor.rgb * 0.85, primaryColor.rgb * 0.70, smoothstep(-0.02, 0.5, up));
    sky = mix(sky, secondaryColor.rgb * 0.85, smoothstep(0.42, 1.0, up));
    float horizonBand = exp(-abs(dir.y + 0.02) * 4.6);
    float sunWash = pow(sunFocus, 2.2);
    vec3 color = sky + accentColor.rgb * horizonBand * (0.28 + sunWash * 0.55);
    color += mix(accentColor.rgb, vec3(1.0), 0.75) * smoothstep(0.9986, 0.99965, sunFocus) * 1.3;
    color += mix(accentColor.rgb, vec3(1.0), 0.4) * pow(sunFocus, 40.0) * 0.55;
    color += accentColor.rgb * pow(sunFocus, 6.0) * 0.30;
    vec2 sp = skyPlane(sunDir, 0.6);
    vec2 pp = skyPlane(dir, 0.6);
    vec2 rel = pp - sp;
    float ang = atan(rel.y, rel.x);
    float rays = (0.5 + 0.5 * sin(ang * 16.0 + t * 0.25)) * (0.5 + 0.5 * sin(ang * 7.0 - t * 0.15 + 1.3));
    color += accentColor.rgb * rays * pow(sunFocus, 3.5) * smoothstep(-0.05, 0.35, dir.y) * 0.30;
    vec3 q = vec3(dir.x * 0.62, dir.y * 2.3, dir.z * 0.62) * (2.9 * invScale) + vec3(t * 0.020, -t * 0.004, t * 0.013);
    float density = fbm3(q) * 0.72 + fbm3(q * 1.8 + vec3(-t * 0.015, t * 0.007, t * 0.009)) * 0.33;
    float cloud = smoothstep(0.46, 0.68, density) * smoothstep(-0.16, 0.78, dir.y);
    float cloudCore = smoothstep(0.64, 0.9, density);
    vec3 cloudColor = mix(primaryColor.rgb * 0.55, secondaryColor.rgb * 0.6, cloudCore);
    cloudColor += accentColor.rgb * cloud * (1.0 - cloudCore) * (0.35 + sunWash * 0.6);
    color = mix(color, cloudColor, cloud * 0.72);
    color += vec3(1.0) * starField(dir, t, 46.0, 1.4) * smoothstep(0.35, 0.95, dir.y) * (1.0 - cloud * 0.6) * smoothstep(-0.08, 0.84, up) * 0.30;
    return color;
}

// Mode 23 — FROST
vec3 renderFrost(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    float invScale = 1.0 / max(scale, 0.001);
    vec2 plane = skyPlane(dir, 0.60) * invScale;
    vec3 domain = dir * (2.5 * invScale);
    vec3 sunDir = normalize(vec3(0.30, 0.55, 0.30));
    float sunFocus = max(dot(dir, sunDir), 0.0);
    float up = smoothstep(-0.30, 0.92, dir.y);
    vec3 color = mix(secondaryColor.rgb * 0.70, primaryColor.rgb * 0.72, up);
    float haze = fbm3(domain * 0.9 + vec3(t * 0.010, -t * 0.005, t * 0.008));
    float mist = smoothstep(0.35, 0.9, haze * 0.65 + fbm3(domain * 1.7) * 0.3);
    color += mix(primaryColor.rgb, vec3(1.0), 0.4) * mist * 0.14;
    color += vec3(1.0) * smoothstep(0.9982, 0.9994, sunFocus) * 1.15;
    color += mix(accentColor.rgb, vec3(1.0), 0.6) * pow(sunFocus, 30.0) * 0.45;
    float ringPos = abs(sunFocus - 0.927);
    float halo = (1.0 - smoothstep(0.0, 0.020, ringPos)) * smoothstep(-0.05, 0.3, dir.y);
    color += mix(vec3(1.0, 0.82, 0.62), mix(accentColor.rgb, vec3(0.7, 0.85, 1.0), 0.5),
                 smoothstep(-0.010, 0.010, sunFocus - 0.927)) * halo * 0.32;
    float pillar = exp(-abs(plane.x - skyPlane(sunDir, 0.60).x * invScale) * 26.0)
                * smoothstep(-0.05, 0.5, dir.y) * (0.4 + pow(sunFocus, 30.0) * 0.6);
    color += mix(accentColor.rgb, vec3(1.0), 0.5) * pillar * 0.16;
    vec2 iceUv = plane * 9.0 + vec2(t * 0.04, -t * 0.02);
    vec2 icell = floor(iceUv);
    float iseed = hash3(vec3(icell, 5.0));
    float glint = step(0.86, iseed) * exp(-dot(fract(iceUv) - 0.5, fract(iceUv) - 0.5) * 140.0)
                * (0.5 + 0.5 * sin(t * 3.0 + iseed * 60.0));
    color += vec3(1.0) * glint * 0.7 * smoothstep(-0.05, 0.6, dir.y);
    color += vec3(0.9, 0.95, 1.0) * starField(dir, t, 58.0, 2.4) * smoothstep(0.1, 0.95, dir.y) * (1.0 - mist * 0.4) * 0.34;
    return color;
}

// Mode 24 — MIRAGE
vec3 renderMirage(vec3 direction, float t) {
    vec3 dir = normalize(direction);
    float invScale = 1.0 / max(scale, 0.001);
    float up = clamp(dir.y, -0.25, 1.0);
    vec3 sunDir = normalize(vec3(0.16, 0.72, -0.24));
    float sunFocus = max(dot(dir, sunDir), 0.0);
    float heat = 1.0 - smoothstep(-0.05, 0.35, dir.y);
    float shimmer = sin(dir.x * 40.0 + t * 3.2) * 0.006 + sin(dir.z * 33.0 - t * 2.4) * 0.006;
    vec3 wdir = normalize(dir + vec3(0.0, shimmer * heat, 0.0));
    float high = smoothstep(-0.04, 0.95, up);
    vec3 hotHorizon = mix(accentColor.rgb, vec3(1.0, 0.96, 0.86), 0.45);
    vec3 color = mix(hotHorizon, primaryColor.rgb * 0.9, high);
    color = mix(color, secondaryColor.rgb * 0.85, smoothstep(0.55, 1.0, up));
    float glareBand = exp(-abs(dir.y - 0.01) * 7.0) * (0.8 + 0.2 * sin(dir.x * 60.0 + t * 4.0));
    color += mix(accentColor.rgb, vec3(1.0), 0.6) * glareBand * 0.30;
    color += vec3(1.0, 0.98, 0.92) * smoothstep(0.9990, 0.99975, sunFocus) * 1.5;
    color += mix(accentColor.rgb, vec3(1.0), 0.7) * pow(sunFocus, 60.0) * 0.7;
    color += accentColor.rgb * pow(sunFocus, 4.0) * 0.35;
    vec3 q = vec3(wdir.x * 0.7, wdir.y * 2.1, wdir.z * 0.7) * (3.2 * invScale) + vec3(t * 0.016, -t * 0.003, t * 0.011);
    float wispDen = fbm3(q) * 0.7 + fbm3(q * 1.9) * 0.32;
    float wisp = smoothstep(0.52, 0.72, wispDen) * smoothstep(-0.05, 0.85, dir.y);
    color = mix(color, mix(vec3(1.0, 0.93, 0.80), accentColor.rgb, 0.35), wisp * 0.32);
    float lum = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(color, vec3(lum), 0.10);
    return color / (1.0 + color * 0.22);
}


void main() {
    vec3 dir = normalize(skyDirection);
    float t = time;

    vec3 color;
    if (mode == 1) {
        color = renderLiquid(dir, t);
    } else if (mode == 2) {
        color = renderVoid(dir, t);
    } else if (mode == 3) {
        color = renderMeteors(dir, t);
    } else if (mode == 4) {
        color = renderSolar(dir, t);
    } else if (mode == 5) {
        color = renderBloom(dir, t);
    } else if (mode == 6) {
        color = renderVeil(dir, t);
    } else if (mode == 7) {
        color = renderHalo(dir, t);
    } else if (mode == 8) {
        color = renderRift(dir, t);
    } else if (mode == 9) {
        color = renderEclipse(dir, t);
    } else if (mode == 10) {
        color = renderStars(dir, t);
    } else if (mode == 11) {
        color = renderAether(dir, t);
    } else if (mode == 12) {
        color = renderPearl(dir, t);
    } else if (mode == 13) {
        color = renderCaustic(dir, t);
    } else if (mode == 14) {
        color = renderNebula(dir, t);
    } else if (mode == 15) {
        color = renderStorm(dir, t);
    } else if (mode == 16) {
        color = renderSakura(dir, t);
    } else if (mode == 17) {
        color = renderCelestial(dir, t);
    } else if (mode == 18) {
        color = renderSunset(dir, t);
    } else if (mode == 19) {
        color = renderDreamscape(dir, t);
    } else if (mode == 20) {
        color = renderEmerald(dir, t);
    } else if (mode == 21) {
        color = renderGalaxy(dir, t);
    } else if (mode == 22) {
        color = renderTwilight(dir, t);
    } else if (mode == 23) {
        color = renderFrost(dir, t);
    } else if (mode == 24) {
        color = renderMirage(dir, t);
    } else {
        color = renderAurora(dir, t);
    }

    if (mode == 15 || mode == 18 || mode == 22 || mode == 24) {
        // Storm/Sunset/Twilight/Mirage build their own horizon gradient.
    } else if (mode >= 5) {
        // Jabalin post: soft horizon lift + a light zenith vignette
        float horizon = smoothstep(-0.45, 0.78, dir.y);
        color = mix(color * 0.54, color, horizon);
        color *= 0.82 + smoothstep(-0.12, 0.95, dir.y) * 0.18;
    } else {
        // ESS post: gentle dimming below the horizon so the dome grounds into fog
        float ground = smoothstep(-0.5, 0.1, dir.y);
        color = mix(color * 0.45, color, ground);
    }

    // Fog has one environment color, so converge to that same palette color at
    // the horizon instead of exposing the vanilla sky through distant terrain.
    color = mix(horizonColor.rgb, color, smoothstep(-0.10, 0.22, dir.y));
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
