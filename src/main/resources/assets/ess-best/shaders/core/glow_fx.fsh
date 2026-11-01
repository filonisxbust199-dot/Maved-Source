#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 local;
layout(location = 2) flat in ivec2 kind;
layout(location = 3) flat in vec2 params;

layout(location = 0) out vec4 fragColor;

const float TAU = 6.2831853;

// Glowing world effects (Trails, HitBubbles, BlockOverlay). Brightness is worked out as light added to the scene,
// then turned into colour and coverage, so the glow keeps its tint over bright blocks and sky as well:
// 0 line: a white-hot core of the given width with a glow fading to the quad's edge;
// 1 ring: a ring spreading out from a hit and fading, with a flash inside;
// 2 burst: the ring of a critical hit with eight rays shooting out of it;
// 3 dot: a round glowing particle;
// 4 sheet: a vertical band, brighter along its top and bottom edges;
// 5 fill: a flat tint.
void main() {
    int mode = kind.x;
    float clock = float(kind.y) / 1000.0;
    float glow = params.x, param = params.y;
    vec3 tint = vertexColor.rgb;
    float strength = vertexColor.a;
    vec3 color;

    if (mode == 0) {
        float across = abs(local.y);
        float core = 1.0 - smoothstep(param * 0.55, param, across);
        float outside = clamp((across - param * 0.5) / max(1.0 - param * 0.5, 1e-3), 0.0, 1.0);
        float halo = glow * (pow(1.0 - outside, 2.0) * 1.1 + exp(-outside * 6.0) * 0.7);
        color = mix(tint, vec3(1.0), 0.3) * core * 1.2 + tint * halo;
    } else if (mode == 1 || mode == 2) {
        float r = length(local);
        if (r > 1.0) discard;
        float t = param;
        float grow = 1.0 - pow(1.0 - t, 2.4);
        float size = 0.18 + 0.7 * grow;
        float width = 0.035 + 0.05 * (1.0 - t);
        float d = abs(r - size);
        float core = 1.0 - smoothstep(width * 0.4, width, d);
        float halo = exp(-d * (7.0 + 10.0 * (1.0 - glow))) * glow;
        float flash = (1.0 - smoothstep(0.0, size, r)) * pow(1.0 - t, 2.0) * 0.55;
        color = tint * (core * 1.3 + halo * 0.9 + flash) + vec3(1.0) * core * 0.5;
        if (mode == 2) {
            float a = atan(local.y, local.x) + clock * TAU * 0.15;
            float ray = pow(abs(cos(a * 4.0)), 60.0);
            float reach = size * (1.0 + 0.35 * grow);
            float band = smoothstep(size * 0.55, size * 0.75, r) * (1.0 - smoothstep(reach * 0.9, reach, r));
            color += (tint * 1.2 + vec3(0.6)) * ray * band;
        }
        color *= pow(1.0 - t, 1.3) * smoothstep(0.0, 0.05, t) * (1.0 - smoothstep(0.92, 1.0, r));
    } else if (mode == 3) {
        float r = length(local);
        if (r > 1.0) discard;
        float core = 1.0 - smoothstep(0.16, 0.34, r);
        float halo = exp(-r * 3.2) * (0.5 + glow * 1.2) * (1.0 - smoothstep(0.75, 1.0, r));
        color = tint * (core * 1.3 + halo) + vec3(1.0) * core * 0.6;
    } else if (mode == 4) {
        float edge = max(1.0 - smoothstep(0.0, 0.14, local.y), 1.0 - smoothstep(0.0, 0.14, 1.0 - local.y));
        color = tint * (0.22 + edge * (0.55 + glow * 0.6)) + vec3(1.0) * edge * edge * 0.25;
    } else {
        fragColor = vec4(tint, param * strength);
        return;
    }
    color *= strength;
    float coverage = min(max(color.r, max(color.g, color.b)), 1.0);
    if (coverage < 0.004) discard;
    fragColor = vec4(min(color / coverage, vec3(1.0)), coverage);
}
