#version 150
#moj_import <ess-best:depth.glsl>

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

// HitEffects strike bolts: the exact same React Bits noise stack as the SkyShader bolt
// (fbm-displaced channel, 1/d glow falloff, 24Hz flicker), but instead of repainting the
// far-plane sky each bolt lives on a camera-facing billboard plane anchored at the victim:
// every pixel ray is intersected with that plane, the in-plane (sideways, height) offset
// becomes the channel uv, and the resulting glow is added over the scene color. The frame's
// depth copy occludes bolts behind walls unless Through Walls is on.
//
// On top of the sky bolt's fine fbm raggedness the channel gets a low-frequency centerline
// wander (so it bends and kinks instead of running straight down), one or two dim branches
// forking off and dying out mid-air, a soft wide halo around the core, and a stepped-leader
// reveal: while reveal < 1 only the part of the channel above the descending front exists,
// so the bolt visibly shoots down before the bright return stroke and impact splash.
// uResolutionTime = (resolution.x, resolution.y, time, strikeCount);
// uConfig = (throughWalls, unused, unused, unused);
// per strike: uStrikePos = (camera-relative impact point xyz, flash envelope),
//             uStrikeInfo = (fbm seed, scale, channel height, leader reveal 0..1),
//             uStrikeColor = (bolt rgb, unused).
layout(std140) uniform HitLightningData {
    mat4 uViewProj;
    mat4 uInvViewProj;
    vec4 uResolutionTime;
    vec4 uConfig;
    vec4 uStrikePos[8];
    vec4 uStrikeInfo[8];
    vec4 uStrikeColor[8];
};

#define OCTAVE_COUNT 10

