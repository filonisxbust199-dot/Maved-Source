#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Sampler0;

layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec2 offset = 0.5 / vec2(textureSize(Sampler0, 0));
    fragColor = (texture(Sampler0, TexCoord - offset)
            + texture(Sampler0, TexCoord + offset)
            + texture(Sampler0, TexCoord + vec2(offset.x, -offset.y))
            + texture(Sampler0, TexCoord + vec2(-offset.x, offset.y))) * 0.25;
}
