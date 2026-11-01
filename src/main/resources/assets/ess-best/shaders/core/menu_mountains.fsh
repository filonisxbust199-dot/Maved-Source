#version 330
#extension GL_ARB_separate_shader_objects : require

// Main menu background: a still winter night in a Minecraft-style valley. Snow-capped block
// mountains, spruce forest and a dark river under a square moon and stars.
//
// The image never moves, so MenuScene renders it many times with a sub-pixel jitter and blends
// the frames together (blend weight in ModelOffset.x, jitter in ModelOffset.yz). The average is an
// anti-aliased, noise-free picture; once it has converged the scene is not rendered at all.
// Because of that the shader can afford long traces, soft shadows and stochastic effects.
//
//   - a block heightfield traced cell by cell (2D DDA over columns);
//   - 16x16 pixel textures per face: snow, snowy grass sides, dirt, stone, gravel, spruce;
//   - moonlight with soft (jittered) shadows, Minecraft-like corner occlusion, snow glitter;
//   - river reflecting the moon and the ridges; valley mist; stars and a faint airglow.

#include <minecraft:dynamictransforms.glsl>

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

#define PI 3.14159265

const float WATER = 30.0;
const float SNOW_LINE = 66.0;
const float M_GROUND = 1.0, M_WATER = 2.0, M_LOG = 3.0, M_LEAVES = 4.0;

vec3 MOON;
const vec3 MOON_COLOR = vec3(0.55, 0.68, 1.0) * 1.05;
const vec3 SKY_LIGHT = vec3(0.07, 0.1, 0.2);
float seed;

// ---- noise ----------------------------------------------------------------------------------------

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}
vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}
float hash13(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1, 0)), u.x),
               mix(hash12(i + vec2(0, 1)), hash12(i + vec2(1, 1)), u.x), u.y);
}
const mat2 OCTAVE = mat2(0.8, 0.6, -0.6, 0.8);
float fbm(vec2 p, int octaves) {
    float sum = 0.0, amp = 0.5;
    for (int i = 0; i < 5; i++) {
        if (i >= octaves) break;
        sum += amp * noise(p);
        p = OCTAVE * p * 2.03;
        amp *= 0.5;
    }
    return sum;
}

// ---- world ----------------------------------------------------------------------------------------

float riverX(float z) {
    return 16.0 * sin(z * 0.019) + 7.0 * sin(z * 0.053 + 1.3);
}
float terrainRaw(vec2 p) {
    float d = abs(p.x - riverX(p.y));
    float valley = smoothstep(6.0, 75.0, d);
    float ridges = 0.0, amp = 1.0;
    vec2 q = p * 0.011 + 5.0;
    for (int i = 0; i < 4; i++) {
        float n = 1.0 - abs(noise(q) * 2.0 - 1.0);
        ridges += n * n * amp;
        amp *= 0.48;
        q = OCTAVE * q * 2.07;
    }
    float h = 33.0 + noise(p * 0.045) * 5.0 + valley * valley * ridges * 50.0;
    return mix(WATER - 2.6, h, smoothstep(5.0, 13.0, d));
}
float ground(vec2 cell) { return floor(terrainRaw(cell + 0.5)); }

void tree(vec2 cell, out vec2 trunk, out vec2 leaves) {
    trunk = vec2(0.0);
    leaves = vec2(0.0);
    vec2 tc = floor(cell / 7.0);
    vec2 h = hash22(tc * 1.37 + 11.0);
    if (h.x < 0.45) return;
    vec2 centre = tc * 7.0 + 2.0 + floor(h * 3.0);
    if (abs(centre.x - riverX(centre.y)) < 15.0) return;
    vec2 off = cell - centre;
    float r = max(abs(off.x), abs(off.y));
    if (r > 2.0 || (abs(off.x) == 2.0 && abs(off.y) == 2.0)) return;
    float g = ground(centre);
    if (g < WATER + 1.0 || g > SNOW_LINE + 6.0) return;
    float th = 5.0 + floor(hash12(tc + 3.3) * 4.0);
    if (r == 0.0) {
        trunk = vec2(g, g + th);
        leaves = vec2(g + th, g + th + 1.0);
    } else if (r == 1.0) {
        leaves = vec2(g + 2.0, g + th);
    } else {
        leaves = vec2(g + 2.0, g + 2.0 + floor((th - 2.0) * 0.5));
    }
}

