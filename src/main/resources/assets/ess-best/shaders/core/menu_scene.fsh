#version 330
#extension GL_ARB_separate_shader_objects : require

// New Year main menu background: a raymarched winter night.
//
//   - snowy hills with drifts and glitter, mountains fading into the fog;
//   - a decorated fir in a clearing: tiered branches with drooping tips and snow on top, a spiral
//     garland of blinking bulbs that light the branches and the snow around, a glowing star;
//   - a log cabin with lit windows and a chimney, a snowman, presents under the tree;
//   - a pine forest around the clearing (domain repetition);
//   - sky with stars, moon, aurora and the occasional shooting star;
//   - moonlight with soft shadows, ambient occlusion, height fog, falling snow in depth layers.
//
// Drawn by MenuScene with core/screenquad into a reduced-resolution target. Per-frame values come
// in DynamicTransforms: ColorModulator = (time, aspect, mouse x, mouse y).

#include <minecraft:dynamictransforms.glsl>

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

vec2 mouse;

#define PI 3.14159265

float T;
const vec3 MOON = vec3(-0.52, 0.40, -0.75);
const vec3 TREE_LIGHT = vec3(0.0, 2.4, 0.0);
const vec3 STAR = vec3(0.0, 5.32, 0.0);
const vec3 CABIN = vec3(4.2, 0.0, -8.4);
const float CABIN_ROT = 0.45;
const vec3 SNOWMAN = vec3(-2.4, 0.0, 2.8);
const int BULBS = 64;
float cabinBase;

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
    for (int i = 0; i < 6; i++) {
        if (i >= octaves) break;
        sum += amp * noise(p);
        p = OCTAVE * p * 2.03;
        amp *= 0.5;
    }
    return sum;
}
mat2 rot(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, s, -s, c);
}

// ---- distance functions ---------------------------------------------------------------------------

float sdBox(vec3 p, vec3 b) {
    vec3 q = abs(p) - b;
    return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
}
float sdCappedCone(vec3 p, float h, float r1, float r2) {
    vec2 q = vec2(length(p.xz), p.y);
    vec2 k1 = vec2(r2, h);
    vec2 k2 = vec2(r2 - r1, 2.0 * h);
    vec2 ca = vec2(q.x - min(q.x, (q.y < 0.0) ? r1 : r2), abs(q.y) - h);
    vec2 cb = q - k1 + k2 * clamp(dot(k1 - q, k2) / dot(k2, k2), 0.0, 1.0);
    float s = (cb.x < 0.0 && ca.y < 0.0) ? -1.0 : 1.0;
    return s * sqrt(min(dot(ca, ca), dot(cb, cb)));
}
float sdCylinder(vec3 p, float r, float h) {
    vec2 d = vec2(length(p.xz) - r, abs(p.y) - h);
    return min(max(d.x, d.y), 0.0) + length(max(d, 0.0));
}
float sdCapsule(vec3 p, vec3 a, vec3 b, float r) {
    vec3 pa = p - a, ba = b - a;
    return length(pa - ba * clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0)) - r;
}
float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}
vec2 opU(vec2 a, vec2 b) { return a.x < b.x ? a : b; }

// Ground: flat clearing around the tree, rolling hills further out, mountains on the horizon.
float terrain(vec2 xz, int octaves) {
    float r = length(xz);
    float clearing = smoothstep(7.0, 18.0, r);
    float hills = (fbm(xz * 0.07, octaves) * 2.8 - 1.1) * clearing;
    float far = smoothstep(28.0, 75.0, r);
    float mountains = far > 0.0 ? fbm(xz * 0.022 + 7.3, octaves) * 26.0 * far : 0.0;
    return hills + mountains;
}

