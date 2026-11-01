#version 330
#extension GL_ARB_separate_shader_objects : require

// WorldTweaks grade (Zenith world_grade): the finished frame is re-saturated and tinted by the weather; the sky is
// left as it is (depth is reversed: the sky is 0).
layout(std140) uniform WorldGrade {
    vec4 Grade;     // x: saturation
    vec4 Tint;      // rgb: tint, a: brightness
};

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;

layout(location = 0) in vec2 TexCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec4 source = texture(SceneSampler, TexCoord);
    if (texture(DepthSampler, TexCoord).r <= 0.000001) {
        fragColor = source;
        return;
    }
    float amount = max(Grade.x, 0.0);
    float luma = dot(source.rgb, vec3(0.299, 0.587, 0.114));
    float boost = max(amount - 1.0, 0.0);
    vec3 color = clamp(mix(vec3(luma), source.rgb, amount) * (1.0 + boost * 0.35), 0.0, 1.0);
    fragColor = vec4(clamp(color * Tint.rgb * Tint.a, 0.0, 1.0), source.a);
}
