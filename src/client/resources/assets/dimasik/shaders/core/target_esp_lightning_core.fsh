#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(location = 0) in vec2 TexCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    float mask = texture(InSampler, TexCoord).a;
    float alpha = smoothstep(0.025, 0.28, mask);
    if (alpha <= 0.002) discard;
    fragColor = vec4(1.0, 1.0, 1.0, alpha);
}
