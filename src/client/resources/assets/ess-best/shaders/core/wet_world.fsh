#version 150
#moj_import <ess-best:depth.glsl>

uniform sampler2D Scene;
uniform sampler2D DepthSampler;

layout(std140) uniform ReflectParams {
    vec4 camPosTime;
    vec4 skyTintReflectivity;
    vec4 sunDirWetness;
    vec4 surface;
    vec4 trace;
    mat4 invViewProj;
    mat4 viewProj;
};

in vec2 texCoord;
out vec4 fragColor;

const int MAX_STEPS = 48;
const int REFINE_STEPS = 4;
const float STEP_GROWTH = 0.12;
const float SURFACE_BIAS = 0.035;
const float EDGE_FADE = 0.12;
const float SILHOUETTE_LIMIT = 0.55;
const float SPEC_POWER = 190.0;

vec3 worldFromDepth(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, essDepthToNdc(depth), 1.0);
    vec4 pos = invViewProj * clip;
    return pos.xyz / pos.w;
}

vec3 faceNormal(vec2 uv, float depth, vec3 view, out float discontinuity) {
    vec2 texel = 1.0 / vec2(textureSize(DepthSampler, 0));
    vec2 uvL = uv - vec2(texel.x, 0.0);
    vec2 uvR = uv + vec2(texel.x, 0.0);
    vec2 uvD = uv - vec2(0.0, texel.y);
    vec2 uvU = uv + vec2(0.0, texel.y);
    float dL = texture(DepthSampler, uvL).r;
    float dR = texture(DepthSampler, uvR).r;
    float dD = texture(DepthSampler, uvD).r;
    float dU = texture(DepthSampler, uvU).r;
    bool leftCloser = abs(dL - depth) < abs(dR - depth);
    bool downCloser = abs(dD - depth) < abs(dU - depth);
    vec3 ddx = leftCloser ? view - worldFromDepth(uvL, dL) : worldFromDepth(uvR, dR) - view;
    vec3 ddy = downCloser ? view - worldFromDepth(uvD, dD) : worldFromDepth(uvU, dU) - view;
    discontinuity = max(length(ddx), length(ddy)) / max(length(view), 1.0);
    vec3 n = cross(ddx, ddy);
    float len = length(n);
    if (len < 1e-8) return normalize(-view);
    n /= len;
    return dot(n, -view) < 0.0 ? -n : n;
}

float interleavedGradient(vec2 pixel) {
    return fract(52.9829189 * fract(dot(pixel, vec2(0.06711056, 0.00583715))));
}

float edgeFade(vec2 uv) {
    vec2 fade = smoothstep(vec2(0.0), vec2(EDGE_FADE), uv)
              * smoothstep(vec2(0.0), vec2(EDGE_FADE), 1.0 - uv);
    return fade.x * fade.y;
}

void main() {
    vec4 src = texture(Scene, texCoord);
    float depth = texture(DepthSampler, texCoord).r;
    if (depth <= 0.000001 || sunDirWetness.w <= 0.001) { fragColor = src; return; }

    vec3 view = worldFromDepth(texCoord, depth);
    float discontinuity;
    vec3 normal = faceNormal(texCoord, depth, view, discontinuity);
    if (discontinuity > SILHOUETTE_LIMIT) { fragColor = src; return; }

    float mask = mix(1.0, smoothstep(0.5, 0.82, normal.y), trace.y);
    if (mask <= 0.002) { fragColor = src; return; }

    vec3 incident = normalize(view);
    float lum = dot(src.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 soaked = mix(vec3(lum), src.rgb, 1.0 + 0.4 * sunDirWetness.w)
                * (1.0 - 0.32 * sunDirWetness.w);
    soaked = mix(src.rgb, soaked, mask);
    vec3 reflected = reflect(incident, normal);
    vec3 origin = view + normal * SURFACE_BIAS;

    int steps = clamp(int(surface.z), 8, MAX_STEPS);
    float stepLength = trace.x / float(steps);
    float travelled = stepLength * (0.35 + interleavedGradient(gl_FragCoord.xy) * 0.65);
    float previous = travelled;
    vec2 hitUv = vec2(0.0);
    float hit = 0.0;

    for (int i = 0; i < MAX_STEPS; i++) {
        if (i >= steps || skyTintReflectivity.w <= 0.001) break;
        vec3 marchPos = origin + reflected * travelled;
        vec4 clip = viewProj * vec4(marchPos, 1.0);
        if (clip.w <= 0.001) break;
        vec2 uv = (clip.xy / clip.w) * 0.5 + 0.5;
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) break;
        float sceneDepth = texture(DepthSampler, uv).r;
        if (sceneDepth > 0.000001) {
            vec3 seen = worldFromDepth(uv, sceneDepth);
            float behind = length(marchPos) - length(seen);
            if (behind > 0.0 && behind < surface.w + travelled * 0.08
                    && dot(seen - view, normal) > 0.04) {
                float lo = previous;
                float hi = travelled;
                for (int r = 0; r < REFINE_STEPS; r++) {
                    float mid = (lo + hi) * 0.5;
                    vec3 probe = origin + reflected * mid;
                    vec4 probeClip = viewProj * vec4(probe, 1.0);
                    vec2 probeUv = clamp((probeClip.xy / max(probeClip.w, 1e-4)) * 0.5 + 0.5, 0.0, 1.0);
                    float probeDepth = texture(DepthSampler, probeUv).r;
                    float probeBehind = length(probe) - length(worldFromDepth(probeUv, probeDepth));
                    if (probeBehind > 0.0) { hi = mid; uv = probeUv; } else { lo = mid; }
                }
                if (dot(worldFromDepth(uv, texture(DepthSampler, uv).r) - view, normal) > 0.025) {
                    hitUv = uv;
                    hit = edgeFade(uv) * (1.0 - smoothstep(trace.x * 0.6, trace.x, hi));
                    break;
                }
            }
        }
        previous = travelled;
        travelled += stepLength * (1.0 + float(i) * STEP_GROWTH);
    }

    vec3 reflection = mix(skyTintReflectivity.xyz, texture(Scene, hitUv).rgb, hit);
    float fresnel = 0.02 + 0.98 * pow(1.0 - clamp(dot(normal, -incident), 0.0, 1.0), 5.0);
    float amount = clamp(skyTintReflectivity.w * (0.12 + 0.88 * fresnel), 0.0, 1.0) * mask * sunDirWetness.w;
    vec3 halfway = normalize(sunDirWetness.xyz - incident);
    float specular = pow(max(dot(normal, halfway), 0.0), SPEC_POWER) * surface.y * mask
                   * max(sunDirWetness.y, 0.0) * skyTintReflectivity.w * sunDirWetness.w;
    vec3 lit = mix(soaked, reflection, amount) + specular;
    fragColor = vec4(mix(src.rgb, clamp(lit, 0.0, 1.0), trace.z), src.a);
}
