#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 local;
layout(location = 2) flat in int mode;
layout(location = 3) flat in float clock;

layout(location = 0) out vec4 fragColor;

const float TAU = 6.2831853;

// Predictions, drawn additively. The path is a ribbon facing the camera: a white-hot core, a hard glow around it
// and bright pulses running towards the landing point. The landing point is a disk lying on the surface: a bright
// ring, four arcs turning around it, a beating centre and a halo; a splash potion shows only its radius.
void main() {
    vec3 tint = vertexColor.rgb;
    float strength = vertexColor.a;
    float time = clock * TAU;
    vec3 color;

    if (mode == 0) {
        float along = local.x, across = abs(local.y);
        float core = 1.0 - smoothstep(0.12, 0.26, across);
        float glow = exp(-across * 3.2) * 0.75 + exp(-across * 9.0) * 0.6;
        float pulse = pow(0.5 + 0.5 * sin((along * 0.9 - time * 1.6)), 14.0);
        float start = smoothstep(0.3, 1.5, along);
        color = tint * (glow + core * 0.9 + pulse * (0.4 + core)) + vec3(1.0) * core * (0.75 + pulse * 0.6);
        color *= start;
    } else {
        float r = length(local);
        if (r > 1.0) discard;
        float a = atan(local.y, local.x);
        if (mode == 3) {
            float ring = 1.0 - smoothstep(0.012, 0.03, abs(r - 0.94));
            float glow = exp(-abs(r - 0.94) * 22.0) * 0.7;
            float dash = step(0.5, fract(a / TAU * 24.0 + clock));
            color = tint * (glow + ring * (0.6 + 0.6 * dash)) + vec3(1.0) * ring * dash * 0.5 + tint * 0.08 * (1.0 - r);
        } else {
            float beat = 0.5 + 0.5 * sin(time * 2.0);
            float ring = 1.0 - smoothstep(0.03, 0.06, abs(r - 0.62));
            float ringGlow = exp(-abs(r - 0.62) * 10.0);
            // Four arcs around the ring, turning.
            float arcs = step(0.62, fract((a + time * 0.5) / TAU * 4.0)) * (1.0 - smoothstep(0.025, 0.05, abs(r - 0.86)));
            float dot = 1.0 - smoothstep(0.08 + 0.04 * beat, 0.16 + 0.04 * beat, r);
            float halo = exp(-r * 3.0) * 0.55;
            float fill = (1.0 - smoothstep(0.55, 0.62, r)) * 0.12;
            color = tint * (ringGlow * 0.9 + ring + arcs * 0.9 + halo + fill + dot * 0.6) + vec3(1.0) * (ring * 0.7 + dot * 0.9 + arcs * 0.35);
            color *= 1.0 - smoothstep(0.9, 1.0, r);
        }
    }
    color *= strength;
    if (max(color.r, max(color.g, color.b)) < 0.003) discard;
    fragColor = vec4(color, 1.0);
}
