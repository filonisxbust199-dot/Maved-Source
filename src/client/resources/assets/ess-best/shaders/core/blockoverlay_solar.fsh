#version 330
#extension GL_ARB_separate_shader_objects : require

// BlockOverlay «Shader» — the CustomWorld Solar look mapped onto the block faces:
// domain-warped boiling sun cells (the exact sky recipe), anchored to WORLD coords
// so the pattern lives on the block, not on the screen.

layout(std140) uniform SolarParams {
    vec4 Cam;       // xyz = camera world position (wrapped), w = time seconds
    vec4 Primary;   // theme primary
    vec4 Secondary; // theme secondary
    vec4 Accent;    // rgb = hot accent, w = pattern scale
};

layout(location = 0) in vec3 fragPos;      // camera-relative surface position
layout(location = 1) in vec4 vertexColor;  // alpha carries presence
layout(location = 0) out vec4 fragColor;

float hash3(vec3 p) {
    uvec3 bits = floatBitsToUint(p + vec3(0.0));
    uint h = bits.x * 1597334677u ^ bits.y * 3812015801u ^ bits.z * 2798796415u;
    h = (h ^ (h >> 16u)) * 2246822519u;
    h = (h ^ (h >> 13u)) * 3266489917u;
    return float((h ^ (h >> 16u)) & 0x00ffffffu) / 16777216.0;
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);

    float n000 = hash3(i);
    float n100 = hash3(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash3(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash3(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash3(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash3(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash3(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash3(i + vec3(1.0, 1.0, 1.0));

    return mix(mix(mix(n000, n100, u.x), mix(n010, n110, u.x), u.y),
               mix(mix(n001, n101, u.x), mix(n011, n111, u.x), u.y), u.z);
}

float fbm3(vec3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    mat3 rot = mat3(
        0.00, 0.80, 0.60,
       -0.80, 0.36, -0.48,
       -0.60, -0.48, 0.64
    );

    for (int i = 0; i < 5; i++) {
        value += noise3(p) * amplitude;
        p = rot * p * 1.95 + vec3(8.7, 4.9, 6.3);
        amplitude *= 0.56;
    }

    return value;
}

void main() {
    float t = Cam.w;
    float invScale = 1.0 / max(Accent.w, 0.001);
    vec3 world = fragPos + Cam.xyz;
    vec3 domain = world * (2.2 * invScale);

    // domain-warped boiling cells — the Solar sky recipe verbatim
    vec3 warp = vec3(
        noise3(domain + vec3(t * 0.10, 0.0, t * 0.07)),
        noise3(domain + vec3(4.2, t * 0.08, 1.7)),
        noise3(domain - vec3(t * 0.06, 2.9, t * 0.09)));
    float cells = fbm3(domain * 1.9 + (warp - 0.5) * 1.6 + vec3(0.0, t * 0.05, 0.0));
    float granule = smoothstep(0.30, 0.85, cells);
    float core = smoothstep(0.62, 0.95, cells);

    vec3 color = mix(Secondary.rgb * 0.7, Primary.rgb, granule);
    color = mix(color, Accent.rgb, core * 0.75);
    color = mix(color, vec3(1.0, 0.97, 0.85), smoothstep(0.80, 0.98, cells) * 0.45);

    // slow global breathing
    color *= 0.9 + 0.1 * sin(t * 0.6 + cells * 4.0);

    fragColor = vec4(color, vertexColor.a);
}
