#version 330
#extension GL_ARB_separate_shader_objects : require

// Emissive silhouette for the ElytraPredict prediction model. The skin is used
// only for its cutout coverage: dark pixels must still emit light instead of
// disappearing from the additive pass.
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
    vec4 skin = texture(Sampler0, texCoord0);
    float coverage = skin.a * vertexColor.a;
    if (coverage <= 0.001) {
        discard;
    }

    float skinLight = dot(skin.rgb, vec3(0.2126, 0.7152, 0.0722));
    float detail = mix(0.82, 1.0, skinLight);
    vec3 hotTint = mix(vertexColor.rgb, vec3(1.0), 0.24);

    fragColor = vec4(hotTint * detail, coverage) * ColorModulator;
}
