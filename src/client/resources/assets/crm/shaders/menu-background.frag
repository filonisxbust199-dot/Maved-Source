#version auto

precision highp float;

#include<shaderColor>

#uniforms {
    vec2 Resolution;
    float Time;
    int PreviousStyle;
    int Style;
    float Transition;
    float Shade;
    vec4 ColorA;
    vec4 ColorB;
    vec4 ThemeBase;
    vec4 ThemeAccent;
};

out vec4 fragColor;

float essMenuHash12(vec2 p) {
    // Integer lattice hashing avoids amplifying backend-specific sin rounding.
    uvec2 cell = uvec2(ivec2(p));
    uint hash = cell.x * 1597334677u ^ cell.y * 3812015801u;
    hash = (hash ^ (hash >> 16u)) * 2246822519u;
    hash = (hash ^ (hash >> 13u)) * 3266489917u;
    hash ^= hash >> 16u;
    return float(hash & 0x00ffffffu) / 16777216.0;
}

float essMenuNoise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = essMenuHash12(i);
    float b = essMenuHash12(i + vec2(1.0, 0.0));
    float c = essMenuHash12(i + vec2(0.0, 1.0));
    float d = essMenuHash12(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float essMenuFbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    mat2 m = mat2(1.7, -1.2, 1.2, 1.7);
    for (int i = 0; i < 4; i++) {
        sum += amp * essMenuNoise2(p);
        p = m * p + vec2(13.1, 7.7);
        amp *= 0.5;
    }
    return sum;
}

vec3 essMenuLiquidBackground(vec2 uv, float aspect) {
    float time = Time * 0.75;
    float intensity = 0.55;
    float scale = 0.22;

    vec2 p = uv - 0.5;
    p.x *= aspect;
    p *= scale;

    float flow = essMenuFbm(p * 4.0 + vec2(time * 0.25, -time * 0.18));
    float detail = essMenuFbm(p * 7.0 - vec2(time * 0.16, time * 0.10));
    float ripple = sin(p.x * 12.0 + time * 1.2) + cos(p.y * 10.0 - time * 1.0);
    float wave = 0.5 + 0.5 * sin(ripple + flow * 3.0 + detail * 1.8);

    vec3 low = ColorA.rgb * 0.30;
    vec3 mid = mix(ColorA.rgb, ColorB.rgb * 0.58, clamp(flow, 0.0, 1.0));
    vec3 high = ColorB.rgb;

    vec3 liquid = mix(low, mid, clamp(flow * 1.15, 0.0, 1.0));
    liquid = mix(liquid, high, smoothstep(0.52, 1.0, wave) * (0.55 + intensity * 0.20));

    float foam = smoothstep(0.80, 1.0, wave);
    float glow = smoothstep(0.55, 1.0, detail) * 0.18 * intensity;
    liquid += ColorB.rgb * foam * 0.22;
    liquid += vec3(0.08, 0.12, 0.18) * glow;

    float topLight = pow(1.0 - uv.y, 2.2) * 0.10;
    liquid += ColorB.rgb * topLight * 0.18;

    return liquid;
}

vec3 essMenuAuroraBackground(vec2 p) {
    float time = Time * 0.18;
    vec3 color = ColorA.rgb * 0.32;
    vec3 tint = mix(ColorB.rgb, ThemeAccent.rgb, 0.28);
    float mist = essMenuFbm(p * 2.3 + vec2(time * 0.16, 0.0));
    color += ColorB.rgb * mist * 0.12;

    // Long, drifting curtains with brighter folds along their lower edge.
    for (int i = 0; i < 3; i++) {
        float layer = float(i);
        float x = p.x * (1.6 + layer * 0.3) + time * (0.16 + layer * 0.04);
        float edge = -0.08 + layer * 0.16 + sin(x + layer * 2.1) * 0.16
                + sin(x * 2.7 - time + layer) * 0.035;
        float distance = p.y - edge;
        float folds = 0.55 + 0.45 * sin(x * 13.0 + sin(x * 3.0) * 2.0 + time);
        float curtain = exp(-abs(distance) * 8.0) * smoothstep(-0.2, 0.04, distance);
        float rim = exp(-abs(distance) * 65.0);
        vec3 layerTint = mix(tint, mix(ThemeAccent.rgb, vec3(1.0), 0.10), layer * 0.28);
        color += layerTint * (curtain * folds * 0.50 + rim * 0.18);
    }

    vec2 starUv = p * 58.0;
    vec2 cell = floor(starUv);
    vec2 center = vec2(essMenuHash12(cell + 4.0), essMenuHash12(cell + 9.0));
    float star = 1.0 - smoothstep(0.02, 0.10, length(fract(starUv) - center));
    star *= step(0.975, essMenuHash12(cell)) * (0.6 + 0.4 * sin(Time * 0.8 + essMenuHash12(cell) * 23.0));
    color += mix(ColorB.rgb, vec3(0.8, 0.9, 1.0), 0.6) * star * 0.32;
    return color;
}

