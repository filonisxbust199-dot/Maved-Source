#version 330
#extension GL_ARB_separate_shader_objects : require

// Plasma: summed sine waves folded into drifting bands of the two menu colours.
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
    vec2 u = (screen() - vec2(ColorModulator.y, 1.0) * 0.5) * 4.0;
    float v = sin(u.x * 1.5 + t * 0.6) + sin(u.y * 1.7 - t * 0.5) + sin((u.x + u.y) * 1.2 + t * 0.4)
            + sin(length(u + 1.5 * vec2(sin(t * 0.3), cos(t * 0.4))) * 2.5 - t * 0.7);
    v *= 0.25;
    // Bands of the field cycle between the two colours; the field's height sets how bright they are.
    float band = 0.5 + 0.5 * sin(v * 6.2831 + t * 0.3);
    vec3 c = mix(D, mix(B, A, band), 0.2 + 0.45 * smoothstep(-0.6, 1.0, v));
    c += A * pow(band, 8.0) * 0.12;
    fragColor = vec4(finish(c), ModelOffset.x);
}
