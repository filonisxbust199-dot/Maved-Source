#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

const float FUSED_ADDITIVE_SCALE = 2.0;

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    float coverage = texel.a * vertexColor.a * ColorModulator.a;
    if (coverage == 0.0) {
        discard;
    }
    vec3 tint = texel.rgb * vertexColor.rgb * ColorModulator.rgb;
    fragColor = vec4(tint * coverage * FUSED_ADDITIVE_SCALE, coverage);
}
