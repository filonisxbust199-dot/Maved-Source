#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 centered = texCoord * 2.0 - 1.0;
    float distanceFromCenter = length(centered);
    if (distanceFromCenter >= 1.0) discard;

    float core = 1.0 - smoothstep(0.0, 1.0, distanceFromCenter);
    float softGlow = core * core * (3.0 - 2.0 * core);
    float alpha = softGlow * vertexColor.a;
    if (alpha <= 0.002) discard;

    fragColor = vec4(alpha, alpha, alpha, alpha);
}