void interval(vec3 ro, vec3 rd, float t0, float t1, float a, float b, vec3 side, float material,
              inout float best, inout vec3 normal, inout float mat) {
    if (b <= a) return;
    float y0 = ro.y + rd.y * t0;
    if (y0 >= a && y0 < b) {
        if (t0 < best) { best = t0; normal = side; mat = material; }
        return;
    }
    if (rd.y < 0.0 && y0 >= b) {
        float t = (b - ro.y) / rd.y;
        if (t <= t1 && t < best) { best = t; normal = vec3(0.0, 1.0, 0.0); mat = material; }
    } else if (rd.y > 0.0 && y0 < a) {
        float t = (a - ro.y) / rd.y;
        if (t <= t1 && t < best) { best = t; normal = vec3(0.0, -1.0, 0.0); mat = material; }
    }
}

float trace(vec3 ro, vec3 rd, int maxSteps, bool water, out vec3 normal, out float mat) {
    normal = vec3(0.0, 1.0, 0.0);
    mat = 0.0;
    vec2 dir = vec2(abs(rd.x) < 1e-5 ? 1e-5 : rd.x, abs(rd.z) < 1e-5 ? 1e-5 : rd.z);
    vec2 cell = floor(ro.xz);
    vec2 stepDir = sign(dir);
    vec2 delta = abs(1.0 / dir);
    vec2 tMax = (cell + max(stepDir, 0.0) - ro.xz) / dir;
    float t = 0.0;
    vec3 side = vec3(0.0, 1.0, 0.0);
    for (int i = 0; i < 400; i++) {
        if (i >= maxSteps) break;
        float tExit = min(tMax.x, tMax.y);
        float best = 1e9;
        float g = ground(cell);
        interval(ro, rd, t, tExit, -1e3, g, side, M_GROUND, best, normal, mat);
        if (water && g < WATER) interval(ro, rd, t, tExit, g, WATER - 0.12, side, M_WATER, best, normal, mat);
        vec2 trunk, leaves;
        tree(cell, trunk, leaves);
        interval(ro, rd, t, tExit, trunk.x, trunk.y, side, M_LOG, best, normal, mat);
        interval(ro, rd, t, tExit, leaves.x, leaves.y, side, M_LEAVES, best, normal, mat);
        if (best < 1e9) return best;
        if (tMax.x < tMax.y) {
            t = tMax.x; tMax.x += delta.x; cell.x += stepDir.x; side = vec3(-stepDir.x, 0.0, 0.0);
        } else {
            t = tMax.y; tMax.y += delta.y; cell.y += stepDir.y; side = vec3(0.0, 0.0, -stepDir.y);
        }
        if (rd.y > 0.0 && ro.y + rd.y * t > 100.0) break;
    }
    return -1.0;
}

// ---- night sky ------------------------------------------------------------------------------------