// The decorated fir: five tiers whose lower rims droop into branch tips.
float tierRadius(int i) { return 2.05 - float(i) * 0.36; }
float tierBase(int i) { return 0.75 + float(i) * 0.82; }
float tierHeight(int i) { return 1.55 - float(i) * 0.13; }
float fir(vec3 p) {
    float a = atan(p.z, p.x);
    float d = 1e3;
    for (int i = 0; i < 5; i++) {
        float fi = float(i), h = tierHeight(i);
        vec3 q = p - vec3(0.0, tierBase(i) + h * 0.5, 0.0);
        float rim = 1.0 - smoothstep(-h * 0.5, -h * 0.5 + 0.7, q.y);
        float tips = 0.5 + 0.5 * sin(a * 11.0 + fi * 2.3);
        q.y += tips * 0.16 * rim;
        float fringe = tips * 0.2 * rim + 0.035 * sin(a * 27.0 + q.y * 11.0 + fi);
        d = min(d, sdCappedCone(q, h * 0.5, tierRadius(i) + fringe, 0.05));
    }
    d = min(d, sdCylinder(p - vec3(0.0, 0.45, 0.0), 0.2, 0.45));
    return d * 0.72;
}
// Background pine, unit scale, about 4.3 high.
float pine(vec3 p) {
    float a = atan(p.z, p.x);
    float d = 1e3;
    for (int i = 0; i < 3; i++) {
        float fi = float(i), h = 1.9 - fi * 0.3;
        vec3 q = p - vec3(0.0, 0.5 + fi * 1.05 + h * 0.5, 0.0);
        float rim = 1.0 - smoothstep(-h * 0.5, -h * 0.5 + 0.55, q.y);
        float tips = 0.5 + 0.5 * sin(a * 8.0 + fi * 1.9);
        q.y += tips * 0.12 * rim;
        d = min(d, sdCappedCone(q, h * 0.5, 1.3 - fi * 0.32 + tips * 0.14 * rim, 0.03));
    }
    d = min(d, sdCylinder(p - vec3(0.0, 0.3, 0.0), 0.13, 0.3));
    return d * 0.78;
}
// Pines on a jittered grid outside the clearing. The distance never exceeds the way to the cell
// border, so the march cannot skip a neighbouring tree.
vec2 forest(vec3 p, float ground) {
    if (p.y - ground > 7.5) return vec2(p.y - ground - 6.5, 0.0);
    const float CELL = 5.5;
    vec2 cell = floor(p.xz / CELL);
    vec2 centre = (cell + 0.5) * CELL;
    vec2 local = p.xz - centre;
    float border = CELL * 0.5 - max(abs(local.x), abs(local.y)) + 0.35;
    vec2 h = hash22(cell);
    if (length(centre) < 14.0 || h.x < 0.16 || length(centre - CABIN.xz) < 6.0) return vec2(border, 0.0);
    vec2 pos = centre + (h - 0.5) * 1.4;
    float scale = 0.75 + hash12(cell + 3.1) * 0.6;
    float base = terrain(pos, 2) - 0.25;
    vec3 q = vec3(p.x - pos.x, p.y - base, p.z - pos.y) / scale;
    float d = pine(q) * scale;
    return d < border ? vec2(d, 2.0) : vec2(border, 0.0);
}
vec3 cabinLocal(vec3 p) {
    vec3 q = p - vec3(CABIN.x, cabinBase, CABIN.z);
    q.xz = rot(CABIN_ROT) * q.xz;
    return q;
}
vec2 cabin(vec3 p) {
    vec3 q = cabinLocal(p);
    float bound = sdBox(q - vec3(0.0, 1.8, 0.0), vec3(2.3, 2.0, 1.9));
    if (bound > 0.5) return vec2(bound, 0.0);
    float body = sdBox(q - vec3(0.0, 1.0, 0.0), vec3(1.7, 1.0, 1.3)) - 0.03;
    // Gable roof as a prism along x; the slope plane passes through the eave and the ridge.
    vec2 n = normalize(vec2(0.875, 1.0));
    float slope = dot(vec2(abs(q.z), q.y - 3.3), n);
    float roof = max(max(slope, 1.9 - q.y), abs(q.x) - 2.0);
    float chimney = sdBox(q - vec3(0.85, 3.2, -0.45), vec3(0.22, 0.6, 0.22));
    vec2 res = vec2(body, 4.0);
    res = opU(res, vec2(roof, 5.0));
    res = opU(res, vec2(chimney, 4.0));
    return res;
}
vec3 snowmanLocal(vec3 p) {
    vec3 q = p - SNOWMAN;
    // Turned to face the camera.
    q.xz = rot(0.62) * q.xz;
    return q;
}
vec2 snowman(vec3 p) {
    vec3 q = snowmanLocal(p);
    float bound = length(q - vec3(0.0, 1.2, 0.0)) - 1.6;
    if (bound > 0.4) return vec2(bound, 0.0);
    float body = length(q - vec3(0.0, 0.5, 0.0)) - 0.6;
    body = smin(body, length(q - vec3(0.0, 1.32, 0.0)) - 0.43, 0.1);
    body = smin(body, length(q - vec3(0.0, 1.95, 0.0)) - 0.3, 0.08);
    vec2 res = vec2(body, 6.0);
    float nose = sdCapsule(q, vec3(0.0, 1.95, 0.25), vec3(0.0, 1.92, 0.58), 0.035);
    res = opU(res, vec2(nose, 8.0));
    float hat = min(sdCylinder(q - vec3(0.0, 2.32, 0.0), 0.19, 0.16), sdCylinder(q - vec3(0.0, 2.18, 0.0), 0.31, 0.02));
    res = opU(res, vec2(hat, 9.0));
    // Twig arms.
    float arms = min(sdCapsule(q, vec3(0.38, 1.4, 0.0), vec3(0.95, 1.75, 0.05), 0.025),
                     sdCapsule(q, vec3(-0.38, 1.4, 0.0), vec3(-0.9, 1.62, -0.1), 0.025));
    res = opU(res, vec2(arms, 13.0));
    return res;
}
vec2 presents(vec3 p) {
    vec3 a = p - vec3(1.35, 0.27, 1.2);  a.xz = rot(0.4) * a.xz;
    vec3 b = p - vec3(-1.1, 0.22, 1.45); b.xz = rot(-0.3) * b.xz;
    vec3 c = p - vec3(0.25, 0.18, 1.95); c.xz = rot(0.9) * c.xz;
    vec2 res = vec2(sdBox(a, vec3(0.27)) - 0.02, 10.0);
    res = opU(res, vec2(sdBox(b, vec3(0.3, 0.22, 0.22)) - 0.02, 11.0));
    res = opU(res, vec2(sdBox(c, vec3(0.18)) - 0.02, 12.0));
    return res;
}
vec2 map(vec3 p) {
    float ground = terrain(p.xz, 4);
    vec2 res = vec2((p.y - ground) * 0.7, 1.0);
    res = opU(res, forest(p, ground));
    float treeBound = max(length(p.xz) - 2.6, max(p.y - 5.5, -p.y));
    res = opU(res, vec2(treeBound > 0.4 ? treeBound : fir(p), 3.0));
    res = opU(res, cabin(p));
    res = opU(res, snowman(p));
    if (length(p.xz - vec2(0.2, 1.5)) < 2.2) res = opU(res, presents(p));
    else res = opU(res, vec2(length(p.xz - vec2(0.2, 1.5)) - 1.8, 0.0));
    return res;
}

