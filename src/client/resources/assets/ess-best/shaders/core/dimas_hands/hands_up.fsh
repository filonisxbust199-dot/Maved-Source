#version 330

layout(std140) uniform FogBlurUp {
    vec2 Offset;
    vec2 HalfPixel;
    vec2 Size;
    vec3 Color;
};

uniform sampler2D Sampler0;

in vec2 TexCoord;

out vec4 fragColor;

void main() {
    vec2 sourceTexel = 1.0 / max(vec2(textureSize(Sampler0, 0)), vec2(1.0));
    vec2 low = sourceTexel * 0.5;
    vec2 high = vec2(1.0) - low;
    vec2 u = clamp(TexCoord, low, high);
    vec2 h = HalfPixel * Offset;
    vec4 s = texture(Sampler0, clamp(u + vec2(-h.x * 2.0, 0.0), low, high))
        + texture(Sampler0, clamp(u + vec2(-h.x, h.y), low, high)) * 2.0
        + texture(Sampler0, clamp(u + vec2(0.0, h.y * 2.0), low, high))
        + texture(Sampler0, clamp(u + vec2(h.x, h.y), low, high)) * 2.0
        + texture(Sampler0, clamp(u + vec2(h.x * 2.0, 0.0), low, high))
        + texture(Sampler0, clamp(u + vec2(h.x, -h.y), low, high)) * 2.0
        + texture(Sampler0, clamp(u + vec2(0.0, -h.y * 2.0), low, high))
        + texture(Sampler0, clamp(u + vec2(-h.x, -h.y), low, high)) * 2.0;
    vec4 r = s / 12.0;
    fragColor = vec4(r.rgb * Color, r.a);
}
