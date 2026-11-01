#version 330
#extension GL_ARB_separate_shader_objects : require

// Chams «Flat» look for the ElytraPredict prediction model: the skin is used
// only as an alpha-cutout silhouette (same 0.1 cutout as the chams mask),
// the fill is one flat tint drawn additively.
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

void main() {
    float coverage = texture(Sampler0, texCoord0).a * vertexColor.a;
    if (coverage < 0.1) discard;
    fragColor = vec4(vertexColor.rgb, coverage) * ColorModulator;
}