// ---- marching and lighting ------------------------------------------------------------------------

vec2 march(vec3 ro, vec3 rd) {
    float t = 0.05;
    for (int i = 0; i < 120; i++) {
        vec3 p = ro + rd * t;
        if (p.y > 32.0 && rd.y > 0.0) break;
        vec2 h = map(p);
        if (abs(h.x) < 0.0015 * t) return vec2(t, h.y);
        t += h.x;
        if (t > 140.0) break;
    }
    return vec2(t, -1.0);
}
vec3 calcNormal(vec3 p, float t) {
    vec2 e = vec2(1.0, -1.0) * 0.0008 * max(t, 1.0);
    return normalize(e.xyy * map(p + e.xyy).x + e.yyx * map(p + e.yyx).x
                   + e.yxy * map(p + e.yxy).x + e.xxx * map(p + e.xxx).x);
}
float softShadow(vec3 ro, vec3 rd) {
    float res = 1.0, t = 0.04;
    for (int i = 0; i < 22; i++) {
        float h = map(ro + rd * t).x;
        res = min(res, 9.0 * h / t);
        t += clamp(h, 0.06, 1.4);
        if (res < 0.01 || t > 24.0) break;
    }
    return clamp(res, 0.0, 1.0);
}
float ambientOcclusion(vec3 p, vec3 n) {
    float occ = 0.0, weight = 1.0;
    for (int i = 1; i <= 4; i++) {
        float h = 0.05 + 0.13 * float(i * i) / 4.0;
        occ += (h - map(p + n * h).x) * weight;
        weight *= 0.65;
    }
    return clamp(1.0 - occ * 1.6, 0.0, 1.0);
}

