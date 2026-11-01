#version 330

layout(std140) uniform FogBlurDown {
    vec2 Offset;
    vec2 HalfPixel;
    int Extract;
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
    float result = (texture(Sampler0, u).r * 4.0
        + texture(Sampler0, clamp(u - h, low, high)).r
        + texture(Sampler0, clamp(u + h, low, high)).r
        + texture(Sampler0, clamp(u + vec2(h.x, -h.y), low, high)).r
        + texture(Sampler0, clamp(u - vec2(h.x, -h.y), low, high)).r) / 8.0;
    fragColor = vec4(result);
}
