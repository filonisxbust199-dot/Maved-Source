#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(InSampler, TexCoord);
    if (color.a <= 0.001) discard;
    fragColor = color;
}
