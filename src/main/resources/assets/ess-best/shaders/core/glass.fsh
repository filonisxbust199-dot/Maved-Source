#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
uniform sampler2D Sampler0;

layout(location = 0) in vec2 localPos;
layout(location = 1) in vec2 halfSize;
layout(location = 2) in vec3 params;
layout(location = 3) in vec4 tint;
layout(location = 4) in vec2 screenUv;

layout(location = 0) out vec4 fragColor;

// params.x = corner radius, params.y = blur radius in pixels of the frame, params.z = opacity.
// Corners are L4 superellipses (x^4 + y^4 = r^4), the same squircle UiGeometry tessellates,
// so blurred and flat surfaces with equal radii share one outline.
// A negative radius (params.x) asks for circular corners instead of the L4 squircle.
bool circular = false;

const float DPI = 6.28318530718;
const float STEP = DPI / 16.0;

float roundedBox(vec2 p, vec2 b, float r) {
    vec2 inner = abs(p) - b + r;
    vec2 q = max(inner, 0.0);
    vec2 q2 = q * q;
    float corner = circular ? length(q) : sqrt(sqrt(q2.x * q2.x + q2.y * q2.y));
    return corner + min(max(inner.x, inner.y), 0.0) - r;
}

// The frame blurred right here, 16 directions of 5 rings around the pixel, under the surface's tint.
void main() {
    vec2 p = localPos;
    circular = params.x < 0.0;
    float radius = min(abs(params.x), min(halfSize.x, halfSize.y));
    float pixel = max(length(vec2(dFdx(p).x, dFdy(p).y)) * 0.70710678, 1e-4);

    float dist = roundedBox(p, halfSize, radius);
    float coverage = clamp(0.5 - dist / pixel, 0.0, 1.0);
    if (coverage <= 0.0) discard;

    vec2 multiplier = params.y / vec2(textureSize(Sampler0, 0));
    vec3 average = texture(Sampler0, screenUv).rgb;
    for (float d = 0.0; d < DPI; d += STEP) {
        for (float i = 0.2; i <= 1.0; i += 0.2) {
            average += texture(Sampler0, screenUv + vec2(cos(d), sin(d)) * multiplier * i).rgb;
        }
    }
    average /= 80.0;

    fragColor = vec4(mix(average, tint.rgb, tint.a), coverage * params.z) * ColorModulator;
}
