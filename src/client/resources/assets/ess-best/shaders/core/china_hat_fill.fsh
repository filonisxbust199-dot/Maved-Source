#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 hatUv;
in vec4 hatColor;
out vec4 fragColor;

float hash12(vec2 p) {
    uvec2 bits = floatBitsToUint(p + vec2(0.0));
    uint h = bits.x * 1597334677u ^ bits.y * 3812015801u;
    h = (h ^ (h >> 16u)) * 2246822519u;
    h = (h ^ (h >> 13u)) * 3266489917u;
    return float((h ^ (h >> 16u)) & 0x00ffffffu) / 16777216.0;
}

vec2 hash22(vec2 p) {
    return vec2(hash12(p + vec2(19.1, 73.7)),
                hash12(p + vec2(91.3, 11.9)));
}

float noise2(vec2 p) {
    vec2 cell = floor(p);
    vec2 blend = fract(p);
    float a = hash12(cell);
    float b = hash12(cell + vec2(1.0, 0.0));
    float c = hash12(cell + vec2(0.0, 1.0));
    float d = hash12(cell + vec2(1.0, 1.0));
    blend = blend * blend * (3.0 - 2.0 * blend);
    return mix(mix(a, b, blend.x), mix(c, d, blend.x), blend.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    mat2 rotation = mat2(1.7, -1.2, 1.2, 1.7);
    for (int i = 0; i < 4; i++) {
        value += amplitude * noise2(p);
        p = rotation * p + vec2(13.1, 7.7);
        amplitude *= 0.5;
    }
    return value;
}

// F1/F2 edge distance and a stable per-cell material variation. Used by
// magma and crystal, not by the flat or liquid fills.
vec3 cells(vec2 p) {
    vec2 base = floor(p);
    vec2 local = fract(p);
    float nearest = 10.0;
    float second = 10.0;
    float material = 0.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 offset = vec2(float(x), float(y));
            vec2 id = base + offset;
            float distanceToCell = length(offset + hash22(id) - local);
            if (distanceToCell < nearest) {
                second = nearest;
                nearest = distanceToCell;
                material = hash12(id + vec2(42.7, 15.3));
            } else if (distanceToCell < second) {
                second = distanceToCell;
            }
        }
    }
    return vec3(nearest, second, material);
}

vec3 liquidFill(vec2 p, float t, vec3 accent) {
    float flow = fbm(p * 4.0 + vec2(t * 0.25, -t * 0.18));
    float detail = fbm(p * 7.0 - vec2(t * 0.16, t * 0.10));
    float ripple = sin(p.x * 12.0 + t * 1.2) + cos(p.y * 10.0 - t);
    float wave = 0.5 + 0.5 * sin(ripple + flow * 3.0 + detail * 1.8);
    vec3 dark = accent * 0.14 + vec3(0.005);
    vec3 mid = mix(dark, accent * 0.58, clamp(flow * 1.15, 0.0, 1.0));
    vec3 color = mix(mid, accent, smoothstep(0.52, 1.0, wave) * 0.66);
    color += accent * smoothstep(0.80, 1.0, wave) * 0.22;
    return color + mix(accent, vec3(1.0), 0.45)
           * smoothstep(0.55, 1.0, detail) * 0.10;
}

vec3 auroraFill(vec2 p, float t, vec3 accent) {
    vec2 q = p * vec2(2.5, 2.0);
    float drift = fbm(q * 1.6 + vec2(t * 0.18, -t * 0.12));
    float curtain = sin(q.x * 3.4 + sin(q.y * 2.3 + t * 0.72) * 0.75
                      + drift * 2.3 - t * 0.86);
    float glow = pow(smoothstep(0.02, 0.95, curtain), 2.0);
    glow *= 0.5 + 0.5 * smoothstep(-0.9, 0.45, p.y);
    float mist = fbm(q * 2.7 + vec2(-t * 0.14, t * 0.10));
    vec3 dark = accent * 0.11 + vec3(0.006);
    vec3 color = mix(dark, accent * 0.48, mist);
    color += mix(accent, vec3(1.0), 0.35) * glow * (0.45 + 0.3 * mist);
    return color;
}