// Garland: a helix of bulbs hugging the branches, five colours blinking out of phase.
float firRadius(float y) {
    float r = 0.0;
    for (int i = 0; i < 5; i++) {
        float s = (y - tierBase(i)) / tierHeight(i);
        if (s >= 0.0 && s <= 1.0) r = max(r, mix(tierRadius(i), 0.05, s));
    }
    return r;
}
vec3 BULB[BULBS];
// Bulb positions are the same for every pixel; computed once per fragment instead of per use.
void placeBulbs() {
    for (int i = 0; i < BULBS; i++) {
        float s = (float(i) + 0.5) / float(BULBS);
        float y = 0.95 + s * 4.0;
        float a = s * PI * 2.0 * 6.5 + 0.4;
        float r = firRadius(y) * 0.9 + 0.05;
        BULB[i] = vec3(cos(a) * r, y, sin(a) * r);
    }
}
vec3 bulbPosition(int i) { return BULB[i]; }
vec3 bulbColor(int i) {
    int k = i - (i / 5) * 5;
    vec3 c = k == 0 ? vec3(1.0, 0.18, 0.12) : k == 1 ? vec3(1.0, 0.72, 0.22) : k == 2 ? vec3(0.25, 0.55, 1.0)
           : k == 3 ? vec3(0.25, 1.0, 0.45) : vec3(1.0, 0.35, 0.85);
    // Waves of light run down the garland, plus a little per-bulb flicker.
    float wave = 0.55 + 0.45 * sin(T * 2.2 - float(i) * 0.45);
    float flicker = 0.85 + 0.15 * sin(T * 13.0 + float(i) * 7.1);
    return c * wave * flicker;
}

vec3 skyColor(vec3 rd) {
    float y = max(rd.y, 0.0);
    vec3 col = mix(vec3(0.05, 0.08, 0.16), vec3(0.008, 0.014, 0.045), pow(y, 0.45));
    col += vec3(0.07, 0.09, 0.15) * exp(-y * 9.0);
    return col;
}
vec3 fogColor(vec3 rd) {
    vec3 moon = normalize(MOON);
    float m = max(dot(rd, moon), 0.0);
    return vec3(0.055, 0.08, 0.14) + vec3(0.08, 0.09, 0.12) * pow(m, 6.0);
}
float curtain(vec2 p) {
    float warp = fbm(p * vec2(0.18, 0.32) + vec2(T * 0.025, 0.0), 3) * 6.0;
    float band = pow(1.0 - abs(sin(p.x * 0.16 + warp)), 7.0);
    float rays = 0.55 + 0.45 * noise(vec2(p.x * 3.0 + T * 0.5, p.y * 0.2));
    return band * rays;
}
vec3 aurora(vec3 rd) {
    if (rd.y < 0.02) return vec3(0.0);
    vec3 acc = vec3(0.0);
    for (int i = 0; i < 26; i++) {
        float fi = float(i) / 26.0;
        float height = 9.0 + fi * 9.0;
        vec2 pos = rd.xz / rd.y * height;
        float c = curtain(pos + vec2(0.0, 30.0));
        // Green at the base, violet towards the top of the curtain.
        vec3 tint = mix(vec3(0.15, 1.0, 0.55), vec3(0.65, 0.25, 1.0), smoothstep(0.2, 0.9, fi));
        acc += tint * c * (1.0 - fi) * 0.07;
    }
    float fade = smoothstep(0.02, 0.25, rd.y) * (1.0 - smoothstep(0.75, 1.0, rd.y));
    return acc * fade;
}
vec3 stars(vec3 rd) {
    vec3 col = vec3(0.0);
    for (int layer = 0; layer < 2; layer++) {
        float scale = layer == 0 ? 160.0 : 320.0;
        vec3 q = rd * scale;
        vec3 cell = floor(q);
        float h = hash13(cell + float(layer) * 47.0);
        if (h > 0.965) {
            vec3 f = fract(q) - 0.5;
            vec3 offset = vec3(hash13(cell + 1.7), hash13(cell + 5.3), hash13(cell + 9.1)) - 0.5;
            float d = length(f - offset * 0.6);
            float twinkle = 0.6 + 0.4 * sin(T * (1.5 + h * 4.0) + h * 80.0);
            float size = layer == 0 ? 0.09 : 0.07;
            vec3 tint = mix(vec3(0.75, 0.85, 1.0), vec3(1.0, 0.9, 0.75), fract(h * 37.0));
            col += tint * smoothstep(size, 0.0, d) * twinkle * (layer == 0 ? 1.6 : 0.8);
        }
    }
    return col * smoothstep(0.0, 0.2, rd.y);
}
vec3 sky(vec3 rd, vec2 p) {
    vec3 col = skyColor(rd);
    col += stars(rd);
    col += aurora(rd);
    vec3 moon = normalize(MOON);
    float m = dot(rd, moon);
    // Moon disc with darker maria, then a tight halo and a wide glow.
    if (m > 0.9988) {
        vec3 side = normalize(cross(moon, vec3(0.0, 1.0, 0.0)));
        vec3 up = cross(side, moon);
        vec2 disc = vec2(dot(rd, side), dot(rd, up)) * 380.0;
        float maria = fbm(disc * 0.9 + 3.0, 4);
        vec3 surface = mix(vec3(1.0, 0.97, 0.9), vec3(0.62, 0.64, 0.7), smoothstep(0.45, 0.7, maria));
        col = mix(col, surface * 2.2, smoothstep(0.99905, 0.99915, m));
    }
    col += vec3(0.65, 0.75, 1.0) * (exp((m - 1.0) * 600.0) * 0.5 + exp((m - 1.0) * 40.0) * 0.12 + exp((m - 1.0) * 6.0) * 0.04);
    // A shooting star every few seconds, crossing the upper sky.
    float period = 6.5;
    float k = floor(T / period);
    float local = T - k * period;
    if (local < 0.9) {
        vec2 h = hash22(vec2(k, 3.7));
        vec2 start = vec2(-0.6 + h.x * 2.2, 0.55 + h.y * 0.35);
        vec2 dir = normalize(vec2(-1.0, -0.38 - h.y * 0.3));
        vec2 head = start + dir * local * 1.2;
        vec2 rel = p - head;
        float along = dot(rel, -dir);
        float across = abs(dot(rel, vec2(-dir.y, dir.x)));
        float tail = smoothstep(0.35, 0.0, along) * step(0.0, along);
        float streak = exp(-across * 900.0) * tail + exp(-dot(rel, rel) * 4000.0) * 2.0;
        col += vec3(0.85, 0.92, 1.0) * streak * sin(local / 0.9 * PI) * 1.4;
    }
    return col;
}

