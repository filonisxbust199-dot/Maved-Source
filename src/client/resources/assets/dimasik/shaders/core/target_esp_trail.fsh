#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    float intensity = vertexColor.a;
    if (intensity <= 0.001) discard;
    vec3 color = mix(vertexColor.rgb, vec3(1.0), intensity * 0.28);
    fragColor = vec4(color * intensity, intensity);
}
