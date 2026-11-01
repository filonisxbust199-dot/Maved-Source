#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Sampler0;

layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 fragColor;

void main() {
#ifdef HANDS_MASK
    fragColor = vec4(texture(Sampler0, TexCoord).r);
#else
    fragColor = texture(Sampler0, TexCoord);
#endif
}