vec3 skyColor(vec3 rd) {
    float y = max(rd.y, 0.0);
    float moonSide = pow(max(dot(rd, MOON), 0.0), 3.0);
    vec3 zenith = vec3(0.006, 0.01, 0.03);
    vec3 horizon = mix(vec3(0.03, 0.045, 0.09), vec3(0.07, 0.09, 0.16), moonSide);
    vec3 col = mix(horizon, zenith, pow(y, 0.4));
    // Faint green airglow low over the ridges.
    col += vec3(0.01, 0.03, 0.02) * exp(-y * 7.0);
    return col;
}
vec3 fogColor(vec3 rd) {
    float moonSide = pow(max(dot(rd, MOON), 0.0), 6.0);
    return mix(vec3(0.035, 0.05, 0.095), vec3(0.09, 0.11, 0.18), moonSide);
}
vec3 stars(vec3 rd) {
    vec3 col = vec3(0.0);
    for (int layer = 0; layer < 3; layer++) {
        float scale = 120.0 + float(layer) * 140.0;
        vec3 q = rd * scale;
        vec3 cell = floor(q);
        float h = hash13(cell + float(layer) * 31.0);
        if (h < 0.955) continue;
        vec3 centre = cell + 0.5 + (vec3(hash13(cell + 1.7), hash13(cell + 5.3), hash13(cell + 9.1)) - 0.5) * 0.6;
        float d = length(q - centre);
        vec3 tint = mix(vec3(0.7, 0.8, 1.0), vec3(1.0, 0.88, 0.72), fract(h * 41.0));
        float bright = layer == 0 ? 1.6 : layer == 1 ? 0.8 : 0.45;
        col += tint * bright * exp(-d * d * 30.0) * (0.5 + 0.5 * fract(h * 97.0));
    }
    return col * smoothstep(0.02, 0.25, rd.y);
}
vec3 sky(vec3 rd) {
    vec3 col = skyColor(rd) + stars(rd);
    // Square moon with its blocky maria, and a soft halo.
    vec3 side = normalize(cross(MOON, vec3(0.0, 1.0, 0.0)));
    vec3 up = cross(side, MOON);
    vec2 q = vec2(dot(rd, side), dot(rd, up));
    if (dot(rd, MOON) > 0.0) {
        float box = max(abs(q.x), abs(q.y));
        if (box < 0.03) {
            vec2 px = floor((q / 0.03 * 0.5 + 0.5) * 8.0);
            float maria = hash12(px + 4.0) > 0.7 ? 0.78 : 1.0;
            col = vec3(0.92, 0.95, 1.0) * 2.4 * maria;
        }
        col += MOON_COLOR * (exp(-box * 22.0) * 0.18 + exp(-box * 5.0) * 0.05);
    }
    return col;
}

// ---- materials ------------------------------------------------------------------------------------

float texel(vec2 uv, vec3 block, float salt) {
    vec2 px = floor(uv * 16.0);
    return hash13(vec3(px + block.xz * 17.0, block.y * 13.0 + salt));
}
vec3 albedo(vec3 pos, vec3 n, float mat, out float snow) {
    snow = 0.0;
    vec3 block = floor(pos - n * 0.001);
    vec2 uv = abs(n.y) > 0.5 ? fract(pos.xz) : abs(n.x) > 0.5 ? fract(pos.zy) : fract(pos.xy);
    float faceSalt = n.y > 0.5 ? 1.0 : n.y < -0.5 ? 2.0 : 3.0;
    float v = texel(uv, block, faceSalt + mat * 7.0);
    vec3 stone = vec3(0.47, 0.47, 0.5) * (0.78 + 0.35 * v);
    vec3 snowCol = vec3(0.9, 0.93, 1.0) * (0.93 + 0.08 * v);
    if (mat == M_GROUND) {
        float top = ground(block.xz) - 1.0;
        float depth = top - block.y;
        if (top < WATER - 1.0) return vec3(0.45, 0.42, 0.4) * (0.7 + 0.5 * v);
        bool mountain = top > SNOW_LINE - 6.0 + hash12(block.xz) * 8.0;
        if (n.y > 0.5) { snow = 1.0; return snowCol; }
        if (depth < 0.5 && uv.y > (mountain ? 0.82 : 0.75) - floor(v * 2.0) / 16.0) { snow = 1.0; return snowCol; }
        if (mountain || depth > 3.5) return stone;
        return vec3(0.42, 0.29, 0.19) * (0.75 + 0.4 * v);
    }
    if (mat == M_LOG) {
        if (abs(n.y) > 0.5) {
            float ring = length(uv - 0.5);
            return mix(vec3(0.45, 0.33, 0.2), vec3(0.3, 0.2, 0.12), step(0.5, fract(ring * 6.0))) * (0.85 + 0.2 * v);
        }
        float stripe = hash12(vec2(floor(uv.x * 16.0), block.x + block.z));
        return vec3(0.23, 0.15, 0.09) * (0.7 + 0.45 * stripe) * (0.9 + 0.2 * v);
    }
    if (mat == M_LEAVES) {
        if (n.y > 0.5 && v < 0.82) { snow = 1.0; return snowCol; }
        if (abs(n.y) < 0.5 && uv.y > 0.8 && v < 0.6) { snow = 1.0; return snowCol; }
        return vec3(0.12, 0.24, 0.16) * (0.55 + 0.7 * v);
    }
    return stone;
}

