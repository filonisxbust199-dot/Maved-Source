#version 330
#extension GL_ARB_separate_shader_objects : require

// Hexes: a honeycomb with a pulse wave rolling out from the centre and lit edges.
//
// Drawn by MenuScene with core/screenquad. Per-frame values come in DynamicTransforms:
// ColorModulator = (time, aspect, mouse x, mouse y), ModelOffset = (output alpha, light theme 0..1, -).

#include <minecraft:dynamictransforms.glsl>

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

const vec3 A = vec3(0.39, 0.78, 1.0);
const vec3 B = vec3(0.62, 0.36, 1.0);
const vec3 D = vec3(0.03, 0.04, 0.07);

float h(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}
vec2 h2(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}
float n(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(h(i), h(i + vec2(1, 0)), f.x), mix(h(i + vec2(0, 1)), h(i + 1.0), f.x), f.y);
}
// Screen position with the height as the unit (0..aspect, 0..1), y up.
vec2 screen() { return texCoord * vec2(ColorModulator.y, 1.0); }
// Ordered noise against banding in the dark gradients.
vec3 finish(vec3 c) {
    vec2 q = texCoord - 0.5;
    c *= 1.0 - dot(q, q) * 0.6;
    // Light theme: the glow above the dark base becomes a tint on white (its complement taken away).
    vec3 f = max(c - D, 0.0);
    float k = max(f.r, max(f.g, f.b));
    vec3 light = vec3(0.95, 0.96, 0.98) - (1.0 - f / max(k, 1e-3)) * min(k, 1.0) * 0.55;
    c = mix(c, light, ModelOffset.y);
    c += (h(gl_FragCoord.xy) - 0.5) / 255.0;
    return c;
}

void main() {
    float t = ColorModulator.x;
    vec2 u = screen() * 9.0;
    vec2 s = vec2(1.0, 1.7320508), a = mod(u, s) - s * 0.5, b = mod(u - s * 0.5, s) - s * 0.5;
    vec2 g = dot(a, a) < dot(b, b) ? a : b;
    vec2 id = u - g;
    float d = max(abs(g.x) * 0.5 + abs(g.y) * 0.866, abs(g.x));
    float w = sin(length(id - vec2(ColorModulator.y, 1.0) * 4.5) * 0.8 - t * 1.5) * 0.5 + 0.5;
    vec3 c = D + mix(B, A, h(id)) * pow(w, 4.0) * 0.35;
    float edge = fwidth(d);
    c += A * smoothstep(0.5 - 2.0 * edge, 0.5, d) * 0.25 * (0.3 + w);
    fragColor = vec4(finish(c), ModelOffset.x);
}
