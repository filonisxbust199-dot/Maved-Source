#version 150
#moj_import <ess-best:depth.glsl>

uniform sampler2D LightSampler; // half-res light blur (premultiplied fog color)
uniform sampler2D HeavySampler; // quarter-res heavy blur (premultiplied fog color)
uniform sampler2D DepthSampler; // a copy of the scene depth (sampleable)

layout(std140) uniform FogParams {
    mat4 InvViewProj; // NDC -> camera-relative world
    vec4 Fog;  // x = opacity, y = camera world Y, z = visibility distance, w unused
    vec4 Fog2; // x = minRatio, y = midRatio, z = maxRatio, w unused
    vec4 Tint; // rgb = fog tint, a = tint strength (0 = untinted)
};

in vec2 texCoord;
out vec4 fragColor;

// world-anchored height fog: full density at/below sea level, thinning with altitude —
// valleys drown, mountain tops stay crisp, and from a peak you see the fog layer itself
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

// optical depth of the height fog along the camera ray, normalized by visibility
float fogRatio(vec2 uv) {
    float d = texture(DepthSampler, uv).r;
    vec4 ndc = vec4(uv * 2.0 - 1.0, essDepthToNdc(d), 1.0);
    vec4 wp = InvViewProj * ndc;
    vec3 world = wp.xyz / wp.w;                        // camera-relative world position
    float dist = length(world);
    float avg = heightAvg(Fog.y, Fog.y + world.y);
    return clamp(dist * avg / Fog.z, 0.0, 1.0);
}

void main() {
    float ratio = fogRatio(texCoord);

    // presence ramps with optical depth; the blur grows from the light level toward the
    // heavy level as the ray sinks deeper into the fog
    float present = smoothstep(Fog2.x, Fog2.z, ratio);
    if (present <= 0.001) {
        discard;
    }
    float heavyMix = smoothstep(Fog2.y, Fog2.z, ratio);

    // the pyramid carries FOG-ONLY premultiplied color (clear pixels weighed 0 in the mask
    // pass) with fog coverage in alpha — renormalize to get the average of foggy pixels
    // alone, so nothing crisp ever bleeds into the haze
    vec4 light = texture(LightSampler, texCoord);
    vec4 heavy = texture(HeavySampler, texCoord);
    vec4 fog4 = mix(light, heavy, heavyMix);
    vec3 blurred = fog4.rgb / max(fog4.a, 0.003);

    // fade where the kernel barely touched any fog (thin borders), killing renorm noise
    float t = present * Fog.x * clamp(fog4.a * 2.0, 0.0, 1.0);
    if (t <= 0.002) {
        discard;
    }

    // colored fog: the tint is modulated by the scene's own brightness, so the haze takes
    // the hue but keeps its structure (bright terrain stays bright) instead of going flat.
    // It deepens with optical depth, like a real colored medium accumulating along the ray.
    if (Tint.a > 0.001) {
        float lum = dot(blurred, vec3(0.299, 0.587, 0.114));
        vec3 tinted = Tint.rgb * (0.35 + 0.65 * lum);
        blurred = mix(blurred, tinted, clamp(Tint.a * present, 0.0, 1.0));
    }

    fragColor = vec4(blurred, t); // alpha-blended over the sharp scene
}