vec3 essMenuWaveBackground(vec2 p) {
    float time = Time * 0.24;
    vec3 color = ColorA.rgb * 0.28;
    vec3 tint = mix(ColorB.rgb, ThemeAccent.rgb, 0.25);
    color += tint * exp(-abs(p.y + p.x * 0.28) * 3.0) * 0.12;

    // Separate luminous strands: thin cores, broad bloom, and travelling pulses.
    for (int i = 0; i < 6; i++) {
        float layer = float(i);
        float x = p.x * 2.2 + layer * 0.35;
        float line = (layer - 2.5) * 0.105 - p.x * 0.28
                + sin(x + time + layer * 0.52) * 0.15
                + sin(x * 1.8 - time * 0.7 + layer) * 0.025;
        float distance = abs(p.y - line);
        float pulse = 0.35 + 0.65 * pow(0.5 + 0.5 * sin(p.x * 3.0 - time * 2.0 + layer), 3.0);
        vec3 strandTint = mix(tint, mix(ThemeAccent.rgb, vec3(1.0), 0.10), layer / 7.0);
        color += strandTint * (exp(-distance * 230.0) * 0.65 + exp(-distance * 38.0) * 0.20) * pulse;
    }
    return color;
}

vec3 essMenuPearlBackground(vec2 p) {
    vec3 color = ThemeBase.rgb * 0.40;
    vec3 tint = mix(ThemeAccent.rgb, vec3(0.60), 0.12);
    for (int i = 0; i < 4; i++) {
        float layer = float(i);
        float distance = p.y + p.x * 0.62 + (layer - 1.5) * 0.30
                + sin(p.x * 2.3 + Time * 0.16 + layer) * 0.10;
        float fold = exp(-abs(distance) * 9.0)
                * (0.5 + 0.5 * sin(p.x * 1.5 + Time * 0.09 + layer * 1.7));
        float sheen = exp(-abs(distance + 0.025) * 60.0);
        color += tint * (fold * 0.20 + sheen * 0.07);
    }
    return color;
}

float essMenuContourNoise(vec2 p) {
    float sum = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += amplitude * essMenuNoise2(p);
        p = mat2(1.7, -1.2, 1.2, 1.7) * p + 7.8;
        amplitude *= 0.5;
    }
    return sum;
}

vec3 essMenuContourBackground(vec2 p) {
    vec3 color = ThemeBase.rgb * 0.48;
    float field = essMenuContourNoise(p * 1.6 + vec2(Time * 0.025, -Time * 0.018)) * 4.0
            + p.x * 0.7 + p.y * 0.2;
    float band = abs(fract(field * 6.0) - 0.5);
    float width = max(fwidth(field * 6.0), 0.002);
    float line = 1.0 - smoothstep(0.015, 0.015 + width * 1.3, band);
    float glow = exp(-band * 25.0);
    float glint = pow(0.5 + 0.5 * sin(field * 3.0 - Time * 0.3), 3.0);
    color += ThemeAccent.rgb * (line * (0.09 + glint * 0.06) + glow * 0.035);
    return color;
}

vec3 essMenuBackground(int style, vec2 uv, vec2 p, float aspect) {
    vec3 color;
    if (style == 2 || style == 4) {
        if (style == 2) color = essMenuContourBackground(p);
        else color = essMenuPearlBackground(p);
        // These are the approved reference shaders: quiet centre, detail around the controls.
        float clearCenter = 1.0 - smoothstep(0.14, 0.48, abs(p.x));
        color = mix(color, ThemeBase.rgb * 0.42, clearCenter * 0.70);
        color *= 0.63 + 0.37 * (1.0 - smoothstep(0.35, 1.2, length(p)));
        return clamp(color, 0.0, 1.0);
    }
    if (style == 1) color = essMenuAuroraBackground(p);
    else if (style == 3) color = essMenuWaveBackground(p);
    else color = essMenuLiquidBackground(uv, aspect);
    float vignette = 1.0 - smoothstep(0.15, 1.25, length(p));
    color = clamp(color * (0.45 + vignette * 0.55), 0.0, 1.0);
    // Preserve the old menu's dark overlay inside each style so crossfades do not change brightness.
    return mix(color, vec3(3.0, 5.0, 8.0) / 255.0, Shade);
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(Resolution, vec2(1.0));
    uv.y = 1.0 - uv.y;
    float aspect = max(Resolution.x / max(Resolution.y, 1.0), 0.0001);
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0);
    vec3 color = essMenuBackground(Style, uv, p, aspect);
    // A settled frame evaluates one effect. Only a short crossfade evaluates both.
    if (PreviousStyle != Style && Transition < 1.0) {
        color = mix(essMenuBackground(PreviousStyle, uv, p, aspect), color, clamp(Transition, 0.0, 1.0));
    }
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0) * shaderColor;
}