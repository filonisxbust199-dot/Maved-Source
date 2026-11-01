#version 330
#extension GL_ARB_separate_shader_objects : require

const int MAX_IMPACTS = 6;

uniform sampler2D MaskSampler;

layout(std140) uniform ImpactNova {
    vec4 Params;
    vec4 Color;
    vec4 Screen;
    vec4 Impacts[MAX_IMPACTS];
    vec4 Hits[MAX_IMPACTS];
    vec4 Timings[MAX_IMPACTS];
};

layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 fragColor;

float targetMask(vec2 uv) {
    vec4 sampleColor = texture(MaskSampler, uv);
    vec3 normalized = sampleColor.rgb / max(sampleColor.a, 0.01);
    float marker = step(0.72, normalized.r)
        * step(normalized.g, 0.28)
        * step(0.65, normalized.b);
    return sampleColor.a * marker;
}

float hash1(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash1(cell);
    float b = hash1(cell + vec2(1.0, 0.0));
    float c = hash1(cell + vec2(0.0, 1.0));
    float d = hash1(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    return noise(p) * 0.55
        + noise(p * 2.07 + 11.3) * 0.30
        + noise(p * 4.11 + 23.7) * 0.15;
}

float impulse(vec2 position, float progress, float age, float growth, float seed) {
    vec2 offset = vec2(seed * 13.7, seed * 7.9);
    float radius = length(position);
    float angle = atan(position.y, position.x);

    float front = 0.10 + growth * 1.02;
    float thickness = 0.10 + growth * 0.14;
    float ring = (1.0 - smoothstep(0.0, thickness, abs(radius - front)))
        * (1.0 - smoothstep(0.35, 0.95, progress));

    float swept = 1.0 - smoothstep(front * 0.30, front + thickness, radius);
    float ridged = 1.0 - abs(fbm(position * 3.0 + offset - vec2(0.0, age * 0.75)) * 2.0 - 1.0);
    float filaments = pow(clamp(ridged, 0.0, 1.0), 3.5);

    float curl = 0.60 + 0.40 * sin(angle * 4.0 + radius * 3.4 + seed * 6.28318 - age * 1.3);
    float veins = mix(1.0, curl, smoothstep(0.12, 0.55, radius));
    float body = swept * veins * (0.16 + filaments * 0.95);

    float bloom = (1.0 - smoothstep(0.0, 0.34 + growth * 0.16, radius))
        * (1.0 - smoothstep(0.0, 0.30, progress));

    return clamp(ring * 0.75 + body * 0.85 + bloom * 0.90, 0.0, 1.0);
}

float burn(vec2 position, float progress, float age, float growth, float seed) {
    vec2 offset = vec2(seed * 21.3, seed * 5.7);
    float radius = length(position);

    float edge = (fbm(position * 1.9 + offset) - 0.5) * 0.46
        + (fbm(position * 4.3 + offset + age * 0.18) - 0.5) * 0.14;
    float reach = 0.06 + smoothstep(0.0, 0.62, progress) * 1.20;
    float distanceToFront = radius + edge - reach;

    float rim = 1.0 - smoothstep(0.0, 0.08 + growth * 0.05, abs(distanceToFront));
    rim *= 0.62 + 0.38 * fbm(position * 6.5 + offset + vec2(age * 0.6, 0.0));
    float inside = 1.0 - smoothstep(-0.04, 0.34, distanceToFront);

    float emberField = fbm(position * 5.4 + offset - vec2(0.0, age * 0.85));
    float embers = smoothstep(0.58, 0.86, emberField) * inside;
    float smoulder = inside * (0.16 + 0.14 * fbm(position * 2.6 + offset + age * 0.3));

    float bloom = (1.0 - smoothstep(0.0, 0.30, radius))
        * (1.0 - smoothstep(0.0, 0.26, progress));

    float cooling = smoothstep(0.5, 1.0, progress) * (1.0 - smoothstep(reach * 0.25, reach, radius));

    return clamp((rim * 0.95 + embers * 0.55 + smoulder) * (1.0 - cooling * 0.85) + bloom * 0.80,
        0.0, 1.0);
}

vec2 toEntityLocal(vec2 uv, vec2 center, float blockScale) {
    float scale = max(blockScale, 1e-5);
    float aspect = Screen.x / max(Screen.y, 1.0);
    vec2 delta = uv - center;
    return vec2(delta.x * aspect, delta.y) / scale;
}

void main() {
    float mask = targetMask(TexCoord);
    if (mask <= 0.002) discard;

    int style = int(Params.x + 0.5);
    int count = min(int(Params.z + 0.5), MAX_IMPACTS);
    float size = max(Params.w, 0.05);

    float energy = 0.0;

    for (int index = 0; index < count; index++) {
        vec4 impact = Impacts[index];
        vec4 hit = Hits[index];
        vec4 timing = Timings[index];
        if (timing.z <= 0.002 || impact.z <= 1e-6) continue;

        vec2 local = toEntityLocal(TexCoord, impact.xy, impact.z);
        vec2 position = (local - hit.xy) / size;
        float locality = 1.0 - smoothstep(0.65, 1.45, length(position));
        if (locality <= 0.0) continue;

        float pattern;
        if (style == 1) {
            pattern = burn(position, timing.x, timing.y, timing.w, impact.w);
        } else {
            pattern = impulse(position, timing.x, timing.y, timing.w, impact.w);
        }

        energy = max(energy, pattern * locality * timing.z);
    }

    float alpha = clamp(mask * energy * Params.y, 0.0, 1.0);
    if (alpha <= 0.003) discard;

    vec3 tint = mix(Color.rgb * 0.45, Color.rgb, smoothstep(0.0, 0.50, energy));
    tint = mix(tint, vec3(1.0), smoothstep(0.68, 1.0, energy) * 0.55);
    tint *= 1.0 + energy * 0.50;

    fragColor = vec4(tint, alpha);
}