float topOcclusion(vec3 pos, float level) {
    vec2 cell = floor(pos.xz);
    vec2 f = fract(pos.xz);
    float s[8];
    vec2 o[8] = vec2[](vec2(-1, 0), vec2(1, 0), vec2(0, -1), vec2(0, 1), vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));
    for (int i = 0; i < 8; i++) s[i] = ground(cell + o[i]) > level ? 1.0 : 0.0;
    float c00 = s[0] + s[2] > 1.5 ? 0.0 : 3.0 - (s[0] + s[2] + s[4]);
    float c10 = s[1] + s[2] > 1.5 ? 0.0 : 3.0 - (s[1] + s[2] + s[5]);
    float c01 = s[0] + s[3] > 1.5 ? 0.0 : 3.0 - (s[0] + s[3] + s[6]);
    float c11 = s[1] + s[3] > 1.5 ? 0.0 : 3.0 - (s[1] + s[3] + s[7]);
    float ao = mix(mix(c00, c10, f.x), mix(c01, c11, f.x), f.y) / 3.0;
    return 0.45 + 0.55 * ao;
}
float sideOcclusion(vec3 pos, vec3 n) {
    float front = ground(floor(pos.xz + n.xz * 0.5));
    float above = pos.y - front;
    return above < 1.0 ? 0.6 + 0.4 * clamp(above, 0.0, 1.0) : 1.0;
}

// Soft moon shadows: each accumulated frame picks a random point on the moon's disc.
float shadowRay(vec3 pos, vec3 n) {
    vec2 r = hash22(gl_FragCoord.xy * 0.37 + seed * 91.7) - 0.5;
    vec3 side = normalize(cross(MOON, vec3(0.0, 1.0, 0.0)));
    vec3 up = cross(side, MOON);
    vec3 dir = normalize(MOON + (side * r.x + up * r.y) * 0.05);
    vec3 nn;
    float m;
    return trace(pos + n * 0.02 + dir * 0.02, dir, 120, false, nn, m) > 0.0 ? 0.0 : 1.0;
}

vec3 light(vec3 pos, vec3 rd, vec3 n, vec3 alb, float snow, float shadow, float ao) {
    float dif = max(dot(n, MOON), 0.0) * shadow;
    vec3 lit = MOON_COLOR * dif + SKY_LIGHT * (0.6 + 0.4 * n.y) * ao;
    vec3 col = alb * lit;
    if (snow > 0.5) {
        vec3 h = normalize(MOON - rd);
        col += MOON_COLOR * pow(max(dot(n, h), 0.0), 40.0) * shadow * 0.12;
        // Glitter: single crystals (texels) that catch the moon.
        vec2 uv = abs(n.y) > 0.5 ? pos.xz : abs(n.x) > 0.5 ? pos.zy : pos.xy;
        float g = hash12(floor(uv * 16.0) + floor(pos.y) * 7.1);
        if (g > 0.992) col += vec3(0.8, 0.9, 1.0) * 1.6 * shadow * (0.4 + 0.6 * fract(g * 731.0));
    }
    return col;
}
vec3 applyFog(vec3 col, vec3 rd, float t, float y) {
    // Thin haze everywhere plus mist pooled in the valley.
    float haze = 1.0 - exp(-t * 0.0028);
    float mist = (1.0 - exp(-t * 0.02)) * exp(-max(y - WATER, 0.0) * 0.18) * 0.55;
    return mix(col, fogColor(rd), clamp(haze + mist, 0.0, 1.0));
}
vec3 shadeSimple(vec3 ro, vec3 rd, float t, vec3 n, float mat) {
    vec3 pos = ro + rd * t;
    float snow;
    vec3 alb = albedo(pos, n, mat, snow);
    return applyFog(light(pos, rd, n, alb, snow, 0.8, 0.85), rd, t, pos.y);
}

