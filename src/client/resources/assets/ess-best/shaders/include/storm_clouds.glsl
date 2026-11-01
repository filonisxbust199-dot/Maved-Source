#moj_import <ess-best:depth.glsl>
layout(std140) uniform CloudParams {
    mat4 InvProjection;
    mat4 InvView;
    mat4 InvSceneProjection;
    vec4 Resolution;
    vec4 CameraTime;
    vec4 Layer;
    vec4 Weather;
    vec4 LightColor;
    vec4 ShadowColor;
    vec4 SunSteps;
    vec4 StyleFlash;
};
uniform sampler2D DepthSampler;
vec2 safeUv(vec2 uv) { return clamp(uv, vec2(.00001), vec2(.99999)); }
vec3 viewPosition(vec2 uv, float depth) {
    vec4 p = InvProjection * vec4(uv * 2.0 - 1.0, essDepthToNdc(depth), 1);
    return p.xyz / max(abs(p.w), .000001) * sign(p.w);
}
float sceneDistance(vec2 uv) {
    float d = texture(DepthSampler, safeUv(uv)).r;
    if (d <= 0.000001) return Layer.w * 2.0;
    vec4 p = InvSceneProjection * vec4(uv * 2.0 - 1.0, essDepthToNdc(d), 1.0);
    return length(p.xyz / max(abs(p.w), .000001));
}
float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * .1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x), mix(hash(i + vec2(0, 1)), hash(i + 1.0), f.x), f.y);
}
float fbm(vec2 p) { return noise(p) * .57 + noise(p * 2.04 + 17.7) * .28 + noise(p * 4.13 - 8.3) * .15; }
float hash3(vec3 p) {
    p = fract(p * .1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}
float volumeNoise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = mix(mix(hash3(i), hash3(i + vec3(1, 0, 0)), f.x),
                  mix(hash3(i + vec3(0, 1, 0)), hash3(i + vec3(1, 1, 0)), f.x), f.y);
    float b = mix(mix(hash3(i + vec3(0, 0, 1)), hash3(i + vec3(1, 0, 1)), f.x),
                  mix(hash3(i + vec3(0, 1, 1)), hash3(i + vec3(1, 1, 1)), f.x), f.y);
    return mix(a, b, f.z);
}
float cloudDensity(vec3 p) {
    float h = (p.y - Layer.x) / Layer.y;
    if (h <= 0.0 || h >= 1.0) return 0.0;
    vec2 wind = vec2(cos(Weather.w), sin(Weather.w)) * CameraTime.w * Weather.z;
    vec2 q = (p.xz - wind) / (120.0 * Layer.z);
    int style = int(StyleFlash.x);
    if (style == 2) q *= vec2(.58, 1.8);
    vec2 warp = vec2(noise(q * .43 + vec2(7.1, 1.8)),
                     noise(q * .43 + vec2(-5.3, 11.4))) - .5;
    q += warp * .38;
    float macro = fbm(q);
    if (style == 1) macro = mix(macro, noise(q * vec2(.62, 1.4) + 10.0), .18);
    float threshold = mix(.80, .33, Weather.x) + (style == 0 ? .04 : 0.0);
    // Empty columns never need the expensive three-dimensional detail.
    if (macro < threshold - .25) return 0.0;

    vec3 cell = vec3(q.x * 2.35, h * 3.2, q.y * 2.35);
    float billow = volumeNoise(cell + vec3(7.3, 1.6, -4.2));
    float erosion = volumeNoise(cell * 3.0 - vec3(2.7, 9.3, 6.1));
    float field = macro + (billow - .5) * .32 - (erosion - .5) * .09;
    float bottom = .035, top = .91;
    if (style == 0) { // Flat bottoms and differently sized cauliflower towers.
        bottom = .055;
        top = .52 + clamp((macro - threshold) * 1.15, 0.0, .4);
    } else if (style == 1) { // Deep, uneven storm shelf.
        bottom += noise(q * 1.8 + 8.1) * .09;
        top = .84 + billow * .13;
        field += .035;
    } else if (style == 2) { // Wide wind-sheared layers.
        bottom = .16;
        top = .72;
        field += sin(h * 20.0 + macro * 5.0) * .025;
    } else { // Hanging mammatus pockets under a broad ceiling.
        bottom = .34 - noise(q * 3.4 + 9.4) * .31;
        top = .84 + macro * .11;
        field += .025;
    }
    float envelope = smoothstep(bottom, bottom + .09, h)
                   * (1.0 - smoothstep(top - .17, top, h));
    float body = smoothstep(threshold - .075, threshold + .11, field);
    return body * envelope * Weather.y * (.68 + billow * .32);
}
