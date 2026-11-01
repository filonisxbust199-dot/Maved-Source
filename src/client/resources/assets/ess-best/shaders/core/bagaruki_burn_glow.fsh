#version 150
#moj_import <ess-best:hands_item_color.glsl>

// Nightix burn_glow — soft fire halo around the hand silhouette (mask + hand color).
uniform sampler2D HandSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform BurnData {
    vec4 ResTime;   // width, height, time, fillMode
    vec4 Flame;     // radiusPx, strength, flameSpeed, colorMix
    vec4 Trail;     // decay, flameHeightPx, flowSpeed, trailStrength
    vec4 FireColor; // rgb, glowStrength
};

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
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
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = p * 2.11 + vec2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

// Soft distance-to-silhouette field in pixels (multi-ring search).
float fieldDist(vec2 uv, float maxR, out float centerMask, out vec2 sourceUv) {
    vec2 px = 1.0 / max(ResTime.xy, vec2(1.0));
    centerMask = texture(MaskSampler, uv).a;
    sourceUv = uv;
    if (centerMask > 0.55) return 0.0;

    float best = maxR + 2.0;
    int rings = 6;
    for (int r = 1; r <= 6; r++) {
        float rr = maxR * (float(r) / float(rings));
        int taps = 12;
        for (int i = 0; i < 12; i++) {
            float ang = (float(i) + float(r) * 0.37) * 0.5235987756;
            vec2 o = vec2(cos(ang), sin(ang)) * rr * px;
            vec2 sampleUv = uv + o;
            if (any(lessThan(sampleUv, vec2(0.0))) || any(greaterThan(sampleUv, vec2(1.0)))) continue;
            float m = texture(MaskSampler, sampleUv).a;
            if (m > 0.35 && rr < best) { best = rr; sourceUv = sampleUv; }
        }
    }
    return best;
}

void main() {
    float time = ResTime.z;
    float fill = ResTime.w;
    float radiusPx = max(Flame.x, 2.0);
    float strength = Flame.y;
    float speed = max(Flame.z, 0.05);
    float colorMix = clamp(Flame.w, 0.0, 1.0);
    float heightPx = max(Trail.y, 4.0);
    float glowStr = max(FireColor.w, 0.05);

    float centerMask;
    vec2 sourceUv;
    float dist = fieldDist(texCoord, radiusPx, centerMask, sourceUv);

    // inside silhouette: only when burn-through is on
    float inside = centerMask * fill;
    float outside = 1.0 - smoothstep(0.0, radiusPx, dist);
    outside = pow(outside, 1.35);
    float shell = max(outside * (1.0 - centerMask * (1.0 - fill * 0.15)), inside);
    if (shell <= 0.004) discard;

    // flame noise rises in screen-Y (hands sit lower → fire climbs up)
    vec2 nUv = texCoord * ResTime.xy * 0.045;
    nUv.y -= time * speed * 1.85;
    nUv.x += sin(texCoord.y * 18.0 + time * speed * 2.4) * 0.35;

    float n1 = fbm(nUv);
    float n2 = fbm(nUv * 2.1 + vec2(3.1, time * speed * 0.7));
    float tongues = pow(clamp(n1 * 0.7 + n2 * 0.45, 0.0, 1.0), 1.4);

    // vertical tongue stretch above the silhouette
    float lift = exp(-max(0.0, dist) / max(heightPx * 0.35, 1.0));
    float flame = shell * (0.35 + tongues * 0.9) * (0.55 + lift * 0.7);
    flame = clamp(flame * strength * glowStr, 0.0, 1.5);

    float core = smoothstep(0.55, 1.2, flame);
    float mid = smoothstep(0.2, 0.75, flame);

    vec3 deep = vec3(0.55, 0.04, 0.01);
    vec3 orange = vec3(1.0, 0.38, 0.04);
    vec3 yellow = vec3(1.0, 0.85, 0.22);
    vec3 white = vec3(1.0, 0.97, 0.78);
    vec3 fire = mix(deep, orange, mid);
    fire = mix(fire, yellow, core * 0.75);
    fire = mix(fire, white, pow(core, 2.0) * 0.55);

    // hand/item color tint
    vec4 hand = texture(HandSampler, sourceUv);
    vec3 itemCol = hand.a > 0.001 ? handsItemColor(hand) : FireColor.rgb;
    // Match reference color semantics: item mode takes the item hue, custom mode overrides it.
    fire = mix(itemCol, FireColor.rgb, colorMix) * (0.85 + core * 0.15);

    float a = clamp(flame, 0.0, 1.0);
    // premultiplied-ish for later additive composite
    fragColor = vec4(fire * a, a);
}
