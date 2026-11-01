#version 330
#extension GL_ARB_separate_shader_objects : require

// Broken tiles: slowly shifting Voronoi shards with dark cracks and a glint along their edges.
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
    vec2 u = screen() * 5.0;
    vec2 i = floor(u), f = fract(u);
    float m = 9.0, m2 = 9.0;
    vec2 id = i;
    for(int y = -1; y <= 1; y++) for(int x = -1; x <= 1; x++) {
        vec2 g = vec2(x, y), o = h2(i + g);
        o = 0.5 + 0.4 * sin(t * 0.3 + 6.2831 * o);
        float d = length(g + o - f);
        if(d < m) { m2 = m; m = d; id = i + g; } else if(d < m2) m2 = d;
    }
    float e = m2 - m;
    float k = h(id);
    float tilt = 0.5 + 0.5 * sin(t * 0.5 + k * 6.2831);
    vec3 c = mix(D * 1.6, mix(A, B, k) * 0.35, 0.25 + 0.5 * k * tilt);
    c += A * smoothstep(0.06, 0.02, e) * 0.45 * tilt;
    c *= smoothstep(0.0, 0.035, e);
    fragColor = vec4(finish(c), ModelOffset.x);
}