vec3 aces(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
    float aspect = ColorModulator.y;
    seed = fract(ModelOffset.y * 13.7 + ModelOffset.z * 71.3);
    // Sub-pixel jitter for this accumulated frame.
    vec2 uv = texCoord + ModelOffset.yz;
    vec2 p = (uv * 2.0 - 1.0) * vec2(aspect, 1.0);
    MOON = normalize(vec3(-0.3, 0.3, -1.0));

    float z = 40.0;
    vec3 ro = vec3(riverX(z), 0.0, z);
    ro.y = max(54.0, terrainRaw(ro.xz) + 12.0);
    vec3 target = vec3(riverX(z - 140.0), 44.0, z - 140.0);
    vec3 ww = normalize(target - ro);
    vec3 uu = normalize(cross(ww, vec3(0.0, 1.0, 0.0)));
    vec3 vv = cross(uu, ww);
    vec3 rd = normalize((p.x - 0.28 * aspect) * uu + p.y * vv + 1.7 * ww);

    vec3 n;
    float mat;
    float t = trace(ro, rd, 340, true, n, mat);
    vec3 col;
    if (t < 0.0) {
        col = sky(rd);
        col = mix(col, fogColor(rd), exp(-max(rd.y, 0.0) * 22.0) * 0.7);
    } else {
        vec3 pos = ro + rd * t;
        if (mat == M_WATER) {
            // Still water: a faint pixel ripple, the moon and the ridges mirrored in it.
            vec2 px = floor(pos.xz * 16.0) / 16.0;
            vec3 wn = normalize(vec3((noise(px * 1.3) - 0.5) * 0.05, 1.0, (noise(px * 2.1 + 7.0) - 0.5) * 0.05));
            vec3 r = reflect(rd, wn);
            vec3 rn;
            float rm;
            float rt = trace(pos + vec3(0.0, 0.01, 0.0), r, 160, false, rn, rm);
            vec3 refl = rt > 0.0 ? shadeSimple(pos, r, rt, rn, rm) : sky(r);
            float depth = WATER - ground(floor(pos.xz));
            vec3 body = mix(vec3(0.03, 0.08, 0.1), vec3(0.01, 0.025, 0.04), clamp(depth / 4.0, 0.0, 1.0)) * (MOON_COLOR * 0.3 + SKY_LIGHT);
            float fresnel = 0.04 + 0.96 * pow(1.0 - max(dot(-rd, wn), 0.0), 5.0);
            col = mix(body, refl, clamp(fresnel * 0.9 + 0.15, 0.0, 1.0));
            col = applyFog(col, rd, t, pos.y);
        } else {
            float snow;
            vec3 alb = albedo(pos, n, mat, snow);
            float shadow = shadowRay(pos, n);
            float ao = 1.0;
            if (t < 120.0) {
                if (n.y > 0.5 && mat == M_GROUND) ao = topOcclusion(pos, floor(pos.y + 0.01));
                else if (abs(n.y) < 0.5) ao = sideOcclusion(pos, n);
            }
            col = light(pos, rd, n, alb, snow, shadow, ao);
            col = applyFog(col, rd, t, pos.y);
        }
    }

    col = aces(col * 1.15);
    col = pow(col, vec3(1.0 / 2.2));
    vec2 v = texCoord;
    col *= 0.55 + 0.45 * pow(16.0 * v.x * v.y * (1.0 - v.x) * (1.0 - v.y), 0.2);
    col *= mix(0.7, 1.0, smoothstep(0.05, 0.5, v.x));
    // Blended into the accumulation target with this frame's weight.
    fragColor = vec4(col, ModelOffset.x);
}
