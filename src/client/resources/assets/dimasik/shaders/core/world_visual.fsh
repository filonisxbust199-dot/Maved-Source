#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform VisualConfig {
    vec4 Tint;
    vec4 Misc;
};

uniform sampler2D InSampler;
layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 fragColor;

const vec3 LUMA = vec3(0.299, 0.587, 0.114);

void main() {
    vec4 scene = texture(InSampler, TexCoord);
    vec3 col = scene.rgb;

    float luma = dot(col, LUMA);
    col = mix(vec3(luma), col, Misc.x);

    float toned = dot(col, LUMA);
    float tintLuma = dot(Tint.rgb, LUMA);
    vec3 recolor = (tintLuma > 0.001) ? Tint.rgb * (toned / tintLuma) : vec3(toned);
    col = mix(col, recolor, Tint.a);

    fragColor = vec4(col, scene.a);
}
