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
layout(location = 1) in vec2 localPos;
layout(location = 2) in vec2 params;
layout(location = 3) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

// Texture clipped to the same L4 squircle used by UI surfaces (player heads); a negative corner radius
// asks for circular corners instead.
void main() {
    float radius = abs(params.y);
    vec2 inner = abs(localPos) - vec2(params.x) + radius;
    vec2 q = max(inner, 0.0);
    vec2 q2 = q * q;
    float corner = params.y < 0.0 ? length(q) : sqrt(sqrt(q2.x * q2.x + q2.y * q2.y));
    float d = corner + min(max(inner.x, inner.y), 0.0) - radius;
    float pixel = max(length(vec2(dFdx(d), dFdy(d))), 1e-4);
    float coverage = clamp(0.5 - d / pixel, 0.0, 1.0);
    vec4 texel = texture(Sampler0, texCoord0);
    fragColor = vec4(texel.rgb * vertexColor.rgb, texel.a * vertexColor.a * coverage) * ColorModulator;
    if (fragColor.a <= 0.0) discard;
}