vec3 shade(vec3 pos, vec3 rd, float t, float m) {
    vec3 n = calcNormal(pos, t);
    vec3 moon = normalize(MOON);
    vec3 albedo = vec3(0.8);
    vec3 emit = vec3(0.0);
    float snow = 0.0, specular = 0.0;

    if (m < 1.5) {
        // Wind-blown ripples and drifts bend the snow normal.
        vec2 w = pos.xz * vec2(1.4, 3.2);
        float r = noise(w) + 0.5 * noise(w * 2.3 + 4.0);
        n = normalize(n + vec3(noise(pos.xz * 2.6) - 0.5, 0.0, r - 0.75) * 0.22);
        albedo = vec3(0.82, 0.88, 1.0);
        snow = 1.0;
    } else if (m < 3.5) {
        // Fir / pine needles with snow settled on the upper surfaces.
        float needles = noise(pos.xz * 18.0 + pos.y * 9.0);
        vec3 green = m < 2.5 ? vec3(0.02, 0.07, 0.05) : vec3(0.03, 0.15, 0.065);
        green *= 0.7 + 0.6 * needles;
        float cover = smoothstep(0.6, 0.95, n.y + (noise(pos.xz * 6.0 + pos.y * 3.0) - 0.5) * 0.45);
        if (m > 2.5) cover *= 0.7;
        albedo = mix(green, vec3(0.85, 0.9, 1.0), cover * 0.9);
        snow = cover;
        if (m > 2.5 && pos.y < 0.8 && length(pos.xz) < 0.3) { albedo = vec3(0.12, 0.07, 0.04); snow = 0.0; }
    } else if (m < 4.5) {
        // Log walls: stacked logs with grain; warm windows and a door on the front.
        vec3 q = cabinLocal(pos);
        float logs = 0.5 + 0.5 * sin(q.y * 21.0);
        float grain = noise(vec2(q.x + q.z, q.y) * vec2(2.0, 40.0));
        albedo = mix(vec3(0.16, 0.09, 0.05), vec3(0.3, 0.17, 0.09), logs * 0.7 + grain * 0.3);
        if (q.y > 2.0) albedo = vec3(0.22, 0.2, 0.2) * (0.7 + 0.3 * noise(q.xy * 12.0));
        bool front = abs(q.z) > 1.25 && q.y < 2.0;
        if (front && q.z > 0.0) {
            vec2 wq = vec2(abs(q.x) - 1.0, q.y - 1.15);
            if (abs(wq.x) < 0.32 && abs(wq.y) < 0.3) {
                bool frame = abs(wq.x) > 0.27 || abs(wq.y) > 0.25 || abs(wq.x) < 0.025 || abs(wq.y) < 0.025;
                float flicker = 0.85 + 0.15 * sin(T * 7.0 + q.x * 3.0) * sin(T * 3.1);
                albedo = frame ? vec3(0.1, 0.06, 0.03) : vec3(0.0);
                if (!frame) emit = vec3(1.0, 0.55, 0.2) * 3.2 * flicker;
            }
            if (abs(q.x) < 0.3 && q.y < 1.45) albedo = vec3(0.1, 0.055, 0.03) * (0.8 + 0.4 * noise(q.xy * vec2(30.0, 3.0)));
        }
        if (abs(q.x) > 1.65 && q.y < 2.0) {
            // Log ends on the corners.
            albedo *= 1.25;
        }
    } else if (m < 5.5) {
        vec3 q = cabinLocal(pos);
        float cover = smoothstep(0.1, 0.4, n.y);
        albedo = mix(vec3(0.14, 0.08, 0.05), vec3(0.86, 0.9, 1.0), cover);
        snow = cover;
        // Icicles' glint under the eaves.
        if (q.y < 2.0 && n.y < -0.2) albedo = vec3(0.1, 0.06, 0.04);
    } else if (m < 6.5) {
        vec3 q = snowmanLocal(pos);
        albedo = vec3(0.86, 0.9, 1.0);
        snow = 1.0;
        // Coal eyes, smile and buttons.
        vec3 eyeL = vec3(-0.1, 2.02, 0.26), eyeR = vec3(0.1, 2.02, 0.26);
        float coal = min(length(q - eyeL), length(q - eyeR)) - 0.035;
        for (int i = 0; i < 5; i++) {
            float a = (float(i) - 2.0) * 0.28;
            coal = min(coal, length(q - vec3(sin(a) * 0.17, 1.84 - cos(a) * 0.03 + 0.03, 0.26)) - 0.02);
        }
        if (coal < 0.0) albedo = vec3(0.02);
        for (int i = 0; i < 3; i++) {
            vec3 b = vec3(0.0, 1.52 - float(i) * 0.2, 0.0);
            b.z = sqrt(max(0.0, 0.43 * 0.43 - pow(b.y - 1.32, 2.0)));
            if (length(q - b) < 0.05) albedo = vec3(0.02);
        }
    } else if (m < 8.5) {
        albedo = vec3(0.95, 0.35, 0.05);
    } else if (m < 9.5) {
        albedo = vec3(0.02);
        vec3 q = snowmanLocal(pos);
        if (abs(q.y - 2.2) < 0.03) albedo = vec3(0.6, 0.05, 0.05);
        specular = 0.4;
    } else if (m < 12.5) {
        // Presents: wrapped boxes with a contrasting ribbon cross.
        vec3 q = m < 10.5 ? pos - vec3(1.35, 0.27, 1.2) : m < 11.5 ? pos - vec3(-1.1, 0.22, 1.45) : pos - vec3(0.25, 0.18, 1.95);
        q.xz = rot(m < 10.5 ? 0.4 : m < 11.5 ? -0.3 : 0.9) * q.xz;
        vec3 paper = m < 10.5 ? vec3(0.7, 0.04, 0.05) : m < 11.5 ? vec3(0.05, 0.18, 0.6) : vec3(0.05, 0.4, 0.15);
        vec3 ribbon = m < 11.5 ? vec3(1.0, 0.8, 0.3) : vec3(0.95, 0.95, 1.0);
        float stripes = 0.85 + 0.15 * step(0.5, fract((q.x + q.y + q.z) * 9.0));
        albedo = (abs(q.x) < 0.045 || abs(q.z) < 0.045) ? ribbon : paper * stripes;
        specular = 0.5;
    } else {
        albedo = vec3(0.12, 0.07, 0.04);
    }

    // Far away the fog hides shadow and occlusion detail, so skip their extra marches there.
    float shadow = t < 40.0 ? softShadow(pos + n * 0.03, moon) : 1.0;
    float occ = t < 30.0 ? ambientOcclusion(pos, n) : 1.0;
    float diffuse = snow > 0.5 ? clamp(dot(n, moon) * 0.75 + 0.25, 0.0, 1.0) : clamp(dot(n, moon), 0.0, 1.0);
    vec3 light = vec3(0.55, 0.66, 1.0) * 0.85 * diffuse * shadow;
    light += vec3(0.08, 0.11, 0.22) * (0.6 + 0.4 * n.y) * occ * 1.1;
    light += vec3(0.06, 0.05, 0.05) * clamp(-n.y, 0.0, 1.0) * occ;

    // Warm light from the garland and the cabin windows.
    vec3 tl = TREE_LIGHT - pos;
    float td2 = dot(tl, tl);
    float pulse = 0.85 + 0.15 * sin(T * 2.2);
    light += vec3(1.0, 0.55, 0.25) * clamp(dot(n, tl * inversesqrt(td2)) * 0.6 + 0.4, 0.0, 1.0) * 3.2 * pulse / (1.0 + td2 * 0.22) * occ;
    vec3 window = vec3(CABIN.x, cabinBase, CABIN.z);
    vec3 offset = vec3(0.0, 1.15, 1.9);
    offset.xz = rot(-CABIN_ROT) * offset.xz;
    vec3 wl = window + offset - pos;
    float wd2 = dot(wl, wl);
    light += vec3(1.0, 0.5, 0.2) * clamp(dot(n, wl * inversesqrt(wd2)), 0.0, 1.0) * 4.0 / (1.0 + wd2 * 0.6) * occ;

    vec3 col = albedo * light + emit;

    // Bulbs light the branches they sit on.
    if (m > 2.5 && m < 3.5) {
        for (int i = 0; i < BULBS; i++) {
            vec3 d = bulbPosition(i) - pos;
            col += bulbColor(i) * albedo * 1.6 / (1.0 + dot(d, d) * 60.0) + bulbColor(i) * 0.03 / (1.0 + dot(d, d) * 400.0);
        }
    }

    vec3 h = normalize(moon - rd);
    col += vec3(0.6, 0.7, 1.0) * pow(clamp(dot(n, h), 0.0, 1.0), 48.0) * shadow * (0.2 * snow + specular);
    // Glitter: sparse snow crystals that catch the moon (or the garland) as the view moves.
    if (snow > 0.5) {
        vec3 cell = floor(pos * 34.0);
        float g = hash13(cell);
        if (g > 0.975) {
            float twinkle = pow(max(0.0, sin(T * 3.0 + g * 200.0 + dot(rd, vec3(40.0)))), 12.0);
            col += vec3(0.85, 0.92, 1.0) * twinkle * (0.4 + shadow * 1.6) * smoothstep(30.0, 4.0, t);
        }
    }

    // Height fog: denser in the valleys, thinning upwards.
    float fog = 1.0 - exp(-t * 0.022 * exp(-max(pos.y, 0.0) * 0.06));
    return mix(col, fogColor(rd), fog);
}

