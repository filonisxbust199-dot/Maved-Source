#version 330

uniform sampler2D Sampler0;

layout(std140) uniform HandsBlur {
    vec4 Direction;
    vec4 Weights;
    vec4 Offsets;
};

in vec2 TexCoord;
out vec4 fragColor;

#ifdef HANDS_MASK
#define BLUR_VALUE float
#define SAMPLE_BLUR(uv) texture(Sampler0, uv).r
#else
#define BLUR_VALUE vec4
#define SAMPLE_BLUR(uv) texture(Sampler0, uv)
#endif

void main() {
    BLUR_VALUE result = SAMPLE_BLUR(TexCoord) * Weights.x;
    vec2 first = Direction.xy * Offsets.x;
    vec2 second = Direction.xy * Offsets.y;
    vec2 third = Direction.xy * Offsets.z;
    result += (SAMPLE_BLUR(TexCoord + first) + SAMPLE_BLUR(TexCoord - first)) * Weights.y;
    result += (SAMPLE_BLUR(TexCoord + second) + SAMPLE_BLUR(TexCoord - second)) * Weights.z;
    result += (SAMPLE_BLUR(TexCoord + third) + SAMPLE_BLUR(TexCoord - third)) * Weights.w;
    fragColor = vec4(result);
}
