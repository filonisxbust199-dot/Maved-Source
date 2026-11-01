#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D TrailSampler;

layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = texture(TrailSampler, TexCoord);
}
