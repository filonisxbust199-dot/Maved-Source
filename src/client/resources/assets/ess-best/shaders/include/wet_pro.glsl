#moj_import <ess-best:depth.glsl>
layout(std140) uniform WetProParams {
    mat4 View;
    mat4 Projection;
    mat4 InvProjection;
    mat4 InvView;
    vec4 Resolution;
    vec4 CameraTime;
    vec4 WetReflectionDistanceCoverage;
    vec4 SizeSoftnessFilmRoughness;
    vec4 RippleSpeedWindStyle;
    vec4 Tint;
    vec4 SkySteps;
};
uniform sampler2D DepthSampler;
uniform sampler2D Scene;

float sat(float x) { return clamp(x, 0.0, 1.0); }
vec2 safeUv(vec2 uv) { return clamp(uv, vec2(0.00001), vec2(0.99999)); }
float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x),
               mix(hash(i + vec2(0, 1)), hash(i + 1.0), f.x), f.y);
}
vec3 positionFromDepth(vec2 uv, float depth) {
    vec4 p = InvProjection * vec4(uv * 2.0 - 1.0, essDepthToNdc(depth), 1.0);
    return p.xyz / max(abs(p.w), 0.000001) * sign(p.w);
}
vec3 normalFromDepth(vec2 uv, vec3 p) {
    vec2 texel = 1.0 / Resolution.xy;
    vec2 l = safeUv(uv - vec2(texel.x, 0)), r = safeUv(uv + vec2(texel.x, 0));
    vec2 d = safeUv(uv - vec2(0, texel.y)), u = safeUv(uv + vec2(0, texel.y));
    vec3 dl = p - positionFromDepth(l, texture(DepthSampler, l).r);
    vec3 dr = positionFromDepth(r, texture(DepthSampler, r).r) - p;
    vec3 dd = p - positionFromDepth(d, texture(DepthSampler, d).r);
    vec3 du = positionFromDepth(u, texture(DepthSampler, u).r) - p;
    vec3 dx = dot(dl, dl) < dot(dr, dr) ? dl : dr;
    vec3 dy = dot(dd, dd) < dot(du, du) ? dd : du;
    // Test the angle between tangents, not their world-space area. At 1080p+
    // near-floor pixels cover a tiny area; an absolute cross-product cutoff
    // rejected the entire bottom of the screen as a degenerate surface.
    float dx2 = dot(dx, dx), dy2 = dot(dy, dy);
    if (dx2 < 1e-20 || dy2 < 1e-20 || max(sqrt(dx2), sqrt(dy2)) > max(length(p) * .18, .5)) return vec3(0);
    vec3 n = cross(dx * inversesqrt(dx2), dy * inversesqrt(dy2));
    if (dot(n, n) < 1e-8) return vec3(0);
    n = normalize(n);
    return dot(n, -p) < 0.0 ? -n : n;
}
float puddleField(vec2 p) {
    p /= SizeSoftnessFilmRoughness.x;
    float base = noise(p * 1.7) * .63 + noise(p * 3.61 + vec2(7.2, 17.3)) * .26;
    base += noise(p * 7.4 + vec2(12.3, -4.1)) * .11;
    int style = int(RippleSpeedWindStyle.w);
    if (style == 1) base = mix(base, noise(p * vec2(.65, 2.1)), .38);
    if (style == 2) base = mix(base, .5 + .5 * sin(p.x * 2.4 + noise(p * 1.7) * 5.0) * sin(p.y * 1.8), .32);
    float coverage = WetReflectionDistanceCoverage.w;
    if (coverage <= .001) return 0.0;
    float threshold = mix(.85, .2, coverage);
    float feather = mix(.015, .17, SizeSoftnessFilmRoughness.y);
    return smoothstep(threshold - feather, threshold + feather, base);
}
float surfaceAt(vec2 uv, out vec3 p, out vec3 n, out vec3 world, out float pool) {
    p = vec3(0); n = vec3(0); world = vec3(0); pool = 0.0;
    if (WetReflectionDistanceCoverage.x <= .001) return 0.0;
    float depth = texture(DepthSampler, uv).r;
    if (depth <= 0.000001) return 0.0;
    p = positionFromDepth(uv, depth);
    float distance = length(p);
    if (distance > WetReflectionDistanceCoverage.z || distance < .35) return 0.0;
    n = normalFromDepth(uv, p);
    if (dot(n, n) < .5) return 0.0;
    float up = (mat3(InvView) * n).y;
    if (up < .68) return 0.0;
    world = (InvView * vec4(p, 1)).xyz + CameraTime.xyz;
    pool = puddleField(world.xz);
    float film = mix(SizeSoftnessFilmRoughness.z, 1.0, pool);
    return WetReflectionDistanceCoverage.x * film * smoothstep(.68, .92, up)
            * (1.0 - smoothstep(WetReflectionDistanceCoverage.z * .75, WetReflectionDistanceCoverage.z, distance))
            * smoothstep(.35, .8, distance);
}
float ringHeight(vec2 p, float scale, float time) {
    vec2 cell = floor(p * scale), local = fract(p * scale) - .5;
    vec2 center = vec2(hash(cell + 7.3), hash(cell - 13.7)) * .56 - .28;
    float phase = fract(time * .67 + hash(cell));
    float delta = length(local - center) - phase * .62;
    return sin(delta * 53.0) * exp(-abs(delta) * 33.0) * pow(1.0 - phase, 2.0) * smoothstep(0.0, .12, phase);
}
float waterHeight(vec2 p) {
    float t = CameraTime.w * RippleSpeedWindStyle.y;
    float height = 0.0;
    if (RippleSpeedWindStyle.x > .001) {
        height = ringHeight(p, .65, t) + ringHeight(p + 13.2, 1.1, t * 1.13) * .45;
        height *= RippleSpeedWindStyle.x * .013;
    }
    if (RippleSpeedWindStyle.z > .001) {
        height += (sin(p.x * 2.7 + p.y * .8 + t * 1.3) + sin(p.y * 3.1 - t * .9)) * RippleSpeedWindStyle.z * .012;
    }
    return height;
}
vec3 waterNormal(vec3 original, vec2 p, float pool) {
    if (RippleSpeedWindStyle.x + RippleSpeedWindStyle.z <= .001) return original;
    float e = .04;
    float dx = (waterHeight(p + vec2(e, 0)) - waterHeight(p - vec2(e, 0))) / (2.0 * e);
    float dz = (waterHeight(p + vec2(0, e)) - waterHeight(p - vec2(0, e))) / (2.0 * e);
    vec3 ripple = normalize(mat3(View) * normalize(vec3(-dx, 1.0, -dz)));
    return normalize(mix(original, ripple, pool * .85));
}
vec2 project(vec3 p, out bool valid) {
    vec4 clip = Projection * vec4(p, 1);
    vec2 uv = clip.xy / max(clip.w, .00001) * .5 + .5;
    valid = clip.w > .00001 && all(greaterThan(uv, vec2(.001))) && all(lessThan(uv, vec2(.999)));
    return safeUv(uv);
}
