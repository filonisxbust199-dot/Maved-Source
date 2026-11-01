#version 150
#moj_import <ess-best:depth.glsl>

uniform sampler2D Sampler0;     // full-res scene color
uniform sampler2D DepthSampler; // full-res scene depth copy

layout(std140) uniform BlurParams {
    vec4 Blur; // xy = per-tap uv step (direction * spread / size)
};

layout(std140) uniform FogParams {
    mat4 InvViewProj; // NDC -> camera-relative world
    vec4 Fog;  // x = opacity, y = camera world Y, z = visibility distance, w unused
    vec4 Fog2; // x = minRatio, y = midRatio, z = maxRatio, w unused
    vec4 Tint; // rgb = fog tint, a = tint strength (0 = untinted) — used by the composite
};

in vec2 texCoord;
out vec4 fragColor;

// world-anchored height fog: full density at/below sea level, thinning with altitude
const float FOG_BASE_Y = 62.0;
const float FOG_FALLOFF = 1.0 / 28.0;

// exact average fog density along the ray between two world heights (piecewise closed
// form: the below-sea part rides at density 1, the part above decays exponentially)
float heightAvg(float yA, float yB) {
    float yLow = min(yA, yB);
    float yHigh = max(yA, yB);
    float span = yHigh - yLow;
    if (span < 1e-3) return exp(-max(0.0, yA - FOG_BASE_Y) * FOG_FALLOFF);
    float below = clamp((FOG_BASE_Y - yLow) / span, 0.0, 1.0);
    float a1 = max(yLow, FOG_BASE_Y) - FOG_BASE_Y;
    float a2 = max(yHigh, FOG_BASE_Y) - FOG_BASE_Y;
    float above = (exp(-a1 * FOG_FALLOFF) - exp(-a2 * FOG_FALLOFF)) / (span * FOG_FALLOFF);
    return clamp(below + above, 0.0, 1.0);
}

// optical depth of the height fog along the camera ray, normalized by visibility:
// the pixel's TRUE world position is reconstructed from depth — the fog is 3D
float fogRatio(vec2 uv) {
    float d = texture(DepthSampler, uv).r;
    vec4 ndc = vec4(uv * 2.0 - 1.0, essDepthToNdc(d), 1.0);
    vec4 wp = InvViewProj * ndc;
    vec3 world = wp.xyz / wp.w;                        // camera-relative world position
    float dist = length(world);
    float avg = heightAvg(Fog.y, Fog.y + world.y);
    return clamp(dist * avg / Fog.z, 0.0, 1.0);
}

// fog-masked premultiplied gaussian: every tap is weighted by its optical depth, so
// near/clear geometry contributes NOTHING to the fog blur — no silhouettes smearing
// into the haze and no haze halos over crisp objects. Alpha accumulates the fog
// coverage and rides down the blur pyramid; the composite renormalizes by it.
void main() {
    float w[5] = float[](0.227027, 0.194595, 0.121622, 0.054054, 0.016216);
    vec2 s = Blur.xy;

    vec4 acc = vec4(0.0);
    for (int i = -4; i <= 4; i++) {
        vec2 uv = texCoord + s * float(i);
        float m = smoothstep(Fog2.x, Fog2.z, fogRatio(uv));
        acc += vec4(texture(Sampler0, uv).rgb * m, m) * w[abs(i)];
    }
    fragColor = acc;
}
