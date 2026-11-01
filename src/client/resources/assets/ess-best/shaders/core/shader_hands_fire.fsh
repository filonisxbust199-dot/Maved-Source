#version 150

uniform sampler2D BeforeSampler;
uniform sampler2D AfterSampler;

layout(std140) uniform HandsSettings {
    vec4 HandsParams0; // texel.xy, time, alpha
    vec4 HandsParams1; // speed, scale, intensity, diff threshold
};

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.55;
    for (int i = 0; i < 5; i++) {
        value += noise(p) * amplitude;
        p = p * 2.04 + vec2(7.1, 3.8);
        amplitude *= 0.5;
    }
    return value;
}

float handMask(vec2 uv) {
    vec2 texel = HandsParams0.xy;
    float threshold = HandsParams1.w;
    float mask = 0.0;

    vec2 offsets[9] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(-1.0, 0.0),
        vec2(0.0, 1.0),
        vec2(0.0, -1.0),
        vec2(1.0, 1.0),
        vec2(-1.0, -1.0),
        vec2(1.0, -1.0),
        vec2(-1.0, 1.0)
    );

    for (int i = 0; i < 9; i++) {
        vec2 tapUv = uv + offsets[i] * texel * 1.25;
        vec3 beforeColor = texture(BeforeSampler, tapUv).rgb;
        vec3 afterColor = texture(AfterSampler, tapUv).rgb;
        float diff = length(afterColor - beforeColor);
        mask = max(mask, smoothstep(threshold, threshold * 5.5, diff));
    }

    return mask;
}

void main() {
    float mask = handMask(texCoord);
    if (mask <= 0.001) {
        discard;
    }

    float time = HandsParams0.z * HandsParams1.x;
    float scale = HandsParams1.y;
    float intensity = HandsParams1.z;
    float alpha = HandsParams0.w;

    vec2 uv = texCoord;
    vec2 flameUv = vec2(uv.x * 4.0, (1.0 - uv.y) * 5.5) * scale;
    flameUv.y += time * 1.65;

    float warpA = fbm(flameUv * 0.65 + vec2(0.0, time * 0.30));
    float warpB = fbm(flameUv * 1.15 + vec2(4.2, -time * 0.18));
    vec2 warped = flameUv + vec2(warpA - 0.5, warpB - 0.5) * 1.7;

    float tongues = fbm(warped + vec2(0.0, time * 0.55));
    float streaks = 1.0 - abs(sin(warped.x * 2.4 + warped.y * 0.72 + time * 2.1 + tongues * 3.5));
    streaks = pow(clamp(streaks, 0.0, 1.0), 4.0);

    float verticalHeat = smoothstep(0.02, 0.88, 1.0 - uv.y);
    float flame = clamp(tongues * 0.70 + streaks * 0.75 + verticalHeat * 0.20, 0.0, 1.0);
    float core = smoothstep(0.62, 1.0, flame);

    vec3 deep = vec3(0.95, 0.12, 0.02);
    vec3 orange = vec3(1.0, 0.43, 0.04);
    vec3 yellow = vec3(1.0, 0.88, 0.22);
    vec3 white = vec3(1.0, 0.97, 0.72);

    vec3 fire = mix(deep, orange, smoothstep(0.08, 0.62, flame));
    fire = mix(fire, yellow, smoothstep(0.45, 0.86, flame));
    fire = mix(fire, white, core * 0.55);

    float edgeGlow = pow(mask, 1.8) * (0.45 + core * 0.55);
    float outAlpha = clamp(alpha * intensity * edgeGlow, 0.0, 1.0);
    if (outAlpha <= 0.001) {
        discard;
    }

    fragColor = vec4(fire, outAlpha);
}
