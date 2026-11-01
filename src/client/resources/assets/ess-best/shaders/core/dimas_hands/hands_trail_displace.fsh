#version 330

uniform sampler2D TrailSampler;

layout(std140) uniform TrailDisplace {
    vec2 Offset;
    vec2 TextureSize;
    vec4 Params;
    float Flicker;
};

in vec2 TexCoord;
out vec4 fragColor;

float wobble(float y, float time) {
    return sin(y * 9.0 + time * 4.3) * 0.6
         + sin(y * 17.0 - time * 2.1) * 0.3
         + sin(y * 4.0 + time * 1.3) * 0.4
         + sin(y * 28.0 + time * 5.7) * 0.15;
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    vec2 smoothValue = f * f * (3.0 - 2.0 * f);
    return mix(a, b, smoothValue.x)
         + (c - a) * smoothValue.y * (1.0 - smoothValue.x)
         + (d - b) * smoothValue.x * smoothValue.y;
}

vec4 blurSample(vec2 uv) {
    vec2 pixel = 1.0 / max(TextureSize, vec2(1.0));
    vec4 color = texture(TrailSampler, uv) * 0.36;
    color += texture(TrailSampler, uv + vec2(pixel.x, 0.0)) * 0.16;
    color += texture(TrailSampler, uv - vec2(pixel.x, 0.0)) * 0.16;
    color += texture(TrailSampler, uv + vec2(0.0, pixel.y)) * 0.16;
    color += texture(TrailSampler, uv - vec2(0.0, pixel.y)) * 0.16;
    return color;
}

void main() {
    float turbulentX = (wobble(TexCoord.y, Params.y) - wobble(TexCoord.y, Params.y - Params.z)) * Params.w;
    float verticalFactor = 0.7 + TexCoord.y * 0.8;
    vec2 displacement = vec2(Offset.x + turbulentX, Offset.y * verticalFactor);
    vec4 color = blurSample(TexCoord - displacement);

    float shimmer = valueNoise(vec2(TexCoord.x * 22.0, TexCoord.y * 22.0 - Params.y * 5.5));
    float flicker = 1.0 - Flicker * 0.55 + Flicker * shimmer * 1.10;
    float oldAlpha = color.a;
    float frameScale = clamp(Params.z * 60.0, 0.0, 6.0);
    float frameRetention = clamp(0.985 * flicker, 0.001, 0.9999);
    float retention = pow(frameRetention, frameScale);
    float fadeLoss = Params.x * (1.0 - retention) / (1.0 - frameRetention);
    float newAlpha = max(0.0, oldAlpha * retention - fadeLoss);
    float scale = oldAlpha > 0.001 ? newAlpha / oldAlpha : 0.0;
    fragColor = vec4(color.rgb * scale, newAlpha);
}