// Bulbs, star and their bloom, drawn over the scene with depth occlusion.
vec3 lights(vec3 ro, vec3 rd, float tHit) {
    vec3 col = vec3(0.0);
    for (int i = 0; i < BULBS; i++) {
        vec3 b = bulbPosition(i) - ro;
        float along = dot(b, rd);
        if (along < 0.0 || along > tHit + 0.3) continue;
        float d = length(b - rd * along) / along;
        col += bulbColor(i) * (exp(-d * d / 0.000025) * 2.6 + 0.00002 / (d * d + 0.00005));
    }
    vec3 s = STAR - ro;
    float along = dot(s, rd);
    if (along > 0.0 && along < tHit + 0.5) {
        vec3 fwd = normalize(s);
        vec3 side = normalize(cross(fwd, vec3(0.0, 1.0, 0.0)));
        vec3 up = cross(side, fwd);
        // Angular offset from the star, scaled so the star keeps its size on screen.
        vec2 q = vec2(dot(rd, side), dot(rd, up)) * 10.0;
        float r = length(q), a = atan(q.x, q.y);
        float shape = r / (0.65 + 0.35 * cos(a * 5.0));
        float pulse = 0.9 + 0.1 * sin(T * 3.0);
        vec3 gold = vec3(1.0, 0.8, 0.35);
        col += gold * smoothstep(0.07, 0.04, shape) * 4.0 * pulse;
        col += gold * 0.012 / (r * r + 0.0015) * pulse;
        // Four-point lens streaks.
        col += gold * (exp(-abs(q.y) * 220.0) * exp(-abs(q.x) * 4.0) + exp(-abs(q.x) * 220.0) * exp(-abs(q.y) * 6.0)) * 0.6 * pulse;
    }
    return col;
}

