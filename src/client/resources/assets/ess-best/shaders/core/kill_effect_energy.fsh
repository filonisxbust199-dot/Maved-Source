#version 150

// KillEffect "Энергия": the held item itself is never transformed.  Every flame
// and ribbon is grown from the captured main-hand silhouette, so movement of the camera or a
// custom ViewModel cannot detach the effect from the item.
uniform sampler2D BloomSampler;
uniform sampler2D HandsSampler;

layout(std140) uniform KillEnergySettings {
    vec4 ColorA;       // rgb: current accent, w: strength
    vec4 ColorB;       // rgb: secondary accent, w: time
    vec4 EnergyParams; // x: timeline, y: hand side, z: aspect, w: rainbow mode
    vec4 TexelParams;  // xy: full-resolution texel
};

in vec2 texCoord;
out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise21(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    value += noise21(p) * 0.56;
    p = p * 2.03 + 11.7;
    value += noise21(p) * 0.28;
    p = p * 2.01 + 7.3;
    value += noise21(p) * 0.16;
    return value;
}

float bloomAt(vec2 uv) {
    if (uv.x <= 0.001 || uv.x >= 0.999 || uv.y <= 0.001 || uv.y >= 0.999) return 0.0;
    return sqrt(clamp(texture(BloomSampler, uv).a, 0.0, 1.0));
}

float stage(float begin, float full, float fadeBegin, float end, float p) {
    return smoothstep(begin, full, p) * (1.0 - smoothstep(fadeBegin, end, p));
}

vec3 rainbowFire(float phase) {
    phase = fract(phase);
    vec3 blue = vec3(0.04, 0.42, 1.00);
    vec3 cyan = vec3(0.04, 0.94, 1.00);
    vec3 green = vec3(0.08, 1.00, 0.30);
    vec3 gold = vec3(1.00, 0.88, 0.08);
    vec3 orange = vec3(1.00, 0.25, 0.025);
    if (phase < 0.25) return mix(blue, cyan, phase * 4.0);
    if (phase < 0.50) return mix(cyan, green, (phase - 0.25) * 4.0);
    if (phase < 0.75) return mix(green, gold, (phase - 0.50) * 4.0);
    return mix(gold, orange, (phase - 0.75) * 4.0);
}

void main() {
    float p = EnergyParams.x;
    float side = EnergyParams.y;
    float rainbowMode = EnergyParams.w;
    float time = ColorB.w;
    float strength = ColorA.w;
    float handMask = texture(HandsSampler, texCoord).a;
    float baseBloom = bloomAt(texCoord);
    float outside = 1.0 - smoothstep(0.02, 0.72, handMask);

    float ignite = stage(0.0, 0.035, 0.20, 0.34, p);
    float flameStage = stage(0.08, 0.18, 0.84, 0.98, p);
    float envelope = smoothstep(0.0, 0.025, p) * (1.0 - smoothstep(0.975, 1.0, p));

    // Animated fire tongues. They are directional dilations of the real item bloom, not a
    // guessed screen coordinate, and therefore remain attached even with custom hand offsets.
    float fire = 0.0;
    for (int i = 1; i <= 11; i++) {
        float f = float(i) / 11.0;
        float wobble = sin(time * 7.2 + texCoord.x * 42.0 + f * 9.0) * (0.003 + f * 0.005);
        wobble += (noise21(vec2(texCoord.x * 34.0 + time * 2.2, f * 13.0)) - 0.5) * 0.006;
        vec2 q = texCoord - vec2(wobble * side, f * 0.085);
        float tongue = bloomAt(q) * (1.0 - f) * (0.64 + fbm(q * 15.0 - vec2(0.0, time * 3.0)) * 0.65);
        fire = max(fire, tongue);
    }
    fire = max(0.0, fire - baseBloom * 0.12) * outside;

    // Tight ignition halo and small dancing embers around the silhouette.
    float shellNoise = fbm(texCoord * vec2(28.0, 18.0) + vec2(time * 1.9, -time * 3.4));
    float shell = baseBloom * outside * (0.42 + shellNoise * 0.92);
    vec2 sparkCell = floor(texCoord / max(TexelParams.xy * 4.0, vec2(0.0025)));
    float sparkRnd = hash21(sparkCell + floor(time * 18.0));
    float sparks = step(0.986, sparkRnd) * smoothstep(0.025, 0.22, baseBloom) * outside;

    vec3 themeTint = mix(ColorA.rgb, ColorB.rgb, clamp(texCoord.y, 0.0, 1.0));
    float firePhase = texCoord.x * 1.15 - texCoord.y * 0.82 - time * 0.24;
    vec3 fireColor = mix(themeTint, rainbowFire(firePhase), rainbowMode);
    vec3 themeHot = mix(ColorA.rgb, ColorB.rgb, shellNoise);
    vec3 rainbowHot = mix(vec3(1.0, 0.18, 0.015), vec3(1.0, 0.82, 0.10), shellNoise);
    vec3 hotColor = mix(themeHot, rainbowHot, rainbowMode);

    vec3 light = hotColor * shell * (ignite * 1.35 + flameStage * 0.22);
    light += fireColor * fire * (flameStage + ignite * 0.55) * 1.42;
    vec3 sparkTheme = mix(ColorA.rgb, ColorB.rgb, sparkRnd);
    vec3 sparkRainbow = mix(vec3(0.15, 0.75, 1.0), vec3(1.0, 0.72, 0.08), sparkRnd);
    light += mix(sparkTheme, sparkRainbow, rainbowMode) * sparks * flameStage * 0.72;
    light *= strength * envelope;

    if (dot(light, light) <= 1.0e-7) discard;
    fragColor = vec4(light, 0.0);
}