vec3 galaxyFill(vec2 p, float t, vec3 accent) {
    float r = length(p);
    float angle = atan(p.y, p.x);
    float dust = fbm(p * 4.1 + vec2(t * 0.10, -t * 0.07));
    float spiral = 0.5 + 0.5 * sin(angle * 3.0 + r * 12.0 + dust * 3.0 - t * 0.52);
    float nebula = smoothstep(0.45, 0.76, dust + 0.16 * spiral);
    float core = exp(-r * r * 8.0) * (0.40 + 0.60 * dust);
    float spin = t * 0.07;
    vec2 orbit = mat2(cos(spin), -sin(spin), sin(spin), cos(spin)) * p;
    vec2 grid = orbit * 17.0;
    vec2 id = floor(grid);
    vec2 starPoint = hash22(id) - vec2(0.5);
    float star = (1.0 - smoothstep(0.02, 0.10, length(fract(grid) - vec2(0.5) - starPoint)))
                 * step(0.93, hash12(id + vec2(13.4, 82.1)));
    star *= 0.58 + 0.42 * sin(t * 2.4 + hash12(id + vec2(7.3, 18.9)) * 6.2831853);
    vec3 color = accent * 0.06 + vec3(0.008);
    color += accent * 0.66 * nebula;
    color += mix(accent, vec3(1.0), 0.62) * (core * 0.45 + star * 0.85);
    return color;
}

vec3 magmaFill(vec2 p, float t, vec3 accent) {
    vec2 warp = vec2(fbm(p * 3.4 + vec2(t * 0.18, 0.0)),
                     fbm(p * 3.4 + vec2(8.1, -t * 0.15))) - vec2(0.5);
    vec3 cell = cells(p * 4.4 + warp * 0.72 + vec2(t * 0.12, -t * 0.08));
    float cracks = 1.0 - smoothstep(0.035, 0.19, cell.y - cell.x);
    float heat = fbm(p * 3.0 + warp + vec2(0.0, t * 0.20));
    vec3 rock = mix(accent * 0.04 + vec3(0.008), accent * 0.24, cell.z * 0.75 + heat * 0.25);
    vec3 fire = mix(accent * 0.72, mix(accent, vec3(1.0), 0.58), smoothstep(0.45, 0.85, heat));
    float pulse = 0.78 + 0.22 * sin(t * 1.7 + cell.z * 6.2831853);
    return rock + fire * cracks * 0.82 * pulse + accent * (0.10 + cracks * 0.12);
}

vec3 crystalFill(vec2 p, float t, vec3 accent) {
    vec2 q = p * 3.8;
    vec3 cell = cells(q);
    float seam = 1.0 - smoothstep(0.015, 0.13, cell.y - cell.x);
    float facet = 0.32 + 0.68 * cell.z;
    float glint = pow(max(0.0, 1.0 - cell.x), 4.0);
    float sweep = exp(-pow(dot(p, normalize(vec2(0.85, 0.53)))
                           - (fract(t * 0.14) * 2.8 - 1.4), 2.0) * 45.0);
    vec3 color = mix(accent * 0.14 + vec3(0.012),
                     mix(accent, vec3(1.0), 0.28) * 0.78, facet);
    color += mix(accent, vec3(1.0), 0.62)
             * (seam * (0.43 + 0.12 * sin(t * 1.5 + cell.z * 6.2831853))
                + glint * 0.13 + sweep * (0.20 + seam * 0.25));
    return color;
}

vec3 pearlFill(vec2 p, float t, vec3 accent) {
    float flow = fbm(p * 3.0 + vec2(t * 0.13, -t * 0.09));
    float phase = p.x * 5.4 + p.y * 3.7 + flow * 4.0 - t * 0.70;
    float sheen = 0.5 + 0.5 * sin(phase);
    vec3 iris = mix(accent, vec3(1.0), 0.20 + 0.58 * sheen);
    vec3 color = mix(accent * 0.25 + vec3(0.045), iris, 0.20 + 0.70 * sheen);
    vec2 glintCenter = vec2(-0.28 + 0.33 * sin(t * 0.45), -0.25 + 0.22 * cos(t * 0.38));
    float glint = exp(-length((p - glintCenter) * vec2(1.2, 2.0)) * 5.0);
    return color + mix(accent, vec3(1.0), 0.78) * glint * 0.26;
}

void main() {
    // Every pattern is anchored to the same circular UV across the whole cone.
    vec2 p = (hatUv - 0.5) * 2.0;
    float time = ColorModulator.x;
    vec3 accent = hatColor.rgb;
    int mode = int(ColorModulator.y + 0.5);
    vec3 color;
    if (mode == 1) {
        color = liquidFill(p, time, accent);
    } else if (mode == 2) {
        color = auroraFill(p, time, accent);
    } else if (mode == 3) {
        color = galaxyFill(p, time, accent);
    } else if (mode == 4) {
        color = magmaFill(p, time, accent);
    } else if (mode == 5) {
        color = crystalFill(p, time, accent);
    } else if (mode == 6) {
        color = pearlFill(p, time, accent);
    } else {
        // The original opaque theme-colored ChinaHat, not a reduced shader hat.
        fragColor = vec4(accent, hatColor.a);
        return;
    }
    color *= 1.0 - 0.10 * smoothstep(0.72, 1.0, length(p));
    fragColor = vec4(clamp(color, 0.0, 1.0), hatColor.a);
}
