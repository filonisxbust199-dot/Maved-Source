#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D HandSampler;
uniform sampler2D HandDepthSampler;

layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    float alpha = texture(HandSampler, TexCoord).a;
    float depth = texture(HandDepthSampler, TexCoord).r;
    float coverage = max(smoothstep(0.001, 0.08, alpha), step(0.000001, depth));
    fragColor = vec4(coverage);
}