// Falling snow in depth layers; a layer is hidden where the scene is closer than it.
vec3 snowfall(vec2 p, float tHit) {
    vec3 col = vec3(0.0);
    for (int i = 0; i < 7; i++) {
        float fi = float(i);
        float depth = 1.2 + fi * 1.9;
        float visible = smoothstep(depth - 0.5, depth + 0.5, tHit);
        if (visible <= 0.0) continue;
        float density = 6.0 + fi * 3.4;
        vec2 q = p * density;
        q += mouse * density * 0.06 / depth;
        q.y += T * (2.6 / (1.0 + fi * 0.35)) * density * 0.12;
        q.x += sin(T * 0.4 + fi) * 0.6 + T * 0.25 * density * 0.05;
        vec2 cell = floor(q);
        vec2 h = hash22(cell + fi * 31.7);
        // Only a few flakes near the camera, more of them further away.
        if (h.x > mix(0.1, 0.45, fi / 6.0)) continue;
        vec2 f = fract(q) - 0.5;
        f -= (h - 0.5) * 0.6;
        f.x += sin(T * (1.0 + h.y) + h.x * 20.0) * 0.12;
        // Near flakes are big and soft (out of focus), far ones small and crisp.
        float size = mix(0.09, 0.03, fi / 6.0);
        float blur = mix(0.08, 0.01, fi / 6.0);
        float flake = smoothstep(size + blur, size - blur * 0.5, length(f));
        col += vec3(0.85, 0.9, 1.0) * flake * visible * mix(0.35, 0.75, fi / 6.0) * (0.6 + 0.4 * h.y);
    }
    return col;
}

