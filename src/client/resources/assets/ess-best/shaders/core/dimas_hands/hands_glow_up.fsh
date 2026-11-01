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
    const float WIDE = 1.508689;
    const float NARROW = 0.624919;
    float result = (
          texture(Sampler0, clamp(u + vec2( h.x * WIDE,    h.y * NARROW), low, high)).r
        + texture(Sampler0, clamp(u + vec2(-h.x * NARROW,  h.y * WIDE), low, high)).r
        + texture(Sampler0, clamp(u + vec2(-h.x * WIDE,   -h.y * NARROW), low, high)).r
        + texture(Sampler0, clamp(u + vec2( h.x * NARROW, -h.y * WIDE), low, high)).r
    ) * 0.25;
    fragColor = vec4(result);
}
