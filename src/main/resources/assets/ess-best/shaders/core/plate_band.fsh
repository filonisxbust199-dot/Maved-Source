#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

layout(location = 0) in vec2 localPos;
layout(location = 1) in vec2 halfSize;
layout(location = 2) in vec3 params;
layout(location = 3) in vec4 tint;
layout(location = 4) in vec2 screenUv;

layout(location = 0) out vec4 fragColor;

// params.x = corner radius, params.y = where the band ends, from the left or top edge, params.z = 1 for top.
// The shape is the one core/glass draws, so the band and its plate share their edge to the pixel.
bool circular = false;

float roundedBox(vec2 p, vec2 b, float r) {
    vec2 inner = abs(p) - b + r;
    vec2 q = max(inner, 0.0);
    vec2 q2 = q * q;
    float corner = circular ? length(q) : sqrt(sqrt(q2.x * q2.x + q2.y * q2.y));
    return corner + min(max(inner.x, inner.y), 0.0) - r;
}

void main() {
    vec2 p = localPos;
    circular = params.x < 0.0;
    float radius = min(abs(params.x), min(halfSize.x, halfSize.y));
    float pixel = max(length(vec2(dFdx(p).x, dFdy(p).y)) * 0.70710678, 1e-4);

    float coverage = clamp(0.5 - roundedBox(p, halfSize, radius) / pixel, 0.0, 1.0);
    float along = params.z > 0.5 ? p.y + halfSize.y : p.x + halfSize.x;
    coverage *= clamp(0.5 - (along - params.y) / pixel, 0.0, 1.0);
    if (coverage <= 0.0) discard;

    fragColor = vec4(tint.rgb, tint.a * coverage) * ColorModulator;
}
