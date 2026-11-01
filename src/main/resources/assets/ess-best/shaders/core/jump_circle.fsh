#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 local;
layout(location = 2) flat in vec4 params;
layout(location = 3) flat in int kind;

layout(location = 0) out vec4 fragColor;

const float PI = 3.14159265;

// Radius of the figure's outline in the direction a, relative to a circle.
float outline(int shape, float a) {
    if (shape == 2) return 0.72 + 0.28 * pow(0.5 + 0.5 * cos(5.0 * a), 2.0);       // star
    if (shape == 3) return cos(PI / 6.0) / cos(mod(a, PI / 3.0) - PI / 6.0) * 0.92; // hexagon
    if (shape == 4) return 0.8 + 0.2 * cos(6.0 * a);                                 // flower
    return 1.0;
}

// Signed distance to a heart of size s, point turned so the tip points away from the player's facing (-y).
float heart(vec2 p, float s) {
    p /= s;
    p.y = -p.y + 0.35;
    p.x = abs(p.x);
    if (p.y + p.x > 1.0) return (sqrt(dot(p - vec2(0.25, 0.75), p - vec2(0.25, 0.75))) - sqrt(2.0) / 4.0) * s;
    vec2 q = p - 0.5 * max(p.x + p.y, 0.0);
    return sqrt(min(dot(p - vec2(0.0, 1.0), p - vec2(0.0, 1.0)), dot(q, q))) * sign(p.x - p.y) * s;
}

// A glowing figure spreading over the ground: a thin bright line with a soft glow, bent by a wobble, fading as
// it grows. Waves draw three lines one after another; a landing adds a soft flash inside.
void main() {
    float t = params.x, wobble = params.y, seed = params.z, clock = params.w;
    int shape = kind & 15;
    bool landing = (kind & 16) != 0;
    float glow = vertexColor.a;
    float r = length(local);
    if (r > 1.0) discard;
    float a = atan(local.y, local.x);

    float grow = 1.0 - pow(1.0 - t, 2.6);
    float time = clock * 6.2831853;
    float bend = wobble * 0.07 * (sin(a * 3.0 + seed + time) * 0.55 + sin(a * 7.0 - time * 1.7 + seed * 0.3) * 0.3
            + sin(r * 14.0 - time * 2.3) * 0.25);
    float size = 0.12 + 0.78 * grow;

    float d;
    if (shape == 5) d = abs(heart(local + bend * normalize(local + 1e-4), size * 0.95));
    else if (shape == 1) {
        d = 1e3;
        for (int i = 0; i < 3; i++) {
            float ring = size - float(i) * 0.2;
            if (ring > 0.05) d = min(d, abs(r + bend - ring) + float(i) * 0.012);
        }
    } else d = abs(r + bend - size * outline(shape, a));

    float width = 0.022 + 0.018 * (1.0 - t);
    float core = 1.0 - smoothstep(width * 0.45, width, d);
    float halo = exp(-d * (9.0 + 14.0 * (1.0 - glow))) * glow;
    float fade = pow(1.0 - t, 1.5) * smoothstep(0.0, 0.06, t);

    vec3 tint = vertexColor.rgb;
    vec3 color = tint * (core * 1.4 + halo * 0.9) + vec3(1.0) * core * 0.55;
    if (landing) color += tint * (1.0 - smoothstep(0.0, size, r)) * 0.22 * (1.0 - t);
    // The grid ends in a square; the figure dims near its edge so the square never shows.
    color *= fade * (1.0 - smoothstep(0.88, 1.0, r));
    if (max(color.r, max(color.g, color.b)) < 0.003) discard;
    fragColor = vec4(color, 1.0);
}
