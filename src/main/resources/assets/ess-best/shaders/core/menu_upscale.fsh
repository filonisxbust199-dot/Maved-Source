#version 330
#extension GL_ARB_separate_shader_objects : require

// Stretches the reduced-resolution menu scene over the screen: Catmull-Rom bicubic filtering
// (9 bilinear taps) instead of plain bilinear, then contrast-adaptive sharpening whose strength
// grows with the upscale factor. vertexColor.a carries that strength (0 at native resolution).

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

vec3 catmullRom(vec2 uv, vec2 size) {
    vec2 pos = uv * size;
    vec2 centre = floor(pos - 0.5) + 0.5;
    vec2 f = pos - centre;
    vec2 w0 = f * (-0.5 + f * (1.0 - 0.5 * f));
    vec2 w1 = 1.0 + f * f * (-2.5 + 1.5 * f);
    vec2 w2 = f * (0.5 + f * (2.0 - 1.5 * f));
    vec2 w3 = f * f * (-0.5 + 0.5 * f);
    // The middle two taps merge into one bilinear fetch.
    vec2 w12 = w1 + w2;
    vec2 offset12 = w2 / w12;
    vec2 t0 = (centre - 1.0) / size;
    vec2 t3 = (centre + 2.0) / size;
    vec2 t12 = (centre + offset12) / size;
    vec3 c = vec3(0.0);
    c += texture(Sampler0, vec2(t0.x, t0.y)).rgb * w0.x * w0.y;
    c += texture(Sampler0, vec2(t12.x, t0.y)).rgb * w12.x * w0.y;
    c += texture(Sampler0, vec2(t3.x, t0.y)).rgb * w3.x * w0.y;
    c += texture(Sampler0, vec2(t0.x, t12.y)).rgb * w0.x * w12.y;
    c += texture(Sampler0, vec2(t12.x, t12.y)).rgb * w12.x * w12.y;
    c += texture(Sampler0, vec2(t3.x, t12.y)).rgb * w3.x * w12.y;
    c += texture(Sampler0, vec2(t0.x, t3.y)).rgb * w0.x * w3.y;
    c += texture(Sampler0, vec2(t12.x, t3.y)).rgb * w12.x * w3.y;
    c += texture(Sampler0, vec2(t3.x, t3.y)).rgb * w3.x * w3.y;
    return max(c, 0.0);
}

void main() {
    vec2 size = vec2(textureSize(Sampler0, 0));
    vec2 texel = 1.0 / size;
    vec3 c = catmullRom(texCoord0, size);

    // AMD CAS-style sharpening: neighbours decide how much detail can be pushed without halos.
    vec3 n = texture(Sampler0, texCoord0 + vec2(0.0, -texel.y)).rgb;
    vec3 s = texture(Sampler0, texCoord0 + vec2(0.0, texel.y)).rgb;
    vec3 e = texture(Sampler0, texCoord0 + vec2(texel.x, 0.0)).rgb;
    vec3 w = texture(Sampler0, texCoord0 + vec2(-texel.x, 0.0)).rgb;
    vec3 lo = min(c, min(min(n, s), min(e, w)));
    vec3 hi = max(c, max(max(n, s), max(e, w)));
    vec3 amount = sqrt(clamp(min(lo, 1.0 - hi) / max(hi, 1e-4), 0.0, 1.0));
    float strength = vertexColor.a * 0.2;
    vec3 weight = -amount * strength;
    vec3 sharp = (c + (n + s + e + w) * weight) / (1.0 + 4.0 * weight);
    fragColor = vec4(clamp(sharp, 0.0, 1.0), 1.0) * vec4(vertexColor.rgb, 1.0) * ColorModulator;
}
