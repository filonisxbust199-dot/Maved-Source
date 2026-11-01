#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 point = texCoord * 2.0 - 1.0;
    float radius = length(point);
    float core = 1.0 - smoothstep(0.0, 0.018, abs(radius - 0.75));
    float glow = exp(-abs(radius - 0.75) * 38.0) * 0.42;
    float intensity = clamp(core + glow, 0.0, 1.0) * vertexColor.a;
    if (radius < 0.61 || radius > 0.89 || intensity <= 0.002) discard;
    vec3 color = mix(vertexColor.rgb, vec3(1.0), core * 0.7);
    fragColor = vec4(color * intensity, intensity);
}