vec3 aces(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
    T = ColorModulator.x;
    float aspect = ColorModulator.y;
    mouse = ColorModulator.zw;
    vec2 screenUv = texCoord;
    vec2 p = (screenUv * 2.0 - 1.0) * vec2(aspect, 1.0);

    // Slow orbit with a mouse parallax; the tree sits to the right of the menu.
    float angle = 0.36 + 0.05 * sin(T * 0.06) + mouse.x * 0.06;
    vec3 ro = vec3(sin(angle) * 10.5, 2.1 + mouse.y * 0.22 + 0.08 * sin(T * 0.13), cos(angle) * 10.5);
    ro.y = max(ro.y, terrain(ro.xz, 3) + 1.0);
    vec3 target = vec3(0.0, 2.6, 0.0);
    vec3 ww = normalize(target - ro);
    vec3 uu = normalize(cross(ww, vec3(0.0, 1.0, 0.0)));
    vec3 vv = cross(uu, ww);
    float shift = 0.42 * aspect;
    vec3 rd = normalize((p.x - shift) * uu + p.y * vv + 1.9 * ww);

    cabinBase = terrain(CABIN.xz, 4) - 0.1;
    placeBulbs();

    vec2 hit = march(ro, rd);
    vec3 col;
    float tHit = hit.y < 0.0 ? 1e4 : hit.x;
    if (hit.y < 0.0) {
        col = sky(rd, p);
        // Horizon haze where the sky meets the fogged ground.
        col = mix(col, fogColor(rd), exp(-max(rd.y, 0.0) * 14.0) * 0.85);
    } else {
        col = shade(ro + rd * hit.x, rd, hit.x, hit.y);
    }
    col += lights(ro, rd, tHit);
    col += snowfall(p, tHit);

    col = aces(col * 0.95);
    col = pow(col, vec3(1.0 / 2.2));
    // Vignette, and a darker left side under the menu buttons.
    vec2 uv = screenUv;
    col *= 0.45 + 0.55 * pow(16.0 * uv.x * uv.y * (1.0 - uv.x) * (1.0 - uv.y), 0.18);
    col *= mix(0.62, 1.0, smoothstep(0.05, 0.5, uv.x));
    // Grain against banding in the dark gradients.
    col += (hash12(gl_FragCoord.xy + fract(T) * 100.0) - 0.5) / 255.0;
    fragColor = vec4(col, 1.0);
}
