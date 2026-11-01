#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 vertexColor;
layout(location = 0) out vec4 fragColor;

void main() {
    float distanceFromCenter = length(texCoord - vec2(0.5));
    float alpha = 1.0 - smoothstep(0.28, 0.5, distanceFromCenter);
    alpha *= vertexColor.a;
    if (alpha <= 0.001) discard;
    fragColor = vec4(vertexColor.rgb * alpha, alpha);
}