float hash11(float p) {
    p = fract(p * .1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * .1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

mat2 rotate2d(float theta) {
    float c = cos(theta);
    float s = sin(theta);
    return mat2(c, -s, s, c);
}

float noise(vec2 p) {
    vec2 ip = floor(p);
    vec2 fp = fract(p);
    float a = hash12(ip);
    float b = hash12(ip + vec2(1.0, 0.0));
    float c = hash12(ip + vec2(0.0, 1.0));
    float d = hash12(ip + vec2(1.0, 1.0));

    vec2 t = smoothstep(0.0, 1.0, fp);
    return mix(mix(a, b, t.x), mix(c, d, t.x), t.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < OCTAVE_COUNT; ++i) {
        value += amplitude * noise(p);
        p *= rotate2d(0.45);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

// Cheaper 4-octave fbm for the smooth low-frequency centerline wander.
float fbm4(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; ++i) {
        value += amplitude * noise(p);
        p *= rotate2d(0.45);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

// Low-frequency sideways drift of the channel centerline - this is what stops the bolt
// from being a straight vertical line. Tapered to zero toward the impact point so the
// bolt still connects with the victim.
float channelWander(float v, float seed, float scale, float height) {
    float w = 2.0 * fbm4(vec2(v * 0.10 / scale, seed * 7.7)) - 1.0;
    return w * 2.4 * scale * smoothstep(0.0, height * 0.4, v);
}

void main() {
    vec3 col = texture(Sampler0, texCoord).rgb;
    float depth = texture(Sampler1, texCoord).r;
    float time = uResolutionTime.z;
    int count = int(uResolutionTime.w + 0.5);
    bool throughWalls = uConfig.x > 0.5;

    // Unproject this pixel into a world-space view direction (camera at the origin).
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 farPoint = uInvViewProj * vec4(ndc, essDepthToNdc(0.000001), 1.0);
    float pw = abs(farPoint.w) < 1e-6 ? 1e-6 : farPoint.w;
    vec3 dir = normalize(farPoint.xyz / pw);

    for (int i = 0; i < 8; ++i) {
        if (i >= count) break;
        float flash = uStrikePos[i].w;
        if (flash < 0.004) continue;
        vec3 base = uStrikePos[i].xyz;
        float seed = uStrikeInfo[i].x;
        float scale = uStrikeInfo[i].y;
        float height = uStrikeInfo[i].z;
        float reveal = uStrikeInfo[i].w;

        // Vertical billboard plane through the channel, always facing the camera; degenerate
        // (and skipped) only when the camera is directly above/below the strike.
        float horizLen = length(base.xz);
        if (horizLen < 0.05) continue;
        vec3 n = vec3(base.x / horizLen, 0.0, base.z / horizLen);
        float denom = dot(dir, n);
        if (abs(denom) < 1e-4) continue;
        float t = horizLen / denom;
        if (t < 0.0) continue;
        vec3 p = dir * t;

        // In-plane coordinates: v = height above the impact point, u = sideways offset.
        float v = p.y - base.y;
        if (v < -2.0 * scale || v > height + 2.0 * scale) continue;
        float u = (p.x - base.x) * (-n.z) + (p.z - base.z) * n.x;
        // Generous bound on how far sideways the wandering channel or a leaning branch can
        // ever reach - keeps the expensive noise work off most of the screen.
        if (abs(u) > 8.0 + 6.0 * scale) continue;

        // Depth occlusion: skip pixels where the scene is in front of the bolt plane.
        if (!throughWalls) {
            vec4 clip = uViewProj * vec4(p, 1.0);
            float boltDepth = essNdcToDepth(clip.z / clip.w);
            if (depth > boltDepth + 5e-4) continue;
        }

        // The stepped-leader front: everything below it does not exist yet, so during the
        // first strike moments the channel visibly shoots down onto the victim.
        float front = height * (1.0 - reveal);
        float glow = 0.0;

        // Main channel: centerline wander + the sky bolt's fbm displacement of the channel
        // uv (fbm of the undisplaced uv shifts buv.x into the jagged channel). Fine
        // displacement converges toward the impact point like the wander does.
        float wanderHere = channelWander(v, seed, scale, height);
        vec2 buv = vec2((u - wanderHere) * 1.5, v * 0.22) / scale;
        if (abs(buv.x) < 4.5) {
            float amp = mix(0.25, 1.3, smoothstep(0.0, height * 0.3, v));
            buv.x += amp * (2.0 * fbm(buv * 0.8 + vec2(seed, seed * 0.57)) - 1.0);
            float d = max(abs(buv.x), 0.003);
            float ends = smoothstep(-1.2, -0.15, v) * (1.0 - smoothstep(height * 0.85, height, v));
            ends *= smoothstep(front - 1.5, front, v);
            // Sharp 1/d core plus a soft wide halo of scattered light around it.
            glow += (0.045 / d + 0.020 / (d * d + 0.35)) * ends;
        }

        // Dim branches forking off the channel and dying out mid-air: each starts on the
        // wandering centerline at its fork height and walks sideways as it descends, with
        // its own (raggeder, thinner) fbm displacement.
        for (int b = 0; b < 2; ++b) {
            float bseed = seed + float(b) * 17.3 + 5.1;
            if (b == 1 && hash11(seed * 9.1) < 0.35) continue;
            float forkV = height * (0.30 + 0.35 * hash11(bseed));
            float lsign = hash11(bseed * 3.9) < 0.5 ? -1.0 : 1.0;
            float lean = lsign * (0.35 + 0.85 * hash11(bseed * 1.7));
            float len = height * (0.15 + 0.15 * hash11(bseed * 2.3));
            float bv = forkV - v;
            if (bv < 0.0 || bv > len) continue;
            float centre = channelWander(forkV, seed, scale, height) + lean * bv;
            vec2 bb = vec2((u - centre) * 1.5, v * 0.22) / scale;
            if (abs(bb.x) > 3.0) continue;
            bb.x += 0.8 * (2.0 * fbm(bb * 1.1 + vec2(bseed * 0.91, bseed * 0.37)) - 1.0);
            float bd = max(abs(bb.x), 0.004);
            float tip = 1.0 - smoothstep(len * 0.4, len, bv);
            tip *= smoothstep(front - 1.5, front, v);
            glow += (0.028 / bd) * 0.5 * tip;
        }

        // Multi-stroke envelope comes from the CPU; the 24Hz flicker lives here like the
        // sky bolt's.
        float flick = 0.75 + 0.25 * hash11(seed * 3.7 + floor(time * 24.0));
        col += uStrikeColor[i].rgb * glow * flash * flick;

        // Impact splash: a radial burst where the channel meets the victim, popping in
        // only once the leader has actually connected.
        vec2 ipos = vec2(u, v) / scale;
        float splash = smoothstep(0.85, 1.0, reveal) * 0.35 / (1.0 + dot(ipos, ipos) * 14.0);
        col += uStrikeColor[i].rgb * splash * flash * flick;
    }

    fragColor = vec4(col, 1.0);
}
